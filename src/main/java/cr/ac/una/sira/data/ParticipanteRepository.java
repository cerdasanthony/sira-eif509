package cr.ac.una.sira.data;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Devuelve todos los participantes, sin filtrar. El filtro de inactivos es una
 * regla de negocio y vive en ParticipanteService.
 *
 * La fuente es una lista en memoria hasta el Lab 2, donde esto pasa a ser
 * Spring Data JPA sobre PostgreSQL.
 */
@Repository
public class ParticipanteRepository {

    private final List<Participante> datosDeEjemplo = List.of(
            new Participante(1L, "Participante A", LocalDate.of(2015, 3, 12), true),
            new Participante(2L, "Participante B", LocalDate.of(2012, 9, 4), true),
            new Participante(3L, "Participante C", LocalDate.of(2018, 1, 27), false));

    public List<Participante> buscarTodos() {
        return datosDeEjemplo;
    }
}
