package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.exception.RecursoNoEncontradoException;
import cr.ac.una.sira.business.exception.ReglaNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class ManejadorErroresApi {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorApi recursoNoEncontrado(RecursoNoEncontradoException excepcion) {
        return error("RECURSO_NO_ENCONTRADO", excepcion.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorApi reglaDeNegocio(ReglaNegocioException excepcion) {
        return error("REGLA_NEGOCIO", excepcion.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorApi entradaInvalida(Exception excepcion) {
        return error("ENTRADA_INVALIDA", excepcion.getMessage());
    }

    private ErrorApi error(String codigo, String mensaje) {
        return new ErrorApi(codigo, mensaje, OffsetDateTime.now());
    }
}
