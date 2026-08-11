package cr.ac.una.sira.business;

import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.ParticipanteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba unitaria de la capa de negocio, sin levantar Spring.
 * Solo es posible porque el servicio recibe su repositorio por constructor:
 * esa es una consecuencia practica de separar las capas.
 */
class ParticipanteServiceTest {

    private final ParticipanteService servicio = new ParticipanteService(new ParticipanteRepository());

    @Test
    void listarActivos_excluyeParticipantesInactivos() {
        List<Participante> activos = servicio.listarActivos();

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
