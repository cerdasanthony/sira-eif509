package cr.ac.una.sira.data;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El mismo conflicto de la suite local, pero contra PostgreSQL real y sobre la
 * columna que crea la migracion V4: comprueba que el bloqueo optimista protege
 * la fila en produccion y no solo con el esquema que Hibernate genera en H2.
 *
 * Sin @Transactional: si toda la prueba corriera dentro de una transaccion, las
 * dos lecturas devolverian el mismo objeto y no habria conflicto que observar.
 */
@SpringBootTest(properties = {
        "sira.demo.enabled=false",
        "spring.data.mongodb.repositories.enabled=false",
        "management.health.mongo.enabled=false"
})
@Testcontainers(disabledWithoutDocker = true)
class BloqueoOptimistaIntegracionTest {

    private static final long RUTINA = 3L;
    private static final OffsetDateTime PRIMERA =
            OffsetDateTime.of(2026, 11, 3, 8, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime SEGUNDA =
            OffsetDateTime.of(2026, 11, 3, 9, 30, 0, 0, ZoneOffset.UTC);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sira").withUsername("sira").withPassword("sira");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired RutinaRepository rutinas;
    @Autowired JdbcTemplate jdbc;

    @Test
    void laMigracionV4CreaLaColumnaVersionYLaDejaEnCero() {
        Integer sinVersion = jdbc.queryForObject(
                "SELECT count(*) FROM rutina WHERE version IS NULL", Integer.class);
        assertThat(sinVersion).isZero();
        // La rutina 1 no la toca la otra prueba, asi que conserva el valor inicial.
        assertThat(rutinas.findById(1L).orElseThrow().getVersion()).isZero();
    }

    @Test
    void laSegundaDeDosCopiasDelMismoBorradorSeRechazaConUnFalloDeBloqueoOptimista() {
        long versionInicial = rutinas.findById(RUTINA).orElseThrow().getVersion();

        // Dos copias leidas por separado, como dos sesiones del profesional.
        Rutina copiaA = rutinas.findById(RUTINA).orElseThrow();
        Rutina copiaB = rutinas.findById(RUTINA).orElseThrow();
        assertThat(copiaA.getVersion()).isEqualTo(copiaB.getVersion());

        copiaA.publicar(PRIMERA);
        rutinas.saveAndFlush(copiaA);
        assertThat(rutinas.findById(RUTINA).orElseThrow().getVersion()).isEqualTo(versionInicial + 1);

        copiaB.publicar(SEGUNDA);
        assertThatThrownBy(() -> rutinas.saveAndFlush(copiaB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // Lo que guardo la primera copia sigue en pie: no hubo actualizacion perdida.
        assertThat(rutinas.findById(RUTINA).orElseThrow().getPublicadoEn().toInstant())
                .isEqualTo(PRIMERA.toInstant());
    }
}
