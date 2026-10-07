package es.gestionpersonas.calendario.api;

import es.gestionpersonas.calendario.dto.CalendarioDtos;
import es.gestionpersonas.calendario.servicio.ServicioCalendario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Edición y eliminación de eventos, incluidas series recurrentes")
public class EventoController {
    private final ServicioCalendario servicio;
    public EventoController(ServicioCalendario servicio) { this.servicio = servicio; }

    @GetMapping("/{eventoId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'EVENTO.LEER')")
    @Operation(summary = "Obtener evento u ocurrencia")
    public CalendarioDtos.VistaEvento evento(
        @PathVariable Long eventoId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicioOcurrencia) {
        return servicio.event(eventoId, fechaInicioOcurrencia);
    }

    @PatchMapping("/{eventoId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'EVENTO.MODIFICAR')")
    @Operation(summary = "Modificar evento: solo esta ocurrencia, esta y siguientes o toda la serie")
    public CalendarioDtos.VistaEvento modificar(
        @PathVariable Long eventoId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicioOcurrencia,
        @RequestParam(required = false) CalendarioDtos.AlcanceRecurrencia alcance,
        @Valid @RequestBody CalendarioDtos.ModificarEvento entrada) {
        return servicio.updateEvent(eventoId, fechaInicioOcurrencia, alcance, entrada);
    }

    @DeleteMapping("/{eventoId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'EVENTO.ELIMINAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar o cancelar evento: solo esta ocurrencia, esta y siguientes o toda la serie")
    public void eliminar(
        @PathVariable Long eventoId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicioOcurrencia,
        @RequestParam(required = false) CalendarioDtos.AlcanceRecurrencia alcance) {
        servicio.deleteEvent(eventoId, fechaInicioOcurrencia, alcance);
    }
}
