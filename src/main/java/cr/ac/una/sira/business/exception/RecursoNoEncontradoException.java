package cr.ac.una.sira.business.exception;

public class RecursoNoEncontradoException extends ReglaNegocioException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con id " + id + " no existe");
    }
}
