package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.PublicacionRutinaInvalidaException;
import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.RolUsuario;
import cr.ac.una.sira.data.Rutina;
import cr.ac.una.sira.data.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfesionalAutorizadoParaPublicarTest {

    private final ProfesionalAutorizadoParaPublicar especificacion =
            new ProfesionalAutorizadoParaPublicar();

    @Test
    void aceptaAlProfesionalActivoAsignado() {
        ContextoPublicacionRutina contexto = contexto(1L, 1L, RolUsuario.PROFESIONAL, true);

        assertThatCode(() -> especificacion.verificar(contexto)).doesNotThrowAnyException();
    }

    @Test
    void rechazaAUnEncargado() {
        ContextoPublicacionRutina contexto = contexto(1L, 1L, RolUsuario.ENCARGADO, true);

        assertThatThrownBy(() -> especificacion.verificar(contexto))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("profesional activo");
    }

    @Test
    void rechazaAUnProfesionalNoAsignado() {
        ContextoPublicacionRutina contexto = contexto(1L, 2L, RolUsuario.PROFESIONAL, true);

        assertThatThrownBy(() -> especificacion.verificar(contexto))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("no esta asignado");
    }

    private ContextoPublicacionRutina contexto(
            Long actorId, Long asignadoId, RolUsuario rol, boolean activo) {
        Usuario actor = mock(Usuario.class);
        Usuario asignado = mock(Usuario.class);
        Participante participante = mock(Participante.class);
        Rutina rutina = mock(Rutina.class);
        when(actor.getId()).thenReturn(actorId);
        when(actor.getRol()).thenReturn(rol);
        when(actor.isActivo()).thenReturn(activo);
        when(asignado.getId()).thenReturn(asignadoId);
        when(participante.getProfesional()).thenReturn(asignado);
        when(rutina.getParticipante()).thenReturn(participante);
        return new ContextoPublicacionRutina(rutina, actor, List.of());
    }
}
