package es.gestionpersonas.cuenta.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class CuentaDtos {
    private CuentaDtos() {}

    public record Cuenta(
        Long id,
        Long personaEntidadId,
        String nombreUsuario,
        String correoAcceso,
        String proveedorAutenticacion,
        boolean preferido,
        String estado,
        LocalDateTime ultimoAcceso
    ) {}

    public record Contexto(
        Long id,
        Long cuentaId,
        Long entidadContextoId,
        String nombreContexto,
        Long rolSeguridadId,
        String codigoRol,
        String nombreRol,
        String alcance,
        boolean habilitado,
        LocalDate validoDesde,
        LocalDate validoHasta
    ) {}

    public record CrearCuenta(
        @NotNull Long personaEntidadId,
        @NotBlank @Size(max=150) String nombreUsuario,
        @Email String correoAcceso,
        @NotBlank @Size(min=4,max=200) String contrasena,
        String proveedorAutenticacion,
        String sujetoExterno,
        Boolean preferido,
        String estado
    ) {}

    public record ModificarCuenta(
        @Email String correoAcceso,
        @Size(min=4,max=200) String nuevaContrasena,
        String proveedorAutenticacion,
        String sujetoExterno,
        Boolean preferido,
        String estado
    ) {}

    public record CrearContexto(
        @NotNull Long entidadContextoId,
        @NotBlank String codigoRol,
        String alcance,
        Boolean habilitado,
        LocalDate validoDesde,
        LocalDate validoHasta
    ) {}

    public record ModificarContexto(
        String codigoRol,
        String alcance,
        Boolean habilitado,
        LocalDate validoDesde,
        LocalDate validoHasta
    ) {}

    public record ExcepcionPermiso(
        Long permisoId,
        String codigoPermiso,
        String descripcion,
        String efecto
    ) {}

    public record ModificarExcepcionPermiso(
        @NotBlank String efecto
    ) {}

    public record PermisoEfectivo(
        Long permisoId,
        String codigo,
        String descripcion,
        boolean concedidoPorRol,
        String excepcion,
        boolean efectivo,
        boolean requiereMfa,
        Integer minutosMfa
    ) {}

    public record AccesoEfectivo(
        Long cuentaId,
        Long contextoCuentaId,
        Long entidadContextoId,
        String nombreContexto,
        String codigoRol,
        String nombreRol,
        String alcance,
        boolean habilitado,
        boolean vigente,
        boolean mfaContextoRequerida,
        Integer minutosMfaContexto,
        List<PermisoEfectivo> permisos
    ) {}

    public record ExplicacionPermiso(
        String codigoPermiso,
        boolean efectivo,
        boolean concedidoPorRol,
        String excepcion,
        String motivo,
        boolean requiereMfa,
        Integer minutosMfa
    ) {}

    public record Sesion(
        Long id,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        LocalDateTime ultimoUso,
        LocalDateTime fechaRevocacion,
        String nivelAutenticacion,
        LocalDateTime mfaVerificadaEn,
        LocalDateTime mfaValidaHasta,
        boolean activa
    ) {}
}
