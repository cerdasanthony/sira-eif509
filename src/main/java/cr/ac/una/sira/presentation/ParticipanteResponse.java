package cr.ac.una.sira.presentation;

/**
 * Lo que expone la API, que no tiene por que ser lo mismo que se guarda.
 * Participante trae ademas la fecha de nacimiento, que el listado no necesita
 * y que en este dominio es dato sensible.
 */
public record ParticipanteResponse(Long id, String nombre) {
}
