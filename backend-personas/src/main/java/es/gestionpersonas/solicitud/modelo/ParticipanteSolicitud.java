package es.gestionpersonas.solicitud.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "participante_solicitud")
public class ParticipanteSolicitud extends EntidadAuditable {
    @EmbeddedId
    private ParticipanteSolicitudId id;

    @Column(name = "estado_participante", nullable = false, length = 30)
    private String participantStatus;

    @Column(name = "fecha_lectura")
    private LocalDate readAt;

    @Column(name = "fecha_respuesta")
    private LocalDate respondedAt;

    protected ParticipanteSolicitud() {}

    public ParticipanteSolicitud(ParticipanteSolicitudId id, String status) {
        this.id = id;
        this.participantStatus = status;
    }

    public void setStatus(String status) {
        this.participantStatus = status;
        if ("COMPLETADA".equals(status) && this.respondedAt == null) {
            this.respondedAt = LocalDate.now();
        }
    }

    public void markRead() {
        if (this.readAt == null) this.readAt = LocalDate.now();
    }

    public ParticipanteSolicitudId getId() { return id; }
    public String getParticipantStatus() { return participantStatus; }
    public LocalDate getReadAt() { return readAt; }
    public LocalDate getRespondedAt() { return respondedAt; }
}
