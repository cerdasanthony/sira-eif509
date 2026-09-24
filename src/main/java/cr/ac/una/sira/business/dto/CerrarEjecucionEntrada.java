package cr.ac.una.sira.business.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CerrarEjecucionEntrada(
        @NotNull @Positive Long rutinaId,
        @NotNull @Positive Long encargadoId,
        @NotNull @PastOrPresent LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotEmpty List<@Valid ResultadoPasoEntrada> resultados) {
}
