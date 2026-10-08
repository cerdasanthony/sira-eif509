package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.business.dto.PublicarRutinaEntrada;
import cr.ac.una.sira.business.dto.PublicarRutinaSalida;
import cr.ac.una.sira.business.exception.*;
import cr.ac.una.sira.data.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class RutinaApiService {
    private final RutinaRepository rutinas;
    private final ParticipanteApiService participantes;
    private final AccesoRecursosService acceso;
    private final PublicacionRutinaService publicacion;

    public RutinaApiService(RutinaRepository rutinas, ParticipanteApiService participantes,
                            AccesoRecursosService acceso, PublicacionRutinaService publicacion) {
        this.rutinas = rutinas;
        this.participantes = participantes;
        this.acceso = acceso;
        this.publicacion = publicacion;
    }

    public Pagina<RutinaResumen> listar(Long participanteId, EstadoRutina estado,
                                        LocalDate vigenteEn, Pageable pageable) {
        Usuario usuario = acceso.actual();
        Specification<Rutina> propietario = (root, query, cb) -> cb.equal(
                root.get("participante").get(acceso.campoPropietario(usuario)).get("id"), usuario.getId());
        if (usuario.getRol() == RolUsuario.ENCARGADO) {
            propietario = propietario.and((root, query, cb) -> cb.equal(root.get("estado"), EstadoRutina.PUBLICADA));
        }
        return Pagina.desde(rutinas.findAll(propietario.and(
                RutinaSpecifications.conFiltros(participanteId, estado, vigenteEn)),
                Paginacion.validar(pageable, "id", "nombre", "horaInicio", "vigenciaDesde", "estado"))
                .map(r -> new RutinaResumen(r.getId(), r.getParticipante().getId(), r.getNombre(),
                        r.getHoraInicio(), r.getVigenciaDesde(), r.getVigenciaHasta(), r.getEstado())));
    }

    public Rutina obtenerEntidad(Long id) {
        Rutina rutina = rutinas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Rutina", id));
        acceso.verificar(rutina.getParticipante());
        if (acceso.actual().getRol() == RolUsuario.ENCARGADO && rutina.getEstado() != EstadoRutina.PUBLICADA) {
            throw new org.springframework.security.access.AccessDeniedException("La rutina no esta publicada");
        }
        return rutina;
    }

    public RutinaSalida obtener(Long id) { return salida(obtenerEntidad(id)); }
    public List<PasoSalida> pasos(Long id) { return obtener(id).pasos(); }

    @Transactional
    public RutinaSalida crear(RutinaEntrada entrada) {
        acceso.exigirRol(RolUsuario.PROFESIONAL);
        validarVigencia(entrada);
        Participante participante = participantes.obtenerEntidad(entrada.participanteId());
        if (!participante.isActivo()) throw new EntradaNegocioInvalidaException("El participante debe estar activo");
        Rutina rutina = new Rutina(participante, entrada.nombre().trim(), entrada.horaInicio(),
                entrada.vigenciaDesde(), entrada.vigenciaHasta(), entrada.diasSemana());
        agregarPasos(rutina, entrada.pasos());
        return salida(rutinas.saveAndFlush(rutina));
    }

    @Transactional
    public RutinaSalida actualizar(Long id, RutinaEntrada entrada) {
        acceso.exigirRol(RolUsuario.PROFESIONAL);
        Rutina rutina = obtenerEntidad(id);
        exigirBorrador(rutina);
        if (!rutina.getParticipante().getId().equals(entrada.participanteId())) {
            throw new ConflictoRecursoException("No se puede cambiar el participante de una rutina");
        }
        validarVigencia(entrada);
        rutina.actualizar(entrada.nombre().trim(), entrada.horaInicio(), entrada.vigenciaDesde(),
                entrada.vigenciaHasta(), entrada.diasSemana());
        // Borrar antes de insertar evita colisiones con UNIQUE(rutina_id, orden).
        rutina.getPasos().clear();
        rutinas.flush();
        agregarPasos(rutina, entrada.pasos());
        return salida(rutinas.saveAndFlush(rutina));
    }

    @Transactional
    public void eliminar(Long id) {
        acceso.exigirRol(RolUsuario.PROFESIONAL);
        Rutina rutina = obtenerEntidad(id);
        exigirBorrador(rutina);
        rutinas.delete(rutina);
        rutinas.flush();
    }

    @Transactional
    public PublicarRutinaSalida publicar(Long id) {
        Usuario profesional = acceso.exigirRol(RolUsuario.PROFESIONAL);
        Rutina rutina = obtenerEntidad(id);
        exigirBorrador(rutina);
        return publicacion.publicar(new PublicarRutinaEntrada(id, profesional.getId()));
    }

    private void exigirBorrador(Rutina rutina) {
        if (rutina.getEstado() != EstadoRutina.BORRADOR) {
            throw new ConflictoRecursoException("Solo se puede modificar, eliminar o publicar un BORRADOR");
        }
    }

    private void validarVigencia(RutinaEntrada entrada) {
        if (entrada.vigenciaHasta() != null && entrada.vigenciaDesde().isAfter(entrada.vigenciaHasta())) {
            throw new EntradaNegocioInvalidaException("La vigencia inicial no puede ser posterior a la final");
        }
    }

    private void agregarPasos(Rutina rutina, List<PasoEntrada> pasos) {
        for (int i = 0; i < pasos.size(); i++) {
            PasoEntrada p = pasos.get(i);
            rutina.agregarPaso(new PasoRutina(rutina, (short) (i + 1), p.descripcion().trim(),
                    p.duracionEstimadaMin(), p.pictograma()));
        }
    }

    private RutinaSalida salida(Rutina r) {
        return new RutinaSalida(r.getId(), r.getParticipante().getId(), r.getNombre(), r.getHoraInicio(),
                r.getVigenciaDesde(), r.getVigenciaHasta(), r.getEstado(), Set.copyOf(r.getDiasSemana()),
                r.getPasos().stream().map(p -> new PasoSalida(p.getId(), p.getOrden(), p.getDescripcion(),
                        p.getDuracionEstimadaMin(), p.getPictograma())).toList(), r.getPublicadoEn());
    }
}
