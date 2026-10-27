package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Freno de fuerza bruta: cinco fallos por cuenta y origen dejan la cuenta en espera. */
class ControlIntentosIngresoTest {

    private static final String CUENTA = "mariana.vargas@sira.local";
    private static final String ORIGEN = "198.51.100.4";
    private static final Instant INICIO = Instant.parse("2026-11-05T09:00:00Z");

    private Instant ahora = INICIO;
    private final Clock reloj = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zona) { return this; }
        @Override public Instant instant() { return ahora; }
    };

    private final ControlIntentosIngreso control = new ControlIntentosIngreso(
            reloj, 5, Duration.ofMinutes(15), Duration.ofMinutes(15));

    @Test
    void losCuatroPrimerosFallosNoBloqueanYElQuintoSi() {
        for (int fallo = 1; fallo <= 4; fallo++) {
            control.registrarFallo(CUENTA, ORIGEN);
            assertThatCode(() -> control.exigirIntentosDisponibles(CUENTA, ORIGEN)).doesNotThrowAnyException();
        }
        control.registrarFallo(CUENTA, ORIGEN);
        assertThatThrownBy(() -> control.exigirIntentosDisponibles(CUENTA, ORIGEN))
                .isInstanceOf(DemasiadosIntentosException.class);
    }

    @Test
    void laEsperaIndicaCuantosSegundosFaltanYTerminaSola() {
        for (int fallo = 1; fallo <= 5; fallo++) control.registrarFallo(CUENTA, ORIGEN);

        DemasiadosIntentosException error = null;
        try {
            control.exigirIntentosDisponibles(CUENTA, ORIGEN);
        } catch (DemasiadosIntentosException ex) {
            error = ex;
        }
        assertThat(error).isNotNull();
        assertThat(error.getSegundosRestantes()).isBetween(1L, Duration.ofMinutes(15).toSeconds());
        assertThat(error.getMessage()).doesNotContain("password");

        ahora = INICIO.plus(Duration.ofMinutes(16));
        assertThatCode(() -> control.exigirIntentosDisponibles(CUENTA, ORIGEN)).doesNotThrowAnyException();
    }

    @Test
    void elBloqueoEsPorCuentaYOrigenYNoDistingueMayusculas() {
        for (int fallo = 1; fallo <= 5; fallo++) control.registrarFallo(CUENTA, ORIGEN);

        assertThatThrownBy(() -> control.exigirIntentosDisponibles(CUENTA.toUpperCase(), ORIGEN))
                .isInstanceOf(DemasiadosIntentosException.class);
        assertThatCode(() -> control.exigirIntentosDisponibles(CUENTA, "203.0.113.1")).doesNotThrowAnyException();
        assertThatCode(() -> control.exigirIntentosDisponibles("otra@sira.local", ORIGEN)).doesNotThrowAnyException();
    }

    @Test
    void unIngresoCorrectoBorraElConteoAnterior() {
        for (int fallo = 1; fallo <= 4; fallo++) control.registrarFallo(CUENTA, ORIGEN);
        control.registrarExito(CUENTA, ORIGEN);

        for (int fallo = 1; fallo <= 4; fallo++) control.registrarFallo(CUENTA, ORIGEN);
        assertThatCode(() -> control.exigirIntentosDisponibles(CUENTA, ORIGEN)).doesNotThrowAnyException();
    }

    @Test
    void losFallosMuySeparadosEnElTiempoNoSeAcumulan() {
        for (int fallo = 1; fallo <= 4; fallo++) control.registrarFallo(CUENTA, ORIGEN);
        ahora = INICIO.plus(Duration.ofMinutes(31));
        control.registrarFallo(CUENTA, ORIGEN);

        assertThatCode(() -> control.exigirIntentosDisponibles(CUENTA, ORIGEN)).doesNotThrowAnyException();
    }
}
