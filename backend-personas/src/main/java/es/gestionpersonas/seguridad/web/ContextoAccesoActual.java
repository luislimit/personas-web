package es.gestionpersonas.seguridad.web;

import es.gestionpersonas.comun.ExcepcionNoAutorizado;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Fachada de compatibilidad para los servicios del CORE.
 *
 * La fuente principal ya es SecurityContextHolder. Los métodos set/clear se
 * conservan temporalmente para no romper código auxiliar antiguo, pero el flujo
 * HTTP normal no depende de un ThreadLocal propio.
 */
public final class ContextoAccesoActual {
    private static final ThreadLocal<AccesoActual> LEGACY = new ThreadLocal<>();

    private ContextoAccesoActual() {}

    public static void set(AccesoActual access) {
        LEGACY.set(access);
    }

    public static AccesoActual get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PrincipalSeguridad principal) {
            return principal.acceso();
        }
        return LEGACY.get();
    }

    public static AccesoActual require() {
        AccesoActual value = get();
        if (value == null) {
            throw new ExcepcionNoAutorizado("No existe contexto de acceso autenticado");
        }
        return value;
    }

    public static void clear() {
        LEGACY.remove();
    }
}
