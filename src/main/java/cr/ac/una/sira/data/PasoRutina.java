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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paso_rutina")
public class PasoRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @Column(nullable = false)
    private Short orden;

    @Column(nullable = false, length = 240)
    private String descripcion;

    @Column(name = "duracion_estimada_min", nullable = false)
    private Short duracionEstimadaMin;

    @Column(length = 120)
    private String pictograma;

    @OneToMany(mappedBy = "pasoRutina", fetch = FetchType.LAZY)
    private List<RegistroPaso> registros = new ArrayList<>();

    protected PasoRutina() {
    }

    public PasoRutina(Rutina rutina, Short orden, String descripcion,
                      Short duracionEstimadaMin, String pictograma) {
        this.rutina = rutina;
        this.orden = orden;
        this.descripcion = descripcion;
        this.duracionEstimadaMin = duracionEstimadaMin;
        this.pictograma = pictograma;
    }

    public Long getId() {
        return id;
    }

    public Rutina getRutina() {
        return rutina;
    }

    public Short getOrden() {
        return orden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Short getDuracionEstimadaMin() {
        return duracionEstimadaMin;
    }

    public String getPictograma() {
        return pictograma;
    }

    public List<RegistroPaso> getRegistros() {
        return registros;
    }
}
