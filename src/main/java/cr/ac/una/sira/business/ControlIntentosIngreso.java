package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.DemasiadosIntentosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita los ingresos fallidos por cuenta y origen. Sin este freno, el formulario de
 * ingreso y /auth/login aceptan intentos ilimitados y una lista de passwords comunes
 * se puede probar entera contra una cuenta conocida.
 *
 * El conteo vive en memoria del proceso: suficiente para una sola instancia y no
 * guarda password alguna, solo la cuenta, el origen y cuantos intentos fallaron.
 */
@Component
public class ControlIntentosIngreso {
    private static final int MAXIMO_ENTRADAS = 10_000;

    private final Map<String, Intentos> intentos = new ConcurrentHashMap<>();
    private final Clock reloj;
    private final int maximoFallos;
    private final Duration ventana;
    private final Duration espera;

    public ControlIntentosIngreso(Clock reloj,
            @Value("${sira.ingreso.maximo-fallos:5}") int maximoFallos,
            @Value("${sira.ingreso.ventana:PT15M}") Duration ventana,
            @Value("${sira.ingreso.espera:PT15M}") Duration espera) {
        this.reloj = reloj;
        this.maximoFallos = maximoFallos;
        this.ventana = ventana;
        this.espera = espera;
    }

    /** Lanza {@link DemasiadosIntentosException} si la cuenta esta en espera. */
    public void exigirIntentosDisponibles(String correo, String origen) {
        Intentos registro = intentos.get(clave(correo, origen));
        if (registro == null) return;
        long restantes = registro.segundosDeEspera(Instant.now(reloj), maximoFallos, espera);
        if (restantes > 0) throw new DemasiadosIntentosException(restantes);
    }

    public void registrarFallo(String correo, String origen) {
        Instant ahora = Instant.now(reloj);
        if (intentos.size() > MAXIMO_ENTRADAS) descartarVencidos(ahora);
        intentos.compute(clave(correo, origen), (ignorado, actual) -> {
            if (actual == null || actual.vencio(ahora, ventana, espera)) return new Intentos(ahora);
            actual.sumar(ahora);
            return actual;
        });
    }

    public void registrarExito(String correo, String origen) {
        intentos.remove(clave(correo, origen));
    }

    private void descartarVencidos(Instant ahora) {
        intentos.entrySet().removeIf(entrada -> entrada.getValue().vencio(ahora, ventana, espera));
    }

    private static String clave(String correo, String origen) {
        String cuenta = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        return cuenta + "|" + (origen == null ? "" : origen);
    }

    private static final class Intentos {
        private final AtomicInteger fallos = new AtomicInteger(1);
        private volatile Instant ultimo;

        private Intentos(Instant ahora) { this.ultimo = ahora; }

        private void sumar(Instant ahora) {
            fallos.incrementAndGet();
            ultimo = ahora;
        }

        private boolean vencio(Instant ahora, Duration ventana, Duration espera) {
            Duration limite = ventana.compareTo(espera) >= 0 ? ventana : espera;
            return ultimo.plus(limite).isBefore(ahora);
        }

        private long segundosDeEspera(Instant ahora, int maximoFallos, Duration espera) {
            if (fallos.get() < maximoFallos) return 0;
            Instant libre = ultimo.plus(espera);
            return libre.isAfter(ahora) ? Math.max(1, Duration.between(ahora, libre).toSeconds()) : 0;
        }
    }
}
