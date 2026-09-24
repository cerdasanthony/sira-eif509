package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.PublicacionRutinaInvalidaException;
import cr.ac.una.sira.data.Rutina;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Component
@Order(30)
public class RutinaSinTraslape implements EspecificacionPublicacionRutina {

    @Override
    public void verificar(ContextoPublicacionRutina contexto) {
        Rutina candidata = contexto.rutina();
        for (Rutina publicada : contexto.rutinasPublicadas()) {
            if (esLaMisma(candidata, publicada)) {
                continue;
            }
            if (compartenDia(candidata, publicada)
                    && vigenciasSeTraslapan(candidata, publicada)
                    && horasSeTraslapan(candidata, publicada)) {
                throw new PublicacionRutinaInvalidaException(
                        "La rutina se traslapa con la rutina publicada "
                                + publicada.getNombre());
            }
        }
    }

    private boolean esLaMisma(Rutina primera, Rutina segunda) {
        return primera == segunda
                || (primera.getId() != null && Objects.equals(primera.getId(), segunda.getId()));
    }

    private boolean compartenDia(Rutina primera, Rutina segunda) {
        return primera.getDiasSemana().stream().anyMatch(segunda.getDiasSemana()::contains);
    }

    private boolean vigenciasSeTraslapan(Rutina primera, Rutina segunda) {
        LocalDate finPrimera = primera.getVigenciaHasta() == null
                ? LocalDate.MAX : primera.getVigenciaHasta();
        LocalDate finSegunda = segunda.getVigenciaHasta() == null
                ? LocalDate.MAX : segunda.getVigenciaHasta();
        return !finPrimera.isBefore(segunda.getVigenciaDesde())
                && !finSegunda.isBefore(primera.getVigenciaDesde());
    }

    private boolean horasSeTraslapan(Rutina primera, Rutina segunda) {
        LocalTime finPrimera = primera.getHoraInicio()
                .plusMinutes(primera.duracionTotalMinutos());
        LocalTime finSegunda = segunda.getHoraInicio()
                .plusMinutes(segunda.duracionTotalMinutos());
        return primera.getHoraInicio().isBefore(finSegunda)
                && segunda.getHoraInicio().isBefore(finPrimera);
    }
}
