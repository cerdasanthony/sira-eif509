package cr.ac.una.sira.data;

import java.util.List;

public interface BitacoraObservacionRepository
        extends RepositorioMongoBase<BitacoraObservacion, String> {

    List<BitacoraObservacion> findByParticipanteIdOrderByFechaDesc(Long participanteId);

    List<BitacoraObservacion> findByEjecucionId(Long ejecucionId);
}
