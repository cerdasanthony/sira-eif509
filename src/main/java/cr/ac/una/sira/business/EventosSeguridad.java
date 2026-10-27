package cr.ac.una.sira.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Registro de los hechos de seguridad que hay que poder reconstruir en produccion:
 * ingresos fallidos, cuentas en espera, accesos denegados y conflictos de version.
 * Cada evento sale como pares clave/valor para que el encoder JSON los publique como
 * campos. Nunca se registran passwords, hashes ni tokens: del correo solo salen las
 * dos primeras letras y el dominio, y del JWT unicamente el identificador del sujeto.
 */
@Component
public class EventosSeguridad {
    private static final Logger LOG = LoggerFactory.getLogger("cr.ac.una.sira.seguridad");

    public void ingresoFallido(String correo, String motivo, String origen) {
        LOG.atWarn().setMessage("Ingreso fallido")
                .addKeyValue("evento", "ingreso_fallido")
                .addKeyValue("cuenta", enmascarar(correo))
                .addKeyValue("origen", texto(origen))
                .addKeyValue("motivo", texto(motivo))
                .log();
    }

    public void ingresoBloqueado(String correo, String origen, long segundosRestantes) {
        LOG.atWarn().setMessage("Ingreso bloqueado por intentos repetidos")
                .addKeyValue("evento", "ingreso_bloqueado")
                .addKeyValue("cuenta", enmascarar(correo))
                .addKeyValue("origen", texto(origen))
                .addKeyValue("esperaSegundos", segundosRestantes)
                .log();
    }

    public void ingresoCorrecto(String correo, String origen) {
        LOG.atInfo().setMessage("Ingreso correcto")
                .addKeyValue("evento", "ingreso_correcto")
                .addKeyValue("cuenta", enmascarar(correo))
                .addKeyValue("origen", texto(origen))
                .log();
    }

    public void accesoDenegado(String metodo, String ruta, String motivo) {
        LOG.atWarn().setMessage("Acceso denegado")
                .addKeyValue("evento", "acceso_denegado")
                .addKeyValue("sujeto", sujeto())
                .addKeyValue("metodo", texto(metodo))
                .addKeyValue("ruta", texto(ruta))
                .addKeyValue("motivo", texto(motivo))
                .log();
    }

    public void autenticacionRequerida(String metodo, String ruta, String motivo) {
        LOG.atWarn().setMessage("Autenticacion requerida")
                .addKeyValue("evento", "autenticacion_requerida")
                .addKeyValue("metodo", texto(metodo))
                .addKeyValue("ruta", texto(ruta))
                .addKeyValue("motivo", texto(motivo))
                .log();
    }

    public void conflictoDeVersion(String metodo, String ruta) {
        LOG.atWarn().setMessage("Conflicto de version por edicion simultanea")
                .addKeyValue("evento", "conflicto_version")
                .addKeyValue("sujeto", sujeto())
                .addKeyValue("metodo", texto(metodo))
                .addKeyValue("ruta", texto(ruta))
                .log();
    }

    /** Identificador del usuario autenticado; el JWT usa el id como subject. */
    private String sujeto() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) return "anonimo";
        String nombre = autenticacion.getName();
        return nombre == null || nombre.isBlank() ? "anonimo"
                : (nombre.contains("@") ? enmascarar(nombre) : nombre);
    }

    /** Deja visible el dominio y dos letras de la cuenta: suficiente para diagnosticar. */
    static String enmascarar(String correo) {
        if (correo == null || correo.isBlank()) return "desconocida";
        int arroba = correo.indexOf('@');
        if (arroba <= 0) return "***";
        String inicio = correo.substring(0, Math.min(2, arroba));
        return inicio + "***" + correo.substring(arroba);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "desconocido" : valor;
    }
}
