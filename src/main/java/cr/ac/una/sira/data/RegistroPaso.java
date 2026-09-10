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
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registro_paso")
public class RegistroPaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecucion_id", nullable = false)
    private Ejecucion ejecucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paso_rutina_id", nullable = false)
    private PasoRutina pasoRutina;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoPaso resultado;

    @Column(name = "observacion_corta", length = 240)
    private String observacionCorta;

    @Column(name = "registrado_en", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime registradoEn;

    protected RegistroPaso() {
    }

    public Long getId() {
        return id;
    }

    public Ejecucion getEjecucion() {
        return ejecucion;
    }

    public PasoRutina getPasoRutina() {
        return pasoRutina;
    }

    public ResultadoPaso getResultado() {
        return resultado;
    }

    public String getObservacionCorta() {
        return observacionCorta;
    }

    public OffsetDateTime getRegistradoEn() {
        return registradoEn;
    }
}
