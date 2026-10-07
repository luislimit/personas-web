package es.gestionpersonas.solicitud.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "mensaje_solicitud")
public class MensajeSolicitud extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long requestId;

    @Column(name = "entidad_remitente_id", nullable = false)
    private Long senderEntityId;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "fecha_mensaje", nullable = false)
    private LocalDate messageDate;

    protected MensajeSolicitud() {}

    public MensajeSolicitud(Long requestId, Long senderEntityId, String message) {
        this.requestId = requestId;
        this.senderEntityId = senderEntityId;
        this.message = message;
        this.messageDate = LocalDate.now();
    }

    public Long getId() { return id; }
    public Long getRequestId() { return requestId; }
    public Long getSenderEntityId() { return senderEntityId; }
    public String getMessage() { return message; }
    public LocalDate getMessageDate() { return messageDate; }
}
