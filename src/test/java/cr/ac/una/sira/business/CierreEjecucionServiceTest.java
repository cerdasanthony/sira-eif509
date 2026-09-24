package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.CerrarEjecucionEntrada;
import cr.ac.una.sira.business.dto.ResultadoPasoEntrada;
import cr.ac.una.sira.business.exception.CierreEjecucionInvalidoException;
import cr.ac.una.sira.business.exception.RecursoNoEncontradoException;
import cr.ac.una.sira.data.Ejecucion;
import cr.ac.una.sira.data.EjecucionRepository;
import cr.ac.una.sira.data.EstadoEjecucion;
import cr.ac.una.sira.data.EstadoRutina;
import cr.ac.una.sira.data.Participante;
import cr.ac.una.sira.data.PasoRutina;
import cr.ac.una.sira.data.RegistroPasoRepository;
import cr.ac.una.sira.data.ResultadoPaso;
import cr.ac.una.sira.data.RolUsuario;
import cr.ac.una.sira.data.Rutina;
import cr.ac.una.sira.data.RutinaRepository;
import cr.ac.una.sira.data.Usuario;
import cr.ac.una.sira.data.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CierreEjecucionServiceTest {

    private static final LocalDate FECHA_VALIDA = LocalDate.of(2026, 8, 26);

    private final RutinaRepository rutinaRepository = mock(RutinaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final EjecucionRepository ejecucionRepository = mock(EjecucionRepository.class);
    private final RegistroPasoRepository registroPasoRepository =
            mock(RegistroPasoRepository.class);
    private final Clock reloj = Clock.fixed(
            Instant.parse("2026-09-24T12:00:00Z"), ZoneId.of("America/Costa_Rica"));
    private final CierreEjecucionService servicio = new CierreEjecucionService(
            rutinaRepository, usuarioRepository, ejecucionRepository,
            registroPasoRepository, new CalculoAdherenciaPonderado(), reloj);

    private Rutina rutina;
    private Usuario encargado;
    private PasoRutina paso1;
    private PasoRutina paso2;

    @BeforeEach
    void preparar() {
        rutina = mock(Rutina.class);
        encargado = mock(Usuario.class);
        Usuario encargadoAsignado = mock(Usuario.class);
        Participante participante = mock(Participante.class);
        paso1 = paso(11L);
        paso2 = paso(12L);

        when(rutina.getId()).thenReturn(5L);
        when(rutina.getEstado()).thenReturn(EstadoRutina.PUBLICADA);
        when(rutina.getParticipante()).thenReturn(participante);
        when(participante.getEncargado()).thenReturn(encargadoAsignado);
        when(encargadoAsignado.getId()).thenReturn(3L);
        when(encargado.getId()).thenReturn(3L);
        when(encargado.getRol()).thenReturn(RolUsuario.ENCARGADO);
        when(encargado.isActivo()).thenReturn(true);
        when(rutina.getVigenciaDesde()).thenReturn(LocalDate.of(2026, 8, 1));
        when(rutina.getDiasSemana()).thenReturn(Set.of((short) 3));
        when(rutina.getPasos()).thenReturn(List.of(paso1, paso2));
        when(rutinaRepository.findById(5L)).thenReturn(Optional.of(rutina));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(encargado));
        when(ejecucionRepository.existsByRutinaIdAndFecha(5L, FECHA_VALIDA))
                .thenReturn(false);
    }

    @Test
    void guardaTodoYCierraConLaAdherenciaCalculada() {
        var salida = servicio.cerrar(entrada(
                FECHA_VALIDA, LocalTime.of(8, 0), LocalTime.of(8, 20),
                List.of(resultado(11L, ResultadoPaso.LOGRADO),
                        resultado(12L, ResultadoPaso.CON_APOYO))));

        verify(ejecucionRepository, org.mockito.Mockito.times(2))
                .saveAndFlush(any(Ejecucion.class));
        verify(registroPasoRepository).saveAllAndFlush(anyList());
        assertThat(salida.adherencia()).isEqualByComparingTo("75.00");
        assertThat(salida.estado()).isEqualTo(EstadoEjecucion.CERRADA);
        assertThat(salida.pasosRegistrados()).isEqualTo(2);
    }

    @Test
    void rechazaUnaRutinaNoPublicada() {
        when(rutina.getEstado()).thenReturn(EstadoRutina.BORRADOR);

        assertThatThrownBy(() -> servicio.cerrar(entradaValida()))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("PUBLICADA");
        verify(ejecucionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaAQuienNoEsEncargadoActivo() {
        when(encargado.getRol()).thenReturn(RolUsuario.PROFESIONAL);

        assertThatThrownBy(() -> servicio.cerrar(entradaValida()))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("encargado activo");
    }

    @Test
    void rechazaAUnEncargadoNoAsignado() {
        when(encargado.getId()).thenReturn(99L);

        assertThatThrownBy(() -> servicio.cerrar(entradaValida()))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("no esta asignado");
    }

    @Test
    void rechazaFechaFutura() {
        var entrada = entrada(
                LocalDate.of(2026, 9, 25), LocalTime.of(8, 0), LocalTime.of(8, 20),
                resultadosValidos());

        assertThatThrownBy(() -> servicio.cerrar(entrada))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("futura");
    }

    @Test
    void rechazaFechaFueraDeVigencia() {
        when(rutina.getVigenciaDesde()).thenReturn(LocalDate.of(2026, 9, 1));

        assertThatThrownBy(() -> servicio.cerrar(entradaValida()))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("vigencia");
    }

    @Test
    void rechazaUnDiaQueNoPerteneceALaRutina() {
        when(rutina.getDiasSemana()).thenReturn(Set.of((short) 1));

        assertThatThrownBy(() -> servicio.cerrar(entradaValida()))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("dia de la semana");
    }

    @Test
    void rechazaHoraFinalAnterior() {
        var entrada = entrada(
                FECHA_VALIDA, LocalTime.of(9, 0), LocalTime.of(8, 59),
                resultadosValidos());

        assertThatThrownBy(() -> servicio.cerrar(entrada))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("hora final");
    }

    @Test
    void rechazaCerrarDosVecesLaMismaFecha() {
        when(ejecucionRepository.existsByRutinaIdAndFecha(5L, FECHA_VALIDA))
                .thenReturn(true);

        assertThatThrownBy(() -> servicio.cerrar(entradaValida()))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("ya tiene una ejecucion");
    }

    @Test
    void exigeExactamenteTodosLosPasos() {
        var entrada = entrada(
                FECHA_VALIDA, LocalTime.of(8, 0), LocalTime.of(8, 20),
                List.of(resultado(11L, ResultadoPaso.LOGRADO)));

        assertThatThrownBy(() -> servicio.cerrar(entrada))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("exactamente todos");
    }

    @Test
    void rechazaUnPasoDuplicado() {
        var entrada = entrada(
                FECHA_VALIDA, LocalTime.of(8, 0), LocalTime.of(8, 20),
                List.of(resultado(11L, ResultadoPaso.LOGRADO),
                        resultado(11L, ResultadoPaso.NO_LOGRADO)));

        assertThatThrownBy(() -> servicio.cerrar(entrada))
                .isInstanceOf(CierreEjecucionInvalidoException.class)
                .hasMessageContaining("mas de una vez");
    }

    @Test
    void informaCuandoLaRutinaNoExiste() {
        when(rutinaRepository.findById(404L)).thenReturn(Optional.empty());
        var entrada = new CerrarEjecucionEntrada(
                404L, 3L, FECHA_VALIDA, LocalTime.NOON, LocalTime.of(12, 10),
                resultadosValidos());

        assertThatThrownBy(() -> servicio.cerrar(entrada))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Rutina");
    }

    private CerrarEjecucionEntrada entradaValida() {
        return entrada(
                FECHA_VALIDA, LocalTime.of(8, 0), LocalTime.of(8, 20),
                resultadosValidos());
    }

    private List<ResultadoPasoEntrada> resultadosValidos() {
        return List.of(
                resultado(11L, ResultadoPaso.LOGRADO),
                resultado(12L, ResultadoPaso.CON_APOYO));
    }

    private CerrarEjecucionEntrada entrada(
            LocalDate fecha, LocalTime inicio, LocalTime fin,
            List<ResultadoPasoEntrada> resultados) {
        return new CerrarEjecucionEntrada(5L, 3L, fecha, inicio, fin, resultados);
    }

    private ResultadoPasoEntrada resultado(Long pasoId, ResultadoPaso resultado) {
        return new ResultadoPasoEntrada(pasoId, resultado, null);
    }

    private PasoRutina paso(Long id) {
        PasoRutina paso = mock(PasoRutina.class);
        when(paso.getId()).thenReturn(id);
        return paso;
    }
}
