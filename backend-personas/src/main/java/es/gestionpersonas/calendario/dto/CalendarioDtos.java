package es.gestionpersonas.calendario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public final class CalendarioDtos {
    private CalendarioDtos() {}

    public enum FrecuenciaRecurrencia { DIARIA, SEMANAL, MENSUAL, ANUAL }
    public enum AlcanceRecurrencia { SOLO_ESTE, ESTE_Y_SIGUIENTES, TODA_LA_SERIE }

    public record VistaCalendario(
        Long id,
        Long entidadPropietariaId,
        String nombrePropietario,
        String nombre,
        String descripcion,
        String colorPredeterminado,
        String zonaHoraria,
        boolean activo
    ) {}

    public record GuardarCalendario(
        @NotNull Long entidadPropietariaId,
        @NotBlank @Size(max = 180) String nombre,
        @Size(max = 500) String descripcion,
        @Size(max = 20) String colorPredeterminado,
        @Size(max = 80) String zonaHoraria,
        Boolean activo
    ) {}

    public record EntradaRecurrencia(
        @NotNull FrecuenciaRecurrencia frecuencia,
        @Min(1) Integer intervalo,
        List<@Pattern(regexp = "MO|TU|WE|TH|FR|SA|SU") String> diasSemana,
        List<@Min(1) @Max(31) Integer> diasMes,
        LocalDateTime hasta,
        @Min(1) Integer numeroOcurrencias,
        @Size(max = 80) String zonaHoraria
    ) {}

    public record GuardarEvento(
        @NotBlank @Size(max = 220) String titulo,
        String descripcion,
        @NotNull LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Boolean diaCompleto,
        @Size(max = 80) String zonaHoraria,
        Long entidadUbicacionId,
        @Pattern(regexp = "PROGRAMADO|CANCELADO|COMPLETADO") String estado,
        Long creadoPorCuentaId,
        @Valid EntradaRecurrencia recurrencia
    ) {}

    public record ModificarEvento(
        @Size(min = 1, max = 220) String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Boolean diaCompleto,
        @Size(max = 80) String zonaHoraria,
        Long entidadUbicacionId,
        @Pattern(regexp = "PROGRAMADO|CANCELADO|COMPLETADO") String estado,
        @Valid EntradaRecurrencia recurrencia
    ) {}

    public record VistaRecurrencia(
        Long id,
        FrecuenciaRecurrencia frecuencia,
        int intervalo,
        List<String> diasSemana,
        List<Integer> diasMes,
        LocalDateTime hasta,
        Integer numeroOcurrencias,
        String zonaHoraria,
        boolean activo
    ) {}

    public record VistaEvento(
        Long id,
        Long eventoSerieId,
        Long calendarioId,
        String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        LocalDateTime fechaInicioOriginal,
        boolean diaCompleto,
        String zonaHoraria,
        Long entidadUbicacionId,
        String nombreUbicacion,
        String estado,
        boolean recurrente,
        boolean ocurrenciaModificada,
        VistaRecurrencia recurrencia
    ) {}
}
