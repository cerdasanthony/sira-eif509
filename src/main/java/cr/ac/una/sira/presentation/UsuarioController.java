package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.UsuarioApiService;
import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.data.RolUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios")
public class UsuarioController {
    private final UsuarioApiService servicio;
    public UsuarioController(UsuarioApiService servicio) { this.servicio = servicio; }

    @GetMapping
    @Operation(summary = "Listar cuentas visibles; filtrar por rol y activo")
    public Pagina<UsuarioSalida> listar(@RequestParam(required = false) RolUsuario rol,
            @RequestParam(required = false) Boolean activo, @ParameterObject Pageable pageable) {
        return servicio.listar(rol, activo, pageable);
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar el perfil propio")
    public UsuarioSalida perfil() { return servicio.perfil(); }

    @PutMapping("/me")
    @Operation(summary = "Actualizar el perfil propio; password opcional")
    public UsuarioSalida actualizar(@Valid @RequestBody PerfilEntrada entrada) {
        return servicio.actualizarPerfil(entrada);
    }

    @DeleteMapping("/me")
    @ApiResponse(responseCode = "204", description = "Cuenta propia desactivada; sus tokens dejan de permitir acceso")
    @Operation(summary = "Desactivar la cuenta propia conservando el historial")
    public ResponseEntity<Void> desactivar() {
        servicio.desactivarPerfil();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar cuenta propia o encargado asignado al profesional")
    public UsuarioSalida obtener(@PathVariable Long id) { return servicio.obtener(id); }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Encargado creado; Location apunta al recurso")
    @Operation(summary = "Crear un ENCARGADO (PROFESIONAL)")
    public ResponseEntity<UsuarioSalida> crear(@Valid @RequestBody UsuarioEntrada entrada) {
        var salida = servicio.crearEncargado(entrada);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + salida.id())).body(salida);
    }
}
