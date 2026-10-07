package es.gestionpersonas.seguridad.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesion_autenticacion")
public class SesionAutenticacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "cuenta_id", nullable = false) private Long accountId;
    @Column(name = "hash_token", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "fecha_creacion", nullable = false) private LocalDateTime createdAt;
    @Column(name = "fecha_expiracion", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "ultimo_uso") private LocalDateTime lastSeenAt;
    @Column(name = "fecha_revocacion") private LocalDateTime revokedAt;
    @Column(name = "nivel_autenticacion", nullable = false, length = 20) private String authenticationLevel;
    @Column(name = "mfa_verificada_en") private LocalDateTime mfaVerifiedAt;
    @Column(name = "mfa_valida_hasta") private LocalDateTime mfaValidUntil;

    protected SesionAutenticacion() {}
    public SesionAutenticacion(Long accountId, String tokenHash, LocalDateTime expiresAt) {
        this.accountId=accountId; this.tokenHash=tokenHash; this.createdAt=LocalDateTime.now().withNano(0);
        this.lastSeenAt=this.createdAt; this.expiresAt=expiresAt; this.authenticationLevel="BASICO";
    }
    public void touch(){this.lastSeenAt=LocalDateTime.now().withNano(0);}
    public void revoke(){if(this.revokedAt==null)this.revokedAt=LocalDateTime.now().withNano(0);}
    public void reforzarMfa(LocalDateTime validaHasta){
        LocalDateTime ahora=LocalDateTime.now().withNano(0);
        this.authenticationLevel="MFA"; this.mfaVerifiedAt=ahora;
        this.mfaValidUntil=validaHasta.isAfter(expiresAt)?expiresAt:validaHasta;
    }
    public boolean mfaVigente(LocalDateTime ahora, int minutosValidez){
        if(!"MFA".equals(authenticationLevel)||mfaVerifiedAt==null||mfaValidUntil==null||!mfaValidUntil.isAfter(ahora)) return false;
        return minutosValidez<=0 || !mfaVerifiedAt.plusMinutes(minutosValidez).isBefore(ahora);
    }
    public boolean isActiveAt(LocalDateTime now){return revokedAt==null&&expiresAt.isAfter(now);}
    public Long getId(){return id;} public Long getAccountId(){return accountId;} public String getTokenHash(){return tokenHash;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getExpiresAt(){return expiresAt;}
    public LocalDateTime getLastSeenAt(){return lastSeenAt;} public LocalDateTime getRevokedAt(){return revokedAt;}
    public String getAuthenticationLevel(){return authenticationLevel;} public LocalDateTime getMfaVerifiedAt(){return mfaVerifiedAt;}
    public LocalDateTime getMfaValidUntil(){return mfaValidUntil;}
}
