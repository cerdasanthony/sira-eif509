package cr.ac.una.sira.business;

import cr.ac.una.sira.data.SaludRepository;
import org.springframework.stereotype.Service;

/**
 * CAPA DE NEGOCIO.
 * Resuelve el problema de negocio. No sabe que existe HTTP.
 *
 * Direccion de la dependencia: negocio --> datos.
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
