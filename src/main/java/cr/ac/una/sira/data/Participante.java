package cr.ac.una.sira.data;

import java.time.LocalDate;

/**
 * La persona que sigue las rutinas de apoyo.
 *
 * Es un record mientras no haya base de datos. En el Lab 2 tiene que volverse
 * una clase normal con @Entity, porque JPA necesita construir el objeto vacio.
 */
public record Participante(
        Long id,
        String nombre,
        LocalDate fechaNacimiento,
        boolean activo) {
}
