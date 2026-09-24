package cr.ac.una.sira.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ejecucion")
public class Ejecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(precision = 5, scale = 2)
    private BigDecimal adherencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEjecucion estado = EstadoEjecucion.ABIERTA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "cerrado_en")
    private OffsetDateTime cerradoEn;

    @OneToMany(mappedBy = "ejecucion", fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<RegistroPaso> registros = new ArrayList<>();

    protected Ejecucion() {
    }

    public Ejecucion(Rutina rutina, LocalDate fecha, LocalTime horaInicio,
                     Usuario registradoPor) {
        this.rutina = rutina;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.registradoPor = registradoPor;
        this.estado = EstadoEjecucion.ABIERTA;
    }

    public void cerrar(LocalTime horaFin, BigDecimal adherencia,
                       OffsetDateTime cerradoEn) {
        this.horaFin = horaFin;
        this.adherencia = adherencia;
        this.cerradoEn = cerradoEn;
        this.estado = EstadoEjecucion.CERRADA;
    }

    public Long getId() {
        return id;
    }

    public Rutina getRutina() {
        return rutina;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public BigDecimal getAdherencia() {
        return adherencia;
    }

    public EstadoEjecucion getEstado() {
        return estado;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public OffsetDateTime getCerradoEn() {
        return cerradoEn;
    }

    public List<RegistroPaso> getRegistros() {
        return registros;
    }
}
