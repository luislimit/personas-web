package es.gestionpersonas.entidad.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;

@Entity
@Table(name = "tipo_relacion")
public class TipoRelacion extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "codigo", nullable = false, unique = true, length = 60)
    private String code;
    @Column(name = "nombre_directo", nullable = false, length = 100)
    private String forwardName;
    @Column(name = "nombre_inverso", nullable = false, length = 100)
    private String reverseName;
    @Column(name = "simetrica", nullable = false)
    private boolean symmetric;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getForwardName() { return forwardName; }
    public String getReverseName() { return reverseName; }
    public boolean isSymmetric() { return symmetric; }
}
