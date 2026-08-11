package cr.ac.una.sira.data;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * CAPA DE DATOS - repositorio.
 * Unico punto del sistema que sabe DE DONDE salen los datos.
 *
 * Hoy la fuente es una lista en memoria; en el Laboratorio 2 se reemplaza por
 * Spring Data JPA sobre PostgreSQL. Ese cambio no debe obligar a tocar la capa
 * de negocio ni la de presentacion: ese es justamente el valor de separar capas.
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
