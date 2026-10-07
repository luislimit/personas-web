package es.gestionpersonas.cuenta.modelo;

import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="contexto_cuenta")
public class ContextoCuenta extends EntidadAuditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="cuenta_id",nullable=false)
    private Long accountId;

    @Column(name="entidad_contexto_id",nullable=false)
    private Long contextEntityId;

    @Column(name="rol_seguridad_id",nullable=false)
    private Long roleId;

    @Column(length=255)
    private String alcance;

    @Column(nullable=false)
    private boolean habilitado;

    @Column(name="valido_desde")
    private LocalDate validFrom;

    @Column(name="valido_hasta")
    private LocalDate validTo;

    protected ContextoCuenta() {}

    public ContextoCuenta(Long accountId, Long contextId, Long roleId, String alcance,
                          boolean habilitado, LocalDate from, LocalDate to) {
        this.accountId=accountId;
        this.contextEntityId=contextId;
        this.roleId=roleId;
        this.alcance=alcance;
        this.habilitado=habilitado;
        this.validFrom=from;
        this.validTo=to;
    }

    public void update(Long roleId, String alcance, Boolean habilitado, LocalDate from, LocalDate to) {
        if(roleId!=null) this.roleId=roleId;
        if(alcance!=null) this.alcance=alcance;
        if(habilitado!=null) this.habilitado=habilitado;
        if(from!=null) this.validFrom=from;
        if(to!=null) this.validTo=to;
    }

    public void disable() {
        this.habilitado=false;
        if(this.validTo==null) this.validTo=LocalDate.now();
    }

    public Long getId(){return id;}
    public Long getAccountId(){return accountId;}
    public Long getContextEntityId(){return contextEntityId;}
    public Long getRoleId(){return roleId;}
    public String getScope(){return alcance;}
    public boolean isEnabled(){return habilitado;}
    public LocalDate getValidFrom(){return validFrom;}
    public LocalDate getValidTo(){return validTo;}
}
