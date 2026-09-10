package cr.ac.una.sira.data;

import java.util.List;

public interface ParticipanteRepository extends RepositorioJpaBase<Participante, Long> {

    List<Participante> findByActivoTrueOrderByNombreAsc();
}
