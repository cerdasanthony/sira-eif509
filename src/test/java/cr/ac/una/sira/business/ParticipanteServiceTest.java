package cr.ac.una.sira.business;

import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.ParticipanteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParticipanteServiceTest {

    private final ParticipanteRepository repositorio = mock(ParticipanteRepository.class);
    private final ParticipanteService servicio = new ParticipanteService(repositorio);

    @Test
    void listarActivos_delegaElFiltroYOrdenamientoAlRepositorio() {
        Participante mateo = mock(Participante.class);
        Participante sofia = mock(Participante.class);
        when(repositorio.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(mateo, sofia));

        List<Participante> activos = servicio.listarActivos();

        assertThat(activos).containsExactly(mateo, sofia);
    }
}
