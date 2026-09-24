package cr.ac.una.sira.business;

import cr.ac.una.sira.data.Rutina;
import cr.ac.una.sira.data.Usuario;

import java.util.List;

public record ContextoPublicacionRutina(
        Rutina rutina,
        Usuario profesional,
        List<Rutina> rutinasPublicadas) {
}
