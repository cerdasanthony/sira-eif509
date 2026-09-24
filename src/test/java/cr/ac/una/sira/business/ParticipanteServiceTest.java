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
        when(mateo.getId()).thenReturn(1L);
        when(mateo.getNombre()).thenReturn("Mateo");
        when(sofia.getId()).thenReturn(2L);
        when(sofia.getNombre()).thenReturn("Sofia");
        when(repositorio.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(mateo, sofia));

        var activos = servicio.listarActivos();

        assertThat(activos)
                .extracting("id", "nombre")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "Mateo"),
                        org.assertj.core.groups.Tuple.tuple(2L, "Sofia"));
    }
}
