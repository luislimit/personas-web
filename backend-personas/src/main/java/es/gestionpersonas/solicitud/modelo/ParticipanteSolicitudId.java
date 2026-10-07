package es.gestionpersonas.solicitud.modelo;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ParticipanteSolicitudId implements Serializable {
    @Column(name = "solicitud_id")
    private Long requestId;
    @Column(name = "entidad_id")
    private Long entityId;
    @Column(name = "rol_participante", length = 50)
    private String role;

    protected ParticipanteSolicitudId() {}
    public ParticipanteSolicitudId(Long requestId, Long entityId, String role) {
        this.requestId = requestId;
        this.entityId = entityId;
        this.role = role;
    }
    public Long getRequestId() { return requestId; }
    public Long getEntityId() { return entityId; }
    public String getRole() { return role; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticipanteSolicitudId that)) return false;
        return Objects.equals(requestId, that.requestId)
            && Objects.equals(entityId, that.entityId)
            && Objects.equals(role, that.role);
    }
    @Override public int hashCode() { return Objects.hash(requestId, entityId, role); }
}
