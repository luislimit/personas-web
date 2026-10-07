package es.gestionpersonas.documento.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DocumentoDtos {
    private DocumentoDtos() {}

    public record Crear(
        @NotBlank String codigoTipo,
        @NotNull Long entidadEmisoraId,
        @NotNull Long entidadDestinatariaId,
        @NotBlank @Size(max=220) String nombre,
        String descripcion,
        @NotBlank String referenciaArchivo,
        LocalDate fechaDocumento,
        Long sustituyeADocumentoId
    ) {}

    public record Modificar(
        String codigoTipo,
        Long entidadEmisoraId,
        Long entidadDestinatariaId,
        String nombre,
        String descripcion,
        String referenciaArchivo,
        LocalDate fechaDocumento,
        String estado
    ) {}

    public record Acceso(
        Long id,
        Long cuentaUsuarioId,
        String nombreUsuario,
        String nombrePersona,
        LocalDateTime fechaAcceso
    ) {}

    public record ResultadoAcceso(
        Long accesoId,
        Long documentoId,
        String referenciaArchivo,
        LocalDateTime fechaAcceso
    ) {}
}
