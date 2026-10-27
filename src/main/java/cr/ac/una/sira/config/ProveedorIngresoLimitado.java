package cr.ac.una.sira.config;

import cr.ac.una.sira.business.ControlIntentosIngreso;
import cr.ac.una.sira.business.EventosSeguridad;
import cr.ac.una.sira.business.exception.DemasiadosIntentosException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * Envuelve la comprobacion de password del formulario MVC con el mismo freno de
 * intentos que usa /auth/login y deja el evento en la bitacora de seguridad.
 */
public class ProveedorIngresoLimitado implements AuthenticationProvider {
    private final AuthenticationProvider delegado;
    private final ControlIntentosIngreso intentos;
    private final EventosSeguridad eventos;

    public ProveedorIngresoLimitado(AuthenticationProvider delegado,
            ControlIntentosIngreso intentos, EventosSeguridad eventos) {
        this.delegado = delegado;
        this.intentos = intentos;
        this.eventos = eventos;
    }

    @Override
    public Authentication authenticate(Authentication autenticacion) throws AuthenticationException {
        String correo = autenticacion.getName();
        String origen = origen(autenticacion);
        try {
            intentos.exigirIntentosDisponibles(correo, origen);
        } catch (DemasiadosIntentosException ex) {
            eventos.ingresoBloqueado(correo, origen, ex.getSegundosRestantes());
            throw new LockedException("Demasiados intentos de ingreso");
        }
        try {
            Authentication resultado = delegado.authenticate(autenticacion);
            intentos.registrarExito(correo, origen);
            eventos.ingresoCorrecto(correo, origen);
            return resultado;
        } catch (AuthenticationException ex) {
            intentos.registrarFallo(correo, origen);
            eventos.ingresoFallido(correo, "credenciales invalidas", origen);
            throw ex;
        }
    }

    @Override
    public boolean supports(Class<?> autenticacion) { return delegado.supports(autenticacion); }

    private static String origen(Authentication autenticacion) {
        return autenticacion.getDetails() instanceof WebAuthenticationDetails detalles
                ? detalles.getRemoteAddress() : null;
    }
}
