package es.gestionpersonas.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@MappedSuperclass
public abstract class EntidadAuditable {

    @Column(name = "auditoria_creado_por_cuenta_id", updatable = false)
    private Long auditoriaCreadoPorCuentaId;

    @Column(name = "auditoria_ip_creacion", updatable = false, length = 45)
    private String auditoriaIpCreacion;

    @Column(name = "auditoria_modificado_por_cuenta_id")
    private Long auditoriaModificadoPorCuentaId;

    @Column(name = "auditoria_ip_modificacion", length = 45)
    private String auditoriaIpModificacion;

    @PrePersist
    protected void auditarCreacion() {
        var datos = ContextoAuditoriaActual.get();
        if (datos == null) return;
        auditoriaCreadoPorCuentaId = datos.cuentaId();
        auditoriaIpCreacion = datos.ip();
        auditoriaModificadoPorCuentaId = datos.cuentaId();
        auditoriaIpModificacion = datos.ip();
    }

    @PreUpdate
    protected void auditarModificacion() {
        var datos = ContextoAuditoriaActual.get();
        if (datos == null) return;
        auditoriaModificadoPorCuentaId = datos.cuentaId();
        auditoriaIpModificacion = datos.ip();
    }

    public Long getAuditoriaCreadoPorCuentaId() { return auditoriaCreadoPorCuentaId; }
    public String getAuditoriaIpCreacion() { return auditoriaIpCreacion; }
    public Long getAuditoriaModificadoPorCuentaId() { return auditoriaModificadoPorCuentaId; }
    public String getAuditoriaIpModificacion() { return auditoriaIpModificacion; }
}
