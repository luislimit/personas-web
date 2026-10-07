package es.gestionpersonas.seguridad.web;

import java.util.Set;

/**
 * Asignación de seguridad efectiva que contribuye al contexto de conexión activo.
 *
 * Una cuenta puede tener una asignación sobre la raíz (por ejemplo CB Tres Cantos)
 * y otras más específicas sobre descendientes (por ejemplo Infantil A). Todas se
 * evalúan dentro del mismo contexto de conexión.
 */
public record AsignacionAccesoActual(
    Long accountContextId,
    Long contextEntityId,
    Long roleId,
    String roleCode,
    String alcance,
    Set<String> permissions,
    Set<String> deniedPermissions
) {}
