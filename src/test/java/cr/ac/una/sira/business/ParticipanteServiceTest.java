package cr.ac.una.sira.business;

import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.ParticipanteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipanteServiceTest {

    // Se puede armar a mano porque recibe el repositorio por constructor.
    private final ParticipanteService servicio = new ParticipanteService(new ParticipanteRepository());

    @Test
    void listarActivos_excluyeParticipantesInactivos() {
        List<Participante> activos = servicio.listarActivos();

        // isNotEmpty va primero: sobre una lista vacia, allMatch pasaria igual.
        assertThat(activos).isNotEmpty();
        assertThat(activos).allMatch(Participante::activo);
    }

    @Test
    void listarActivos_devuelveOrdenadoPorNombre() {
        List<String> nombres = servicio.listarActivos().stream()
                .map(Participante::nombre)
                .toList();

        assertThat(nombres).isSorted();
    }
}
