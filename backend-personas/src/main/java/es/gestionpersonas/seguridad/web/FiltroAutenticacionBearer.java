package es.gestionpersonas.seguridad.web;

import es.gestionpersonas.auditoria.ContextoAuditoriaActual;
import es.gestionpersonas.comun.ExcepcionNoAutorizado;
import es.gestionpersonas.seguridad.modelo.SesionAutenticacion;
import es.gestionpersonas.seguridad.servicio.ServicioAutenticacion;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.servicio.ServicioMfa;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Adapta el token opaco y el contexto propio del CORE al SecurityContext de Spring.
 *
 * Spring Security pasa a ser responsable de la autenticación técnica y de exponer
 * los permisos efectivos como GrantedAuthority. El motor propio conserva la
 * resolución de contexto, alcance, relaciones, privacidad y reglas PERSONALIZADO.
 */
@Component
public class FiltroAutenticacionBearer extends OncePerRequestFilter {
    private final ServicioAutenticacion autenticacion;
    private final ServicioControlAcceso controlAcceso;
    private final ServicioMfa servicioMfa;
    private final HandlerExceptionResolver resolverExcepciones;

    public FiltroAutenticacionBearer(
        ServicioAutenticacion autenticacion,
        ServicioControlAcceso controlAcceso,
        ServicioMfa servicioMfa,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolverExcepciones
    ) {
        this.autenticacion = autenticacion;
        this.controlAcceso = controlAcceso;
        this.servicioMfa = servicioMfa;
        this.resolverExcepciones = resolverExcepciones;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta.startsWith("/v3/api-docs")
            || ruta.startsWith("/swagger-ui")
            || ruta.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        ContextoAuditoriaActual.iniciar(request.getRemoteAddr());

        try {
            if (esPublica(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            AccesoActual acceso;
            try {
                String token = bearer(request.getHeader("Authorization"));
                SesionAutenticacion sesion = autenticacion.authenticate(token);
                Long cuentaId = sesion.getAccountId();
                ContextoAuditoriaActual.establecerCuenta(cuentaId);

                if (requiereContexto(request.getRequestURI())) {
                    Long entidadContextoId = contexto(request.getHeader("X-Contexto-Id"));
                    acceso = controlAcceso.resolve(sesion.getId(), cuentaId, entidadContextoId);
                    servicioMfa.exigirContexto(acceso);
                } else {
                    acceso = new AccesoActual(
                        sesion.getId(), cuentaId, null, null, null, null, null, Set.of(), List.of()
                    );
                }
            } catch (Exception ex) {
                resolverExcepciones.resolveException(request, response, null, ex);
                return;
            }

            var authorities = new ArrayList<SimpleGrantedAuthority>();
            acceso.permissions().stream()
                .sorted()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
            if (acceso.roleCode() != null && !acceso.roleCode().isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + acceso.roleCode()));
            }

            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new PrincipalSeguridad(acceso), null, List.copyOf(authorities)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } finally {
            ContextoAuditoriaActual.limpiar();
            SecurityContextHolder.clearContext();
        }
    }

    private boolean esPublica(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
            || ruta.equals("/api/sistema/salud")
            || ruta.equals("/api/autenticacion/iniciar-sesion");
    }

    private boolean requiereContexto(String ruta) {
        if (!ruta.startsWith("/api/")) return false;
        if (ruta.startsWith("/api/autenticacion/mfa/")
            || ruta.equals("/api/autenticacion/mfa")
            || ruta.equals("/api/autenticacion/yo")
            || ruta.equals("/api/autenticacion/cerrar-sesion")
            || ruta.equals("/api/autenticacion/contextos")) {
            return false;
        }
        return !ruta.matches("^/api/autenticacion/contextos/\\d+/activar$");
    }

    private Long contexto(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ExcepcionNoAutorizado("Cabecera X-Contexto-Id obligatoria");
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new ExcepcionNoAutorizado("X-Contexto-Id no válido");
        }
    }

    private String bearer(String cabecera) {
        if (cabecera == null || !cabecera.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ExcepcionNoAutorizado("Cabecera Authorization Bearer obligatoria");
        }
        String token = cabecera.substring(7).trim();
        if (token.isBlank()) throw new ExcepcionNoAutorizado("Token Bearer obligatorio");
        return token;
    }
}
