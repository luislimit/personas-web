package es.gestionpersonas.entidad.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*; import java.time.*;
@Entity @Table(name="relacion_entidad") public class RelacionEntidad extends EntidadAuditable {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="entidad_origen_id",nullable=false) private Long sourceEntityId; @Column(name="entidad_destino_id",nullable=false) private Long targetEntityId; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tipo_relacion_id",nullable=false) private TipoRelacion relationType; @Column(name="valido_desde") private LocalDate validFrom; @Column(name="valido_hasta") private LocalDate validTo; @Column(name="estado",nullable=false,length=30) private String status; @Column(name="fecha_creacion",nullable=false) private LocalDateTime createdAt;
 protected RelacionEntidad(){} public RelacionEntidad(Long source,Long target,TipoRelacion type,LocalDate from,LocalDate to,String status){this.sourceEntityId=source;this.targetEntityId=target;this.relationType=type;this.validFrom=from;this.validTo=to;this.status=status;this.createdAt=LocalDateTime.now().withNano(0);}
 public void update(LocalDate from,LocalDate to,String status){this.validFrom=from;this.validTo=to;if(status!=null)this.status=status;} public void end(){this.status="FINALIZADO";if(this.validTo==null)this.validTo=LocalDate.now();}
 public Long getId(){return id;} public Long getSourceEntityId(){return sourceEntityId;} public Long getTargetEntityId(){return targetEntityId;} public TipoRelacion getRelationType(){return relationType;} public LocalDate getValidFrom(){return validFrom;} public LocalDate getValidTo(){return validTo;} public String getStatus(){return status;}
}
