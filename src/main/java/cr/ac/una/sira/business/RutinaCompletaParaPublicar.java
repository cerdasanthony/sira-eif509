package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.PublicacionRutinaInvalidaException;
import cr.ac.una.sira.data.EstadoRutina;
import cr.ac.una.sira.data.PasoRutina;
import cr.ac.una.sira.data.Rutina;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@Order(20)
public class RutinaCompletaParaPublicar implements EspecificacionPublicacionRutina {

    @Override
    public void verificar(ContextoPublicacionRutina contexto) {
        Rutina rutina = contexto.rutina();

        if (rutina.getEstado() != EstadoRutina.BORRADOR) {
            throw new PublicacionRutinaInvalidaException(
                    "Solo se puede publicar una rutina en estado BORRADOR");
        }
        if (!rutina.getParticipante().isActivo()) {
            throw new PublicacionRutinaInvalidaException(
                    "El participante debe estar activo");
        }
        if (rutina.getVigenciaHasta() != null
                && rutina.getVigenciaDesde().isAfter(rutina.getVigenciaHasta())) {
            throw new PublicacionRutinaInvalidaException(
                    "La vigencia inicial no puede ser posterior a la final");
        }
        if (rutina.getDiasSemana().isEmpty()) {
            throw new PublicacionRutinaInvalidaException(
                    "La rutina debe indicar al menos un dia de la semana");
        }

        List<PasoRutina> pasos = rutina.getPasos().stream()
                .sorted(Comparator.comparingInt(paso -> paso.getOrden()))
                .toList();
        if (pasos.size() < 2) {
            throw new PublicacionRutinaInvalidaException(
                    "La rutina debe contener al menos dos pasos");
        }
        for (int indice = 0; indice < pasos.size(); indice++) {
            if (pasos.get(indice).getOrden() != indice + 1) {
                throw new PublicacionRutinaInvalidaException(
                        "Los pasos deben tener orden consecutivo desde 1");
            }
        }
    }
}
