package cr.ac.una.sira.data;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EjecucionRepository extends RepositorioJpaBase<Ejecucion, Long> {

    boolean existsByRutinaIdAndFecha(Long rutinaId, LocalDate fecha);

    // Consulta de negocio JPQL 2: historial cerrado de una rutina en un rango.
    @Query("""
            select e from Ejecucion e
            where e.rutina.id = :rutinaId
              and e.estado = cr.ac.una.sira.data.EstadoEjecucion.CERRADA
              and e.fecha between :desde and :hasta
            order by e.fecha
            """)
    List<Ejecucion> buscarCerradasDeRutinaEnRango(
            @Param("rutinaId") Long rutinaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}
