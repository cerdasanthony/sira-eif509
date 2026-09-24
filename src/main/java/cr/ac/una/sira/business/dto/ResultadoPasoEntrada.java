package cr.ac.una.sira.business.dto;

import cr.ac.una.sira.data.ResultadoPaso;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResultadoPasoEntrada(
        @NotNull @Positive Long pasoRutinaId,
        @NotNull ResultadoPaso resultado,
        @Size(max = 240) String observacionCorta) {
}
