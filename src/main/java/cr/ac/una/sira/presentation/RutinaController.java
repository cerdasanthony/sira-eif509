package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.RutinaApiService;
import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.business.dto.PublicarRutinaSalida;
import cr.ac.una.sira.data.EstadoRutina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rutinas")
@Tag(name = "Rutinas")
public class RutinaController {
    private final RutinaApiService servicio;
    public RutinaController(RutinaApiService servicio) { this.servicio = servicio; }

    @GetMapping
    @Operation(summary = "Listar rutinas propias; filtrar participante, estado y vigencia")
    public Pagina<RutinaResumen> listar(@RequestParam(required = false) Long participanteId,
            @RequestParam(required = false) EstadoRutina estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vigenteEn,
            @ParameterObject Pageable pageable) {
        return servicio.listar(participanteId, estado, vigenteEn, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar rutina propia con sus pasos")
    public RutinaSalida obtener(@PathVariable Long id) { return servicio.obtener(id); }

    @GetMapping("/{id}/pasos")
    @Operation(summary = "Consultar pasos de una rutina propia")
    public List<PasoSalida> pasos(@PathVariable Long id) { return servicio.pasos(id); }

    @PostMapping
    @ApiResponse(responseCode = "201", description = "Borrador creado; Location apunta al recurso")
    @Operation(summary = "Crear borrador con pasos ordenados (PROFESIONAL)")
    public ResponseEntity<RutinaSalida> crear(@Valid @RequestBody RutinaEntrada entrada) {
        var salida = servicio.crear(entrada);
        return ResponseEntity.created(URI.create("/api/v1/rutinas/" + salida.id())).body(salida);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reemplazar datos y pasos de un borrador propio (PROFESIONAL)")
    public RutinaSalida actualizar(@PathVariable Long id, @Valid @RequestBody RutinaEntrada entrada) {
        return servicio.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Borrador eliminado")
    @Operation(summary = "Eliminar borrador propio (PROFESIONAL)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publicaciones")
    @Operation(summary = "Publicar borrador propio tras validar reglas del Lab 4 (PROFESIONAL)")
    public PublicarRutinaSalida publicar(@PathVariable Long id) { return servicio.publicar(id); }
}
