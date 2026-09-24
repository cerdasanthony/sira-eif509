package cr.ac.una.sira.business;

import cr.ac.una.sira.data.ResultadoPaso;

import java.math.BigDecimal;
import java.util.List;

/** Strategy que permite cambiar la formula sin modificar el caso de uso. */
public interface CalculoAdherencia {

    BigDecimal calcular(List<ResultadoPaso> resultados);
}
