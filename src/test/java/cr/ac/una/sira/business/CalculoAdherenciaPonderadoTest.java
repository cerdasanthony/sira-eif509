package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.CierreEjecucionInvalidoException;
import cr.ac.una.sira.data.ResultadoPaso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculoAdherenciaPonderadoTest {

    private final CalculoAdherencia estrategia = new CalculoAdherenciaPonderado();

    @Test
    void calculaLosTresPuntajesYRedondeaADosDecimales() {
        BigDecimal resultado = estrategia.calcular(List.of(
                ResultadoPaso.LOGRADO,
                ResultadoPaso.LOGRADO,
                ResultadoPaso.CON_APOYO,
                ResultadoPaso.NO_LOGRADO));

        assertThat(resultado).isEqualByComparingTo("62.50");
    }

    @Test
    void todosLogradosProducenCien() {
        assertThat(estrategia.calcular(List.of(
                ResultadoPaso.LOGRADO, ResultadoPaso.LOGRADO)))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void rechazaUnaListaVacia() {
        assertThatThrownBy(() -> estrategia.calcular(List.of()))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("al menos un resultado");
    }

    @Test
    void rechazaUnResultadoNulo() {
        assertThatThrownBy(() -> estrategia.calcular(
                java.util.Arrays.asList(ResultadoPaso.LOGRADO, null)))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("Todos los pasos");
    }
}
