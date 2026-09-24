package cr.ac.una.sira.business.exception;

public abstract class ReglaNegocioException extends RuntimeException {

    protected ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
