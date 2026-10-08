package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErroresApi {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresApi.class);
    private final ProblemasHttp problemas;
    public ManejadorErroresApi(ProblemasHttp problemas) { this.problemas = problemas; }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest req) {
        return respuesta(404, "RECURSO_NO_ENCONTRADO", ex.getMessage(), req);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<ProblemDetail> negocio(ReglaNegocioException ex, HttpServletRequest req) {
        return respuesta(422, "REGLA_NEGOCIO", ex.getMessage(), req);
    }

    @ExceptionHandler({ConflictoRecursoException.class, DataIntegrityViolationException.class})
    ResponseEntity<ProblemDetail> conflicto(Exception ex, HttpServletRequest req) {
        String detalle = ex instanceof ConflictoRecursoException ? ex.getMessage()
                : "La operacion entra en conflicto con los datos existentes";
        return respuesta(409, "CONFLICTO_RECURSO", detalle, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var p = problemas.crear(400, "ENTRADA_INVALIDA", "Revise los campos de la solicitud", req.getRequestURI());
        p.setProperty("errores", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(), "mensaje",
                        e.getDefaultMessage() == null ? "Valor invalido" : e.getDefaultMessage())).toList());
        return ResponseEntity.badRequest().body(p);
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class,
            HttpMessageNotReadableException.class, TypeMismatchException.class})
    ResponseEntity<ProblemDetail> entrada(Exception ex, HttpServletRequest req) {
        return respuesta(400, "ENTRADA_INVALIDA", "Los parametros de la solicitud no son validos", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> autenticacion(AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problemas.crear(401, "CREDENCIALES_INVALIDAS", "Credenciales invalidas", req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> prohibido(AccessDeniedException ex, HttpServletRequest req) {
        return respuesta(403, "ACCESO_DENEGADO", "No tiene permiso para este recurso u operacion", req);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                    .body(problemas.crear(error.getStatusCode().value(), "SOLICITUD_INVALIDA",
                            "Solicitud no valida", req.getRequestURI()));
        }
        LOG.error("Error al procesar {}", req.getRequestURI(), ex);
        return respuesta(500, "ERROR_INTERNO", "No fue posible completar la operacion", req);
    }

    private ResponseEntity<ProblemDetail> respuesta(int status, String codigo, String detalle, HttpServletRequest req) {
        return ResponseEntity.status(status).body(problemas.crear(status, codigo, detalle, req.getRequestURI()));
    }
}
