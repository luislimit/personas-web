package es.gestionpersonas.documento.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentoDto(
    Long id,
    Long entidadEmisoraId,
    Long entidadDestinatariaId,
    String codigoTipo,
    String nombreTipo,
    Integer maximoAccesos,
    String codigoClasificacion,
    String nombreClasificacion,
    String codigoCategoriaSensible,
    String nombreCategoriaSensible,
    String nombre,
    String descripcion,
    LocalDate fechaDocumento,
    String estado,
    Long sustituyeADocumentoId,
    Long creadoPorCuentaId,
    LocalDateTime fechaCreacion,
    LocalDate fechaDisponibilidad,
    long numeroAccesos
) {}
