package cr.ac.una.sira.business;

import cr.ac.una.sira.data.SaludRepository;
import org.springframework.stereotype.Service;

/**
 * Por ahora solo delega. Cuando haya que revisar varias fuentes (PostgreSQL,
 * Mongo) y decidir un estado global, esa decision es de negocio y va aca.
 */
@Service
public class SaludService {

    private final SaludRepository saludRepository;

    public SaludService(SaludRepository saludRepository) {
        this.saludRepository = saludRepository;
    }

    public String estadoDelSistema() {
        return saludRepository.leerEstado();
    }
}
