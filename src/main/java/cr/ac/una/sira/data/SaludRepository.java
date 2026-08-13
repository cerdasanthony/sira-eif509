package cr.ac.una.sira.data;

import org.springframework.stereotype.Repository;

/**
 * Estado de la fuente de datos. Hoy es un valor fijo; cuando haya base de datos
 * esto pasa a ser una consulta real de verificacion.
 */
@Repository
public class SaludRepository {

    public String leerEstado() {
        return "OK - SIRA en linea";
    }
}
