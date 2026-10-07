package es.gestionpersonas.solicitud.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public final class SolicitudDtos {
    private SolicitudDtos() {}

    public record EntradaParticipante(@NotNull Long entidadId, @NotBlank String rolParticipante) {}
    public record CrearSolicitud(@NotBlank String codigoTipoSolicitud, @NotBlank @Size(max=220) String asunto, String descripcion, @NotNull Long creadoPorEntidadId, @NotEmpty List<@Valid EntradaParticipante> participantes) {}
    public record AgregarMensaje(@NotNull Long entidadRemitenteId, @NotBlank String mensaje) {}
    public record ModificarSolicitud(String asunto, String descripcion, String estadoGlobal, Long eventoGeneradoId, Long documentoGeneradoId) {}
    public record ModificarParticipante(@NotBlank String estado, Boolean marcarLeido) {}
    public record EntradaAccion(Long entidadActorId, @NotBlank String tipoAccion, String estadoGlobal, Long entidadParticipanteId, String rolParticipante, String estadoParticipante, Map<String,Object> datos) {}
    public record VistaParticipante(Long entidadId, String nombreEntidad, String rolParticipante, String estado, LocalDate fechaLectura, LocalDate fechaRespuesta) {}
    public record VistaMensaje(Long id, Long entidadRemitenteId, String nombreRemitente, String mensaje, LocalDate fechaCreacion) {}
    public record VistaAccion(Long id, Long entidadActorId, String nombreActor, String tipoAccion, LocalDateTime fechaCreacion, Map<String,Object> datos) {}
    public record Resumen(Long id, String codigoTipoSolicitud, String nombreTipoSolicitud, String asunto, Long creadoPorEntidadId, String nombreCreador, LocalDate fechaCreacion, String estadoGlobal) {}
    public record Detalle(Long id, String codigoTipoSolicitud, String nombreTipoSolicitud, String asunto, String descripcion, Long creadoPorEntidadId, String nombreCreador, LocalDate fechaCreacion, String estadoGlobal, LocalDate fechaResolucion, LocalDate fechaCierre, Long solicitudPadreId, Long eventoGeneradoId, Long documentoGeneradoId, List<VistaParticipante> participantes, List<VistaMensaje> mensajes, List<VistaAccion> acciones) {}
}
