package cr.ac.una.sira.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Document(collection = "bitacora_observaciones")
public class BitacoraObservacion {

    @Id
    private String id;

    private Long ejecucionId;
    private Long participanteId;
    private Long rutinaId;
    private AutorObservacion autor;
    private Instant fecha;
    private TipoObservacion tipo;
    private String nota;
    private Set<String> etiquetas;
    private Map<String, Object> contexto;

    @Field("creadoEn")
    private Instant creadoEn;

    protected BitacoraObservacion() {
    }

    public String getId() {
        return id;
    }

    public Long getEjecucionId() {
        return ejecucionId;
    }

    public Long getParticipanteId() {
        return participanteId;
    }

    public Long getRutinaId() {
        return rutinaId;
    }

    public AutorObservacion getAutor() {
        return autor;
    }

    public Instant getFecha() {
        return fecha;
    }

    public TipoObservacion getTipo() {
        return tipo;
    }

    public String getNota() {
        return nota;
    }

    public Set<String> getEtiquetas() {
        return etiquetas;
    }

    public Map<String, Object> getContexto() {
        return contexto;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
