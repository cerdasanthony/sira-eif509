package cr.ac.una.sira.presentation;

import java.time.OffsetDateTime;

public record ErrorApi(String codigo, String mensaje, OffsetDateTime fecha) {
}
