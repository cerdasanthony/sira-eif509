package cr.ac.una.sira.data;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class ParticipanteRepository {

    // Lista en memoria hasta el Lab 2, donde esto pasa a Spring Data JPA.
    private final List<Participante> datosDeEjemplo = List.of(
            new Participante(1L, "Participante A", LocalDate.of(2015, 3, 12), true),
            new Participante(2L, "Participante B", LocalDate.of(2012, 9, 4), true),
            new Participante(3L, "Participante C", LocalDate.of(2018, 1, 27), false));

    // Devuelve todos, sin filtrar. El filtro de inactivos es regla de negocio.
    public List<Participante> buscarTodos() {
        return datosDeEjemplo;
    }
}
