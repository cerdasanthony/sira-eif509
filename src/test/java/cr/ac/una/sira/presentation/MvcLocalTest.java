package cr.ac.una.sira.presentation;

import cr.ac.una.sira.data.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mvc;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false", "spring.data.mongodb.repositories.enabled=false",
        "management.health.mongo.enabled=false", "sira.demo.enabled=false",
        "sira.cors.origins=https://familias.example.org"
})
@AutoConfigureMockMvc
@Transactional
@Sql("/api-datos-locales.sql")
class MvcLocalTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder passwords;

    @BeforeEach
    void credenciales() {
        for (long id : new long[]{1, 3}) {
            var u = usuarios.findById(id).orElseThrow();
            u.cambiarPassword(passwords.encode("ClaveDePrueba2026!"));
            usuarios.saveAndFlush(u);
        }
    }

    private MockHttpSession ingresar(String correo) throws Exception {
        return (MockHttpSession) mvc.perform(post("/admin/login").with(csrf())
                .param("correo", correo).param("password", "ClaveDePrueba2026!"))
                .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }

    @Test
    void loginYFormulariosIncluyenCsrfYNoSePuedeGuardarSinToken() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(post("/admin/login").param("correo", "mariana.vargas@sira.local")
                .param("password", "ClaveDePrueba2026!" )).andExpect(status().isForbidden());
        var session = ingresar("mariana.vargas@sira.local");
        mvc.perform(get("/admin/participantes/nuevo").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(post("/admin/participantes/guardar").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void vistasPaginadasReutilizanPropiedadYLaSesionNoAutenticaLaApi() throws Exception {
        var session = ingresar("mariana.vargas@sira.local");
        mvc.perform(get("/admin/participantes").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mateo")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Sofia"))));
        for (String ruta : new String[]{"/admin/encargados", "/admin/rutinas", "/admin/encargados/nuevo",
                "/admin/rutinas/nueva", "/admin/participantes/1/editar"}) {
            mvc.perform(get(ruta).session(session)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/v1/participantes").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/participantes/2/editar").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/participantes").session(ingresar("laura.mendez@sira.local")))
                .andExpect(status().isForbidden());
    }

    @Test
    void participanteValidaYGuardaConPostRedirectGet() throws Exception {
        var session = ingresar("mariana.vargas@sira.local");
        mvc.perform(post("/admin/participantes/guardar").session(session).with(csrf())
                .param("nombre", "").param("fechaNacimiento", "2016-01-01")
                .param("encargadoId", "3").param("activo", "true"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "nombre"));
        mvc.perform(post("/admin/participantes/guardar").session(session).with(csrf())
                .param("nombre", "Nuevo participante").param("fechaNacimiento", "2016-01-01")
                .param("encargadoId", "3").param("activo", "true"))
                .andExpect(redirectedUrl("/admin/participantes")).andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    void encargadoValidaCorreoYPasswordYGuardaConPrg() throws Exception {
        var session = ingresar("mariana.vargas@sira.local");
        mvc.perform(post("/admin/encargados/guardar").session(session).with(csrf())
                .param("nombre", "Persona").param("correo", "invalido").param("password", "corta"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form", "correo", "password"));
        mvc.perform(post("/admin/encargados/guardar").session(session).with(csrf())
                .param("nombre", "Persona").param("correo", "nuevo@example.org").param("password", "PasswordNuevo2026!"))
                .andExpect(redirectedUrl("/admin/encargados"));
        assertThat(usuarios.findByCorreoIgnoreCase("nuevo@example.org")).isPresent();
    }

    @Test
    void rutinaValidaPasosYGuardaBorradorSinDuplicarAlRecargar() throws Exception {
        var session = ingresar("mariana.vargas@sira.local");
        mvc.perform(post("/admin/rutinas/guardar").session(session).with(csrf())
                .param("participanteId", "1").param("nombre", "Rutina nueva").param("horaInicio", "10:00")
                .param("vigenciaDesde", "2026-10-01").param("diasSemana", "1", "2")
                .param("pasosTexto", "Paso sin duracion"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        mvc.perform(post("/admin/rutinas/guardar").session(session).with(csrf())
                .param("participanteId", "1").param("nombre", "Rutina nueva").param("horaInicio", "10:00")
                .param("vigenciaDesde", "2026-10-01").param("diasSemana", "1", "2")
                .param("pasosTexto", "Preparar los materiales | 3\nGuardar los materiales | 2"))
                .andExpect(redirectedUrl("/admin/rutinas"));
        mvc.perform(get("/admin/rutinas/10/editar").session(session)).andExpect(status().isOk());
        mvc.perform(post("/admin/rutinas/10/publicar").session(session).with(csrf()))
                .andExpect(redirectedUrl("/admin/rutinas")).andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    void corsAceptaSoloOrigenConfigurado() throws Exception {
        mvc.perform(options("/api/v1/rutinas").header("Origin", "https://familias.example.org")
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://familias.example.org"));
        mvc.perform(options("/api/v1/rutinas").header("Origin", "https://otro.example.org")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
