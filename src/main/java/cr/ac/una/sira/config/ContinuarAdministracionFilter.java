package cr.ac.una.sira.config;

import cr.ac.una.sira.business.AccesoAdministracionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Accepts only a POST from the trusted portal with a short lived, single use grant. */
public final class ContinuarAdministracionFilter extends OncePerRequestFilter {
    private final AccesoAdministracionService accesos;
    private final Set<String> origenes;
    private final String portalUrl;
    private final HttpSessionSecurityContextRepository sesiones = new HttpSessionSecurityContextRepository();
    private final AccessDeniedHandlerImpl denegado = new AccessDeniedHandlerImpl();

    public ContinuarAdministracionFilter(AccesoAdministracionService accesos, String origenes, String portalUrl) {
        this.accesos = accesos;
        this.origenes = Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        this.portalUrl = portalUrl;
        sesiones.setDisableUrlRewriting(true);
        denegado.setErrorPage("/admin/acceso-denegado");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST")
                || !request.getRequestURI().equals(request.getContextPath() + "/admin/continuar");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        if (!origenes.contains(request.getHeader("Origin"))) {
            denegado.handle(request, response, new AccessDeniedException("Origen no autorizado"));
            return;
        }
        try {
            var usuario = accesos.consumir(request.getParameter("codigo"));
            // A fresh session also rotates the session ID and discards the previous CSRF token.
            var anterior = request.getSession(false);
            if (anterior != null) anterior.invalidate();
            var autenticacion = UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
            sesiones.saveContext(contexto, request, response);
            response.sendRedirect(request.getContextPath() + "/admin/participantes");
        } catch (AccessDeniedException ex) {
            response.sendRedirect(portalUrl + (portalUrl.contains("?") ? "&" : "?") + "acceso=expirado");
        }
    }
}
