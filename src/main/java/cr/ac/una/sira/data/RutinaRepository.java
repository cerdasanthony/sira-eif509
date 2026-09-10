package cr.ac.una.sira.data;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RutinaRepository extends RepositorioJpaBase<Rutina, Long> {

    // Consulta de negocio JPQL 1: rutinas publicadas de un participante.
    @Query("""
            select r from Rutina r
            where r.participante.id = :participanteId
              and r.estado = cr.ac.una.sira.data.EstadoRutina.PUBLICADA
            order by r.horaInicio
            """)
    List<Rutina> buscarPublicadasDelParticipante(
            @Param("participanteId") Long participanteId);

    // Punto de partida de la demostracion N+1: carga rutinas, pero no sus pasos.
    @Query("""
            select r from Rutina r
            where r.estado = cr.ac.una.sira.data.EstadoRutina.PUBLICADA
            order by r.id
            """)
    List<Rutina> buscarPublicadasSinPasos();

    // Correccion del N+1: el EntityGraph incorpora los pasos en la consulta inicial.
    @EntityGraph(attributePaths = "pasos")
    @Query("""
            select distinct r from Rutina r
            where r.estado = cr.ac.una.sira.data.EstadoRutina.PUBLICADA
            order by r.id
            """)
    List<Rutina> buscarPublicadasConPasos();
}
