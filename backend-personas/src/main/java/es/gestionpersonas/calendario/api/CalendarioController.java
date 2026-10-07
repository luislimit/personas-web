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
import java.util.List;

@RestController
@RequestMapping("/api/calendarios")
@Tag(name = "Calendarios", description = "Calendarios y eventos")
public class CalendarioController {
    private final ServicioCalendario servicio;
    public CalendarioController(ServicioCalendario servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CALENDARIO.LEER')")
    @Operation(summary = "Listar calendarios")
    public List<CalendarioDtos.VistaCalendario> calendarios(
        @RequestParam(required = false) Long entidadPropietariaId) {
        return servicio.calendars(entidadPropietariaId);
    }

    @GetMapping("/{calendarioId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CALENDARIO.LEER')")
    @Operation(summary = "Obtener un calendario")
    public CalendarioDtos.VistaCalendario calendario(@PathVariable Long calendarioId) {
        return servicio.calendario(calendarioId);
    }

    @PostMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CALENDARIO.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear calendario")
    public CalendarioDtos.VistaCalendario crearCalendario(
        @Valid @RequestBody CalendarioDtos.GuardarCalendario entrada) {
        return servicio.createCalendar(entrada);
    }

    @PutMapping("/{calendarioId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CALENDARIO.MODIFICAR')")
    @Operation(summary = "Modificar calendario")
    public CalendarioDtos.VistaCalendario modificarCalendario(
        @PathVariable Long calendarioId,
        @Valid @RequestBody CalendarioDtos.GuardarCalendario entrada) {
        return servicio.updateCalendar(calendarioId, entrada);
    }

    @DeleteMapping("/{calendarioId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CALENDARIO.ELIMINAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar calendario")
    public void desactivarCalendario(@PathVariable Long calendarioId) {
        servicio.deactivateCalendar(calendarioId);
    }

    @GetMapping("/{calendarioId}/eventos")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'EVENTO.LEER')")
    @Operation(summary = "Eventos y ocurrencias de un calendario")
    public List<CalendarioDtos.VistaEvento> eventos(
        @PathVariable Long calendarioId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return servicio.events(calendarioId, desde, hasta);
    }

    @PostMapping("/{calendarioId}/eventos")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'EVENTO.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear evento único o recurrente")
    public CalendarioDtos.VistaEvento crearEvento(
        @PathVariable Long calendarioId,
        @Valid @RequestBody CalendarioDtos.GuardarEvento entrada) {
        return servicio.createEvent(calendarioId, entrada);
    }
}
