package cr.ac.una.sira.data;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RutinaSpecifications {

    private RutinaSpecifications() {
    }

    // Consulta dinamica 1: cada argumento opcional agrega un predicado Criteria.
    public static Specification<Rutina> conFiltros(
            Long participanteId, EstadoRutina estado, LocalDate vigenteEn) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();

            if (participanteId != null) {
                filtros.add(cb.equal(root.get("participante").get("id"), participanteId));
            }
            if (estado != null) {
                filtros.add(cb.equal(root.get("estado"), estado));
            }
            if (vigenteEn != null) {
                filtros.add(cb.lessThanOrEqualTo(
                        root.<LocalDate>get("vigenciaDesde"), vigenteEn));
                filtros.add(cb.or(
                        cb.isNull(root.get("vigenciaHasta")),
                        cb.greaterThanOrEqualTo(
                                root.<LocalDate>get("vigenciaHasta"), vigenteEn)));
            }

            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }
}
