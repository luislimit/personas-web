package es.gestionpersonas.seguridad.web;

import java.util.List;
import java.util.Set;

public record AccesoActual(
    Long sessionId,
    Long accountId,
    Long personEntityId,
    Long accountContextId,
    Long contextEntityId,
    String roleCode,
    String alcance,
    Set<String> permissions,
    List<AsignacionAccesoActual> asignaciones
) {}
