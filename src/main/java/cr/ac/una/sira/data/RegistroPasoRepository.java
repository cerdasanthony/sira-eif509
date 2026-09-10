package cr.ac.una.sira.data;

import java.util.List;

public interface RegistroPasoRepository extends RepositorioJpaBase<RegistroPaso, Long> {

    List<RegistroPaso> findByEjecucionIdOrderByIdAsc(Long ejecucionId);
}
