package es.gestionpersonas.entidad.api;

import es.gestionpersonas.cuenta.dto.CuentaDtos;
import es.gestionpersonas.documento.dto.DocumentoDto;
import es.gestionpersonas.documento.servicio.ServicioDocumento;
import es.gestionpersonas.entidad.dto.EntidadDtos;
import es.gestionpersonas.entidad.servicio.ServicioEntidad;
import es.gestionpersonas.seguridad.servicio.ServicioControlAcceso;
import es.gestionpersonas.seguridad.web.ContextoAccesoActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entidades")
@Tag(name = "Entidades", description = "Entidades, personas, relaciones y contactos")
public class EntidadController {
    private final ServicioEntidad servicioEntidad;
    private final ServicioDocumento servicioDocumento;
    private final ServicioControlAcceso controlAcceso;

    public EntidadController(ServicioEntidad servicioEntidad, ServicioDocumento servicioDocumento,
                             ServicioControlAcceso controlAcceso) {
        this.servicioEntidad = servicioEntidad;
        this.servicioDocumento = servicioDocumento;
        this.controlAcceso = controlAcceso;
    }

    @GetMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.LEER')")
    @Operation(summary = "Buscar entidades")
    public List<EntidadDtos.Resumen> buscar(
        @RequestParam(required = false) String codigoTipo,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String nombre) {
        return servicioEntidad.search(codigoTipo, estado, nombre);
    }

    @PostMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear entidad o persona")
    public EntidadDtos.Detalle crear(@Valid @RequestBody EntidadDtos.CrearEntidad entrada) {
        return servicioEntidad.create(entrada);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.LEER')")
    @Operation(summary = "Obtener entidad")
    public EntidadDtos.Detalle obtener(@PathVariable Long id) {
        EntidadDtos.Detalle detalle = servicioEntidad.get(id);
        if (detalle.persona() != null && !controlAcceso.canReadPersonalData(ContextoAccesoActual.require(), id)) {
            return new EntidadDtos.Detalle(
                detalle.id(), detalle.codigoTipo(), detalle.nombre(), detalle.estado(),
                detalle.fechaCreacion(), detalle.fechaModificacion(), null
            );
        }
        return detalle;
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.MODIFICAR')")
    @Operation(summary = "Modificar entidad")
    public EntidadDtos.Detalle modificar(@PathVariable Long id,
                                         @Valid @RequestBody EntidadDtos.ModificarEntidad entrada) {
        if (entrada.persona() != null) {
            controlAcceso.requireUpdatePersonalData(ContextoAccesoActual.require(), id);
        }
        return servicioEntidad.update(id, entrada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.ELIMINAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archivar entidad")
    public void archivar(@PathVariable Long id) {
        servicioEntidad.archive(id);
    }

    @GetMapping("/{id}/contactos")
    public List<EntidadDtos.Contacto> contactos(@PathVariable Long id) {
        controlAcceso.requireReadPersonalData(ContextoAccesoActual.require(), id);
        return servicioEntidad.contacts(id);
    }

    @PostMapping("/{id}/contactos")
    @ResponseStatus(HttpStatus.CREATED)
    public EntidadDtos.Contacto anadirContacto(@PathVariable Long id,
                                                @Valid @RequestBody EntidadDtos.CrearContacto entrada) {
        controlAcceso.requireUpdatePersonalData(ContextoAccesoActual.require(), id);
        return servicioEntidad.addContact(id, entrada);
    }

    @PatchMapping("/{id}/contactos/{contactoId}")
    public EntidadDtos.Contacto modificarContacto(@PathVariable Long id, @PathVariable Long contactoId,
                                                   @RequestBody EntidadDtos.ModificarContacto entrada) {
        controlAcceso.requireUpdatePersonalData(ContextoAccesoActual.require(), id);
        return servicioEntidad.updateContact(id, contactoId, entrada);
    }

    @DeleteMapping("/{id}/contactos/{contactoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarContacto(@PathVariable Long id, @PathVariable Long contactoId) {
        controlAcceso.requireUpdatePersonalData(ContextoAccesoActual.require(), id);
        servicioEntidad.deleteContact(id, contactoId);
    }

    @GetMapping("/{id}/relaciones")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.LEER')")
    public List<EntidadDtos.Relacion> relaciones(@PathVariable Long id) {
        return servicioEntidad.relations(id);
    }

    @PostMapping("/{id}/relaciones")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.MODIFICAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public EntidadDtos.Relacion anadirRelacion(@PathVariable Long id,
                                                @Valid @RequestBody EntidadDtos.CrearRelacion entrada) {
        return servicioEntidad.addRelation(id, entrada);
    }

    @PatchMapping("/{id}/relaciones/{relacionId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.MODIFICAR')")
    public EntidadDtos.Relacion modificarRelacion(@PathVariable Long id, @PathVariable Long relacionId,
                                                   @RequestBody EntidadDtos.ModificarRelacion entrada) {
        return servicioEntidad.updateRelation(id, relacionId, entrada);
    }

    @DeleteMapping("/{id}/relaciones/{relacionId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'ENTIDAD.MODIFICAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finalizarRelacion(@PathVariable Long id, @PathVariable Long relacionId) {
        servicioEntidad.endRelation(id, relacionId);
    }

    @GetMapping("/{id}/cuentas")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CUENTA.LEER')")
    public List<CuentaDtos.Cuenta> cuentas(@PathVariable Long id) {
        return servicioEntidad.accounts(id);
    }

    @GetMapping("/{id}/documentos")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'DOCUMENTO.LEER')")
    public List<DocumentoDto> documentos(@PathVariable Long id) {
        return servicioDocumento.porDestinatario(id);
    }
}
