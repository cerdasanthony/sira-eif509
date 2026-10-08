package cr.ac.una.sira.config;

import cr.ac.una.sira.data.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "sira.demo.enabled", havingValue = "true")
public class CredencialesDemoConfig implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;

    public CredencialesDemoConfig(UsuarioRepository usuarios, PasswordEncoder passwords) {
        this.usuarios = usuarios;
        this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String correo : new String[]{"mariana.vargas@sira.local", "carlos.brenes@sira.local",
                "laura.mendez@sira.local", "andres.solano@sira.local"}) {
            usuarios.findByCorreoIgnoreCase(correo).filter(u -> u.getPasswordHash() == null)
                    .ifPresent(u -> u.cambiarPassword(passwords.encode("SiraDemo2026!")));
        }
    }
}
