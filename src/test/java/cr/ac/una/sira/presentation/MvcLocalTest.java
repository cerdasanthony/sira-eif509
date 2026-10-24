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
    @Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;

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

    private String accesoDesdePortal(String correo) throws Exception {
        var login = mvc.perform(post("/auth/login").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("correo", correo, "password", "ClaveDePrueba2026!"))))
                .andExpect(status().isOk()).andReturn();
        String jwt = mapper.readTree(login.getResponse().getContentAsString()).path("accessToken").asText();
        var acceso = mvc.perform(post("/api/v1/acceso/administracion").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        assertThat(acceso.getRequest().getSession(false)).isNull();
        return mapper.readTree(acceso.getResponse().getContentAsString()).path("codigo").asText();
    }

    @Test
    void ingresoDelPortalAbreLaAdministracionSinPedirLaPasswordOtraVez() throws Exception {
        String codigo = accesoDesdePortal("mariana.vargas@sira.local");
        var anterior = new MockHttpSession();
        String idAnterior = anterior.getId();
        anterior.setAttribute("sesionAnterior", "dato");
        var ingreso = mvc.perform(post("/admin/continuar").session(anterior)
                .header("Origin", "https://familias.example.org").param("codigo", codigo))
                .andExpect(redirectedUrl("/admin/participantes")).andReturn();
        var session = (MockHttpSession) ingreso.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotEqualTo(idAnterior);
        assertThat(anterior.isInvalid()).isTrue();
        assertThat(session.getAttribute("sesionAnterior")).isNull();
        mvc.perform(get("/admin/participantes").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mateo")));
        mvc.perform(get("/admin/login").session(session)).andExpect(redirectedUrl("/admin/participantes"));
        mvc.perform(post("/admin/participantes/guardar").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/participantes").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/continuar").header("Origin", "https://familias.example.org").param("codigo", codigo))
                .andExpect(redirectedUrl("http://localhost:5173?acceso=expirado"));
        mvc.perform(post("/admin/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("http://localhost:5173"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void elSaltoRechazaOtrosOrigenesSinConsumirElAcceso() throws Exception {
        String codigo = accesoDesdePortal("mariana.vargas@sira.local");
        for (String origen : new String[]{"https://malicioso.example", "null"}) {
            var intento = mvc.perform(post("/admin/continuar").header("Origin", origen).param("codigo", codigo))
                    .andExpect(status().isForbidden()).andReturn();
            assertThat(intento.getRequest().getSession(false)).isNull();
        }
        mvc.perform(post("/admin/continuar").param("codigo", codigo)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/continuar").header("Origin", "https://familias.example.org").param("codigo", codigo))
                .andExpect(redirectedUrl("/admin/participantes"));
        mvc.perform(post("/admin/continuar").header("Origin", "https://familias.example.org").param("codigo", "inventado"))
                .andExpect(redirectedUrl("http://localhost:5173?acceso=expirado"));
    }

    @Test
    void elEncargadoNoPuedeSolicitarAccesoAdministrativo() throws Exception {
        var login = mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"correo\":\"laura.mendez@sira.local\",\"password\":\"ClaveDePrueba2026!\"}"))
                .andExpect(status().isOk()).andReturn();
        String jwt = mapper.readTree(login.getResponse().getContentAsString()).path("accessToken").asText();
        mvc.perform(post("/api/v1/acceso/administracion").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/acceso/administracion")).andExpect(status().isUnauthorized());
    }

    @Test
    void elAccesoVenceYVuelveAComprobarSiLaCuentaEstaActiva() throws Exception {
        var reloj = org.mockito.Mockito.mock(java.time.Clock.class);
        var ahora = java.time.Instant.parse("2026-10-22T12:00:00Z");
        org.mockito.Mockito.when(reloj.instant()).thenReturn(ahora);
        var accesos = new cr.ac.una.sira.business.AccesoAdministracionService(usuarios, reloj);
        String codigo = accesos.emitir(1L);
        org.mockito.Mockito.when(reloj.instant()).thenReturn(ahora.plusSeconds(60));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accesos.consumir(codigo))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        String vigente = accesoDesdePortal("mariana.vargas@sira.local");
        usuarios.findById(1L).orElseThrow().desactivar();
        usuarios.flush();
        var intento = mvc.perform(post("/admin/continuar").header("Origin", "https://familias.example.org").param("codigo", vigente))
                .andExpect(redirectedUrl("http://localhost:5173?acceso=expirado")).andReturn();
        assertThat(intento.getRequest().getSession(false)).isNull();
    }

    @Test
    void loginYFormulariosIncluyenCsrfYNoSePuedeGuardarSinToken() throws Exception {
        mvc.perform(get("/admin/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        mvc.perform(post("/admin/login").param("correo", "mariana.vargas@sira.local")
                .param("password", "ClaveDePrueba2026!" )).andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/admin/acceso-denegado"));
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
                .andExpect(status().isForbidden()).andExpect(forwardedUrl("/admin/acceso-denegado"));
    }

    @Test
    void accesoDenegadoExplicaElRolYPermiteRecuperarUnFormularioVencido() throws Exception {
        mvc.perform(get("/admin/acceso-denegado"))
                .andExpect(status().isForbidden()).andExpect(view().name("admin/acceso-denegado"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Este espacio es para profesionales.")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Mateo"))));
        mvc.perform(post("/admin/acceso-denegado").with(csrf())
                .requestAttr(org.springframework.security.web.WebAttributes.ACCESS_DENIED_403,
                        new org.springframework.security.web.csrf.MissingCsrfTokenException(null)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vuelva a abrir el formulario.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no se guardó la solicitud")));
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

    @Test
    void portalPuedeComprobarSaludSinSesionNiDetallesPrivados() throws Exception {
        var response = mvc.perform(get("/actuator/health").header("Origin", "https://familias.example.org"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://familias.example.org"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist()).andReturn();
        assertThat(response.getRequest().getSession(false)).isNull();
        mvc.perform(get("/actuator/health").header("Origin", "https://otro.example.org"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
