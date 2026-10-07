package es.gestionpersonas.seguridad.api;

import es.gestionpersonas.seguridad.dto.ReglaAlcanceDtos;
import es.gestionpersonas.seguridad.servicio.ServicioReglaAlcance;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas/{cuentaId}/contextos/{contextoCuentaId}/reglas-alcance")
@Tag(name = "Reglas de alcance", description = "Reglas personalizadas de autorización por contexto")
public class ReglaAlcanceController {
    private final ServicioReglaAlcance servicio;
    public ReglaAlcanceController(ServicioReglaAlcance servicio) { this.servicio = servicio; }

    @GetMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.LEER')")
    public List<ReglaAlcanceDtos.Regla> listar(@PathVariable Long cuentaId, @PathVariable Long contextoCuentaId) {
        return servicio.listar(cuentaId, contextoCuentaId);
    }

    @PostMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ReglaAlcanceDtos.Regla crear(@PathVariable Long cuentaId, @PathVariable Long contextoCuentaId,
                                        @Valid @RequestBody ReglaAlcanceDtos.CrearRegla entrada) {
        return servicio.crear(cuentaId, contextoCuentaId, entrada);
    }

    @PutMapping("/{reglaId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.MODIFICAR')")
    public ReglaAlcanceDtos.Regla reemplazar(@PathVariable Long cuentaId, @PathVariable Long contextoCuentaId,
                                              @PathVariable Long reglaId,
                                              @Valid @RequestBody ReglaAlcanceDtos.ReemplazarRegla entrada) {
        return servicio.reemplazar(cuentaId, contextoCuentaId, reglaId, entrada);
    }

    @DeleteMapping("/{reglaId}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'CONTEXTO.ELIMINAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long cuentaId, @PathVariable Long contextoCuentaId, @PathVariable Long reglaId) {
        servicio.eliminar(cuentaId, contextoCuentaId, reglaId);
    }
}
