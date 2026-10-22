package cr.ac.una.sira.business;

import cr.ac.una.sira.data.UsuarioRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La sesion MVC usa el mismo id y rol que el JWT para reutilizar los servicios. */
@Service
public class CuentasMvcService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public CuentasMvcService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        var usuario = usuarios.findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
        if (usuario.getPasswordHash() == null) throw new UsernameNotFoundException("Credenciales invalidas");
        return User.withUsername(usuario.getId().toString()).password(usuario.getPasswordHash())
                .roles(usuario.getRol().name()).disabled(!usuario.isActivo()).build();
    }
}
