package cr.ac.una.sira.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * La rutina es la fila con contencion real del dominio: el formulario de edicion y la
 * publicacion escriben el mismo borrador, y el profesional puede tener la administracion
 * MVC abierta en una ventana y el portal en otra. Sin @Version la segunda escritura
 * pisa la primera sin que nadie lo note; con @Version la segunda se rechaza.
 *
 * Estas pruebas no corren dentro de una transaccion de prueba: cada unidad de trabajo
 * se confirma de verdad, que es la unica forma de provocar el conflicto.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.data.mongodb.repositories.enabled=false",
        "management.health.mongo.enabled=false",
        "sira.demo.enabled=false"
})
@Sql(scripts = {"/concurrencia-limpiar.sql", "/api-datos-locales.sql"},
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
class BloqueoOptimistaRutinaTest {

    private static final long RUTINA = 2L;

    @Autowired EntityManagerFactory fabrica;
    @Autowired RutinaRepository rutinas;
    @Autowired TransactionTemplate transacciones;

    @Test
    void dosSesionesQueGuardanElMismoBorradorALaVezSoloDejanPasarALaPrimera() {
        EntityManager sesionA = fabrica.createEntityManager();
        EntityManager sesionB = fabrica.createEntityManager();
        try {
            sesionA.getTransaction().begin();
            sesionB.getTransaction().begin();
            Rutina vistaPorA = sesionA.find(Rutina.class, RUTINA);
            Rutina vistaPorB = sesionB.find(Rutina.class, RUTINA);
            assertThat(vistaPorA.getVersion()).isEqualTo(vistaPorB.getVersion());

            renombrar(vistaPorA, "Preparar mochila (ajuste de la profesional)");
            sesionA.getTransaction().commit();

            renombrar(vistaPorB, "Preparar mochila (ajuste del suplente)");
            Throwable conflicto = catchThrowable(() -> sesionB.getTransaction().commit());

            assertThat(conflicto).isNotNull();
            assertThat(causas(conflicto)).anyMatch(causa -> causa instanceof OptimisticLockException
                    || causa instanceof StaleStateException);
        } finally {
            cerrar(sesionA);
            cerrar(sesionB);
        }
        // Gana quien guardo primero y nada se perdio en silencio.
        Rutina guardada = rutinas.findById(RUTINA).orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo("Preparar mochila (ajuste de la profesional)");
        assertThat(guardada.getVersion()).isEqualTo(1L);
    }

    @Test
    void guardarUnaCopiaVencidaDesdeElRepositorioSeTraduceAErrorDeConcurrenciaDeSpring() {
        Rutina copiaVencida = transacciones.execute(estado -> {
            Rutina rutina = rutinas.findById(RUTINA).orElseThrow();
            rutina.getPasos().size();
            rutina.getDiasSemana().size();
            return rutina;
        });

        // Otra sesion guarda primero y sube la version de la fila.
        transacciones.executeWithoutResult(estado ->
                renombrar(rutinas.findById(RUTINA).orElseThrow(), "Preparar mochila (otra sesion)"));

        renombrar(copiaVencida, "Preparar mochila (copia del formulario abierto)");
        Throwable conflicto = catchThrowable(() -> transacciones.executeWithoutResult(estado ->
                rutinas.saveAndFlush(copiaVencida)));

        assertThat(conflicto).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(rutinas.findById(RUTINA).orElseThrow().getNombre())
                .isEqualTo("Preparar mochila (otra sesion)");
    }

    @Test
    void cadaGuardadoCorrectoSubeLaVersionEnUno() {
        long inicial = rutinas.findById(RUTINA).orElseThrow().getVersion();
        for (int cambio = 1; cambio <= 2; cambio++) {
            String nombre = "Preparar mochila escolar v" + cambio;
            transacciones.executeWithoutResult(estado ->
                    renombrar(rutinas.findById(RUTINA).orElseThrow(), nombre));
            assertThat(rutinas.findById(RUTINA).orElseThrow().getVersion()).isEqualTo(inicial + cambio);
        }
    }

    private static void renombrar(Rutina rutina, String nombre) {
        rutina.actualizar(nombre, rutina.getHoraInicio(), rutina.getVigenciaDesde(),
                rutina.getVigenciaHasta(), new HashSet<>(rutina.getDiasSemana()));
    }

    private static List<Throwable> causas(Throwable error) {
        List<Throwable> cadena = new ArrayList<>();
        for (Throwable actual = error; actual != null && !cadena.contains(actual); actual = actual.getCause()) {
            cadena.add(actual);
        }
        return cadena;
    }

    private static void cerrar(EntityManager sesion) {
        if (sesion.getTransaction().isActive()) sesion.getTransaction().rollback();
        sesion.close();
    }
}
