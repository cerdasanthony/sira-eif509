package cr.ac.una.sira.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "participante")
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Usuario profesional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encargado_id", nullable = false)
    private Usuario encargado;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @OneToMany(mappedBy = "participante", fetch = FetchType.LAZY)
    private List<Rutina> rutinas = new ArrayList<>();

    protected Participante() {
    }

    public Participante(Usuario profesional, Usuario encargado, String nombre,
                        LocalDate fechaNacimiento, boolean activo) {
        this.profesional = profesional;
        this.encargado = encargado;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public Usuario getProfesional() {
        return profesional;
    }

    public Usuario getEncargado() {
        return encargado;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public boolean isActivo() {
        return activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<Rutina> getRutinas() {
        return rutinas;
    }
}
