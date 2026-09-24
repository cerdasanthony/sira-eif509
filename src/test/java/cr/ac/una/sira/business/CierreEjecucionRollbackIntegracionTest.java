package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.CerrarEjecucionEntrada;
import cr.ac.una.sira.business.dto.ResultadoPasoEntrada;
import cr.ac.una.sira.data.EjecucionRepository;
import cr.ac.una.sira.data.ResultadoPaso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.data.mongodb.repositories.enabled=false",
        "management.health.mongo.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class CierreEjecucionRollbackIntegracionTest {

    private static final LocalDate FECHA_PRUEBA = LocalDate.of(2026, 8, 26);

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sira")
                    .withUsername("sira")
                    .withPassword("sira");

    @DynamicPropertySource
    static void configurarPostgreSql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CierreEjecucionService servicio;

    @Autowired
    private EjecucionRepository ejecucionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void instalarFalloControladoEnLaSegundaEscritura() {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fallo_intermedio_lab4()
                RETURNS trigger AS $$
                BEGIN
                    IF NEW.observacion_corta = 'FORZAR_FALLO_INTERMEDIO' THEN
                        RAISE EXCEPTION 'fallo intermedio controlado';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER tg_fallo_intermedio_lab4
                BEFORE INSERT ON registro_paso
                FOR EACH ROW EXECUTE FUNCTION fallo_intermedio_lab4()
                """);
    }

    @AfterEach
    void retirarFalloControlado() {
        jdbcTemplate.execute(
                "DROP TRIGGER IF EXISTS tg_fallo_intermedio_lab4 ON registro_paso");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fallo_intermedio_lab4()");
    }

    @Test
    void unFalloAlInsertarRegistrosRevierteTambienLaEjecucion() {
        CerrarEjecucionEntrada entrada = new CerrarEjecucionEntrada(
                1L,
                3L,
                FECHA_PRUEBA,
                LocalTime.of(6, 30),
                LocalTime.of(6, 50),
                List.of(
                        new ResultadoPasoEntrada(1L, ResultadoPaso.LOGRADO, null),
                        new ResultadoPasoEntrada(
                                2L, ResultadoPaso.CON_APOYO,
                                "FORZAR_FALLO_INTERMEDIO"),
                        new ResultadoPasoEntrada(3L, ResultadoPaso.LOGRADO, null),
                        new ResultadoPasoEntrada(4L, ResultadoPaso.NO_LOGRADO, null)));

        assertThat(ejecucionRepository.existsByRutinaIdAndFecha(1L, FECHA_PRUEBA))
                .isFalse();

        assertThatThrownBy(() -> servicio.cerrar(entrada))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("fallo intermedio controlado");

        assertThat(ejecucionRepository.existsByRutinaIdAndFecha(1L, FECHA_PRUEBA))
                .as("la primera escritura debe desaparecer por rollback")
                .isFalse();
        Integer registros = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM registro_paso rp
                JOIN ejecucion e ON e.id = rp.ejecucion_id
                WHERE e.rutina_id = 1 AND e.fecha = DATE '2026-08-26'
                """, Integer.class);
        assertThat(registros).isZero();
    }
}
