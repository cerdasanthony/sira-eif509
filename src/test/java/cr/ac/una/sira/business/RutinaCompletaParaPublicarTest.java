package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.PublicacionRutinaInvalidaException;
import cr.ac.una.sira.data.EstadoRutina;
import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.PasoRutina;
import cr.ac.una.sira.data.Rutina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RutinaCompletaParaPublicarTest {

    private final RutinaCompletaParaPublicar especificacion =
            new RutinaCompletaParaPublicar();
    private Rutina rutina;
    private Participante participante;

    @BeforeEach
    void preparar() {
        rutina = mock(Rutina.class);
        participante = mock(Participante.class);
        when(rutina.getEstado()).thenReturn(EstadoRutina.BORRADOR);
        when(rutina.getParticipante()).thenReturn(participante);
        when(participante.isActivo()).thenReturn(true);
        when(rutina.getVigenciaDesde()).thenReturn(LocalDate.of(2026, 8, 1));
        when(rutina.getDiasSemana()).thenReturn(Set.of((short) 1));
        PasoRutina primero = paso(1);
        PasoRutina segundo = paso(2);
        when(rutina.getPasos()).thenReturn(List.of(primero, segundo));
    }

    @Test
    void aceptaUnaRutinaCompleta() {
        assertThatCode(() -> especificacion.verificar(contexto()))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaUnaRutinaQueYaNoEsBorrador() {
        when(rutina.getEstado()).thenReturn(EstadoRutina.PUBLICADA);

        assertThatThrownBy(() -> especificacion.verificar(contexto()))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("BORRADOR");
    }

    @Test
    void rechazaParticipanteInactivo() {
        when(participante.isActivo()).thenReturn(false);

        assertThatThrownBy(() -> especificacion.verificar(contexto()))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("participante debe estar activo");
    }

    @Test
    void rechazaMenosDeDosPasos() {
        PasoRutina unico = paso(1);
        when(rutina.getPasos()).thenReturn(List.of(unico));

        assertThatThrownBy(() -> especificacion.verificar(contexto()))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("al menos dos pasos");
    }

    @Test
    void rechazaPasosConHuecos() {
        PasoRutina primero = paso(1);
        PasoRutina tercero = paso(3);
        when(rutina.getPasos()).thenReturn(List.of(primero, tercero));

        assertThatThrownBy(() -> especificacion.verificar(contexto()))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("consecutivo");
    }

    @Test
    void rechazaVigenciaInvertida() {
        when(rutina.getVigenciaHasta()).thenReturn(LocalDate.of(2026, 7, 31));

        assertThatThrownBy(() -> especificacion.verificar(contexto()))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("vigencia inicial");
    }

    @Test
    void rechazaRutinaSinDias() {
        when(rutina.getDiasSemana()).thenReturn(Set.of());

        assertThatThrownBy(() -> especificacion.verificar(contexto()))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("dia de la semana");
    }

    private ContextoPublicacionRutina contexto() {
        return new ContextoPublicacionRutina(rutina, mock(cr.ac.una.sira.data.Usuario.class), List.of());
    }

    private PasoRutina paso(int orden) {
        PasoRutina paso = mock(PasoRutina.class);
        when(paso.getOrden()).thenReturn((short) orden);
        return paso;
    }
}
