package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.business.exception.*;
import cr.ac.una.sira.data.*;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UsuarioApiService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final AccesoRecursosService acceso;

    public UsuarioApiService(UsuarioRepository usuarios, PasswordEncoder passwords, AccesoRecursosService acceso) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.acceso = acceso;
    }

    public UsuarioSalida perfil() { return salida(acceso.actual()); }

    public Pagina<UsuarioSalida> listar(RolUsuario rol, Boolean activo, Pageable pageable) {
        Usuario actual = acceso.actual();
        Specification<Usuario> alcance = (root, query, cb) -> {
            query.distinct(true);
            var propio = cb.equal(root.get("id"), actual.getId());
            var visible = propio;
            if (actual.getRol() == RolUsuario.PROFESIONAL) {
                var asignados = root.join("participantesACargo", JoinType.LEFT);
                visible = cb.or(propio, cb.equal(root.get("creadoPorId"), actual.getId()),
                        cb.equal(asignados.get("profesional").get("id"), actual.getId()));
            }
            return cb.and(visible, rol == null ? cb.conjunction() : cb.equal(root.get("rol"), rol),
                    activo == null ? cb.conjunction() : cb.equal(root.get("activo"), activo));
        };
        return Pagina.desde(usuarios.findAll(alcance, Paginacion.validar(pageable,
                "id", "nombre", "correo", "rol", "activo")).map(this::salida));
    }

    @Transactional
    public void desactivarPerfil() {
        Usuario actual = acceso.actual();
        actual.desactivar();
        usuarios.saveAndFlush(actual);
    }

    @Transactional
    public UsuarioSalida actualizarPerfil(PerfilEntrada entrada) {
        Usuario usuario = acceso.actual();
        String correo = correoDisponible(entrada.correo(), usuario.getId());
        usuario.actualizarPerfil(entrada.nombre().trim(), correo);
        if (entrada.password() != null) usuario.cambiarPassword(passwords.encode(entrada.password()));
        return salida(usuarios.saveAndFlush(usuario));
    }

    @Transactional
    public UsuarioSalida crearEncargado(UsuarioEntrada entrada) {
        Usuario profesional = acceso.exigirRol(RolUsuario.PROFESIONAL);
        Usuario usuario = new Usuario(entrada.nombre().trim(), correoDisponible(entrada.correo(), null),
                RolUsuario.ENCARGADO, true);
        usuario.cambiarPassword(passwords.encode(entrada.password()));
        usuario.asignarCreador(profesional.getId());
        return salida(usuarios.saveAndFlush(usuario));
    }

    public UsuarioSalida obtener(Long id) {
        Usuario actual = acceso.actual();
        Usuario encontrado = usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if (!actual.getId().equals(id)) {
            acceso.exigirRol(RolUsuario.PROFESIONAL);
            boolean asignado = encontrado.getParticipantesACargo().stream()
                    .anyMatch(p -> p.getProfesional().getId().equals(actual.getId()));
            if (!asignado && !actual.getId().equals(encontrado.getCreadoPorId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Solo puede consultar su cuenta o encargados de sus participantes");
            }
        }
        return salida(encontrado);
    }

    private String correoDisponible(String correo, Long id) {
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        usuarios.findByCorreoIgnoreCase(normalizado).filter(u -> !u.getId().equals(id)).ifPresent(u -> {
            throw new ConflictoRecursoException("El correo ya esta registrado");
        });
        return normalizado;
    }

    private UsuarioSalida salida(Usuario u) {
        return new UsuarioSalida(u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.isActivo());
    }
}
