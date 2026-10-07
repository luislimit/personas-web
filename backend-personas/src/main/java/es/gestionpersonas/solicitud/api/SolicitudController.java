package es.gestionpersonas.solicitud.api;

import es.gestionpersonas.solicitud.dto.SolicitudDtos;
import es.gestionpersonas.solicitud.servicio.ServicioSolicitud;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
@Tag(name = "Solicitudes", description = "Flujo genérico de solicitudes")
public class SolicitudController {
    private final ServicioSolicitud servicio;
    public SolicitudController(ServicioSolicitud servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.LEER')")
    public List<SolicitudDtos.Resumen> listar() { return servicio.list(); }

    @GetMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.LEER')")
    public SolicitudDtos.Detalle obtener(@PathVariable Long id) { return servicio.get(id); }

    @PostMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDtos.Detalle crear(@Valid @RequestBody SolicitudDtos.CrearSolicitud entrada) {
        return servicio.create(entrada);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.MODIFICAR')")
    public SolicitudDtos.Detalle modificar(@PathVariable Long id, @RequestBody SolicitudDtos.ModificarSolicitud entrada) {
        return servicio.update(id, entrada);
    }

    @PostMapping("/{id}/mensajes")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.MODIFICAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDtos.VistaMensaje anadirMensaje(@PathVariable Long id,
                                                     @Valid @RequestBody SolicitudDtos.AgregarMensaje entrada) {
        return servicio.addMessage(id, entrada);
    }

    @PostMapping("/{id}/participantes")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.MODIFICAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDtos.VistaParticipante anadirParticipante(@PathVariable Long id,
                                                               @Valid @RequestBody SolicitudDtos.EntradaParticipante entrada) {
        return servicio.addParticipant(id, entrada);
    }

    @PatchMapping("/{id}/participantes/{entidadId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.MODIFICAR')")
    public SolicitudDtos.VistaParticipante modificarParticipante(
        @PathVariable Long id, @PathVariable Long entidadId,
        @RequestParam String rolParticipante,
        @Valid @RequestBody SolicitudDtos.ModificarParticipante entrada) {
        return servicio.updateParticipant(id, entidadId, rolParticipante, entrada);
    }

    @DeleteMapping("/{id}/participantes/{entidadId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.MODIFICAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarParticipante(@PathVariable Long id, @PathVariable Long entidadId,
                                     @RequestParam String rolParticipante) {
        servicio.removeParticipant(id, entidadId, rolParticipante);
    }

    @PostMapping("/{id}/acciones")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'SOLICITUD.MODIFICAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudDtos.VistaAccion accion(@PathVariable Long id,
                                             @Valid @RequestBody SolicitudDtos.EntradaAccion entrada) {
        return servicio.action(id, entrada);
    }
}
