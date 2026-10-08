package cr.ac.una.sira.business;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class Paginacion {
    private Paginacion() { }

    public static Pageable validar(Pageable pageable, String... campos) {
        Set<String> permitidos = Set.of(campos);
        for (Sort.Order orden : pageable.getSort()) {
            if (!permitidos.contains(orden.getProperty())) {
                throw new IllegalArgumentException("Campo de orden no permitido: " + orden.getProperty());
            }
        }
        Sort sort = pageable.getSort().isUnsorted() ? Sort.by("id") : pageable.getSort();
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
