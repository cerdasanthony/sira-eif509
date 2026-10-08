package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.EjecucionApiService;
import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.business.dto.CerrarEjecucionSalida;
import cr.ac.una.sira.data.EstadoEjecucion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/ejecuciones")
@Tag(name = "Ejecuciones")
public class EjecucionController {
    private final EjecucionApiService servicio;
    public EjecucionController(EjecucionApiService servicio) { this.servicio = servicio; }

    @GetMapping
    @Operation(summary = "Consultar historial propio por rutina, estado, fechas y adherencia minima")
    public Pagina<EjecucionResumen> listar(@RequestParam(required = false) Long rutinaId,
            @RequestParam(required = false) EstadoEjecucion estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) @DecimalMin("0") @DecimalMax("100") BigDecimal adherenciaMinima,
            @ParameterObject Pageable pageable) {
        return servicio.listar(rutinaId, estado, desde, hasta, adherenciaMinima, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar ejecucion propia con los resultados de sus pasos")
    public EjecucionSalida obtener(@PathVariable Long id) { return servicio.obtener(id); }

    @GetMapping("/{id}/registros")
    @Operation(summary = "Consultar resultados de una ejecucion propia")
    public List<RegistroSalida> registros(@PathVariable Long id) { return servicio.registros(id); }

    @PostMapping("/cierres")
    @ApiResponse(responseCode = "201", description = "Ejecucion cerrada; Location apunta al recurso")
    @Operation(summary = "Crear y cerrar ejecucion con todos sus resultados (ENCARGADO)")
    public ResponseEntity<CerrarEjecucionSalida> cerrar(@Valid @RequestBody CierreEntrada entrada) {
        var salida = servicio.cerrar(entrada);
        return ResponseEntity.created(URI.create("/api/v1/ejecuciones/" + salida.ejecucionId())).body(salida);
    }
}
