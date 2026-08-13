package cr.ac.una.sira.business;

import cr.ac.una.sira.data.SaludRepository;
import org.springframework.stereotype.Service;

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
