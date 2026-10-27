package cr.ac.una.sira.business.exception;

/** Alguien guardo el recurso mientras el usuario tenia abierta una copia anterior. */
public class ConflictoVersionException extends ConflictoRecursoException {
    public ConflictoVersionException(String recurso) {
        super("Otra persona guardo " + recurso + " mientras usted la editaba. Vuelva a abrirla y repita el cambio.");
    }
}
