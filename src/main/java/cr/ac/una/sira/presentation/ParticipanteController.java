package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.ParticipanteApiService;
import cr.ac.una.sira.business.dto.ContratoApi.*;
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
@RequestMapping("/api/v1/participantes")
@Tag(name = "Participantes")
public class ParticipanteController {
    private final ParticipanteApiService servicio;
    public ParticipanteController(ParticipanteApiService servicio) { this.servicio = servicio; }

    @GetMapping
    @Operation(summary = "Listar participantes propios; filtrar por activo y nombre")
    public Pagina<ParticipanteSalida> listar(@RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String nombre, @ParameterObject Pageable pageable) {
        return servicio.listar(activo, nombre, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un participante propio")
    public ParticipanteSalida obtener(@PathVariable Long id) { return servicio.obtener(id); }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Participante creado; Location apunta al recurso")
    @Operation(summary = "Crear un participante del profesional autenticado")
    public ResponseEntity<ParticipanteSalida> crear(@Valid @RequestBody ParticipanteEntrada entrada) {
        var salida = servicio.crear(entrada);
        return ResponseEntity.created(URI.create("/api/v1/participantes/" + salida.id())).body(salida);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un participante propio (PROFESIONAL)")
    public ParticipanteSalida actualizar(@PathVariable Long id, @Valid @RequestBody ParticipanteEntrada entrada) {
        return servicio.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Participante eliminado")
    @Operation(summary = "Eliminar participante sin rutinas; con historial devuelve 409")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
