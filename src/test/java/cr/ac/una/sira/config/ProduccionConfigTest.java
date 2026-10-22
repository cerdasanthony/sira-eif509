package cr.ac.una.sira.config;

import cr.ac.una.sira.data.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProduccionConfigTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final String clave = Base64.getEncoder().encodeToString(new byte[32]);

    private ProduccionConfig config(MockEnvironment env, String secret, String origins, String password) {
        return new ProduccionConfig(usuarios, passwords, env, secret, origins, password, "PasswordEncargado2026!");
    }

    @Test
    void rechazaClaveVaciaODemasiadoCortaYOrigenesInseguros() {
        for (String secret : List.of("", Base64.getEncoder().encodeToString(new byte[12])))
            assertThatThrownBy(() -> config(new MockEnvironment(), secret, "https://spa.example.org", "PasswordProfesional2026!"))
                    .isInstanceOf(IllegalArgumentException.class);
        for (String origin : List.of("", "*", "http://localhost:5173", "https://spa.example.org/", "https://spa.example.org/ruta"))
            assertThatThrownBy(() -> config(new MockEnvironment(), clave, origin, "PasswordProfesional2026!"))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaMezclaConDemoYPasswordPublica() {
        var env = new MockEnvironment(); env.setActiveProfiles("prod", "dev");
        assertThatThrownBy(() -> config(env, clave, "https://spa.example.org", "PasswordProfesional2026!"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config(new MockEnvironment(), clave, "https://spa.example.org", "SiraDemo2026!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inicializaSoloLasCuentasSinPasswordYNoSobrescribePasswordsExistentes() {
        var profesional = new Usuario("Profesional", "mariana.vargas@sira.local", RolUsuario.PROFESIONAL, true);
        var encargado = new Usuario("Encargado", "laura.mendez@sira.local", RolUsuario.ENCARGADO, true);
        encargado.cambiarPassword("hash-existente");
        when(usuarios.findByCorreoIgnoreCase(profesional.getCorreo())).thenReturn(Optional.of(profesional));
        when(usuarios.findByCorreoIgnoreCase(encargado.getCorreo())).thenReturn(Optional.of(encargado));
        when(passwords.encode("PasswordProfesional2026!")).thenReturn("hash-nuevo");
        config(new MockEnvironment(), clave, "https://spa.example.org", "PasswordProfesional2026!").run();
        assertThat(profesional.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(encargado.getPasswordHash()).isEqualTo("hash-existente");
        verify(passwords, times(1)).encode(anyString());
    }
}
