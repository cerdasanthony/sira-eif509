package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.business.dto.CerrarEjecucionEntrada;
import cr.ac.una.sira.business.dto.CerrarEjecucionSalida;
import cr.ac.una.sira.business.exception.*;
import cr.ac.una.sira.data.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EjecucionApiService {
    private final EjecucionRepository ejecuciones;
    private final RutinaApiService rutinas;
    private final AccesoRecursosService acceso;
    private final CierreEjecucionService cierre;

    public EjecucionApiService(EjecucionRepository ejecuciones, RutinaApiService rutinas,
                               AccesoRecursosService acceso, CierreEjecucionService cierre) {
        this.ejecuciones = ejecuciones;
        this.rutinas = rutinas;
        this.acceso = acceso;
        this.cierre = cierre;
    }

    public Pagina<EjecucionResumen> listar(Long rutinaId, EstadoEjecucion estado, LocalDate desde,
                                           LocalDate hasta, BigDecimal adherenciaMinima, Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("Rango de fechas invalido");
        }
        Usuario usuario = acceso.actual();
        Specification<Ejecucion> propietario = (root, query, cb) -> cb.equal(
                root.get("rutina").get("participante").get(acceso.campoPropietario(usuario)).get("id"),
                usuario.getId());
        return Pagina.desde(ejecuciones.findAll(propietario.and(EjecucionSpecifications.conFiltros(
                rutinaId, estado, desde, hasta, adherenciaMinima)),
                Paginacion.validar(pageable, "id", "fecha", "adherencia", "estado", "horaInicio"))
                .map(e -> new EjecucionResumen(e.getId(), e.getRutina().getId(), e.getFecha(),
                        e.getHoraInicio(), e.getHoraFin(), e.getAdherencia(), e.getEstado())));
    }

    public EjecucionSalida obtener(Long id) {
        Ejecucion e = ejecuciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ejecucion", id));
        acceso.verificar(e.getRutina().getParticipante());
        return new EjecucionSalida(e.getId(), e.getRutina().getId(), e.getFecha(), e.getHoraInicio(),
                e.getHoraFin(), e.getAdherencia(), e.getEstado(), e.getRegistradoPor().getId(),
                e.getRegistros().stream().map(r -> new RegistroSalida(r.getId(), r.getPasoRutina().getId(),
                        r.getResultado(), r.getObservacionCorta())).toList());
    }

    public List<RegistroSalida> registros(Long id) { return obtener(id).registros(); }

    @Transactional
    public CerrarEjecucionSalida cerrar(CierreEntrada entrada) {
        Usuario encargado = acceso.exigirRol(RolUsuario.ENCARGADO);
        rutinas.obtenerEntidad(entrada.rutinaId());
        if (ejecuciones.existsByRutinaIdAndFecha(entrada.rutinaId(), entrada.fecha())) {
            throw new ConflictoRecursoException("La rutina ya tiene una ejecucion en esa fecha");
        }
        return cierre.cerrar(new CerrarEjecucionEntrada(entrada.rutinaId(), encargado.getId(),
                entrada.fecha(), entrada.horaInicio(), entrada.horaFin(), entrada.resultados()));
    }
}
