package cr.ac.una.sira.presentation.mvc;

import cr.ac.una.sira.business.dto.ContratoApi.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public record RutinaFormulario(
        @NotNull @Positive Long participanteId,
        @NotBlank @Size(max = 120) String nombre,
        @NotNull LocalTime horaInicio,
        @NotNull LocalDate vigenciaDesde,
        LocalDate vigenciaHasta,
        @NotEmpty Set<@Min(1) @Max(7) Short> diasSemana,
        @NotBlank @Size(max = 26000) String pasosTexto) {

    public RutinaEntrada entrada() {
        var lineas = pasosTexto.lines().filter(s -> !s.isBlank()).toList();
        if (lineas.isEmpty() || lineas.size() > 100)
            throw new IllegalArgumentException("Incluya entre 1 y 100 pasos");
        List<PasoEntrada> pasos = new ArrayList<>();
        for (String linea : lineas) {
            int separador = linea.lastIndexOf('|');
            if (separador < 1) throw new IllegalArgumentException("Cada paso debe tener descripcion | minutos");
            String descripcion = linea.substring(0, separador).trim();
            short minutos;
            try { minutos = Short.parseShort(linea.substring(separador + 1).trim()); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("La duracion debe ser un numero entero"); }
            if (descripcion.isBlank() || descripcion.length() > 240 || minutos < 1 || minutos > 180)
                throw new IllegalArgumentException("Cada paso requiere descripcion (max. 240) y de 1 a 180 minutos");
            pasos.add(new PasoEntrada(descripcion, minutos, null));
        }
        return new RutinaEntrada(participanteId, nombre, horaInicio, vigenciaDesde, vigenciaHasta, diasSemana, pasos);
    }
}
