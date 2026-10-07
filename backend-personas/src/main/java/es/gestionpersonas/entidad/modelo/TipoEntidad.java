package es.gestionpersonas.entidad.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;

@Entity
@Table(name = "tipo_entidad")
public class TipoEntidad extends EntidadAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String code;
    @Column(name = "nombre", nullable = false, length = 100)
    private String name;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
