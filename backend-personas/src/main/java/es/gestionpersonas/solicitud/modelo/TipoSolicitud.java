package es.gestionpersonas.solicitud.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;

@Entity
@Table(name = "tipo_solicitud")
public class TipoSolicitud extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "codigo", nullable = false, unique = true, length = 60)
    private String code;
    @Column(name = "nombre", nullable = false, length = 120)
    private String name;
    @Column(name = "puede_generar_evento", nullable = false)
    private boolean canGenerateEvent;
    @Column(name = "puede_generar_documento", nullable = false)
    private boolean canGenerateDocument;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isCanGenerateEvent() { return canGenerateEvent; }
    public boolean isCanGenerateDocument() { return canGenerateDocument; }
}
