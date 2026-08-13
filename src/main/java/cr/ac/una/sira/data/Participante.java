package cr.ac.una.sira.data;

import java.time.LocalDate;

// En el Lab 2 tiene que volverse una clase con @Entity: JPA no funciona con records.
public record Participante(
        Long id,
        String nombre,
        LocalDate fechaNacimiento,
        boolean activo) {
}
