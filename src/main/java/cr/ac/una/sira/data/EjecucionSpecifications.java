package cr.ac.una.sira.data;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class EjecucionSpecifications {

    private EjecucionSpecifications() {
    }

    // Consulta dinamica 2: permite combinar rutina, estado, fechas y adherencia.
    public static Specification<Ejecucion> conFiltros(
            Long rutinaId,
            EstadoEjecucion estado,
            LocalDate desde,
            LocalDate hasta,
            BigDecimal adherenciaMinima) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();

            if (rutinaId != null) {
                filtros.add(cb.equal(root.get("rutina").get("id"), rutinaId));
            }
            if (estado != null) {
                filtros.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                filtros.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("fecha"), desde));
            }
            if (hasta != null) {
                filtros.add(cb.lessThanOrEqualTo(root.<LocalDate>get("fecha"), hasta));
            }
            if (adherenciaMinima != null) {
                filtros.add(cb.greaterThanOrEqualTo(
                        root.<BigDecimal>get("adherencia"), adherenciaMinima));
            }

            return cb.and(filtros.toArray(Predicate[]::new));
        };
    }
}
