package es.gestionpersonas.entidad.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*; import java.time.LocalDate;
@Entity @Table(name="contacto_entidad")
public class ContactoEntidad extends EntidadAuditable {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="entidad_id",nullable=false) private Long entityId; @Column(name="tipo_contacto",nullable=false,length=50) private String contactType; @Column(name="valor_contacto",nullable=false,length=255) private String contactValue; @Column(length=100) private String etiqueta; @Column(name="preferido",nullable=false) private boolean preferred; @Column(name="activo",nullable=false) private boolean active; @Column(name="fecha_verificacion") private LocalDate verifiedAt; @Column(name="valido_desde") private LocalDate validFrom; @Column(name="valido_hasta") private LocalDate validTo;
 protected ContactoEntidad(){} public ContactoEntidad(Long entityId,String type,String value,String etiqueta,boolean preferred,boolean active,LocalDate verifiedAt,LocalDate validFrom,LocalDate validTo){this.entityId=entityId;this.contactType=type;this.contactValue=value;this.etiqueta=etiqueta;this.preferred=preferred;this.active=active;this.verifiedAt=verifiedAt;this.validFrom=validFrom;this.validTo=validTo;}
 public void update(String type,String value,String etiqueta,Boolean preferred,Boolean active,LocalDate verifiedAt,LocalDate validFrom,LocalDate validTo){if(type!=null)this.contactType=type;if(value!=null)this.contactValue=value;this.etiqueta=etiqueta;if(preferred!=null)this.preferred=preferred;if(active!=null)this.active=active;this.verifiedAt=verifiedAt;this.validFrom=validFrom;this.validTo=validTo;} public void setPreferred(boolean value){this.preferred=value;} public void deactivate(){this.active=false;this.preferred=false;if(this.validTo==null)this.validTo=LocalDate.now();}
 public Long getId(){return id;} public Long getEntityId(){return entityId;} public String getContactType(){return contactType;} public String getContactValue(){return contactValue;} public String getLabel(){return etiqueta;} public boolean isPreferred(){return preferred;} public boolean isActive(){return active;} public LocalDate getVerifiedAt(){return verifiedAt;} public LocalDate getValidFrom(){return validFrom;} public LocalDate getValidTo(){return validTo;}
}
