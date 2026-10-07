package es.gestionpersonas.cuenta.servicio;

import es.gestionpersonas.cuenta.dto.CuentaDtos;
import es.gestionpersonas.cuenta.modelo.*;
import es.gestionpersonas.cuenta.repositorio.*;
import es.gestionpersonas.comun.*;
import es.gestionpersonas.entidad.repositorio.RepositorioEntidad;
import es.gestionpersonas.seguridad.modelo.SesionAutenticacion;
import es.gestionpersonas.seguridad.repositorio.RepositorioSesionAutenticacion;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.servicio.ServicioPoliticaMfa;
import es.gestionpersonas.seguridad.web.AccesoActual;
import es.gestionpersonas.seguridad.web.ContextoAccesoActual;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class ServicioCuenta {
    private final RepositorioCuentaUsuario accounts;
    private final RepositorioContextoCuenta contexts;
    private final RepositorioEntidad entities;
    private final RepositorioSesionAutenticacion sessions;
    private final ServicioControlAcceso controlAcceso;
    private final ServicioPoliticaMfa politicaMfa;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager em;

    public ServicioCuenta(
        RepositorioCuentaUsuario accounts,
        RepositorioContextoCuenta contexts,
        RepositorioEntidad entities,
        RepositorioSesionAutenticacion sessions,
        ServicioControlAcceso controlAcceso,
        ServicioPoliticaMfa politicaMfa,
        PasswordEncoder passwordEncoder,
        EntityManager em
    ) {
        this.accounts=accounts;
        this.contexts=contexts;
        this.entities=entities;
        this.sessions=sessions;
        this.controlAcceso=controlAcceso;
        this.politicaMfa=politicaMfa;
        this.passwordEncoder=passwordEncoder;
        this.em=em;
    }

    public List<CuentaDtos.Cuenta> search(String texto, String estado, Long personaEntidadId) {
        AccesoActual actor=ContextoAccesoActual.require();
        String q=texto==null?null:texto.trim().toLowerCase();
        String st=estado==null?null:status(estado);
        return accounts.findAll().stream()
            .filter(a -> personaEntidadId==null || Objects.equals(personaEntidadId,a.getPersonEntityId()))
            .filter(a -> st==null || st.equals(a.getStatus()))
            .filter(a -> q==null || q.isBlank()
                || a.getUsername().toLowerCase().contains(q)
                || (a.getLoginEmail()!=null && a.getLoginEmail().toLowerCase().contains(q)))
            .filter(a -> canAdminAccount(actor,a))
            .sorted(Comparator.comparing(CuentaUsuario::getUsername,String.CASE_INSENSITIVE_ORDER))
            .map(this::view)
            .toList();
    }

    public CuentaDtos.Cuenta get(Long id) {
        CuentaUsuario account=require(id);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        return view(account);
    }

    @Transactional
    public CuentaDtos.Cuenta create(CuentaDtos.CrearCuenta in) {
        AccesoActual actor=ContextoAccesoActual.require();
        requirePerson(in.personaEntidadId());
        controlAcceso.requireEntityAccess(actor,in.personaEntidadId());
        if(accounts.existsByUsername(in.nombreUsuario().trim())) {
            throw new ExcepcionConflicto("Ya existe el nombre de usuario "+in.nombreUsuario());
        }

        String provider=norm(in.proveedorAutenticacion(),"LOCAL");
        if(!"LOCAL".equals(provider)) {
            throw new IllegalArgumentException("En esta versión la creación administrativa admite proveedor LOCAL");
        }

        String accountStatus=status(in.estado()==null?"ACTIVO":in.estado());
        boolean preferred=Boolean.TRUE.equals(in.preferido())&&"ACTIVO".equals(accountStatus);
        if(preferred) clearPreferred(in.personaEntidadId(),null);

        CuentaUsuario account=accounts.save(new CuentaUsuario(
            in.personaEntidadId(),
            in.nombreUsuario().trim(),
            in.correoAcceso(),
            passwordEncoder.encode(in.contrasena()),
            provider,
            in.sujetoExterno(),
            preferred,
            accountStatus
        ));
        return view(account);
    }

    @Transactional
    public CuentaDtos.Cuenta update(Long id, CuentaDtos.ModificarCuenta in) {
        CuentaUsuario account=require(id);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);

        String newStatus=in.estado()==null?null:status(in.estado());
        boolean wantsPreferred=Boolean.TRUE.equals(in.preferido());
        if(wantsPreferred && account.getPersonEntityId()!=null
            && (newStatus==null?"ACTIVO".equals(account.getStatus()):"ACTIVO".equals(newStatus))) {
            clearPreferred(account.getPersonEntityId(),id);
        }

        String passwordHash=null;
        if(in.nuevaContrasena()!=null && !in.nuevaContrasena().isBlank()) {
            passwordHash=passwordEncoder.encode(in.nuevaContrasena());
        }

        account.update(
            in.correoAcceso(),
            passwordHash,
            in.proveedorAutenticacion()==null?null:in.proveedorAutenticacion().trim().toUpperCase(),
            in.sujetoExterno(),
            in.preferido(),
            newStatus
        );
        if(!"ACTIVO".equals(account.getStatus())) account.setPreferred(false);
        if(passwordHash!=null || (newStatus!=null && !"ACTIVO".equals(newStatus))) revokeAllSessions(account.getId());
        return view(account);
    }

    @Transactional
    public CuentaDtos.Cuenta activate(Long id) {
        CuentaUsuario account=require(id);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        account.activate();
        return view(account);
    }

    @Transactional
    public CuentaDtos.Cuenta block(Long id) {
        CuentaUsuario account=require(id);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        account.block();
        revokeAllSessions(account.getId());
        return view(account);
    }

    @Transactional
    public void disable(Long id) {
        CuentaUsuario account=require(id);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        account.disable();
        revokeAllSessions(account.getId());
    }

    @Transactional
    public CuentaDtos.Cuenta linkPerson(Long accountId, Long personId) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        requirePerson(personId);
        controlAcceso.requireEntityAccess(actor,personId);
        if(account.isPreferred() && account.getPersonEntityId()!=null) account.setPreferred(false);
        account.linkPerson(personId);
        revokeAllSessions(accountId);
        return view(account);
    }

    @Transactional
    public CuentaDtos.Cuenta unlinkPerson(Long accountId) {
        CuentaUsuario account=require(accountId);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        account.unlinkPerson();
        revokeAllSessions(accountId);
        return view(account);
    }

    public List<CuentaDtos.Contexto> contexts(Long accountId) {
        CuentaUsuario account=require(accountId);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        return contexts.findByAccountIdOrderByIdAsc(accountId).stream().map(this::contextView).toList();
    }

    @Transactional
    public CuentaDtos.Contexto addContext(Long accountId, CuentaDtos.CrearContexto in) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        requireContextManageable(actor,in.entidadContextoId());
        if(!entities.existsById(in.entidadContextoId())) {
            throw new ExcepcionNoEncontrado("No existe el contexto/entidad "+in.entidadContextoId());
        }
        if(contexts.existsByAccountIdAndContextEntityId(accountId,in.entidadContextoId())) {
            throw new ExcepcionConflicto("La cuenta ya tiene asignado ese contexto");
        }

        Object[] role=rowByCode(
            "select id,codigo,nombre from rol_seguridad where codigo=:code",
            in.codigoRol().trim().toUpperCase()
        );
        validateAssignableRole(actor,String.valueOf(role[1]));
        String scope=scope(in.alcance()==null?"CONTEXTO":in.alcance());

        ContextoCuenta context=contexts.save(new ContextoCuenta(
            accountId,
            in.entidadContextoId(),
            number(role[0]),
            scope,
            in.habilitado()==null||in.habilitado(),
            in.validoDesde(),
            in.validoHasta()
        ));
        return contextView(context);
    }

    @Transactional
    public CuentaDtos.Contexto updateContext(Long accountId, Long contextId, CuentaDtos.ModificarContexto in) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        ContextoCuenta context=requireContext(accountId,contextId);
        requireContextManageable(actor,context.getContextEntityId());

        Long roleId=null;
        if(in.codigoRol()!=null && !in.codigoRol().isBlank()) {
            Object[] role=rowByCode(
                "select id,codigo,nombre from rol_seguridad where codigo=:code",
                in.codigoRol().trim().toUpperCase()
            );
            validateAssignableRole(actor,String.valueOf(role[1]));
            roleId=number(role[0]);
        }
        String scope=in.alcance()==null?null:scope(in.alcance());
        context.update(roleId,scope,in.habilitado(),in.validoDesde(),in.validoHasta());
        return contextView(context);
    }

    @Transactional
    public void disableContext(Long accountId, Long contextId) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        ContextoCuenta context=requireContext(accountId,contextId);
        requireContextManageable(actor,context.getContextEntityId());
        context.disable();
    }

    public List<CuentaDtos.ExcepcionPermiso> permissionOverrides(Long accountId, Long contextId) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        ContextoCuenta context=requireContext(accountId,contextId);
        requireContextManageable(actor,context.getContextEntityId());
        return overrides(contextId);
    }

    @Transactional
    public CuentaDtos.ExcepcionPermiso putPermissionOverride(Long accountId, Long contextId,
                                                              String permissionCode,
                                                              CuentaDtos.ModificarExcepcionPermiso in) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        ContextoCuenta context=requireContext(accountId,contextId);
        requireContextManageable(actor,context.getContextEntityId());

        String effect=effect(in.efecto());
        Object[] permission=permissionByCode(permissionCode);
        String normalizedCode=String.valueOf(permission[1]);
        if("PERMITIR".equals(effect) && !isPlatformAdmin(actor) && !actor.permissions().contains(normalizedCode)) {
            throw new ExcepcionProhibido("No puede conceder un permiso que no posee: "+normalizedCode);
        }

        em.createNativeQuery("""
            insert into excepcion_permiso_acceso(
                contexto_cuenta_id,permiso_id,efecto,
                auditoria_creado_por_cuenta_id,auditoria_ip_creacion,
                auditoria_modificado_por_cuenta_id,auditoria_ip_modificacion
            ) values (:ctx,:perm,:effect,:actor,:ip,:actor,:ip)
            on duplicate key update
                efecto=values(efecto),
                auditoria_modificado_por_cuenta_id=values(auditoria_modificado_por_cuenta_id),
                auditoria_ip_modificacion=values(auditoria_ip_modificacion)
            """)
            .setParameter("ctx",contextId)
            .setParameter("perm",number(permission[0]))
            .setParameter("effect",effect)
            .setParameter("actor",actor.accountId())
            .setParameter("ip",auditIp())
            .executeUpdate();

        return overrides(contextId).stream()
            .filter(x -> normalizedCode.equals(x.codigoPermiso()))
            .findFirst()
            .orElseThrow();
    }

    @Transactional
    public void deletePermissionOverride(Long accountId, Long contextId, String permissionCode) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        ContextoCuenta context=requireContext(accountId,contextId);
        requireContextManageable(actor,context.getContextEntityId());
        Object[] permission=permissionByCode(permissionCode);
        em.createNativeQuery("delete from excepcion_permiso_acceso where contexto_cuenta_id=:ctx and permiso_id=:perm")
            .setParameter("ctx",contextId)
            .setParameter("perm",number(permission[0]))
            .executeUpdate();
    }

    public CuentaDtos.AccesoEfectivo effectiveAccess(Long accountId, Long contextId) {
        CuentaUsuario account=require(accountId);
        AccesoActual actor=ContextoAccesoActual.require();
        requireAdminAccount(actor,account);
        ContextoCuenta context=requireContext(accountId,contextId);
        requireContextManageable(actor,context.getContextEntityId());

        CuentaDtos.Contexto view=contextView(context);
        Map<String,String> overrideMap=new LinkedHashMap<>();
        for(CuentaDtos.ExcepcionPermiso override:overrides(contextId)) {
            overrideMap.put(override.codigoPermiso(),override.efecto());
        }

        Set<String> rolePermissions=rolePermissions(context.getRoleId());
        List<Object[]> allPermissions=allPermissions();
        AccesoActual synthetic=new AccesoActual(
            null,accountId,account.getPersonEntityId(),contextId,context.getContextEntityId(),
            view.codigoRol(),context.getScope(),Set.of(),List.of()
        );

        OptionalInt mfaContext=politicaMfa.minutosContexto(synthetic);
        List<CuentaDtos.PermisoEfectivo> permissions=new ArrayList<>();
        for(Object[] row:allPermissions) {
            Long permissionId=number(row[0]);
            String code=String.valueOf(row[1]);
            String description=row[2]==null?null:String.valueOf(row[2]);
            boolean fromRole=rolePermissions.contains(code);
            String override=overrideMap.get(code);
            boolean effective="PERMITIR".equals(override) || (fromRole && !"DENEGAR".equals(override));
            OptionalInt permissionMfa=effective?politicaMfa.minutosPermiso(synthetic,code):OptionalInt.empty();
            permissions.add(new CuentaDtos.PermisoEfectivo(
                permissionId,code,description,fromRole,override,effective,
                permissionMfa.isPresent(),permissionMfa.isPresent()?permissionMfa.getAsInt():null
            ));
        }

        LocalDate today=LocalDate.now();
        boolean vigente=context.isEnabled()
            && (context.getValidFrom()==null || !context.getValidFrom().isAfter(today))
            && (context.getValidTo()==null || !context.getValidTo().isBefore(today));

        return new CuentaDtos.AccesoEfectivo(
            accountId,contextId,context.getContextEntityId(),view.nombreContexto(),
            view.codigoRol(),view.nombreRol(),context.getScope(),context.isEnabled(),vigente,
            mfaContext.isPresent(),mfaContext.isPresent()?mfaContext.getAsInt():null,
            List.copyOf(permissions)
        );
    }

    public CuentaDtos.ExplicacionPermiso explainPermission(Long accountId, Long contextId, String permissionCode) {
        String code=permissionCode.trim().toUpperCase();
        CuentaDtos.AccesoEfectivo access=effectiveAccess(accountId,contextId);
        CuentaDtos.PermisoEfectivo permission=access.permisos().stream()
            .filter(p -> code.equals(p.codigo()))
            .findFirst()
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe el permiso "+code));

        String reason;
        if("DENEGAR".equals(permission.excepcion())) {
            reason="DENEGADO por excepción explícita del contexto de cuenta";
        } else if("PERMITIR".equals(permission.excepcion())) {
            reason="PERMITIDO por excepción explícita del contexto de cuenta";
        } else if(permission.concedidoPorRol()) {
            reason="PERMITIDO por el rol de seguridad "+access.codigoRol();
        } else {
            reason="DENEGADO: ni el rol de seguridad ni una excepción PERMITIR conceden el permiso";
        }
        return new CuentaDtos.ExplicacionPermiso(
            code,permission.efectivo(),permission.concedidoPorRol(),permission.excepcion(),reason,
            permission.requiereMfa(),permission.minutosMfa()
        );
    }

    public List<CuentaDtos.Sesion> sessions(Long accountId) {
        CuentaUsuario account=require(accountId);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        LocalDateTime now=LocalDateTime.now().withNano(0);
        return sessions.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
            .map(s -> sessionView(s,now))
            .toList();
    }

    @Transactional
    public void revokeSession(Long accountId, Long sessionId) {
        CuentaUsuario account=require(accountId);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        SesionAutenticacion session=sessions.findById(sessionId)
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe la sesión "+sessionId));
        if(!Objects.equals(accountId,session.getAccountId())) {
            throw new ExcepcionNoEncontrado("La sesión no pertenece a la cuenta indicada");
        }
        session.revoke();
    }

    @Transactional
    public void revokeAllSessionsAdministrative(Long accountId) {
        CuentaUsuario account=require(accountId);
        requireAdminAccount(ContextoAccesoActual.require(),account);
        revokeAllSessions(accountId);
    }

    private void revokeAllSessions(Long accountId) {
        for(SesionAutenticacion session:sessions.findByAccountIdOrderByCreatedAtDesc(accountId)) {
            if(session.getRevokedAt()==null) session.revoke();
        }
    }

    private CuentaDtos.Sesion sessionView(SesionAutenticacion s, LocalDateTime now) {
        return new CuentaDtos.Sesion(
            s.getId(),s.getCreatedAt(),s.getExpiresAt(),s.getLastSeenAt(),s.getRevokedAt(),
            s.getAuthenticationLevel(),s.getMfaVerifiedAt(),s.getMfaValidUntil(),s.isActiveAt(now)
        );
    }

    private List<CuentaDtos.ExcepcionPermiso> overrides(Long contextId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows=em.createNativeQuery("""
            select p.id,p.codigo,p.descripcion,e.efecto
              from excepcion_permiso_acceso e
              join permiso p on p.id=e.permiso_id
             where e.contexto_cuenta_id=:ctx
             order by p.codigo
            """).setParameter("ctx",contextId).getResultList();
        return rows.stream().map(r -> new CuentaDtos.ExcepcionPermiso(
            number(r[0]),String.valueOf(r[1]),r[2]==null?null:String.valueOf(r[2]),String.valueOf(r[3])
        )).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> allPermissions() {
        return em.createNativeQuery("select id,codigo,descripcion from permiso order by codigo").getResultList();
    }

    private Set<String> rolePermissions(Long roleId) {
        @SuppressWarnings("unchecked")
        List<Object> rows=em.createNativeQuery("""
            select p.codigo
              from rol_seguridad_permiso rp
              join permiso p on p.id=rp.permiso_id
             where rp.rol_seguridad_id=:roleId
            """).setParameter("roleId",roleId).getResultList();
        Set<String> result=new LinkedHashSet<>();
        rows.stream().map(String::valueOf).forEach(result::add);
        return result;
    }

    private Object[] permissionByCode(String code) {
        String normalized=code.trim().toUpperCase();
        List<?> rows=em.createNativeQuery("select id,codigo,descripcion from permiso where codigo=:code")
            .setParameter("code",normalized).getResultList();
        if(rows.isEmpty()) throw new ExcepcionNoEncontrado("No existe el permiso "+normalized);
        return (Object[])rows.get(0);
    }

    private void requirePerson(Long personId) {
        if(personId==null || !entities.existsById(personId)) {
            throw new ExcepcionNoEncontrado("No existe la persona "+personId);
        }
        String type=String.valueOf(em.createNativeQuery("""
            select et.codigo
              from entidad e join tipo_entidad et on et.id=e.tipo_entidad_id
             where e.id=:id
            """).setParameter("id",personId).getSingleResult());
        if(!"PERSONA".equals(type)) throw new IllegalArgumentException("La cuenta debe asociarse a una entidad PERSONA");
    }

    private void requireAdminAccount(AccesoActual actor, CuentaUsuario target) {
        if(!canAdminAccount(actor,target)) {
            throw new ExcepcionProhibido("La cuenta queda fuera del contexto administrable");
        }
    }

    private boolean canAdminAccount(AccesoActual actor, CuentaUsuario target) {
        if(isPlatformAdmin(actor)) return true;
        if(actor==null || actor.contextEntityId()==null) return false;
        if(Objects.equals(actor.accountId(),target.getId())) return true;

        Number byContext=(Number)em.createNativeQuery("""
            select count(*) from contexto_cuenta
             where cuenta_id=:accountId and entidad_contexto_id=:contextId
            """)
            .setParameter("accountId",target.getId())
            .setParameter("contextId",actor.contextEntityId())
            .getSingleResult();
        if(byContext.longValue()>0) return true;

        return target.getPersonEntityId()!=null && controlAcceso.canAccessEntity(actor,target.getPersonEntityId());
    }

    private void requireContextManageable(AccesoActual actor, Long contextEntityId) {
        if(isPlatformAdmin(actor)) return;
        if(actor==null || contextEntityId==null || !controlAcceso.canAccessEntity(actor,contextEntityId)) {
            throw new ExcepcionProhibido(
                "Solo puede administrar accesos de entidades incluidas en el contexto raíz activo"
            );
        }
    }

    private void validateAssignableRole(AccesoActual actor, String roleCode) {
        if("ADMIN_PLATAFORMA".equalsIgnoreCase(roleCode) && !isPlatformAdmin(actor)) {
            throw new ExcepcionProhibido("Solo un ADMIN_PLATAFORMA puede asignar el rol ADMIN_PLATAFORMA");
        }
    }

    private boolean isPlatformAdmin(AccesoActual actor) {
        return actor!=null && "ADMIN_PLATAFORMA".equalsIgnoreCase(actor.roleCode());
    }

    private void clearPreferred(Long personId, Long except) {
        if(personId==null) return;
        for(CuentaUsuario account:accounts.findByPersonEntityIdAndStatusAndPreferredTrue(personId,"ACTIVO")) {
            if(except==null || !except.equals(account.getId())) account.setPreferred(false);
        }
    }

    private CuentaUsuario require(Long id) {
        return accounts.findById(id).orElseThrow(() -> new ExcepcionNoEncontrado("No existe la cuenta "+id));
    }

    private ContextoCuenta requireContext(Long accountId, Long contextId) {
        return contexts.findByIdAndAccountId(contextId,accountId)
            .orElseThrow(() -> new ExcepcionNoEncontrado("No existe el contexto de cuenta "+contextId));
    }

    private CuentaDtos.Cuenta view(CuentaUsuario a) {
        return new CuentaDtos.Cuenta(
            a.getId(),a.getPersonEntityId(),a.getUsername(),a.getLoginEmail(),a.getAuthProvider(),
            a.isPreferred(),a.getStatus(),a.getLastLoginAt()
        );
    }

    private CuentaDtos.Contexto contextView(ContextoCuenta c) {
        String contextName=String.valueOf(em.createNativeQuery("select nombre from entidad where id=:id")
            .setParameter("id",c.getContextEntityId()).getSingleResult());
        Object[] role=(Object[])em.createNativeQuery("select codigo,nombre from rol_seguridad where id=:id")
            .setParameter("id",c.getRoleId()).getSingleResult();
        return new CuentaDtos.Contexto(
            c.getId(),c.getAccountId(),c.getContextEntityId(),contextName,c.getRoleId(),
            String.valueOf(role[0]),String.valueOf(role[1]),c.getScope(),c.isEnabled(),c.getValidFrom(),c.getValidTo()
        );
    }

    private Object[] rowByCode(String sql, String code) {
        List<?> rows=em.createNativeQuery(sql).setParameter("code",code).getResultList();
        if(rows.isEmpty()) throw new ExcepcionNoEncontrado("No existe el rol "+code);
        return (Object[])rows.get(0);
    }

    private Long number(Object value) {
        return ((Number)value).longValue();
    }

    private String status(String value) {
        String normalized=value.trim().toUpperCase();
        if(!Set.of("ACTIVO","BLOQUEADA","DESHABILITADA").contains(normalized)) {
            throw new IllegalArgumentException("Estado de cuenta no válido: "+value);
        }
        return normalized;
    }

    private String scope(String value) {
        String normalized=value.trim().toUpperCase();
        if(!Set.of("PROPIO","RELACIONADOS","CONTEXTO","ARBOL_CONTEXTO","PERSONALIZADO").contains(normalized)) {
            throw new IllegalArgumentException("Alcance no válido: "+value);
        }
        return normalized;
    }

    private String effect(String value) {
        String normalized=value.trim().toUpperCase();
        if(!Set.of("PERMITIR","DENEGAR").contains(normalized)) {
            throw new IllegalArgumentException("Efecto no válido: "+value);
        }
        return normalized;
    }

    private String norm(String value, String defaultValue) {
        return value==null||value.isBlank()?defaultValue:value.trim().toUpperCase();
    }

    private String auditIp() {
        var audit=es.gestionpersonas.auditoria.ContextoAuditoriaActual.get();
        return audit==null?null:audit.ip();
    }
}
