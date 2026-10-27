package cr.ac.una.sira.business;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.JsonEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La bitacora de seguridad tiene que servir para diagnosticar en produccion y, a la vez,
 * no convertirse en una fuga: aqui se comprueban los campos de cada evento, que la salida
 * sea JSON por campo y que no aparezcan passwords, hashes ni tokens.
 */
class EventosSeguridadTest {

    private static final String PASSWORD = "SiraDemo2026!";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.carga.firma";

    private final EventosSeguridad eventos = new EventosSeguridad();
    private final ListAppender<ILoggingEvent> registradas = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void escuchar() {
        logger = (Logger) LoggerFactory.getLogger("cr.ac.una.sira.seguridad");
        logger.setLevel(Level.INFO);
        registradas.start();
        logger.addAppender(registradas);
    }

    @AfterEach
    void soltar() {
        logger.detachAppender(registradas);
        registradas.stop();
    }

    @Test
    void elIngresoFallidoDejaEvento_cuentaEnmascarada_origenYMotivo() {
        eventos.ingresoFallido("mariana.vargas@sira.local", "credenciales invalidas", "198.51.100.4");

        ILoggingEvent evento = unico();
        assertThat(evento.getLevel()).isEqualTo(Level.WARN);
        assertThat(campos(evento)).containsEntry("evento", "ingreso_fallido")
                .containsEntry("cuenta", "ma***@sira.local")
                .containsEntry("origen", "198.51.100.4")
                .containsEntry("motivo", "credenciales invalidas");
    }

    @Test
    void laCuentaEnEsperaYElAccesoDenegadoQuedanRegistradosConSuContexto() {
        eventos.ingresoBloqueado("laura.mendez@sira.local", "203.0.113.9", 900);
        eventos.accesoDenegado("POST", "/api/v1/participantes", "rol insuficiente");
        eventos.conflictoDeVersion("PUT", "/api/v1/rutinas/2");

        assertThat(registradas.list).hasSize(3);
        assertThat(campos(registradas.list.get(0))).containsEntry("evento", "ingreso_bloqueado")
                .containsEntry("cuenta", "la***@sira.local").containsEntry("esperaSegundos", 900L);
        assertThat(campos(registradas.list.get(1))).containsEntry("evento", "acceso_denegado")
                .containsEntry("ruta", "/api/v1/participantes").containsEntry("sujeto", "anonimo");
        assertThat(campos(registradas.list.get(2))).containsEntry("evento", "conflicto_version")
                .containsEntry("metodo", "PUT");
    }

    @Test
    void ningunEventoArrastraLaPasswordNiElToken() {
        eventos.ingresoFallido("mariana.vargas@sira.local", "credenciales invalidas", "198.51.100.4");
        eventos.ingresoCorrecto("mariana.vargas@sira.local", "198.51.100.4");
        eventos.autenticacionRequerida("GET", "/api/v1/rutinas", "token invalido");

        String todo = registradas.list.stream()
                .map(evento -> evento.getFormattedMessage() + " " + campos(evento))
                .collect(Collectors.joining(" "));
        assertThat(todo).doesNotContain(PASSWORD).doesNotContain(TOKEN)
                .doesNotContain("$2a$").doesNotContain("Bearer ")
                .doesNotContain("mariana.vargas@sira.local");
    }

    @Test
    void elEncoderJsonPublicaCadaEventoComoUnObjetoConSusCampos() {
        eventos.accesoDenegado("DELETE", "/api/v1/usuarios/1", "rol insuficiente");

        JsonEncoder encoder = new JsonEncoder();
        encoder.setContext(logger.getLoggerContext());
        encoder.start();
        String linea = new String(encoder.encode(unico()), StandardCharsets.UTF_8);
        encoder.stop();

        // El encoder escapa las barras, asi que la ruta viaja como \/api\/v1\/usuarios\/1.
        assertThat(linea).startsWith("{").contains("\"level\"").contains("WARN")
                .contains("\"loggerName\"").contains("cr.ac.una.sira.seguridad")
                .contains("\"evento\"").contains("acceso_denegado")
                .contains("\"ruta\"").contains("usuarios");
        assertThat(linea).doesNotContain(PASSWORD).doesNotContain(TOKEN);
    }

    @Test
    void elEnmascaradoConservaElDominioYNuncaDevuelveLaCuentaCompleta() {
        assertThat(EventosSeguridad.enmascarar("mariana.vargas@sira.local")).isEqualTo("ma***@sira.local");
        assertThat(EventosSeguridad.enmascarar("a@sira.local")).isEqualTo("a***@sira.local");
        assertThat(EventosSeguridad.enmascarar("sin-arroba")).isEqualTo("***");
        assertThat(EventosSeguridad.enmascarar(null)).isEqualTo("desconocida");
        assertThat(EventosSeguridad.enmascarar("   ")).isEqualTo("desconocida");
    }

    private ILoggingEvent unico() {
        assertThat(registradas.list).hasSize(1);
        return registradas.list.get(0);
    }

    private static Map<String, Object> campos(ILoggingEvent evento) {
        return evento.getKeyValuePairs().stream()
                .collect(Collectors.toMap(par -> par.key, (KeyValuePair par) -> par.value));
    }
}
