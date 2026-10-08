package cr.ac.una.sira.data;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class ParticipanteSpecifications {
    private ParticipanteSpecifications() { }

    public static Specification<Participante> conFiltros(Boolean activo, String nombre) {
        return (root, query, cb) -> {
            var estado = activo == null ? cb.conjunction() : cb.equal(root.get("activo"), activo);
            // El texto se interpreta literalmente, sin comodines aportados por el cliente.
            String texto = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            return cb.and(estado, cb.like(cb.lower(root.get("nombre")), "%" + texto + "%", '\\'));
        };
    }
}
