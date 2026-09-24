package cr.ac.una.sira.business;

import cr.ac.una.sira.business.dto.PublicarRutinaEntrada;
import cr.ac.una.sira.business.dto.PublicarRutinaSalida;
import cr.ac.una.sira.business.exception.RecursoNoEncontradoException;
import cr.ac.una.sira.data.Rutina;
import cr.ac.una.sira.data.RutinaRepository;
import cr.ac.una.sira.data.Usuario;
import cr.ac.una.sira.data.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Validated
public class PublicacionRutinaService {

    private final RutinaRepository rutinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final List<EspecificacionPublicacionRutina> especificaciones;
    private final Clock reloj;

    public PublicacionRutinaService(
            RutinaRepository rutinaRepository,
            UsuarioRepository usuarioRepository,
            List<EspecificacionPublicacionRutina> especificaciones,
            Clock reloj) {
        this.rutinaRepository = rutinaRepository;
        this.usuarioRepository = usuarioRepository;
        this.especificaciones = List.copyOf(especificaciones);
        this.reloj = reloj;
    }

    @Transactional
    public PublicarRutinaSalida publicar(@Valid PublicarRutinaEntrada entrada) {
        Rutina rutina = rutinaRepository.findById(entrada.rutinaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Rutina", entrada.rutinaId()));
        Usuario profesional = usuarioRepository.findById(entrada.profesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario", entrada.profesionalId()));
        List<Rutina> publicadas = rutinaRepository
                .buscarPublicadasConDetalleDelParticipante(
                        rutina.getParticipante().getId());

        ContextoPublicacionRutina contexto = new ContextoPublicacionRutina(
                rutina, profesional, publicadas);
        especificaciones.forEach(especificacion -> especificacion.verificar(contexto));

        int duracionTotal = rutina.duracionTotalMinutos();
        OffsetDateTime publicadaEn = OffsetDateTime.now(reloj);
        rutina.publicar(publicadaEn);
        rutinaRepository.save(rutina);

        return new PublicarRutinaSalida(
                rutina.getId(), rutina.getEstado(), duracionTotal, publicadaEn);
    }
}
