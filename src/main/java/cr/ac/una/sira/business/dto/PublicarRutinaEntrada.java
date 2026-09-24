package cr.ac.una.sira.business.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PublicarRutinaEntrada(
        @NotNull @Positive Long rutinaId,
        @NotNull @Positive Long profesionalId) {
}
