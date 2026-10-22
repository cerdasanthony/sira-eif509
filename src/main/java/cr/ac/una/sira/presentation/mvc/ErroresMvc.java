package cr.ac.una.sira.presentation.mvc;

import cr.ac.una.sira.business.exception.RecursoNoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes = AdminController.class)
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ErroresMvc {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String inexistente(Model model) {
        model.addAttribute("detalle", "El recurso solicitado no existe.");
        return "admin/error";
    }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String prohibido(Model model) {
        model.addAttribute("detalle", "Su cuenta no tiene acceso a este recurso.");
        return "admin/error";
    }
    @ExceptionHandler(AuthenticationException.class)
    public String sesion() { return "redirect:/admin/login?error"; }
}
