package cr.ac.una.sira.data;

import org.springframework.stereotype.Repository;

/**
 * CAPA DE DATOS.
 * Lee y escribe informacion de la fuente de datos. Es la capa mas interna:
 * no depende de ninguna otra capa del sistema.
 *
 * Hoy devuelve un valor fijo. En el Laboratorio 2 esta capa hablara con
 * PostgreSQL (datos relacionales) y MongoDB (bitacora de observaciones).
 */
@Repository
public class SaludRepository {

    public String leerEstado() {
        return "OK - SIRA en linea";
    }
}
