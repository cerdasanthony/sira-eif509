package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.data.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Service
public class AutenticacionService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final Clock reloj;
    private final String issuer;
    private final Duration duracion;
    private final String hashInexistente;

    public AutenticacionService(UsuarioRepository usuarios, PasswordEncoder passwords, JwtEncoder encoder,
                                Clock reloj, @Value("${sira.jwt.issuer}") String issuer,
                                @Value("${sira.jwt.duration}") Duration duracion) {
        this.usuarios = usuarios;
        this.passwords = passwords;
        this.encoder = encoder;
        this.reloj = reloj;
        this.issuer = issuer;
        this.duracion = duracion;
        this.hashInexistente = passwords.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenSalida login(LoginEntrada entrada) {
        var usuario = usuarios.findByCorreoIgnoreCase(entrada.correo().trim()).orElse(null);
        String hash = usuario == null || usuario.getPasswordHash() == null
                ? hashInexistente : usuario.getPasswordHash();
        boolean coincide = passwords.matches(entrada.password(), hash);
        if (!coincide || usuario == null || !usuario.isActivo() || usuario.getPasswordHash() == null) {
            throw new BadCredentialsException("Credenciales invalidas");
        }
        var ahora = reloj.instant();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(usuario.getId().toString())
                .audience(List.of("sira-api")).issuedAt(ahora).expiresAt(ahora.plus(duracion))
                .claim("roles", List.of(usuario.getRol().name())).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenSalida(token, "Bearer", duracion.toSeconds());
    }
}
