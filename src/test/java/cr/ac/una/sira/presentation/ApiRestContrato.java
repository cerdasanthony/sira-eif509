package cr.ac.una.sira.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cr.ac.una.sira.data.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contrato compartido: filtros JWT reales, MVC y servicios sin mocks. */
@AutoConfigureMockMvc
@Transactional
abstract class ApiRestContrato {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired JwtEncoder tokens;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    String profesional;
    String encargado;

    @BeforeEach
    void autenticar() throws Exception {
        for (long id = 1; id <= 4; id++) {
            var u = usuarios.findById(id).orElseThrow();
            if (u.getPasswordHash() == null) {
                u.cambiarPassword(passwords.encode("SiraDemo2026!"));
                usuarios.saveAndFlush(u);
            }
        }
        profesional = login("mariana.vargas@sira.local");
        encargado = login("laura.mendez@sira.local");
    }

    @Test
    void loginEmiteTokenYNoCreaSesionNiExponePassword() throws Exception {
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", profesional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("PROFESIONAL"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("correo", "mariana.vargas@sira.local", "password", "SiraDemo2026!"))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void sinTokenYTokenAlteradoDevuelven401ProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/participantes"))
                .andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/v1/rutinas").header("Authorization", profesional + "corrupto"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenExpiradoEmisorOAudienciaIncorrectosDevuelven401() throws Exception {
        var ahora = java.time.Instant.now();
        for (JwtClaimsSet claims : new JwtClaimsSet[]{
                JwtClaimsSet.builder().issuer("sira").subject("1").audience(java.util.List.of("sira-api"))
                        .issuedAt(ahora.minusSeconds(600)).expiresAt(ahora.minusSeconds(300))
                        .claim("roles", java.util.List.of("PROFESIONAL")).build(),
                JwtClaimsSet.builder().issuer("otro-emisor").subject("1").audience(java.util.List.of("sira-api"))
                        .issuedAt(ahora).expiresAt(ahora.plusSeconds(300)).claim("roles", java.util.List.of("PROFESIONAL")).build(),
                JwtClaimsSet.builder().issuer("sira").subject("1").audience(java.util.List.of("otra-api"))
                        .issuedAt(ahora).expiresAt(ahora.plusSeconds(300)).claim("roles", java.util.List.of("PROFESIONAL")).build()}) {
            String token = tokens.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                    .getTokenValue();
            mvc.perform(get("/api/v1/participantes").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        }
    }

    @Test
    void tokenSinAudienciaOSinExpiracionDevuelve401() throws Exception {
        var ahora = java.time.Instant.now();
        for (JwtClaimsSet claims : new JwtClaimsSet[]{
                JwtClaimsSet.builder().issuer("sira").subject("1")
                        .issuedAt(ahora).expiresAt(ahora.plusSeconds(300))
                        .claim("roles", java.util.List.of("PROFESIONAL")).build(),
                JwtClaimsSet.builder().issuer("sira").subject("1").audience(java.util.List.of("sira-api"))
                        .issuedAt(ahora).claim("roles", java.util.List.of("PROFESIONAL")).build()}) {
            String token = tokens.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                    .getTokenValue();
            mvc.perform(get("/api/v1/participantes").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        }
    }

    @Test
    void credencialesIncorrectasYCuentaSinCredencialesDevuelven401() throws Exception {
        for (String correo : new String[]{"mariana.vargas@sira.local", "inexistente@sira.local"}) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("correo", correo, "password", "incorrecto"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Credenciales invalidas"));
        }
        jdbc.update("UPDATE usuario SET password_hash = NULL WHERE id = 1");
        entityManager.clear();
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("correo", "mariana.vargas@sira.local", "password", "SiraDemo2026!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolesYPropiedadImpidenAccesoHorizontal403() throws Exception {
        mvc.perform(post("/api/v1/participantes").header("Authorization", encargado)
                .contentType(MediaType.APPLICATION_JSON).content(participante("Nuevo", 3)))
                .andExpect(status().isForbidden());
        for (String ruta : new String[]{"/api/v1/participantes/2", "/api/v1/rutinas/3", "/api/v1/ejecuciones/4"}) {
            mvc.perform(get(ruta).header("Authorization", profesional))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
            mvc.perform(get(ruta).header("Authorization", encargado)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/rutinas/3/publicaciones").header("Authorization", profesional))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/ejecuciones/cierres").header("Authorization", encargado)
                .contentType(MediaType.APPLICATION_JSON).content(cierre(3)))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearConsultarActualizarYEliminarParticipante201Location200204() throws Exception {
        var creado = mvc.perform(post("/api/v1/participantes").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(participante("Nuevo", 3)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.profesionalId").value(1)).andReturn();
        String location = creado.getResponse().getHeader("Location");
        assertThat(location).startsWith("/api/v1/participantes/");
        mvc.perform(get(location).header("Authorization", profesional)).andExpect(status().isOk());
        mvc.perform(put(location).header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(participante("Actualizado", 4)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.encargadoId").value(4));
        mvc.perform(delete(location).header("Authorization", profesional)).andExpect(status().isNoContent());
        mvc.perform(get(location).header("Authorization", profesional)).andExpect(status().isNotFound());
    }

    @Test
    void dtoInvalidoJsonRotoEnumIncorrectoYCamposDesconocidosDevuelven400() throws Exception {
        for (String cuerpo : new String[]{"{}", "{", participante("", 3),
                participante("Nuevo", 3).replace("\"activo\":true", "\"activo\":true,\"profesionalId\":2")}) {
            mvc.perform(post("/api/v1/participantes").header("Authorization", profesional)
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
        mvc.perform(get("/api/v1/rutinas?estado=INVALIDO").header("Authorization", profesional))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/ejecuciones?adherenciaMinima=101").header("Authorization", profesional))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recursosInexistentesDevuelven404YNoFiltranEntidades() throws Exception {
        for (String recurso : new String[]{"participantes", "rutinas", "ejecuciones", "usuarios"}) {
            mvc.perform(get("/api/v1/" + recurso + "/999999").header("Authorization", profesional))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.instance").value("/api/v1/" + recurso + "/999999"));
        }
        mvc.perform(post("/api/v1/participantes").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(participante("Nuevo", 999999)))
                .andExpect(status().isNotFound());
    }

    @Test
    void metodoNoPermitidoConservaAllowYTipoDeContenidoIncorrectoDevuelve415() throws Exception {
        mvc.perform(patch("/api/v1/participantes/1").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("GET")))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(405));
        mvc.perform(post("/api/v1/participantes").header("Authorization", profesional)
                .contentType(MediaType.TEXT_PLAIN).content("entrada"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void reglasDeNegocioDevuelven422YRestriccionesDeHistorial409() throws Exception {
        mvc.perform(post("/api/v1/participantes").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(participante("Nuevo", 1)))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.status").value(422));
        mvc.perform(delete("/api/v1/participantes/1").header("Authorization", profesional))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/rutinas/1").header("Authorization", profesional)).andExpect(status().isConflict());
    }

    @Test
    void paginacionOrdenYFiltrosSeAplicanEnBdConAlcanceDelPropietario() throws Exception {
        mvc.perform(get("/api/v1/participantes?activo=true&nombre=Mateo&size=1&sort=nombre,desc")
                .header("Authorization", profesional)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.tamano").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(1));
        mvc.perform(get("/api/v1/rutinas?participanteId=1&estado=PUBLICADA&vigenteEn=2026-08-24&size=1&sort=horaInicio,desc")
                .header("Authorization", profesional)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2)).andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido[0].id").value(2));
        mvc.perform(get("/api/v1/rutinas?page=1&size=1&sort=horaInicio,desc").header("Authorization", profesional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].id").value(1));
        mvc.perform(get("/api/v1/rutinas?participanteId=2").header("Authorization", profesional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/v1/ejecuciones?rutinaId=1&desde=2026-08-24&hasta=2026-08-25&adherenciaMinima=80")
                .header("Authorization", encargado)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.contenido[0].id").value(1));
        mvc.perform(get("/api/v1/participantes?nombre=%25").header("Authorization", profesional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void ordenNoPermitidoYRangoInvalidoDevuelven400() throws Exception {
        for (String ruta : new String[]{"/api/v1/rutinas?sort=passwordHash", "/api/v1/participantes?sort=noExiste",
                "/api/v1/ejecuciones?desde=2026-09-01&hasta=2026-08-01"}) {
            mvc.perform(get(ruta).header("Authorization", profesional)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void rutinaBorradorSeCreaActualizaPublicaYCongelaPasos() throws Exception {
        String ruta = crearRutina(2);
        mvc.perform(get(ruta + "/pasos").header("Authorization", profesional))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].orden").value(1));
        mvc.perform(put(ruta).header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(rutina(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pasos.length()").value(2));
        mvc.perform(get(ruta).header("Authorization", encargado)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/rutinas?estado=BORRADOR").header("Authorization", encargado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(post(ruta + "/publicaciones").header("Authorization", profesional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.duracionTotalMinutos").value(10));
        mvc.perform(get(ruta).header("Authorization", encargado)).andExpect(status().isOk());
        mvc.perform(put(ruta).header("Authorization", profesional).contentType(MediaType.APPLICATION_JSON).content(rutina(2)))
                .andExpect(status().isConflict());
        mvc.perform(post(ruta + "/publicaciones").header("Authorization", profesional)).andExpect(status().isConflict());
    }

    @Test
    void publicarRutinaIncompleta422YEliminarBorrador204() throws Exception {
        String ruta = crearRutina(1);
        mvc.perform(post(ruta + "/publicaciones").header("Authorization", profesional))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(delete(ruta).header("Authorization", profesional)).andExpect(status().isNoContent());
        mvc.perform(get(ruta).header("Authorization", profesional)).andExpect(status().isNotFound());
    }

    @Test
    void crearRutinaConVigenciaInvertidaOInactivo422YCambiarParticipante409() throws Exception {
        mvc.perform(post("/api/v1/rutinas").header("Authorization", profesional).contentType(MediaType.APPLICATION_JSON)
                .content(rutina(2).replace("\"vigenciaHasta\":null", "\"vigenciaHasta\":\"2026-07-01\"")))
                .andExpect(status().isUnprocessableEntity());
        String ruta = crearRutina(2);
        mvc.perform(put(ruta).header("Authorization", profesional).contentType(MediaType.APPLICATION_JSON)
                .content(rutina(2).replace("\"participanteId\":1", "\"participanteId\":2")))
                .andExpect(status().isConflict());
        jdbc.update("UPDATE participante SET activo=false WHERE id=1");
        entityManager.clear();
        mvc.perform(post("/api/v1/rutinas").header("Authorization", profesional).contentType(MediaType.APPLICATION_JSON)
                .content(rutina(2))).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cerrarEjecucion201LocationAdherenciaYRegistrosYRepetir409() throws Exception {
        var resultado = mvc.perform(post("/api/v1/ejecuciones/cierres").header("Authorization", encargado)
                .contentType(MediaType.APPLICATION_JSON).content(cierre(1)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.adherencia").value(62.5))
                .andExpect(jsonPath("$.pasosRegistrados").value(4)).andReturn();
        String ruta = resultado.getResponse().getHeader("Location");
        assertThat(ruta).startsWith("/api/v1/ejecuciones/");
        mvc.perform(get(ruta).header("Authorization", profesional)).andExpect(status().isOk())
                .andExpect(jsonPath("$.registradoPorId").value(3)).andExpect(jsonPath("$.registros.length()").value(4));
        mvc.perform(get(ruta + "/registros").header("Authorization", encargado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(post("/api/v1/ejecuciones/cierres").header("Authorization", encargado)
                .contentType(MediaType.APPLICATION_JSON).content(cierre(1))).andExpect(status().isConflict());
    }

    @Test
    void cierreIncompleto422NoPersisteDatosYNoAceptaIdentidadEnCuerpo400() throws Exception {
        mvc.perform(post("/api/v1/ejecuciones/cierres").header("Authorization", encargado)
                .contentType(MediaType.APPLICATION_JSON).content(cierre(1).replace("\"pasoRutinaId\":4", "\"pasoRutinaId\":99")))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ejecucion WHERE rutina_id=1 AND fecha='2026-08-26'", Integer.class)).isZero();
        mvc.perform(post("/api/v1/ejecuciones/cierres").header("Authorization", encargado)
                .contentType(MediaType.APPLICATION_JSON).content(cierre(1).replace("\"rutinaId\":1", "\"rutinaId\":1,\"encargadoId\":4")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ejecuciones/cierres").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(cierre(1))).andExpect(status().isForbidden());
    }

    @Test
    void crearEncargado201PasswordHasheadoYCorreoDuplicado409() throws Exception {
        String cuerpo = json(Map.of("nombre", "Nuevo Encargado", "correo", "nuevo@sira.local", "password", "Password2026!"));
        var creado = mvc.perform(post("/api/v1/usuarios").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("ENCARGADO"))
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        assertThat(creado.getResponse().getHeader("Location")).startsWith("/api/v1/usuarios/");
        mvc.perform(get(creado.getResponse().getHeader("Location")).header("Authorization", profesional))
                .andExpect(status().isOk());
        assertThat(usuarios.findByCorreoIgnoreCase("nuevo@sira.local").orElseThrow().getPasswordHash()).startsWith("$2a$");
        mvc.perform(post("/api/v1/usuarios").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo.replace("nuevo@sira.local", "NUEVO@sira.local")))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarPerfilPropioYConsultarEncargadoAsignado() throws Exception {
        mvc.perform(put("/api/v1/usuarios/me").header("Authorization", encargado).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre", "Laura", "correo", "laura.nuevo@sira.local", "password", "PasswordNueva2026!"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Laura"));
        mvc.perform(get("/api/v1/usuarios/3").header("Authorization", profesional)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/usuarios/4").header("Authorization", profesional)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios/1").header("Authorization", encargado)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/usuarios/me").header("Authorization", profesional).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre", "Mariana", "correo", "mariana.vargas@sira.local"))))
                .andExpect(status().isOk());
    }

    @Test
    void cuentaDesactivadaORolCambiadoInvalidaAccesoAunqueJwtNoHayaExpirado() throws Exception {
        jdbc.update("UPDATE usuario SET activo=false WHERE id=1");
        entityManager.clear();
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", profesional)).andExpect(status().isUnauthorized());
        jdbc.update("UPDATE usuario SET activo=true, rol='ENCARGADO' WHERE id=1");
        entityManager.clear();
        mvc.perform(get("/api/v1/usuarios/me").header("Authorization", profesional)).andExpect(status().isUnauthorized());
    }

    @Test
    void listadoUsuariosRespetaPropiedadYDesactivarPerfil204ImpideReutilizarToken() throws Exception {
        mvc.perform(get("/api/v1/usuarios?rol=ENCARGADO&activo=true&sort=nombre")
                .header("Authorization", profesional)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1)).andExpect(jsonPath("$.contenido[0].id").value(3));
        mvc.perform(get("/api/v1/usuarios").header("Authorization", encargado))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(delete("/api/v1/usuarios/me").header("Authorization", encargado)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/participantes").header("Authorization", encargado)).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerYOpenApiPublicosDocumentanContratoYBearer() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/ejecuciones/cierres'].post.responses['201']").exists())
                .andExpect(result -> {
                    JsonNode security = mapper.readTree(result.getResponse().getContentAsString())
                            .path("paths").path("/auth/login").path("post").path("security");
                    assertThat(security.isMissingNode() || security.isEmpty()).isTrue();
                });
    }

    private String login(String correo) throws Exception {
        var resultado = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("correo", correo, "password", "SiraDemo2026!"))))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + mapper.readTree(resultado.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String json(Object objeto) throws Exception { return mapper.writeValueAsString(objeto); }

    private String participante(String nombre, long encargadoId) throws Exception {
        return json(Map.of("nombre", nombre, "fechaNacimiento", "2016-04-18", "encargadoId", encargadoId, "activo", true));
    }

    private String rutina(int pasos) {
        return """
                {"participanteId":1,"nombre":"Rutina nocturna","horaInicio":"21:00:00",
                 "vigenciaDesde":"2026-08-01","vigenciaHasta":null,"diasSemana":[1,2,3,4,5],"pasos":[
                 {"descripcion":"Preparar pijama","duracionEstimadaMin":5,"pictograma":"pijama"}
                """ + (pasos == 2 ? ",{\"descripcion\":\"Guardar ropa\",\"duracionEstimadaMin\":5}" : "") + "]}";
    }

    private String crearRutina(int pasos) throws Exception {
        return mvc.perform(post("/api/v1/rutinas").header("Authorization", profesional)
                .contentType(MediaType.APPLICATION_JSON).content(rutina(pasos)))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
    }

    private String cierre(long rutinaId) {
        return """
                {"rutinaId":%d,"fecha":"2026-08-26","horaInicio":"06:30:00","horaFin":"06:50:00",
                 "resultados":[{"pasoRutinaId":1,"resultado":"LOGRADO"},
                 {"pasoRutinaId":2,"resultado":"LOGRADO"},
                 {"pasoRutinaId":3,"resultado":"CON_APOYO"},
                 {"pasoRutinaId":4,"resultado":"NO_LOGRADO"}]}
                """.formatted(rutinaId);
    }
}
