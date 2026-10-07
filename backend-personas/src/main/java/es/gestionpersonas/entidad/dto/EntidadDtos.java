package es.gestionpersonas.entidad.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;

public final class EntidadDtos {
    private EntidadDtos() {}

    public record Resumen(Long id, String codigoTipo, String nombre, String estado) {}
    public record DatosPersona(String nombre, String primerApellido, String segundoApellido, LocalDate fechaNacimiento, String genero) {}
    public record Detalle(Long id, String codigoTipo, String nombre, String estado, LocalDateTime fechaCreacion, LocalDateTime fechaModificacion, DatosPersona persona) {}
    public record Contacto(Long id, String tipo, String valor, String etiqueta, boolean preferido, boolean activo, LocalDate fechaVerificacion, LocalDate validoDesde, LocalDate validoHasta) {}
    public record Relacion(Long id, Long entidadRelacionadaId, String nombreEntidadRelacionada, String codigoRelacion, String nombreRelacion, String direccion, LocalDate validoDesde, LocalDate validoHasta, String estado) {}

    public record EntradaPersona(@NotBlank String nombre, @NotBlank String primerApellido, String segundoApellido, LocalDate fechaNacimiento, String genero) {}
    public record CrearEntidad(@NotBlank String codigoTipo, @NotBlank @Size(max=200) String nombre, String estado, @Valid EntradaPersona persona) {}
    public record ModificarEntidad(@Size(max=200) String nombre, String estado, @Valid EntradaPersona persona) {}
    public record CrearContacto(@NotBlank String tipo, @NotBlank @Size(max=255) String valor, String etiqueta, Boolean preferido, Boolean activo, LocalDate fechaVerificacion, LocalDate validoDesde, LocalDate validoHasta) {}
    public record ModificarContacto(String tipo, String valor, String etiqueta, Boolean preferido, Boolean activo, LocalDate fechaVerificacion, LocalDate validoDesde, LocalDate validoHasta) {}
    public record CrearRelacion(@NotNull Long entidadDestinoId, @NotBlank String codigoTipoRelacion, LocalDate validoDesde, LocalDate validoHasta, String estado) {}
    public record ModificarRelacion(LocalDate validoDesde, LocalDate validoHasta, String estado) {}
}
