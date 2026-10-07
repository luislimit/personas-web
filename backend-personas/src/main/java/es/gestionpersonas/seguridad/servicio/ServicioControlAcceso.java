package es.gestionpersonas.seguridad.servicio;

import es.gestionpersonas.comun.*;
import es.gestionpersonas.seguridad.dto.AutenticacionDtos;
import es.gestionpersonas.seguridad.modelo.ReglaAlcanceAcceso;
import es.gestionpersonas.seguridad.repositorio.RepositorioReglaAlcanceAcceso;
import es.gestionpersonas.seguridad.web.AccesoActual;
import es.gestionpersonas.seguridad.web.AsignacionAccesoActual;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ServicioControlAcceso {
    private final EntityManager em;
    private final ServicioAutenticacion authService;
    private final RepositorioReglaAlcanceAcceso reglasAlcance;

    public ServicioControlAcceso(EntityManager em, ServicioAutenticacion authService,
                                 RepositorioReglaAlcanceAcceso reglasAlcance) {
        this.em = em;
        this.authService = authService;
        this.reglasAlcance = reglasAlcance;
    }

    /**
     * Resuelve un contexto de conexión raíz y agrega todas las asignaciones activas
     * de la misma cuenta situadas dentro de su árbol estructural.
     *
     * Ejemplo:
     *   CB Tres Cantos -> AUTOSERVICIO
     *   Infantil A     -> GESTOR_CONTEXTO
     *
     * Al conectarse a CB Tres Cantos ambas asignaciones contribuyen al acceso.
     */
    public AccesoActual resolve(Long sessionId, Long accountId, Long contextEntityId) {
        AutenticacionDtos.VistaContexto rootView = authService.contexts(accountId).stream()
            .filter(c -> Objects.equals(c.entidadContextoId(), contextEntityId))
            .findFirst()
            .orElseThrow(() -> new ExcepcionProhibido(
                "El contexto no está disponible como contexto de conexión para la cuenta"
            ));

        Object[] rootRow = contextRow(accountId, contextEntityId);
        Long rootAccountContextId = number(rootRow[0]);
        Long personEntityId = rootRow[1] == null ? null : number(rootRow[1]);
        String rootRoleCode = String.valueOf(rootRow[2]);
        String rootScope = rootRow[3] == null ? null : String.valueOf(rootRow[3]);

        List<AsignacionAccesoActual> assignments = assignmentsForRoot(accountId, contextEntityId);
        Set<String> aggregatePermissions = new LinkedHashSet<>();
        assignments.forEach(a -> aggregatePermissions.addAll(a.permissions()));

        return new AccesoActual(
            sessionId,
            accountId,
            personEntityId,
            rootAccountContextId,
            contextEntityId,
            rootRoleCode,
            rootScope,
            Collections.unmodifiableSet(aggregatePermissions),
            List.copyOf(assignments)
        );
    }

    public AutenticacionDtos.RespuestaContextoActivo activate(Long accountId, Long contextEntityId) {
        AccesoActual access = resolve(null, accountId, contextEntityId);
        AutenticacionDtos.VistaContexto context = authService.contexts(accountId).stream()
            .filter(c -> Objects.equals(c.entidadContextoId(), contextEntityId))
            .findFirst()
            .orElseThrow(() -> new ExcepcionProhibido("El contexto no está disponible para la cuenta"));
        return new AutenticacionDtos.RespuestaContextoActivo(context, access.permissions());
    }

    public void requirePermission(AccesoActual access, String permiso) {
        if (!hasPermission(access, permiso)) {
            throw new ExcepcionProhibido("Permiso requerido: " + permiso);
        }
    }

    /**
     * Permiso agregado del contexto de conexión. Se utiliza como filtro técnico
     * de entrada en Spring Security: basta con que alguna asignación del árbol lo
     * conceda. La autorización fina sobre una entidad concreta se realiza con
     * hasPermissionForEntity / requirePermissionForEntity.
     */
    public boolean hasPermission(AccesoActual access, String permiso) {
        return access != null && access.permissions().contains(permiso);
    }

    /**
     * Evalúa un permiso sobre una entidad concreta acumulando las asignaciones que
     * realmente alcanzan a esa entidad. Una excepción DENEGAR explícita aplicable
     * prevalece sobre cualquier concesión de otra asignación.
     */
    public boolean hasPermissionForEntity(AccesoActual access, String permiso, Long targetEntityId) {
        if (access == null || permiso == null || targetEntityId == null) return false;

        boolean allowed = false;
        for (AsignacionAccesoActual assignment : access.asignaciones()) {
            if (!scopeAllows(access, assignment, targetEntityId)) continue;
            if (assignment.deniedPermissions().contains(permiso)) return false;
            if (assignment.permissions().contains(permiso)) allowed = true;
        }
        return allowed;
    }

    public void requirePermissionForEntity(AccesoActual access, String permiso, Long targetEntityId) {
        if (!hasPermissionForEntity(access, permiso, targetEntityId)) {
            throw new ExcepcionProhibido(
                "Permiso requerido sobre la entidad " + targetEntityId + ": " + permiso
            );
        }
    }

    /**
     * Una entidad es visible si queda dentro del alcance de al menos una de las
     * asignaciones que componen el contexto raíz activo.
     */
    public boolean canAccessEntity(AccesoActual access, Long targetEntityId) {
        if (access == null || targetEntityId == null) return false;
        return access.asignaciones().stream().anyMatch(a -> scopeAllows(access, a, targetEntityId));
    }

    public void requireEntityAccess(AccesoActual access, Long targetEntityId) {
        if (!canAccessEntity(access, targetEntityId)) {
            throw new ExcepcionProhibido(
                "La entidad " + targetEntityId + " queda fuera del alcance del contexto activo"
            );
        }
    }

    public boolean canReadPersonalData(AccesoActual access, Long targetEntityId) {
        return canPersonalData(access, targetEntityId, false);
    }

    public boolean canUpdatePersonalData(AccesoActual access, Long targetEntityId) {
        return canPersonalData(access, targetEntityId, true);
    }

    public void requireReadPersonalData(AccesoActual access, Long targetEntityId) {
        requireEntityAccess(access, targetEntityId);
        if (!canReadPersonalData(access, targetEntityId)) {
            throw new ExcepcionProhibido(
                "No tiene permiso para consultar los datos personales de la entidad " + targetEntityId
            );
        }
    }

    public void requireUpdatePersonalData(AccesoActual access, Long targetEntityId) {
        requireEntityAccess(access, targetEntityId);
        if (!canUpdatePersonalData(access, targetEntityId)) {
            throw new ExcepcionProhibido(
                "No tiene permiso para modificar los datos personales de la entidad " + targetEntityId
            );
        }
    }

    private boolean canPersonalData(AccesoActual access, Long targetEntityId, boolean update) {
        if (!canAccessEntity(access, targetEntityId)) return false;

        if (access.personEntityId() != null && Objects.equals(access.personEntityId(), targetEntityId)) {
            return true;
        }

        if (access.personEntityId() != null && isChildOrWard(access.personEntityId(), targetEntityId)) {
            return true;
        }

        return hasPermissionForEntity(
            access,
            update ? "DATOS_PERSONALES.MODIFICAR" : "DATOS_PERSONALES.LEER",
            targetEntityId
        );
    }

    private boolean scopeAllows(AccesoActual access, AsignacionAccesoActual assignment, Long targetEntityId) {
        if ("ADMIN_PLATAFORMA".equalsIgnoreCase(assignment.roleCode())) return true;

        if (Objects.equals(assignment.contextEntityId(), targetEntityId)) return true;
        if (access.personEntityId() != null && Objects.equals(access.personEntityId(), targetEntityId)) return true;

        String scope = assignment.alcance() == null
            ? "CONTEXTO"
            : assignment.alcance().trim().toUpperCase();

        return switch (scope) {
            case "CONTEXTO", "ARBOL_CONTEXTO" ->
                isInContextTree(assignment.contextEntityId(), targetEntityId);
            case "RELACIONADOS" ->
                access.personEntityId() != null
                    && isDirectlyRelated(access.personEntityId(), targetEntityId);
            case "PERSONALIZADO" ->
                canAccessByCustomRules(access, assignment, targetEntityId);
            case "PROPIO" -> false;
            default -> false;
        };
    }

    private boolean canAccessByCustomRules(AccesoActual access,
                                           AsignacionAccesoActual assignment,
                                           Long targetEntityId) {
        List<ReglaAlcanceAcceso> rules = reglasAlcance
            .findByContextoCuentaIdAndActivoTrueOrderByPrioridadDescIdAsc(assignment.accountContextId());

        boolean permitido = false;
        for (ReglaAlcanceAcceso rule : rules) {
            if (!matchesCustomRule(rule, assignment.contextEntityId(), targetEntityId)) continue;
            if ("DENEGAR".equalsIgnoreCase(rule.getEfecto())) return false;
            if ("PERMITIR".equalsIgnoreCase(rule.getEfecto())) permitido = true;
        }
        return permitido;
    }

    private boolean matchesCustomRule(ReglaAlcanceAcceso rule,
                                      Long assignmentContextEntityId,
                                      Long targetEntityId) {
        int criterios = 0;

        if (rule.getTipoRelacionId() != null) {
            criterios++;
            if (!matchesRelationRule(
                assignmentContextEntityId,
                targetEntityId,
                rule.getTipoRelacionId(),
                rule.isIncluirDescendientes()
            )) return false;
        }

        if (rule.getRolFuncionalId() != null) {
            criterios++;
            if (!hasFunctionalRole(
                targetEntityId,
                assignmentContextEntityId,
                rule.getRolFuncionalId()
            )) return false;
        }

        if (rule.getTipoEntidadObjetivoId() != null) {
            criterios++;
            if (!hasEntityType(targetEntityId, rule.getTipoEntidadObjetivoId())) return false;
        }

        if (criterios == 0) return false;

        if (rule.getTipoRelacionId() == null
            && rule.getRolFuncionalId() == null
            && rule.getTipoEntidadObjetivoId() != null) {
            return isInContextTree(assignmentContextEntityId, targetEntityId);
        }

        return true;
    }

    private boolean matchesRelationRule(Long contextEntityId, Long targetEntityId, Long relationTypeId,
                                        boolean includeDescendants) {
        if (!includeDescendants) {
            Number count = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                  FROM relacion_entidad re
                 WHERE re.entidad_origen_id = :contextId
                   AND re.entidad_destino_id = :targetId
                   AND re.tipo_relacion_id = :relationTypeId
                   AND re.estado = 'ACTIVO'
                   AND (re.valido_desde IS NULL OR re.valido_desde <= CURRENT_DATE())
                   AND (re.valido_hasta IS NULL OR re.valido_hasta >= CURRENT_DATE())
                """)
                .setParameter("contextId", contextEntityId)
                .setParameter("targetId", targetEntityId)
                .setParameter("relationTypeId", relationTypeId)
                .getSingleResult();
            return count.longValue() > 0;
        }

        Number count = (Number) em.createNativeQuery("""
            WITH RECURSIVE alcance_relacion(id) AS (
                SELECT :contextId
                UNION DISTINCT
                SELECT re.entidad_destino_id
                  FROM relacion_entidad re
                  JOIN alcance_relacion ar ON ar.id = re.entidad_origen_id
                 WHERE re.tipo_relacion_id = :relationTypeId
                   AND re.estado = 'ACTIVO'
                   AND (re.valido_desde IS NULL OR re.valido_desde <= CURRENT_DATE())
                   AND (re.valido_hasta IS NULL OR re.valido_hasta >= CURRENT_DATE())
            )
            SELECT COUNT(*) FROM alcance_relacion WHERE id = :targetId
            """)
            .setParameter("contextId", contextEntityId)
            .setParameter("targetId", targetEntityId)
            .setParameter("relationTypeId", relationTypeId)
            .getSingleResult();
        return count.longValue() > 0;
    }

    private boolean hasFunctionalRole(Long targetEntityId, Long contextEntityId, Long roleId) {
        Number count = (Number) em.createNativeQuery("""
            SELECT COUNT(*)
              FROM rol_funcional_entidad_contexto rfc
             WHERE rfc.entidad_id = :targetId
               AND rfc.entidad_contexto_id = :contextId
               AND rfc.rol_funcional_id = :roleId
               AND rfc.estado = 'ACTIVO'
               AND (rfc.valido_desde IS NULL OR rfc.valido_desde <= CURRENT_DATE())
               AND (rfc.valido_hasta IS NULL OR rfc.valido_hasta >= CURRENT_DATE())
            """)
            .setParameter("targetId", targetEntityId)
            .setParameter("contextId", contextEntityId)
            .setParameter("roleId", roleId)
            .getSingleResult();
        return count.longValue() > 0;
    }

    private boolean hasEntityType(Long targetEntityId, Long typeId) {
        Number count = (Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM entidad
             WHERE id = :targetId AND tipo_entidad_id = :typeId AND estado <> 'ARCHIVADO'
            """)
            .setParameter("targetId", targetEntityId)
            .setParameter("typeId", typeId)
            .getSingleResult();
        return count.longValue() > 0;
    }

    private boolean isInContextTree(Long contextEntityId, Long targetEntityId) {
        if (contextEntityId == null || targetEntityId == null) return false;

        Number count = (Number) em.createNativeQuery("""
            WITH RECURSIVE arbol_contexto(id) AS (
                SELECT :contextId
                UNION DISTINCT
                SELECT re.entidad_destino_id
                  FROM relacion_entidad re
                  JOIN arbol_contexto ac ON ac.id = re.entidad_origen_id
                  JOIN tipo_relacion tr ON tr.id = re.tipo_relacion_id
                 WHERE re.estado = 'ACTIVO'
                   AND (re.valido_desde IS NULL OR re.valido_desde <= CURRENT_DATE())
                   AND (re.valido_hasta IS NULL OR re.valido_hasta >= CURRENT_DATE())
                   AND tr.codigo IN ('PERTENENCIA','PROPIEDAD','GESTION')
            )
            SELECT COUNT(*) FROM arbol_contexto WHERE id = :targetId
            """)
            .setParameter("contextId", contextEntityId)
            .setParameter("targetId", targetEntityId)
            .getSingleResult();

        return count.longValue() > 0;
    }

    private boolean isDirectlyRelated(Long personId, Long targetId) {
        Number count = (Number) em.createNativeQuery("""
            SELECT COUNT(*)
              FROM relacion_entidad re
             WHERE re.estado = 'ACTIVO'
               AND (re.valido_desde IS NULL OR re.valido_desde <= CURRENT_DATE())
               AND (re.valido_hasta IS NULL OR re.valido_hasta >= CURRENT_DATE())
               AND (
                    (re.entidad_origen_id = :personId AND re.entidad_destino_id = :targetId)
                 OR (re.entidad_destino_id = :personId AND re.entidad_origen_id = :targetId)
               )
            """)
            .setParameter("personId", personId)
            .setParameter("targetId", targetId)
            .getSingleResult();

        return count.longValue() > 0;
    }

    private boolean isChildOrWard(Long personId, Long targetId) {
        Number count = (Number) em.createNativeQuery("""
            select count(*)
              from relacion_entidad er
              join tipo_relacion rt on rt.id = er.tipo_relacion_id
             where er.entidad_origen_id = :personId
               and er.entidad_destino_id = :targetId
               and er.estado = 'ACTIVO'
               and (er.valido_desde is null or er.valido_desde <= current_date())
               and (er.valido_hasta is null or er.valido_hasta >= current_date())
               and rt.codigo in ('PADRE_HIJO','TUTOR_TUTELADO')
            """)
            .setParameter("personId", personId)
            .setParameter("targetId", targetId)
            .getSingleResult();
        return count.longValue() > 0;
    }

    @SuppressWarnings("unchecked")
    private List<AsignacionAccesoActual> assignmentsForRoot(Long accountId, Long rootContextEntityId) {
        List<Object[]> rows = em.createNativeQuery("""
            select ac.id, ac.entidad_contexto_id, r.id, r.codigo, ac.alcance
              from contexto_cuenta ac
              join rol_seguridad r on r.id = ac.rol_seguridad_id
             where ac.cuenta_id = :accountId
               and ac.habilitado = true
               and (ac.valido_desde is null or ac.valido_desde <= current_date())
               and (ac.valido_hasta is null or ac.valido_hasta >= current_date())
             order by ac.id
            """)
            .setParameter("accountId", accountId)
            .getResultList();

        List<AsignacionAccesoActual> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long assignmentContextEntityId = number(row[1]);
            if (!isInContextTree(rootContextEntityId, assignmentContextEntityId)) continue;

            Long accountContextId = number(row[0]);
            Long roleId = number(row[2]);
            PermissionSnapshot snapshot = permissionSnapshot(accountContextId, roleId);

            result.add(new AsignacionAccesoActual(
                accountContextId,
                assignmentContextEntityId,
                roleId,
                String.valueOf(row[3]),
                row[4] == null ? null : String.valueOf(row[4]),
                Collections.unmodifiableSet(snapshot.allowed()),
                Collections.unmodifiableSet(snapshot.denied())
            ));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private PermissionSnapshot permissionSnapshot(Long accountContextId, Long roleId) {
        Set<String> allowed = new LinkedHashSet<>();
        Set<String> denied = new LinkedHashSet<>();

        List<?> rolePermRows = em.createNativeQuery("""
            select p.codigo
              from rol_seguridad_permiso rp
              join permiso p on p.id = rp.permiso_id
             where rp.rol_seguridad_id = :roleId
            """)
            .setParameter("roleId", roleId)
            .getResultList();
        rolePermRows.stream().map(String::valueOf).forEach(allowed::add);

        List<Object[]> overrides = em.createNativeQuery("""
            select p.codigo, apo.efecto
              from excepcion_permiso_acceso apo
              join permiso p on p.id = apo.permiso_id
             where apo.contexto_cuenta_id = :accountContextId
            """)
            .setParameter("accountContextId", accountContextId)
            .getResultList();

        for (Object[] row : overrides) {
            String code = String.valueOf(row[0]);
            String effect = String.valueOf(row[1]);
            if ("DENEGAR".equals(effect)) {
                allowed.remove(code);
                denied.add(code);
            } else if ("PERMITIR".equals(effect)) {
                denied.remove(code);
                allowed.add(code);
            }
        }
        return new PermissionSnapshot(allowed, denied);
    }

    private Object[] contextRow(Long accountId, Long contextEntityId) {
        List<?> rows = em.createNativeQuery("""
            select ac.id, ua.persona_entidad_id, r.codigo, ac.alcance, r.id
              from contexto_cuenta ac
              join cuenta_usuario ua on ua.id = ac.cuenta_id
              join rol_seguridad r on r.id = ac.rol_seguridad_id
             where ac.cuenta_id = :accountId
               and ac.entidad_contexto_id = :contextId
               and ac.habilitado = true
               and ua.estado = 'ACTIVO'
               and (ac.valido_desde is null or ac.valido_desde <= current_date())
               and (ac.valido_hasta is null or ac.valido_hasta >= current_date())
             limit 1
            """)
            .setParameter("accountId", accountId)
            .setParameter("contextId", contextEntityId)
            .getResultList();
        if (rows.isEmpty()) {
            throw new ExcepcionProhibido("El contexto no está habilitado para esta cuenta");
        }
        return (Object[]) rows.get(0);
    }

    private record PermissionSnapshot(Set<String> allowed, Set<String> denied) {}

    private Long number(Object value) {
        return ((Number) value).longValue();
    }
}
