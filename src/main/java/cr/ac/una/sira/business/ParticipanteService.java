package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.ParticipanteResumen;
import cr.ac.una.sira.data.ParticipanteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;

    public ParticipanteService(ParticipanteRepository participanteRepository) {
        this.participanteRepository = participanteRepository;
    }

    // Regla: solo se asignan rutinas a participantes activos, asi que el listado
    // operativo no muestra a los inactivos.
    public List<ParticipanteResumen> listarActivos() {
        return participanteRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(participante -> new ParticipanteResumen(
                        participante.getId(), participante.getNombre()))
                .toList();
    }
}
