package cr.ac.una.sira.data;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class PersistenciaIntegracionTest {

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
    private EntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private RutinaRepository rutinaRepository;

    @Autowired
    private PasoRutinaRepository pasoRutinaRepository;

    @Autowired
    private EjecucionRepository ejecucionRepository;

    @Autowired
    private RegistroPasoRepository registroPasoRepository;

    @Test
    void flywayReconstruyeElEsquemaYCargaLosDatosDeEjemplo() {
        assertThat(usuarioRepository.count()).isEqualTo(4);
        assertThat(participanteRepository.count()).isEqualTo(2);
        assertThat(rutinaRepository.count()).isEqualTo(3);
        assertThat(pasoRutinaRepository.count()).isEqualTo(10);
        assertThat(ejecucionRepository.count()).isEqualTo(4);
        assertThat(registroPasoRepository.count()).isEqualTo(14);
    }

    @Test
    void mapeoJpaMantieneLasRelacionesEnLazy() {
        entityManager.clear();

        Participante participante = participanteRepository.findById(1L).orElseThrow();

        assertThat(Hibernate.isInitialized(participante.getProfesional())).isFalse();
        assertThat(Hibernate.isInitialized(participante.getEncargado())).isFalse();
        assertThat(Hibernate.isInitialized(participante.getRutinas())).isFalse();
    }

    @Test
    void repositorioGenericoProveeCrudATodasLasEntidadesSql() {
        Usuario nuevo = new Usuario(
                "Ana Prueba", "ana.prueba@sira.local", RolUsuario.PROFESIONAL, true);

        Usuario guardado = usuarioRepository.saveAndFlush(nuevo);

        assertThat(guardado.getId()).isNotNull();
        assertThat(usuarioRepository.findByCorreoIgnoreCase("ANA.PRUEBA@SIRA.LOCAL"))
                .contains(guardado);
    }

    @Test
    void jpqlBuscaLasRutinasPublicadasDeUnParticipante() {
        List<Rutina> encontradas = rutinaRepository.buscarPublicadasDelParticipante(1L);

        assertThat(encontradas)
                .extracting(Rutina::getNombre)
                .containsExactly(
                        "Rutina de higiene de la manana",
                        "Preparar mochila escolar");
    }

    @Test
    void jpqlBuscaEjecucionesCerradasDeUnaRutinaEnUnRango() {
        List<Ejecucion> encontradas = ejecucionRepository.buscarCerradasDeRutinaEnRango(
                1L, LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 25));

        assertThat(encontradas)
                .extracting(Ejecucion::getAdherencia)
                .containsExactly(new BigDecimal("87.50"), new BigDecimal("62.50"));
    }

    @Test
    void criteriaFiltraRutinasPorParticipanteEstadoYVigencia() {
        List<Rutina> encontradas = rutinaRepository.findAll(
                RutinaSpecifications.conFiltros(
                        2L, EstadoRutina.PUBLICADA, LocalDate.of(2026, 8, 24)));

        assertThat(encontradas)
                .extracting(Rutina::getNombre)
                .containsExactly("Rutina de llegada a casa");
    }

    @Test
    void criteriaCombinaRangoEstadoYAdherenciaMinima() {
        List<Ejecucion> encontradas = ejecucionRepository.findAll(
                EjecucionSpecifications.conFiltros(
                        null,
                        EstadoEjecucion.CERRADA,
                        LocalDate.of(2026, 8, 24),
                        LocalDate.of(2026, 8, 24),
                        new BigDecimal("85.00")));

        assertThat(encontradas)
                .extracting(Ejecucion::getAdherencia)
                .containsExactlyInAnyOrder(new BigDecimal("87.50"), new BigDecimal("100.00"));
    }

    @Test
    void entityGraphCorrigeElNMasUnoAlCargarPasos() {
        Statistics estadisticas = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();

        entityManager.clear();
        estadisticas.clear();
        List<Rutina> sinFetch = rutinaRepository.buscarPublicadasSinPasos();
        sinFetch.forEach(rutina -> rutina.getPasos().size());
        long consultasAntes = estadisticas.getPrepareStatementCount();

        entityManager.clear();
        estadisticas.clear();
        List<Rutina> conFetch = rutinaRepository.buscarPublicadasConPasos();
        conFetch.forEach(rutina -> rutina.getPasos().size());
        long consultasDespues = estadisticas.getPrepareStatementCount();

        assertThat(consultasAntes).isEqualTo(4); // 1 rutina + 3 colecciones
        assertThat(consultasDespues).isEqualTo(1);
        assertThat(conFetch).allMatch(rutina -> Hibernate.isInitialized(rutina.getPasos()));
    }
}
