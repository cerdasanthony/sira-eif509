package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.AutenticacionService;
import cr.ac.una.sira.business.dto.ContratoApi.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacion")
public class AuthController {
    private final AutenticacionService servicio;
    public AuthController(AutenticacionService servicio) { this.servicio = servicio; }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autenticar y emitir un JWT de 15 minutos")
    public ResponseEntity<TokenSalida> login(@Valid @RequestBody LoginEntrada entrada) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(servicio.login(entrada));
    }
}
