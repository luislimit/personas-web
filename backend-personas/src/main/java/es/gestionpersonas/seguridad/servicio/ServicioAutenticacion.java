package es.gestionpersonas.seguridad.servicio;

import es.gestionpersonas.auditoria.ContextoAuditoriaActual;
import es.gestionpersonas.cuenta.modelo.CuentaUsuario;
import es.gestionpersonas.cuenta.repositorio.RepositorioCuentaUsuario;
import es.gestionpersonas.comun.*;
import es.gestionpersonas.seguridad.dto.AutenticacionDtos;
import es.gestionpersonas.seguridad.modelo.SesionAutenticacion;
import es.gestionpersonas.seguridad.repositorio.RepositorioSesionAutenticacion;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

@Service
@Transactional(readOnly = true)
public class ServicioAutenticacion {
    private final RepositorioCuentaUsuario accounts;
    private final RepositorioSesionAutenticacion sessions;
    private final EntityManager em;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long sessionHours;

    public ServicioAutenticacion(RepositorioCuentaUsuario accounts,
                       RepositorioSesionAutenticacion sessions,
                       EntityManager em,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.session-hours:12}") long sessionHours) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.em = em;
        this.passwordEncoder = passwordEncoder;
        this.sessionHours = sessionHours;
    }

    @Transactional
    public AutenticacionDtos.RespuestaInicioSesion login(AutenticacionDtos.SolicitudInicioSesion input) {
        CuentaUsuario account = accounts.findByUsername(input.nombreUsuario().trim())
            .orElseThrow(() -> new ExcepcionNoAutorizado("Usuario o contraseña incorrectos"));

        if (!"ACTIVO".equals(account.getStatus())) {
            throw new ExcepcionNoAutorizado("La cuenta no está activa");
        }
        if (!"LOCAL".equalsIgnoreCase(account.getAuthProvider())) {
            throw new ExcepcionNoAutorizado("Esta cuenta utiliza un proveedor de autenticación externo");
        }
        if (!passwordEncoder.matches(input.contrasena(), account.getPasswordHash())) {
            throw new ExcepcionNoAutorizado("Usuario o contraseña incorrectos");
        }

        ContextoAuditoriaActual.establecerCuenta(account.getId());

        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = sha256(token);
        LocalDateTime expiresAt = LocalDateTime.now().withNano(0).plusHours(sessionHours);

        SesionAutenticacion session = sessions.save(new SesionAutenticacion(account.getId(), hash, expiresAt));
        account.markLogin();

        return new AutenticacionDtos.RespuestaInicioSesion(
            token, "Bearer", expiresAt, account.getId(), account.getPersonEntityId(),
            account.getUsername(), contexts(account.getId())
        );
    }

    @Transactional
    public void logout(String rawToken) {
        SesionAutenticacion session = authenticate(rawToken);
        session.revoke();
    }

    @Transactional
    public void logoutSession(Long sessionId) {
        if (sessionId == null) throw new ExcepcionNoAutorizado("Sesión no válida");
        SesionAutenticacion session = sessions.findById(sessionId)
            .orElseThrow(() -> new ExcepcionNoAutorizado("Sesión no válida"));
        session.revoke();
    }

    @Transactional
    public SesionAutenticacion authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new ExcepcionNoAutorizado("Token Bearer obligatorio");
        SesionAutenticacion session = sessions.findByTokenHash(sha256(rawToken))
            .orElseThrow(() -> new ExcepcionNoAutorizado("Sesión no válida"));

        if (!session.isActiveAt(LocalDateTime.now().withNano(0))) {
            throw new ExcepcionNoAutorizado("La sesión ha caducado o ha sido revocada");
        }

        CuentaUsuario account = accounts.findById(session.getAccountId())
            .orElseThrow(() -> new ExcepcionNoAutorizado("La cuenta de la sesión ya no existe"));
        if (!"ACTIVO".equals(account.getStatus())) {
            throw new ExcepcionNoAutorizado("La cuenta no está activa");
        }

        ContextoAuditoriaActual.establecerCuenta(account.getId());
        session.touch();
        return session;
    }

    public AutenticacionDtos.RespuestaYo me(Long accountId) {
        CuentaUsuario account = accounts.findById(accountId)
            .orElseThrow(() -> new ExcepcionNoAutorizado("Cuenta no válida"));
        return new AutenticacionDtos.RespuestaYo(
            account.getId(), account.getPersonEntityId(), account.getUsername(),
            account.getLoginEmail(), contexts(accountId)
        );
    }

    @SuppressWarnings("unchecked")
    public List<AutenticacionDtos.VistaContexto> contexts(Long accountId) {
        List<Object[]> rows = em.createNativeQuery("""
            select ac.id, ac.entidad_contexto_id, e.nombre, et.codigo,
                   r.id, r.codigo, r.nombre, ac.alcance, ac.valido_desde, ac.valido_hasta
              from contexto_cuenta ac
              join entidad e on e.id = ac.entidad_contexto_id
              join tipo_entidad et on et.id = e.tipo_entidad_id
              join rol_seguridad r on r.id = ac.rol_seguridad_id
             where ac.cuenta_id = :accountId
               and ac.habilitado = true
               and (ac.valido_desde is null or ac.valido_desde <= current_date())
               and (ac.valido_hasta is null or ac.valido_hasta >= current_date())
             order by e.nombre, r.nombre
            """)
            .setParameter("accountId", accountId)
            .getResultList();

        // contexto_cuenta sigue representando cada asignación concreta de acceso.
        // Para el selector de conexión solo devolvemos las asignaciones raíz: si una
        // asignación (Infantil A) cuelga estructuralmente de otra asignación de la
        // misma cuenta (CB Tres Cantos), queda incluida en la raíz y no aparece como
        // un segundo contexto seleccionable.
        List<Object[]> roots = rows.stream()
            .filter(candidate -> rows.stream().noneMatch(other ->
                !Objects.equals(number(other[1]), number(candidate[1]))
                    && isInStructuralTree(number(other[1]), number(candidate[1]))
            ))
            .toList();

        return roots.stream().map(r -> new AutenticacionDtos.VistaContexto(
            number(r[0]), number(r[1]), String.valueOf(r[2]), String.valueOf(r[3]),
            number(r[4]), String.valueOf(r[5]), String.valueOf(r[6]),
            r[7] == null ? null : String.valueOf(r[7]),
            toLocalDate(r[8]), toLocalDate(r[9])
        )).toList();
    }

    private boolean isInStructuralTree(Long ancestorId, Long targetId) {
        if (ancestorId == null || targetId == null) return false;
        if (Objects.equals(ancestorId, targetId)) return true;

        Number count = (Number) em.createNativeQuery("""
            WITH RECURSIVE arbol(id) AS (
                SELECT :ancestorId
                UNION DISTINCT
                SELECT re.entidad_destino_id
                  FROM relacion_entidad re
                  JOIN arbol a ON a.id = re.entidad_origen_id
                  JOIN tipo_relacion tr ON tr.id = re.tipo_relacion_id
                 WHERE re.estado = 'ACTIVO'
                   AND (re.valido_desde IS NULL OR re.valido_desde <= CURRENT_DATE())
                   AND (re.valido_hasta IS NULL OR re.valido_hasta >= CURRENT_DATE())
                   AND tr.codigo IN ('PERTENENCIA','PROPIEDAD','GESTION')
            )
            SELECT COUNT(*) FROM arbol WHERE id = :targetId
            """)
            .setParameter("ancestorId", ancestorId)
            .setParameter("targetId", targetId)
            .getSingleResult();

        return count.longValue() > 0;
    }

    public String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de sesión", e);
        }
    }

    private Long number(Object value) { return ((Number) value).longValue(); }
    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        throw new IllegalStateException("Fecha inesperada: " + value.getClass());
    }
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        throw new IllegalStateException("Fecha inesperada: " + value.getClass());
    }
}
