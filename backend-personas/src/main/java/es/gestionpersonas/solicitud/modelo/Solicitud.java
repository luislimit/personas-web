package es.gestionpersonas.solicitud.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "solicitud")
public class Solicitud extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_solicitud_id", nullable = false)
    private TipoSolicitud requestType;

    @Column(name = "asunto", nullable = false, length = 220)
    private String subject;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "creado_por_entidad_id", nullable = false)
    private Long createdByEntityId;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDate requestDate;

    @Column(name = "estado_global", nullable = false, length = 30)
    private String globalStatus;

    @Column(name = "fecha_resolucion")
    private LocalDate resolvedAt;

    @Column(name = "fecha_cierre")
    private LocalDate closedAt;

    @Column(name = "solicitud_padre_id")
    private Long parentRequestId;

    @Column(name = "evento_generado_id")
    private Long generatedEventId;

    @Column(name = "documento_generado_id")
    private Long generatedDocumentId;

    protected Solicitud() {}

    public Solicitud(TipoSolicitud type, String subject, String description, Long creator) {
        this.requestType = type;
        this.subject = subject;
        this.description = description;
        this.createdByEntityId = creator;
        this.requestDate = LocalDate.now();
        this.globalStatus = "ABIERTA";
    }

    public void update(
        String subject,
        String description,
        String status,
        Long generatedEventId,
        Long generatedDocumentId
    ) {
        if (subject != null) this.subject = subject;
        if (description != null) this.description = description;

        if (status != null) {
            this.globalStatus = status;
            if ("RESUELTA".equals(status) && this.resolvedAt == null) {
                this.resolvedAt = LocalDate.now();
            }
            if ("CERRADA".equals(status) && this.closedAt == null) {
                this.closedAt = LocalDate.now();
            }
        }

        if (generatedEventId != null) this.generatedEventId = generatedEventId;
        if (generatedDocumentId != null) this.generatedDocumentId = generatedDocumentId;
    }

    public Long getId() { return id; }
    public TipoSolicitud getRequestType() { return requestType; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public Long getCreatedByEntityId() { return createdByEntityId; }
    public LocalDate getRequestDate() { return requestDate; }
    public String getGlobalStatus() { return globalStatus; }
    public LocalDate getResolvedAt() { return resolvedAt; }
    public LocalDate getClosedAt() { return closedAt; }
    public Long getParentRequestId() { return parentRequestId; }
    public Long getGeneratedEventId() { return generatedEventId; }
    public Long getGeneratedDocumentId() { return generatedDocumentId; }
}
