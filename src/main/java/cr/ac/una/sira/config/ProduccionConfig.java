package cr.ac.una.sira.config;

import cr.ac.una.sira.data.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;

/** Falla antes de aceptar trafico si la configuracion de produccion es insegura. */
@Component
@Profile("prod")
public class ProduccionConfig implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final String profesionalPassword;
    private final String encargadoPassword;

    public ProduccionConfig(UsuarioRepository usuarios, PasswordEncoder passwords, Environment env,
            @Value("${sira.jwt.secret}") String secret, @Value("${sira.cors.origins}") String origins,
            @Value("${SIRA_PROFESIONAL_PASSWORD}") String profesionalPassword,
            @Value("${SIRA_ENCARGADO_PASSWORD}") String encargadoPassword) {
        if (secret.isBlank() || Base64.getDecoder().decode(secret).length < 32)
            throw new IllegalArgumentException("SIRA_JWT_SECRET requiere 32 bytes aleatorios en Base64");
        if (env.getProperty("sira.demo.enabled", Boolean.class, false)
                || Arrays.stream(env.getActiveProfiles()).anyMatch(p -> p.equals("dev") || p.equals("demo")))
            throw new IllegalArgumentException("No combine prod con credenciales de demostracion");
        if (origins.isBlank()) throw new IllegalArgumentException("Configure SIRA_CORS_ORIGINS");
        for (String origin : origins.split(",")) {
            URI uri = URI.create(origin.trim());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || !uri.getPath().isEmpty())
                throw new IllegalArgumentException("CORS requiere origenes HTTPS exactos, sin ruta");
        }
        validarPassword(profesionalPassword);
        validarPassword(encargadoPassword);
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.profesionalPassword = profesionalPassword;
        this.encargadoPassword = encargadoPassword;
    }

    private void validarPassword(String password) {
        int bytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (password.length() < 12 || bytes > 72 || password.equals("SiraDemo2026!"))
            throw new IllegalArgumentException("Use passwords propios de produccion de 12 a 72 bytes");
    }

    @Override
    @Transactional
    public void run(String... args) {
        inicializar("mariana.vargas@sira.local", profesionalPassword);
        inicializar("laura.mendez@sira.local", encargadoPassword);
    }

    private void inicializar(String correo, String password) {
        var usuario = usuarios.findByCorreoIgnoreCase(correo).orElseThrow();
        if (usuario.getPasswordHash() != null && passwords.matches("SiraDemo2026!", usuario.getPasswordHash()))
            throw new IllegalStateException("Una cuenta de produccion conserva una password demo; cambiela antes de desplegar");
        if (usuario.getPasswordHash() == null) usuario.cambiarPassword(passwords.encode(password));
    }
}
