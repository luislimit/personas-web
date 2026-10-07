package es.gestionpersonas.seguridad.web;

import java.security.Principal;

/**
 * Principal autenticado expuesto por Spring Security.
 *
 * Mantiene el AccesoActual resuelto para la petición. En endpoints autenticados
 * que no requieren contexto (yo, contextos, activar, cerrar sesión), el acceso
 * contiene únicamente sesión y cuenta.
 */
public record PrincipalSeguridad(AccesoActual acceso) implements Principal {
    @Override
    public String getName() {
        return acceso == null || acceso.accountId() == null
            ? "anonimo"
            : "cuenta:" + acceso.accountId();
    }

    public Long cuentaId() {
        return acceso.accountId();
    }

    public Long sesionId() {
        return acceso.sessionId();
    }
}
