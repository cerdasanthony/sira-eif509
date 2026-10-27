package cr.ac.una.sira.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.una.sira.business.EventosSeguridad;
import cr.ac.una.sira.business.exception.ConflictoVersionException;
import cr.ac.una.sira.business.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El conflicto de concurrencia tiene que salir como 409 con Problem Details, tanto
 * cuando lo detecta la comprobacion de version del servicio como cuando lo detecta
 * Hibernate al escribir dos transacciones simultaneas.
 */
class ManejadorErroresApiTest {

    private final ManejadorErroresApi manejador =
            new ManejadorErroresApi(new ProblemasHttp(new ObjectMapper()), new EventosSeguridad());

    @Test
    void elFalloDeBloqueoOptimistaDeHibernateSeTraduceA409() {
        var respuesta = manejador.conflictoDeConcurrencia(
                new ObjectOptimisticLockingFailureException("Rutina", 2L),
                peticion("PUT", "/api/v1/rutinas/2"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
        ProblemDetail problema = cuerpo(respuesta);
        assertThat(problema.getProperties()).containsEntry("codigo", "CONFLICTO_VERSION");
        assertThat(problema.getInstance()).hasToString("/api/v1/rutinas/2");
        assertThat(problema.getDetail()).contains("Vuelva a abrirlo");
    }

    @Test
    void laVersionVencidaDelServicioUsaElMismoCodigoYEstado() {
        var respuesta = manejador.conflicto(
                new ConflictoVersionException("la rutina"), peticion("PUT", "/api/v1/rutinas/7"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
        assertThat(cuerpo(respuesta).getProperties()).containsEntry("codigo", "CONFLICTO_VERSION");
    }

    @Test
    void elExcesoDeIntentosDevuelve429ConRetryAfterYSinRevelarLaPassword() {
        var respuesta = manejador.demasiadosIntentos(
                new DemasiadosIntentosException(900), peticion("POST", "/auth/login"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(429);
        assertThat(respuesta.getHeaders().getFirst("Retry-After")).isEqualTo("900");
        ProblemDetail problema = cuerpo(respuesta);
        assertThat(problema.getProperties()).containsEntry("codigo", "DEMASIADOS_INTENTOS");
        assertThat(problema.getDetail()).doesNotContain("password").contains("900");
    }

    private static MockHttpServletRequest peticion(String metodo, String ruta) {
        return new MockHttpServletRequest(metodo, ruta);
    }

    private static ProblemDetail cuerpo(ResponseEntity<ProblemDetail> respuesta) {
        ProblemDetail problema = respuesta.getBody();
        assertThat(problema).isNotNull();
        return problema;
    }
}
