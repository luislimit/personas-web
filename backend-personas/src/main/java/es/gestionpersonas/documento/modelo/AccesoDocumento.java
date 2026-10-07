package es.gestionpersonas.documento.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="acceso_documento")
public class AccesoDocumento extends EntidadAuditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="documento_id",nullable=false) private Long documentoId;
    @Column(name="cuenta_usuario_id",nullable=false) private Long cuentaUsuarioId;
    @Column(name="fecha_acceso",nullable=false) private LocalDateTime fechaAcceso;

    protected AccesoDocumento() {}
    public AccesoDocumento(Long documentoId, Long cuentaUsuarioId) {
        this.documentoId=documentoId;
        this.cuentaUsuarioId=cuentaUsuarioId;
        this.fechaAcceso=LocalDateTime.now().withNano(0);
    }
    public Long getId(){return id;}
    public Long getDocumentoId(){return documentoId;}
    public Long getCuentaUsuarioId(){return cuentaUsuarioId;}
    public LocalDateTime getFechaAcceso(){return fechaAcceso;}
}
