package es.gestionpersonas.cuenta.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="cuenta_usuario")
public class CuentaUsuario extends EntidadAuditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="persona_entidad_id")
    private Long personEntityId;

    @Column(name="nombre_usuario",nullable=false,unique=true,length=150)
    private String username;

    @Column(name="correo_acceso",length=255)
    private String loginEmail;

    @Column(name="hash_contrasena",nullable=false,length=255)
    private String passwordHash;

    @Column(name="proveedor_autenticacion",nullable=false,length=50)
    private String authProvider;

    @Column(name="sujeto_externo",length=255)
    private String externalSubject;

    @Column(name="preferido",nullable=false)
    private boolean preferred;

    @Column(name="estado",nullable=false,length=30)
    private String status;

    @Column(name="ultimo_acceso")
    private LocalDateTime lastLoginAt;

    @Column(name="fecha_creacion",nullable=false)
    private LocalDateTime createdAt;

    @Column(name="fecha_modificacion",nullable=false)
    private LocalDateTime updatedAt;

    protected CuentaUsuario() {}

    public CuentaUsuario(Long personId, String username, String loginEmail, String passwordHash,
                         String provider, String external, boolean preferred, String status) {
        this.personEntityId=personId;
        this.username=username;
        this.loginEmail=loginEmail;
        this.passwordHash=passwordHash;
        this.authProvider=provider;
        this.externalSubject=external;
        this.preferred=preferred;
        this.status=status;
        this.createdAt=LocalDateTime.now().withNano(0);
        this.updatedAt=this.createdAt;
    }

    public void update(String loginEmail, String passwordHash, String provider, String external,
                       Boolean preferred, String status) {
        if(loginEmail!=null) this.loginEmail=loginEmail;
        if(passwordHash!=null) this.passwordHash=passwordHash;
        if(provider!=null) this.authProvider=provider;
        if(external!=null) this.externalSubject=external;
        if(preferred!=null) this.preferred=preferred;
        if(status!=null) this.status=status;
        this.updatedAt=LocalDateTime.now().withNano(0);
    }

    public void linkPerson(Long personEntityId) {
        this.personEntityId=personEntityId;
        this.updatedAt=LocalDateTime.now().withNano(0);
    }

    public void unlinkPerson() {
        this.personEntityId=null;
        this.preferred=false;
        this.updatedAt=LocalDateTime.now().withNano(0);
    }

    public void changePassword(String passwordHash) {
        this.passwordHash=passwordHash;
        this.updatedAt=LocalDateTime.now().withNano(0);
    }

    public void setStatus(String status) {
        this.status=status;
        if(!"ACTIVO".equals(status)) this.preferred=false;
        this.updatedAt=LocalDateTime.now().withNano(0);
    }

    public void setPreferred(boolean value) {
        preferred=value;
        updatedAt=LocalDateTime.now().withNano(0);
    }

    public void disable() { setStatus("DESHABILITADA"); }
    public void block() { setStatus("BLOQUEADA"); }
    public void activate() { setStatus("ACTIVO"); }

    public void markLogin() {
        lastLoginAt=LocalDateTime.now().withNano(0);
        updatedAt=lastLoginAt;
    }

    public Long getId(){return id;}
    public Long getPersonEntityId(){return personEntityId;}
    public String getUsername(){return username;}
    public String getLoginEmail(){return loginEmail;}
    public String getAuthProvider(){return authProvider;}
    public String getExternalSubject(){return externalSubject;}
    public boolean isPreferred(){return preferred;}
    public String getStatus(){return status;}
    public String getPasswordHash(){return passwordHash;}
    public LocalDateTime getLastLoginAt(){return lastLoginAt;}
}
