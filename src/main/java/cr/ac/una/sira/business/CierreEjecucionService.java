package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.CerrarEjecucionEntrada;
import cr.ac.una.sira.business.dto.CerrarEjecucionSalida;
import cr.ac.una.sira.business.dto.ResultadoPasoEntrada;
import cr.ac.una.sira.business.exception.CierreEjecucionInvalidoException;
import cr.ac.una.sira.business.exception.RecursoNoEncontradoException;
import cr.ac.una.sira.data.Ejecucion;
import cr.ac.una.sira.data.EjecucionRepository;
import cr.ac.una.sira.data.EstadoRutina;
import cr.ac.una.sira.data.PasoRutina;
import cr.ac.una.sira.data.RegistroPaso;
import cr.ac.una.sira.data.RegistroPasoRepository;
import cr.ac.una.sira.data.ResultadoPaso;
import cr.ac.una.sira.data.RolUsuario;
import cr.ac.una.sira.data.Rutina;
import cr.ac.una.sira.data.RutinaRepository;
import cr.ac.una.sira.data.Usuario;
import cr.ac.una.sira.data.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Validated
public class CierreEjecucionService {

    private final RutinaRepository rutinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EjecucionRepository ejecucionRepository;
    private final RegistroPasoRepository registroPasoRepository;
    private final CalculoAdherencia calculoAdherencia;
    private final Clock reloj;

    public CierreEjecucionService(
            RutinaRepository rutinaRepository,
            UsuarioRepository usuarioRepository,
            EjecucionRepository ejecucionRepository,
            RegistroPasoRepository registroPasoRepository,
            CalculoAdherencia calculoAdherencia,
            Clock reloj) {
        this.rutinaRepository = rutinaRepository;
        this.usuarioRepository = usuarioRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.registroPasoRepository = registroPasoRepository;
        this.calculoAdherencia = calculoAdherencia;
        this.reloj = reloj;
    }

    @Transactional
    public CerrarEjecucionSalida cerrar(@Valid CerrarEjecucionEntrada entrada) {
        Rutina rutina = rutinaRepository.findById(entrada.rutinaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Rutina", entrada.rutinaId()));
        Usuario encargado = usuarioRepository.findById(entrada.encargadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario", entrada.encargadoId()));

        validarRutinaYEncargado(rutina, encargado);
        validarFechaYHoras(rutina, entrada);
        validarQueNoExistaCierre(entrada);

        Map<Long, ResultadoPasoEntrada> resultadosPorPaso =
                validarYOrdenarResultados(rutina, entrada.resultados());
        List<ResultadoPaso> resultados = rutina.getPasos().stream()
                .map(paso -> resultadosPorPaso.get(paso.getId()).resultado())
                .toList();
        BigDecimal adherencia = calculoAdherencia.calcular(resultados);

        Ejecucion ejecucion = new Ejecucion(
                rutina, entrada.fecha(), entrada.horaInicio(), encargado);
        ejecucionRepository.saveAndFlush(ejecucion);

        List<RegistroPaso> registros = rutina.getPasos().stream()
                .map(paso -> crearRegistro(ejecucion, paso, resultadosPorPaso.get(paso.getId())))
                .toList();
        registroPasoRepository.saveAllAndFlush(registros);

        ejecucion.cerrar(
                entrada.horaFin(), adherencia, OffsetDateTime.now(reloj));
        ejecucionRepository.saveAndFlush(ejecucion);

        return new CerrarEjecucionSalida(
                ejecucion.getId(), rutina.getId(), entrada.fecha(), adherencia,
                ejecucion.getEstado(), registros.size());
    }

    private void validarRutinaYEncargado(Rutina rutina, Usuario encargado) {
        if (rutina.getEstado() != EstadoRutina.PUBLICADA) {
            throw new CierreEjecucionInvalidoException(
                    "Solo se puede cerrar una ejecucion de una rutina PUBLICADA");
        }
        if (!encargado.isActivo() || encargado.getRol() != RolUsuario.ENCARGADO) {
            throw new CierreEjecucionInvalidoException(
                    "Solo un encargado activo puede cerrar una ejecucion");
        }
        if (!Objects.equals(
                encargado.getId(), rutina.getParticipante().getEncargado().getId())) {
            throw new CierreEjecucionInvalidoException(
                    "El encargado no esta asignado al participante de la rutina");
        }
    }

    private void validarFechaYHoras(
            Rutina rutina, CerrarEjecucionEntrada entrada) {
        LocalDate fecha = entrada.fecha();
        if (fecha.isAfter(LocalDate.now(reloj))) {
            throw new CierreEjecucionInvalidoException(
                    "La fecha de ejecucion no puede ser futura");
        }
        if (fecha.isBefore(rutina.getVigenciaDesde())
                || (rutina.getVigenciaHasta() != null
                && fecha.isAfter(rutina.getVigenciaHasta()))) {
            throw new CierreEjecucionInvalidoException(
                    "La fecha esta fuera de la vigencia de la rutina");
        }
        if (!rutina.getDiasSemana().contains((short) fecha.getDayOfWeek().getValue())) {
            throw new CierreEjecucionInvalidoException(
                    "La rutina no corresponde al dia de la semana indicado");
        }
        if (entrada.horaFin().isBefore(entrada.horaInicio())) {
            throw new CierreEjecucionInvalidoException(
                    "La hora final no puede ser anterior a la hora inicial");
        }
    }

    private void validarQueNoExistaCierre(CerrarEjecucionEntrada entrada) {
        if (ejecucionRepository.existsByRutinaIdAndFecha(
                entrada.rutinaId(), entrada.fecha())) {
            throw new CierreEjecucionInvalidoException(
                    "La rutina ya tiene una ejecucion en esa fecha");
        }
    }

    private Map<Long, ResultadoPasoEntrada> validarYOrdenarResultados(
            Rutina rutina, List<ResultadoPasoEntrada> entradas) {
        Set<Long> esperados = rutina.getPasos().stream()
                .map(PasoRutina::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> recibidos = new HashSet<>();
        Map<Long, ResultadoPasoEntrada> porPaso = new HashMap<>();

        for (ResultadoPasoEntrada entrada : entradas) {
            if (!recibidos.add(entrada.pasoRutinaId())) {
                throw new CierreEjecucionInvalidoException(
                        "No se puede registrar el mismo paso mas de una vez");
            }
            porPaso.put(entrada.pasoRutinaId(), entrada);
        }
        if (!recibidos.equals(esperados)) {
            throw new CierreEjecucionInvalidoException(
                    "Deben enviarse exactamente todos los pasos de la rutina");
        }
        return porPaso;
    }

    private RegistroPaso crearRegistro(
            Ejecucion ejecucion, PasoRutina paso, ResultadoPasoEntrada entrada) {
        return new RegistroPaso(
                ejecucion, paso, entrada.resultado(), entrada.observacionCorta());
    }
}
