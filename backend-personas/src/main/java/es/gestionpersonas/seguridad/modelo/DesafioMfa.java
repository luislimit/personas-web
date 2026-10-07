package es.gestionpersonas.seguridad.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="desafio_mfa")
public class DesafioMfa extends EntidadAuditable {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="sesion_id",nullable=false) private Long sesionId;
 @Column(name="cuenta_id",nullable=false) private Long cuentaId;
 @Column(name="canal",nullable=false,length=20) private String canal;
 @Column(name="mecanismo",nullable=false,length=20) private String mecanismo;
 @Column(name="destino_enmascarado",nullable=false,length=255) private String destinoEnmascarado;
 @Column(name="hash_secreto",nullable=false,length=64) private String hashSecreto;
 @Column(name="fecha_expiracion",nullable=false) private LocalDateTime fechaExpiracion;
 @Column(name="intentos",nullable=false) private int intentos;
 @Column(name="maximo_intentos",nullable=false) private int maximoIntentos;
 @Column(name="fecha_consumo") private LocalDateTime fechaConsumo;
 @Column(name="fecha_invalidacion") private LocalDateTime fechaInvalidacion;
 @Column(name="fecha_creacion",nullable=false) private LocalDateTime fechaCreacion;
 @Column(name="fecha_modificacion",nullable=false) private LocalDateTime fechaModificacion;
 protected DesafioMfa(){}
 public DesafioMfa(Long sesionId,Long cuentaId,String canal,String mecanismo,String destinoEnmascarado,String hashSecreto,LocalDateTime fechaExpiracion,int maximoIntentos){
  this.sesionId=sesionId;this.cuentaId=cuentaId;this.canal=canal;this.mecanismo=mecanismo;this.destinoEnmascarado=destinoEnmascarado;this.hashSecreto=hashSecreto;this.fechaExpiracion=fechaExpiracion;this.maximoIntentos=maximoIntentos;this.fechaCreacion=LocalDateTime.now().withNano(0);this.fechaModificacion=this.fechaCreacion;
 }
 public boolean activa(LocalDateTime ahora){return fechaConsumo==null&&fechaInvalidacion==null&&fechaExpiracion.isAfter(ahora)&&intentos<maximoIntentos;}
 public void fallo(){intentos++;fechaModificacion=LocalDateTime.now().withNano(0);if(intentos>=maximoIntentos)fechaInvalidacion=fechaModificacion;}
 public void consumir(){fechaConsumo=LocalDateTime.now().withNano(0);fechaModificacion=fechaConsumo;}
 public void invalidar(){if(fechaInvalidacion==null&&fechaConsumo==null){fechaInvalidacion=LocalDateTime.now().withNano(0);fechaModificacion=fechaInvalidacion;}}
 public Long getId(){return id;} public Long getSesionId(){return sesionId;} public Long getCuentaId(){return cuentaId;} public String getCanal(){return canal;} public String getMecanismo(){return mecanismo;} public String getDestinoEnmascarado(){return destinoEnmascarado;} public String getHashSecreto(){return hashSecreto;} public LocalDateTime getFechaExpiracion(){return fechaExpiracion;} public int getIntentos(){return intentos;} public int getMaximoIntentos(){return maximoIntentos;} public LocalDateTime getFechaConsumo(){return fechaConsumo;}
}
