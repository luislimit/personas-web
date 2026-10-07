package es.gestionpersonas.seguridad.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class ReglaAlcanceDtos {
    private ReglaAlcanceDtos() {}

    public record Regla(
        Long id, Long contextoCuentaId, String tipoRegla,
        Long tipoRelacionId, String codigoTipoRelacion,
        Long rolFuncionalId, String codigoRolFuncional,
        Long tipoEntidadObjetivoId, String codigoTipoEntidadObjetivo,
        boolean incluirDescendientes, String efecto, int prioridad, boolean activo,
        LocalDateTime fechaCreacion, LocalDateTime fechaModificacion
    ) {}

    public record CrearRegla(
        @NotBlank String tipoRegla,
        String codigoTipoRelacion,
        String codigoRolFuncional,
        String codigoTipoEntidadObjetivo,
        Boolean incluirDescendientes,
        @NotBlank String efecto,
        Integer prioridad,
        Boolean activo
    ) {}

    public record ReemplazarRegla(
        @NotBlank String tipoRegla,
        String codigoTipoRelacion,
        String codigoRolFuncional,
        String codigoTipoEntidadObjetivo,
        Boolean incluirDescendientes,
        @NotBlank String efecto,
        Integer prioridad,
        Boolean activo
    ) {}
}
