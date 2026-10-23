package cr.ac.una.sira.business;

import cr.ac.una.sira.data.RolUsuario;
import cr.ac.una.sira.data.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Transfers an already authenticated professional to MVC without sharing the JWT in a URL. */
@Service
public class AccesoAdministracionService {
    private record Entrada(Long usuarioId, Instant vence) { }
    private final Map<String, Entrada> pendientes = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final Clock reloj;

    public AccesoAdministracionService(UsuarioRepository usuarios, Clock reloj) {
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public synchronized String emitir(Long usuarioId) {
        profesional(usuarioId);
        Instant ahora = reloj.instant();
        pendientes.values().removeIf(entrada -> !entrada.vence().isAfter(ahora) || entrada.usuarioId().equals(usuarioId));
        if (pendientes.size() >= 1024) throw new AccessDeniedException("Intente ingresar de nuevo");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendientes.put(codigo, new Entrada(usuarioId, ahora.plusSeconds(60)));
        return codigo;
    }

    @Transactional(readOnly = true)
    public synchronized UserDetails consumir(String codigo) {
        Entrada entrada = codigo == null ? null : pendientes.remove(codigo);
        if (entrada == null || !entrada.vence().isAfter(reloj.instant())) {
            throw new AccessDeniedException("El acceso ha vencido o ya fue utilizado");
        }
        return profesional(entrada.usuarioId());
    }

    private UserDetails profesional(Long id) {
        var usuario = usuarios.findById(id).filter(u -> u.isActivo()
                && u.getRol() == RolUsuario.PROFESIONAL && u.getPasswordHash() != null)
                .orElseThrow(() -> new AccessDeniedException("Se requiere una cuenta profesional activa"));
        return User.withUsername(usuario.getId().toString()).password("").roles("PROFESIONAL").build();
    }
}
