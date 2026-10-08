package cr.ac.una.sira.business.dto;

import cr.ac.una.sira.data.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Set;

/** DTOs del contrato v1. Nunca serializa entidades ni credenciales almacenadas. */
public final class ContratoApi {
    private ContratoApi() { }

    public record LoginEntrada(
            @NotBlank @Email @Size(max = 160) String correo,
            @NotBlank @Size(max = 72) @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password) { }

    public record TokenSalida(String accessToken, String tokenType, long expiresIn) { }

    public record UsuarioEntrada(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Email @Size(max = 160) String correo,
            @NotBlank @Size(min = 12, max = 72)
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password) { }

    public record PerfilEntrada(
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Email @Size(max = 160) String correo,
            @Size(min = 12, max = 72)
            @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password) { }

    public record UsuarioSalida(Long id, String nombre, String correo, RolUsuario rol, boolean activo) { }

    public record ParticipanteEntrada(
            @NotBlank @Size(max = 120) String nombre,
            @NotNull @PastOrPresent LocalDate fechaNacimiento,
            @NotNull @Positive Long encargadoId,
            @NotNull Boolean activo) { }

    public record ParticipanteSalida(Long id, String nombre, LocalDate fechaNacimiento,
                                     Long profesionalId, Long encargadoId, boolean activo) { }

    public record PasoEntrada(
            @NotBlank @Size(max = 240) String descripcion,
            @NotNull @Min(1) @Max(180) Short duracionEstimadaMin,
            @Size(max = 120) String pictograma) { }

    public record PasoSalida(Long id, short orden, String descripcion,
                             short duracionEstimadaMin, String pictograma) { }

    public record RutinaEntrada(
            @NotNull @Positive Long participanteId,
            @NotBlank @Size(max = 120) String nombre,
            @NotNull LocalTime horaInicio,
            @NotNull LocalDate vigenciaDesde,
            LocalDate vigenciaHasta,
            @NotNull @Size(max = 7) Set<@NotNull @Min(1) @Max(7) Short> diasSemana,
            @NotNull @Size(max = 100) List<@NotNull @Valid PasoEntrada> pasos) { }

    public record RutinaResumen(Long id, Long participanteId, String nombre, LocalTime horaInicio,
                                LocalDate vigenciaDesde, LocalDate vigenciaHasta, EstadoRutina estado) { }

    public record RutinaSalida(Long id, Long participanteId, String nombre, LocalTime horaInicio,
                               LocalDate vigenciaDesde, LocalDate vigenciaHasta, EstadoRutina estado,
                               Set<Short> diasSemana, List<PasoSalida> pasos, OffsetDateTime publicadoEn) { }

    // La identidad del encargado procede del JWT, nunca del cuerpo recibido.
    public record CierreEntrada(
            @NotNull @Positive Long rutinaId,
            @NotNull @PastOrPresent LocalDate fecha,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFin,
            @NotEmpty List<@NotNull @Valid ResultadoPasoEntrada> resultados) { }

    public record RegistroSalida(Long id, Long pasoRutinaId, ResultadoPaso resultado,
                                 String observacionCorta) { }

    public record EjecucionResumen(Long id, Long rutinaId, LocalDate fecha, LocalTime horaInicio,
                                   LocalTime horaFin, BigDecimal adherencia, EstadoEjecucion estado) { }

    public record EjecucionSalida(Long id, Long rutinaId, LocalDate fecha, LocalTime horaInicio,
                                  LocalTime horaFin, BigDecimal adherencia, EstadoEjecucion estado,
                                  Long registradoPorId, List<RegistroSalida> registros) { }

    public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos,
                             int totalPaginas, boolean primera, boolean ultima) {
        public static <T> Pagina<T> desde(Page<T> page) {
            return new Pagina<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
        }
    }
}
