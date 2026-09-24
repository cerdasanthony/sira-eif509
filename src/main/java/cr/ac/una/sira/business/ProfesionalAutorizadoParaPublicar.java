package cr.ac.una.sira.business;

import cr.ac.una.sira.business.exception.PublicacionRutinaInvalidaException;
import cr.ac.una.sira.data.RolUsuario;
import cr.ac.una.sira.data.Usuario;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Order(10)
public class ProfesionalAutorizadoParaPublicar
        implements EspecificacionPublicacionRutina {

    @Override
    public void verificar(ContextoPublicacionRutina contexto) {
        Usuario profesional = contexto.profesional();
        Usuario asignado = contexto.rutina().getParticipante().getProfesional();

        if (!profesional.isActivo() || profesional.getRol() != RolUsuario.PROFESIONAL) {
            throw new PublicacionRutinaInvalidaException(
                    "Solo un profesional activo puede publicar una rutina");
        }
        if (!Objects.equals(profesional.getId(), asignado.getId())) {
            throw new PublicacionRutinaInvalidaException(
                    "El profesional no esta asignado al participante de la rutina");
        }
    }
}
