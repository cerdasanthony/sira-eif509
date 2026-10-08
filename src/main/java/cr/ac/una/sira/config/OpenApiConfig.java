package cr.ac.una.sira.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.headers.Header;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI contrato() {
        return new OpenAPI().info(new Info().title("SIRA API REST").version("v1")
                .description("Rutinas y apoyos. Autentique con POST /auth/login y copie accessToken en Authorize. "
                        + "Colecciones paginadas (page, size, sort). Errores RFC 9457. "
                        + "PROFESIONAL gestiona sus participantes y borradores; ENCARGADO cierra sus ejecuciones."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OpenApiCustomizer erroresYLocation() {
        return api -> {
            var problema = new ObjectSchema()
                    .addProperty("type", new StringSchema().format("uri"))
                    .addProperty("title", new StringSchema())
                    .addProperty("status", new IntegerSchema())
                    .addProperty("detail", new StringSchema())
                    .addProperty("instance", new StringSchema().format("uri"))
                    .addProperty("codigo", new StringSchema())
                    .addProperty("errores", new ArraySchema().items(new ObjectSchema()
                            .addProperty("campo", new StringSchema()).addProperty("mensaje", new StringSchema())));
            api.getComponents().addSchemas("Problema", problema);
            api.getPaths().forEach((ruta, item) -> item.readOperationsMap().forEach((metodo, op) -> {
                agregarProblema(op, "401", "Credenciales o token ausentes, invalidos o expirados");
                if (!ruta.equals("/auth/login")) agregarProblema(op, "403", "Rol o propietario no autorizado");
                if (ruta.contains("{id}")) agregarProblema(op, "404", "Recurso no encontrado");
                if (metodo == PathItem.HttpMethod.GET || metodo == PathItem.HttpMethod.POST
                        || metodo == PathItem.HttpMethod.PUT) agregarProblema(op, "400", "Formato o parametros invalidos");
                if (metodo == PathItem.HttpMethod.POST || metodo == PathItem.HttpMethod.PUT) {
                    if (!ruta.equals("/auth/login")) {
                        agregarProblema(op, "404", "Recurso relacionado no encontrado");
                        agregarProblema(op, "409", "Conflicto con el estado o los datos existentes");
                        agregarProblema(op, "422", "Se incumple una regla de negocio");
                    }
                }
                if (metodo == PathItem.HttpMethod.DELETE) agregarProblema(op, "409", "El historial impide eliminar el recurso");
                var creado = op.getResponses().get("201");
                if (creado != null) creado.addHeaderObject("Location", new Header()
                        .description("URI del recurso creado").schema(new StringSchema().format("uri-reference")));
            }));
        };
    }

    private static void agregarProblema(Operation op, String codigo, String descripcion) {
        op.getResponses().addApiResponse(codigo, new ApiResponse().description(descripcion)
                .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/Problema")))));
    }
}
