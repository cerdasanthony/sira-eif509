package cr.ac.una.sira.data;

import java.time.LocalDate;

/**
 * CAPA DE DATOS - modelo.
 * Participante: la persona que sigue las rutinas de apoyo.
 *
 * En el Laboratorio 2 este tipo pasa a ser una entidad JPA persistida en
 * PostgreSQL. Por ahora es un record inmutable para que el esqueleto compile
 * y levante sin base de datos.
 */
public record Participante(
        Long id,
        String nombre,
        LocalDate fechaNacimiento,
        boolean activo) {
}
