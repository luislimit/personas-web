package es.gestionpersonas.entidad.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="entidad")
public class Entidad extends EntidadAuditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="tipo_entidad_id", nullable=false) private TipoEntidad entityType;
    @Column(name="nombre",nullable=false,length=200) private String name;
    @Column(name="estado",nullable=false,length=30) private String status;
    @Column(name="fecha_creacion",nullable=false) private LocalDateTime createdAt;
    @Column(name="fecha_modificacion",nullable=false) private LocalDateTime updatedAt;
    protected Entidad() {}
    public Entidad(TipoEntidad entityType,String name,String status){this.entityType=entityType;this.name=name;this.status=status;this.createdAt=LocalDateTime.now().withNano(0);this.updatedAt=this.createdAt;}
    public void update(String name,String status){if(name!=null)this.name=name;if(status!=null)this.status=status;this.updatedAt=LocalDateTime.now().withNano(0);}
    public Long getId(){return id;} public TipoEntidad getEntityType(){return entityType;} public String getName(){return name;} public String getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
