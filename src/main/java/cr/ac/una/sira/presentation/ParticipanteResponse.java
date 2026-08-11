package cr.ac.una.sira.presentation;

/**
 * CAPA DE PRESENTACION - DTO de salida.
 * Define QUE expone la API, independiente de como se guarden los datos por dentro.
 * Asi un cambio en el modelo de datos no rompe automaticamente el contrato HTTP.
 */
public record ParticipanteResponse(Long id, String nombre) {
}
