package es.gestionpersonas.seguridad.modelo;
import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="politica_mfa")
public class PoliticaMfa extends EntidadAuditable {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="entidad_contexto_id") private Long entidadContextoId;
 @Column(name="rol_seguridad_id") private Long rolSeguridadId;
 @Column(name="permiso_id") private Long permisoId;
 @Column(name="minutos_validez",nullable=false) private int minutosValidez;
 @Column(name="activo",nullable=false) private boolean activo;
 @Column(name="fecha_creacion",nullable=false) private LocalDateTime fechaCreacion;
 @Column(name="fecha_modificacion",nullable=false) private LocalDateTime fechaModificacion;
 protected PoliticaMfa(){}
 public PoliticaMfa(Long c,Long r,Long p,int m,boolean a){entidadContextoId=c;rolSeguridadId=r;permisoId=p;minutosValidez=m;activo=a;fechaCreacion=LocalDateTime.now().withNano(0);fechaModificacion=fechaCreacion;}
 public void actualizar(Long c,Long r,Long p,int m,boolean a){entidadContextoId=c;rolSeguridadId=r;permisoId=p;minutosValidez=m;activo=a;fechaModificacion=LocalDateTime.now().withNano(0);}
 public Long getId(){return id;} public Long getEntidadContextoId(){return entidadContextoId;} public Long getRolSeguridadId(){return rolSeguridadId;} public Long getPermisoId(){return permisoId;} public int getMinutosValidez(){return minutosValidez;} public boolean isActivo(){return activo;} public LocalDateTime getFechaCreacion(){return fechaCreacion;} public LocalDateTime getFechaModificacion(){return fechaModificacion;}
}
