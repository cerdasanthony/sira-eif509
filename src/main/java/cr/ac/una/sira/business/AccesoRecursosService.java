package cr.ac.una.sira.business;

import cr.ac.una.sira.data.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Revalida la cuenta en BD y aplica propiedad en la capa de negocio. */
@Service
public class AccesoRecursosService {
    private final UsuarioRepository usuarios;

    public AccesoRecursosService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    public Usuario actual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Autenticacion requerida");
        }
        try {
            Usuario usuario = usuarios.findById(Long.valueOf(authentication.getName()))
                    .filter(Usuario::isActivo)
                    .orElseThrow(() -> new BadCredentialsException("Cuenta no disponible"));
            if (authentication.getAuthorities().stream().noneMatch(
                    a -> a.getAuthority().equals("ROLE_" + usuario.getRol().name()))) {
                throw new BadCredentialsException("El rol de la cuenta ha cambiado");
            }
            return usuario;
        } catch (NumberFormatException ex) {
            throw new BadCredentialsException("Identidad invalida");
        }
    }

    public Usuario exigirRol(RolUsuario rol) {
        Usuario usuario = actual();
        if (usuario.getRol() != rol) throw new AccessDeniedException("Rol no autorizado");
        return usuario;
    }

    public void verificar(Participante participante) {
        Usuario usuario = actual();
        Long responsable = usuario.getRol() == RolUsuario.PROFESIONAL
                ? participante.getProfesional().getId() : participante.getEncargado().getId();
        if (!Objects.equals(usuario.getId(), responsable)) {
            throw new AccessDeniedException("No tiene acceso a este participante");
        }
    }

    public String campoPropietario(Usuario usuario) {
        return usuario.getRol() == RolUsuario.PROFESIONAL ? "profesional" : "encargado";
    }
}
