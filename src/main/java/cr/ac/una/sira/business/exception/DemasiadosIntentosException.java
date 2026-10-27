package cr.ac.una.sira.business.exception;

/** La cuenta acumulo demasiados ingresos fallidos y queda en espera. */
public class DemasiadosIntentosException extends RuntimeException {
    private final long segundosRestantes;

    public DemasiadosIntentosException(long segundosRestantes) {
        super("Demasiados intentos de ingreso. Espere " + segundosRestantes + " segundos.");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() { return segundosRestantes; }
}
