package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.business.exception.*;
import cr.ac.una.sira.data.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ParticipanteApiService {
    private final ParticipanteRepository participantes;
    private final UsuarioRepository usuarios;
    private final AccesoRecursosService acceso;

    public ParticipanteApiService(ParticipanteRepository participantes, UsuarioRepository usuarios,
                                  AccesoRecursosService acceso) {
        this.participantes = participantes;
        this.usuarios = usuarios;
        this.acceso = acceso;
    }

    public Pagina<ParticipanteSalida> listar(Boolean activo, String nombre, Pageable pageable) {
        Usuario usuario = acceso.actual();
        Specification<Participante> propietario = (root, query, cb) -> cb.equal(
                root.get(acceso.campoPropietario(usuario)).get("id"), usuario.getId());
        return Pagina.desde(participantes.findAll(propietario.and(
                ParticipanteSpecifications.conFiltros(activo, nombre)),
                Paginacion.validar(pageable, "id", "nombre", "fechaNacimiento", "activo"))
                .map(this::salida));
    }

    public Participante obtenerEntidad(Long id) {
        Participante participante = participantes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Participante", id));
        acceso.verificar(participante);
        return participante;
    }

    public ParticipanteSalida obtener(Long id) { return salida(obtenerEntidad(id)); }

    @Transactional
    public ParticipanteSalida crear(ParticipanteEntrada entrada) {
        Usuario profesional = acceso.exigirRol(RolUsuario.PROFESIONAL);
        return salida(participantes.saveAndFlush(new Participante(profesional,
                encargado(entrada.encargadoId()), entrada.nombre().trim(),
                entrada.fechaNacimiento(), entrada.activo())));
    }

    @Transactional
    public ParticipanteSalida actualizar(Long id, ParticipanteEntrada entrada) {
        acceso.exigirRol(RolUsuario.PROFESIONAL);
        Participante participante = obtenerEntidad(id);
        participante.actualizar(encargado(entrada.encargadoId()), entrada.nombre().trim(),
                entrada.fechaNacimiento(), entrada.activo());
        return salida(participantes.saveAndFlush(participante));
    }

    @Transactional
    public void eliminar(Long id) {
        acceso.exigirRol(RolUsuario.PROFESIONAL);
        Participante participante = obtenerEntidad(id);
        if (!participante.getRutinas().isEmpty()) {
            throw new ConflictoRecursoException("El participante tiene rutinas; puede desactivarlo con PUT");
        }
        participantes.delete(participante);
        participantes.flush();
    }

    private Usuario encargado(Long id) {
        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if (usuario.getRol() != RolUsuario.ENCARGADO || !usuario.isActivo()) {
            throw new EntradaNegocioInvalidaException("El encargado debe ser un usuario ENCARGADO activo");
        }
        return usuario;
    }

    private ParticipanteSalida salida(Participante p) {
        return new ParticipanteSalida(p.getId(), p.getNombre(), p.getFechaNacimiento(),
                p.getProfesional().getId(), p.getEncargado().getId(), p.isActivo());
    }
}
