package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.SaludService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SaludController {

    private final SaludService saludService;

    public SaludController(SaludService saludService) {
        this.saludService = saludService;
    }

    @GetMapping("/api/salud")
    public Map<String, String> salud() {
        return Map.of("estado", saludService.estadoDelSistema());
    }
}
