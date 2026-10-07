package es.gestionpersonas.seguridad.api;

import es.gestionpersonas.seguridad.dto.AdministracionSeguridadDtos;
import es.gestionpersonas.seguridad.servicio.ServicioAdministracionSeguridad;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seguridad/catalogos")
@Tag(name = "Seguridad - catálogos", description = "Roles y permisos disponibles para administración de accesos")
public class AdministracionSeguridadController {
    private final ServicioAdministracionSeguridad servicio;

    public AdministracionSeguridadController(ServicioAdministracionSeguridad servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/roles")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.LEER')")
    public List<AdministracionSeguridadDtos.RolSeguridad> roles() {
        return servicio.roles();
    }

    @GetMapping("/permisos")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.LEER')")
    public List<AdministracionSeguridadDtos.Permiso> permisos() {
        return servicio.permisos();
    }
}
