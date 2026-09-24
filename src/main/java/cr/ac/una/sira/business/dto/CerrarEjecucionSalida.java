package cr.ac.una.sira.business.dto;

import cr.ac.una.sira.data.EstadoEjecucion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CerrarEjecucionSalida(
        Long ejecucionId,
        Long rutinaId,
        LocalDate fecha,
        BigDecimal adherencia,
        EstadoEjecucion estado,
        int pasosRegistrados) {
}
