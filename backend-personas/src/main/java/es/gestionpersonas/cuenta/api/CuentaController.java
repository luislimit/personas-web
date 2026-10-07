package es.gestionpersonas.cuenta.api;

import es.gestionpersonas.cuenta.dto.CuentaDtos;
import es.gestionpersonas.cuenta.servicio.ServicioCuenta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
@Tag(name = "Cuentas", description = "Administración de cuentas, contextos, roles, permisos efectivos y sesiones")
public class CuentaController {
    private final ServicioCuenta servicio;

    public CuentaController(ServicioCuenta servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.LEER')")
    @Operation(summary = "Buscar cuentas administrables desde el contexto activo")
    public List<CuentaDtos.Cuenta> buscar(
        @RequestParam(required = false) String texto,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) Long personaEntidadId
    ) {
        return servicio.search(texto, estado, personaEntidadId);
    }

    @PostMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaDtos.Cuenta crear(@Valid @RequestBody CuentaDtos.CrearCuenta entrada) {
        return servicio.create(entrada);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.LEER')")
    public CuentaDtos.Cuenta obtener(@PathVariable Long id) {
        return servicio.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.MODIFICAR')")
    public CuentaDtos.Cuenta modificar(@PathVariable Long id, @Valid @RequestBody CuentaDtos.ModificarCuenta entrada) {
        return servicio.update(id, entrada);
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.MODIFICAR')")
    public CuentaDtos.Cuenta activar(@PathVariable Long id) {
        return servicio.activate(id);
    }

    @PostMapping("/{id}/bloquear")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.MODIFICAR')")
    public CuentaDtos.Cuenta bloquear(@PathVariable Long id) {
        return servicio.block(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.ELIMINAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deshabilitar(@PathVariable Long id) {
        servicio.disable(id);
    }

    @PutMapping("/{id}/persona/{personaId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.MODIFICAR')")
    public CuentaDtos.Cuenta vincularPersona(@PathVariable Long id, @PathVariable Long personaId) {
        return servicio.linkPerson(id, personaId);
    }

    @DeleteMapping("/{id}/persona")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.MODIFICAR')")
    public CuentaDtos.Cuenta desvincularPersona(@PathVariable Long id) {
        return servicio.unlinkPerson(id);
    }

    @GetMapping("/{cuentaId}/contextos")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.LEER')")
    public List<CuentaDtos.Contexto> contextos(@PathVariable Long cuentaId) {
        return servicio.contexts(cuentaId);
    }

    @PostMapping("/{cuentaId}/contextos")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaDtos.Contexto anadirContexto(@PathVariable Long cuentaId,
                                               @Valid @RequestBody CuentaDtos.CrearContexto entrada) {
        return servicio.addContext(cuentaId, entrada);
    }

    @PatchMapping("/{cuentaId}/contextos/{contextoCuentaId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.MODIFICAR')")
    public CuentaDtos.Contexto modificarContexto(@PathVariable Long cuentaId,
                                                  @PathVariable Long contextoCuentaId,
                                                  @Valid @RequestBody CuentaDtos.ModificarContexto entrada) {
        return servicio.updateContext(cuentaId, contextoCuentaId, entrada);
    }

    @DeleteMapping("/{cuentaId}/contextos/{contextoCuentaId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.ELIMINAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deshabilitarContexto(@PathVariable Long cuentaId, @PathVariable Long contextoCuentaId) {
        servicio.disableContext(cuentaId, contextoCuentaId);
    }

    @GetMapping("/{cuentaId}/contextos/{contextoCuentaId}/excepciones-permiso")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.LEER')")
    public List<CuentaDtos.ExcepcionPermiso> excepcionesPermiso(@PathVariable Long cuentaId,
                                                                 @PathVariable Long contextoCuentaId) {
        return servicio.permissionOverrides(cuentaId, contextoCuentaId);
    }

    @PutMapping("/{cuentaId}/contextos/{contextoCuentaId}/excepciones-permiso/{codigoPermiso}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.MODIFICAR')")
    public CuentaDtos.ExcepcionPermiso establecerExcepcionPermiso(
        @PathVariable Long cuentaId,
        @PathVariable Long contextoCuentaId,
        @PathVariable String codigoPermiso,
        @Valid @RequestBody CuentaDtos.ModificarExcepcionPermiso entrada
    ) {
        return servicio.putPermissionOverride(cuentaId, contextoCuentaId, codigoPermiso, entrada);
    }

    @DeleteMapping("/{cuentaId}/contextos/{contextoCuentaId}/excepciones-permiso/{codigoPermiso}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.MODIFICAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarExcepcionPermiso(
        @PathVariable Long cuentaId,
        @PathVariable Long contextoCuentaId,
        @PathVariable String codigoPermiso
    ) {
        servicio.deletePermissionOverride(cuentaId, contextoCuentaId, codigoPermiso);
    }

    @GetMapping("/{cuentaId}/contextos/{contextoCuentaId}/acceso-efectivo")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.LEER')")
    public CuentaDtos.AccesoEfectivo accesoEfectivo(@PathVariable Long cuentaId,
                                                     @PathVariable Long contextoCuentaId) {
        return servicio.effectiveAccess(cuentaId, contextoCuentaId);
    }

    @GetMapping("/{cuentaId}/contextos/{contextoCuentaId}/permisos/{codigoPermiso}/explicacion")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.LEER')")
    public CuentaDtos.ExplicacionPermiso explicacionPermiso(
        @PathVariable Long cuentaId,
        @PathVariable Long contextoCuentaId,
        @PathVariable String codigoPermiso
    ) {
        return servicio.explainPermission(cuentaId, contextoCuentaId, codigoPermiso);
    }

    @GetMapping("/{cuentaId}/sesiones")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.LEER')")
    public List<CuentaDtos.Sesion> sesiones(@PathVariable Long cuentaId) {
        return servicio.sessions(cuentaId);
    }

    @DeleteMapping("/{cuentaId}/sesiones/{sesionId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.MODIFICAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revocarSesion(@PathVariable Long cuentaId, @PathVariable Long sesionId) {
        servicio.revokeSession(cuentaId, sesionId);
    }

    @PostMapping("/{cuentaId}/sesiones/revocar-todas")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.MODIFICAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revocarTodasLasSesiones(@PathVariable Long cuentaId) {
        servicio.revokeAllSessionsAdministrative(cuentaId);
    }
}
