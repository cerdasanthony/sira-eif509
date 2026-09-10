package cr.ac.una.sira.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @OneToMany(mappedBy = "profesional", fetch = FetchType.LAZY)
    private List<Participante> participantesAtendidos = new ArrayList<>();

    @OneToMany(mappedBy = "encargado", fetch = FetchType.LAZY)
    private List<Participante> participantesACargo = new ArrayList<>();

    @OneToMany(mappedBy = "registradoPor", fetch = FetchType.LAZY)
    private List<Ejecucion> ejecucionesRegistradas = new ArrayList<>();

    protected Usuario() {
    }

    public Usuario(String nombre, String correo, RolUsuario rol, boolean activo) {
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<Participante> getParticipantesAtendidos() {
        return participantesAtendidos;
    }

    public List<Participante> getParticipantesACargo() {
        return participantesACargo;
    }

    public List<Ejecucion> getEjecucionesRegistradas() {
        return ejecucionesRegistradas;
    }
}
