package es.gestionpersonas.seguridad.api;

import es.gestionpersonas.seguridad.dto.AutenticacionDtos;
import es.gestionpersonas.seguridad.servicio.ServicioAutenticacion;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.web.PrincipalSeguridad;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autenticacion")
@Tag(name = "Autenticación", description = "Autenticación, sesión y selección de contexto")
public class AutenticacionController {
    private final ServicioAutenticacion servicioAutenticacion;
    private final ServicioControlAcceso controlAcceso;

    public AutenticacionController(ServicioAutenticacion servicioAutenticacion, ServicioControlAcceso controlAcceso) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.controlAcceso = controlAcceso;
    }

    @PostMapping("/iniciar-sesion")
    public AutenticacionDtos.RespuestaInicioSesion iniciarSesion(
        @Valid @RequestBody AutenticacionDtos.SolicitudInicioSesion entrada) {
        return servicioAutenticacion.login(entrada);
    }

    @PostMapping("/cerrar-sesion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@AuthenticationPrincipal PrincipalSeguridad principal) {
        servicioAutenticacion.logoutSession(principal.sesionId());
    }

    @GetMapping("/yo")
    public AutenticacionDtos.RespuestaYo yo(@AuthenticationPrincipal PrincipalSeguridad principal) {
        return servicioAutenticacion.me(principal.cuentaId());
    }

    @GetMapping("/contextos")
    public java.util.List<AutenticacionDtos.VistaContexto> contextos(
        @AuthenticationPrincipal PrincipalSeguridad principal) {
        return servicioAutenticacion.contexts(principal.cuentaId());
    }

    @PostMapping("/contextos/{entidadContextoId}/activar")
    public AutenticacionDtos.RespuestaContextoActivo activar(
        @PathVariable Long entidadContextoId,
        @AuthenticationPrincipal PrincipalSeguridad principal) {
        return controlAcceso.activate(principal.cuentaId(), entidadContextoId);
    }
}
