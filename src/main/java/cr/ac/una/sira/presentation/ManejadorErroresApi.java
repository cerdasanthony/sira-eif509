package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cr.ac.una.sira.business.EventosSeguridad;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
public class ManejadorErroresApi {
    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresApi.class);
    private final ProblemasHttp problemas;
    private final EventosSeguridad eventos;

    public ManejadorErroresApi(ProblemasHttp problemas, EventosSeguridad eventos) {
        this.problemas = problemas;
        this.eventos = eventos;
    }

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
        if (ex instanceof ConflictoVersionException conflicto) {
            eventos.conflictoDeVersion(req.getMethod(), req.getRequestURI());
            return respuesta(409, "CONFLICTO_VERSION", conflicto.getMessage(), req);
        }
        String detalle = ex instanceof ConflictoRecursoException ? ex.getMessage()
                : "La operacion entra en conflicto con los datos existentes";
        return respuesta(409, "CONFLICTO_RECURSO", detalle, req);
    }

    /** Dos transacciones escribieron la misma fila; @Version rechazo la segunda. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> conflictoDeConcurrencia(
            OptimisticLockingFailureException ex, HttpServletRequest req) {
        eventos.conflictoDeVersion(req.getMethod(), req.getRequestURI());
        return respuesta(409, "CONFLICTO_VERSION",
                "Otra persona guardo este recurso mientras usted lo editaba."
                        + " Vuelva a abrirlo y repita el cambio.", req);
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    ResponseEntity<ProblemDetail> demasiadosIntentos(
            DemasiadosIntentosException ex, HttpServletRequest req) {
        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.getSegundosRestantes()))
                .body(problemas.crear(429, "DEMASIADOS_INTENTOS", ex.getMessage(), req.getRequestURI()));
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
        eventos.ingresoFallido(null, "credenciales invalidas", req.getRemoteAddr());
        return ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problemas.crear(401, "CREDENCIALES_INVALIDAS", "Credenciales invalidas", req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> prohibido(AccessDeniedException ex, HttpServletRequest req) {
        eventos.accesoDenegado(req.getMethod(), req.getRequestURI(), "regla de propiedad o rol");
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
