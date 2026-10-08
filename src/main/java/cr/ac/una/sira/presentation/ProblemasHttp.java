package cr.ac.una.sira.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class ProblemasHttp {
    private final ObjectMapper mapper;
    public ProblemasHttp(ObjectMapper mapper) { this.mapper = mapper; }

    public ProblemDetail crear(int status, String codigo, String detalle, String ruta) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detalle);
        p.setTitle(HttpStatus.valueOf(status).getReasonPhrase());
        p.setType(URI.create("urn:sira:problema:" + codigo.toLowerCase(java.util.Locale.ROOT)));
        p.setInstance(URI.create(ruta));
        p.setProperty("codigo", codigo);
        return p;
    }

    public void escribir(HttpServletRequest req, HttpServletResponse res, int status,
                         String codigo, String detalle) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        if (status == 401) res.setHeader("WWW-Authenticate", "Bearer");
        mapper.writeValue(res.getOutputStream(), crear(status, codigo, detalle, req.getRequestURI()));
    }
}
