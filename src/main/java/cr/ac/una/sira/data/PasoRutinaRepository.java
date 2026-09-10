package cr.ac.una.sira.data;

import java.util.List;

public interface PasoRutinaRepository extends RepositorioJpaBase<PasoRutina, Long> {

    List<PasoRutina> findByRutinaIdOrderByOrdenAsc(Long rutinaId);
}
