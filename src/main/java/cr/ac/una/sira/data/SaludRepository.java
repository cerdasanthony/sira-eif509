package cr.ac.una.sira.data;

import org.springframework.stereotype.Repository;

@Repository
public class SaludRepository {

    // Valor fijo por ahora. Con base de datos pasa a ser una consulta real.
    public String leerEstado() {
        return "OK - SIRA en linea";
    }
}
