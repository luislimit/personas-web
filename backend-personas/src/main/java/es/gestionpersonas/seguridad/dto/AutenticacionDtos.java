package es.gestionpersonas.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class AutenticacionDtos {
    private AutenticacionDtos() {}

    public record SolicitudInicioSesion(@NotBlank String nombreUsuario, @NotBlank String contrasena) {}

    public record VistaContexto(
        Long contextoCuentaId,
        Long entidadContextoId,
        String nombreContexto,
        String tipoContexto,
        Long rolSeguridadId,
        String codigoRol,
        String nombreRol,
        String alcance,
        LocalDate validoDesde,
        LocalDate validoHasta
    ) {}

    public record RespuestaInicioSesion(
        String tokenAcceso,
        String tipoToken,
        LocalDateTime fechaExpiracion,
        Long cuentaId,
        Long personaEntidadId,
        String nombreUsuario,
        List<VistaContexto> contextos
    ) {}

    public record RespuestaYo(
        Long cuentaId,
        Long personaEntidadId,
        String nombreUsuario,
        String correoAcceso,
        List<VistaContexto> contextos
    ) {}

    public record RespuestaContextoActivo(VistaContexto contexto, Set<String> permisos) {}
}
