package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.PublicacionRutinaService;
import cr.ac.una.sira.business.dto.PublicarRutinaEntrada;
import cr.ac.una.sira.business.dto.PublicarRutinaSalida;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rutinas")
public class RutinaController {

    private final PublicacionRutinaService publicacionRutinaService;

    public RutinaController(PublicacionRutinaService publicacionRutinaService) {
        this.publicacionRutinaService = publicacionRutinaService;
    }

    @PostMapping("/publicaciones")
    @ResponseStatus(HttpStatus.OK)
    public PublicarRutinaSalida publicar(
            @Valid @RequestBody PublicarRutinaEntrada entrada) {
        return publicacionRutinaService.publicar(entrada);
    }
}
