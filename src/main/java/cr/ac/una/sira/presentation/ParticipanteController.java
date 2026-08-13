package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.ParticipanteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/participantes")
public class ParticipanteController {

    private final ParticipanteService participanteService;

    public ParticipanteController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    @GetMapping
    public List<ParticipanteResponse> listar() {
        return participanteService.listarActivos().stream()
                .map(p -> new ParticipanteResponse(p.id(), p.nombre()))
                .toList();
    }
}
