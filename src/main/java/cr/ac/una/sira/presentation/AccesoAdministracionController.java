package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.AccesoAdministracionService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccesoAdministracionController {
    public record AccesoSalida(String codigo) { }
    private final AccesoAdministracionService accesos;

    public AccesoAdministracionController(AccesoAdministracionService accesos) { this.accesos = accesos; }

    @PostMapping("/api/v1/acceso/administracion")
    public ResponseEntity<AccesoSalida> continuar(Authentication usuario) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new AccesoSalida(accesos.emitir(Long.valueOf(usuario.getName()))));
    }
}
