package es.gestionpersonas.seguridad.dto;

public final class AdministracionSeguridadDtos {
    private AdministracionSeguridadDtos() {}

    public record RolSeguridad(Long id, String codigo, String nombre) {}
    public record Permiso(Long id, String codigo, String descripcion) {}
}
