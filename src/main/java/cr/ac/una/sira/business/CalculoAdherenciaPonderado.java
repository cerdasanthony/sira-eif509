package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.CierreEjecucionInvalidoException;
import cr.ac.una.sira.data.ResultadoPaso;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CalculoAdherenciaPonderado implements CalculoAdherencia {

    private static final BigDecimal CIEN = new BigDecimal("100");

    @Override
    public BigDecimal calcular(List<ResultadoPaso> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            throw new CierreEjecucionInvalidoException(
                    "La adherencia requiere al menos un resultado");
        }

        BigDecimal puntos = resultados.stream()
                .map(this::puntaje)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return puntos.multiply(CIEN)
                .divide(BigDecimal.valueOf(resultados.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal puntaje(ResultadoPaso resultado) {
        if (resultado == null) {
            throw new CierreEjecucionInvalidoException(
                    "Todos los pasos deben tener un resultado");
        }
        return switch (resultado) {
            case LOGRADO -> BigDecimal.ONE;
            case CON_APOYO -> new BigDecimal("0.5");
            case NO_LOGRADO -> BigDecimal.ZERO;
        };
    }
}
