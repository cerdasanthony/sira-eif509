package cr.ac.una.sira.data;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "rutina")
public class Rutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    private Participante participante;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRutina estado = EstadoRutina.BORRADOR;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "publicado_en")
    private OffsetDateTime publicadoEn;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "rutina_dia_semana", joinColumns = @JoinColumn(name = "rutina_id"))
    @Column(name = "dia_semana", nullable = false)
    private Set<Short> diasSemana = new HashSet<>();

    @OneToMany(mappedBy = "rutina", fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<PasoRutina> pasos = new ArrayList<>();

    @OneToMany(mappedBy = "rutina", fetch = FetchType.LAZY)
    @OrderBy("fecha ASC")
    private List<Ejecucion> ejecuciones = new ArrayList<>();

    protected Rutina() {
    }

    public void publicar(OffsetDateTime fechaPublicacion) {
        this.estado = EstadoRutina.PUBLICADA;
        this.publicadoEn = fechaPublicacion;
    }

    public int duracionTotalMinutos() {
        return pasos.stream()
                .mapToInt(paso -> paso.getDuracionEstimadaMin())
                .sum();
    }

    public Long getId() {
        return id;
    }

    public Participante getParticipante() {
        return participante;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalDate getVigenciaDesde() {
        return vigenciaDesde;
    }

    public LocalDate getVigenciaHasta() {
        return vigenciaHasta;
    }

    public EstadoRutina getEstado() {
        return estado;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getPublicadoEn() {
        return publicadoEn;
    }

    public Set<Short> getDiasSemana() {
        return diasSemana;
    }

    public List<PasoRutina> getPasos() {
        return pasos;
    }

    public List<Ejecucion> getEjecuciones() {
        return ejecuciones;
    }
}
