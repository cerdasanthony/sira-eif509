package cr.ac.una.sira.presentation;

import cr.ac.una.sira.business.CierreEjecucionService;
import cr.ac.una.sira.business.dto.CerrarEjecucionEntrada;
import cr.ac.una.sira.business.dto.CerrarEjecucionSalida;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ejecuciones")
public class EjecucionController {

    private final CierreEjecucionService cierreEjecucionService;

    public EjecucionController(CierreEjecucionService cierreEjecucionService) {
        this.cierreEjecucionService = cierreEjecucionService;
    }

    @PostMapping("/cierres")
    @ResponseStatus(HttpStatus.CREATED)
    public CerrarEjecucionSalida cerrar(
            @Valid @RequestBody CerrarEjecucionEntrada entrada) {
        return cierreEjecucionService.cerrar(entrada);
    }
}
