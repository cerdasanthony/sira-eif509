package cr.ac.una.sira.business.dto;

import cr.ac.una.sira.data.EstadoRutina;

import java.time.OffsetDateTime;

public record PublicarRutinaSalida(
        Long rutinaId,
        EstadoRutina estado,
        int duracionTotalMinutos,
        OffsetDateTime publicadaEn) {
}
