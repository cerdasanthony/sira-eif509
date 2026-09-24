package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.PublicacionRutinaInvalidaException;
import cr.ac.una.sira.data.Rutina;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RutinaSinTraslapeTest {

    private final RutinaSinTraslape especificacion = new RutinaSinTraslape();

    @Test
    void rechazaFranjasTraslapadasEnElMismoDia() {
        Rutina candidata = rutina(1L, "Nueva", LocalTime.of(8, 0), 30, Set.of((short) 1));
        Rutina publicada = rutina(2L, "Existente", LocalTime.of(8, 20), 20, Set.of((short) 1));

        assertThatThrownBy(() -> especificacion.verificar(
                new ContextoPublicacionRutina(candidata, null, List.of(publicada))))
                .isInstanceOf(PublicacionRutinaInvalidaException.class)
                .hasMessageContaining("Existente");
    }

    @Test
    void aceptaLaMismaHoraEnDiasDistintos() {
        Rutina candidata = rutina(1L, "Nueva", LocalTime.of(8, 0), 30, Set.of((short) 1));
        Rutina publicada = rutina(2L, "Existente", LocalTime.of(8, 0), 30, Set.of((short) 2));

        assertThatCode(() -> especificacion.verificar(
                new ContextoPublicacionRutina(candidata, null, List.of(publicada))))
                .doesNotThrowAnyException();
    }

    @Test
    void aceptaFranjasContiguasQueNoSeTraslapan() {
        Rutina candidata = rutina(1L, "Nueva", LocalTime.of(8, 0), 30, Set.of((short) 1));
        Rutina publicada = rutina(2L, "Existente", LocalTime.of(8, 30), 20, Set.of((short) 1));

        assertThatCode(() -> especificacion.verificar(
                new ContextoPublicacionRutina(candidata, null, List.of(publicada))))
                .doesNotThrowAnyException();
    }

    private Rutina rutina(
            Long id, String nombre, LocalTime inicio, int duracion, Set<Short> dias) {
        Rutina rutina = mock(Rutina.class);
        when(rutina.getId()).thenReturn(id);
        when(rutina.getNombre()).thenReturn(nombre);
        when(rutina.getHoraInicio()).thenReturn(inicio);
        when(rutina.duracionTotalMinutos()).thenReturn(duracion);
        when(rutina.getDiasSemana()).thenReturn(dias);
        when(rutina.getVigenciaDesde()).thenReturn(LocalDate.of(2026, 1, 1));
        return rutina;
    }
}
