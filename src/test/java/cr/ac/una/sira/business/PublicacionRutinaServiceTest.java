package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.PublicarRutinaEntrada;
import cr.ac.una.sira.business.exception.RecursoNoEncontradoException;
import cr.ac.una.sira.data.EstadoRutina;
import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.Rutina;
import cr.ac.una.sira.data.RutinaRepository;
import cr.ac.una.sira.data.Usuario;
import cr.ac.una.sira.data.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicacionRutinaServiceTest {

    private final RutinaRepository rutinaRepository = mock(RutinaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EspecificacionPublicacionRutina primera =
            mock(EspecificacionPublicacionRutina.class);
    private final EspecificacionPublicacionRutina segunda =
            mock(EspecificacionPublicacionRutina.class);
    private final Clock reloj = Clock.fixed(
            Instant.parse("2026-09-24T15:00:00Z"), ZoneOffset.UTC);
    private final PublicacionRutinaService servicio = new PublicacionRutinaService(
            rutinaRepository, usuarioRepository, List.of(primera, segunda), reloj);

    private Rutina rutina;
    private Usuario profesional;

    @BeforeEach
    void preparar() {
        rutina = mock(Rutina.class);
        profesional = mock(Usuario.class);
        Participante participante = mock(Participante.class);
        when(rutina.getId()).thenReturn(9L);
        when(rutina.getParticipante()).thenReturn(participante);
        when(participante.getId()).thenReturn(4L);
        when(rutina.duracionTotalMinutos()).thenReturn(17);
        when(rutina.getEstado()).thenReturn(EstadoRutina.PUBLICADA);
        when(rutinaRepository.findById(9L)).thenReturn(Optional.of(rutina));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(profesional));
        when(rutinaRepository.buscarPublicadasConDetalleDelParticipante(4L))
                .thenReturn(List.of());
    }

    @Test
    void publicaLuegoDeCumplirTodasLasEspecificaciones() {
        var salida = servicio.publicar(new PublicarRutinaEntrada(9L, 2L));

        verify(primera).verificar(org.mockito.ArgumentMatchers.any());
        verify(segunda).verificar(org.mockito.ArgumentMatchers.any());
        verify(rutina).publicar(OffsetDateTime.parse("2026-09-24T15:00Z"));
        verify(rutinaRepository).save(rutina);
        assertThat(salida.duracionTotalMinutos()).isEqualTo(17);
        assertThat(salida.rutinaId()).isEqualTo(9L);
    }

    @Test
    void informaCuandoLaRutinaNoExiste() {
        when(rutinaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.publicar(
                new PublicarRutinaEntrada(99L, 2L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Rutina");
    }

    @Test
    void informaCuandoElUsuarioNoExiste() {
        when(usuarioRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.publicar(
                new PublicarRutinaEntrada(9L, 88L)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Usuario");
    }
}
