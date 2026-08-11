package cr.ac.una.sira.business;

import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.ParticipanteRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * CAPA DE NEGOCIO.
 * Aqui viven las reglas. En el Laboratorio 1 se demuestra una sola, minima,
 * para evidenciar que la capa existe y tiene responsabilidad propia:
 *
 *   Regla: solo se pueden asignar rutinas a participantes activos, por lo que
 *   el listado operativo excluye a los inactivos.
 *
 * Los procesos completos (publicar rutina, cerrar ejecucion diaria) se
 * implementan en los Laboratorios 3 y 4.
 */
@Service
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;

    public ParticipanteService(ParticipanteRepository participanteRepository) {
        this.participanteRepository = participanteRepository;
    }

    /**
     * Devuelve los participantes activos, ordenados por nombre.
     * La consulta cruda la resuelve la capa de datos; el filtro y el orden son
     * decisiones de negocio y por eso viven aqui.
     */
    public List<Participante> listarActivos() {
        return participanteRepository.buscarTodos().stream()
                .filter(Participante::activo)
                .sorted(Comparator.comparing(Participante::nombre))
                .toList();
    }
}
