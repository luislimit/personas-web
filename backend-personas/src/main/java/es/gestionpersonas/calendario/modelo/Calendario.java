package es.gestionpersonas.calendario.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;

@Entity
@Table(name = "calendario")
public class Calendario extends EntidadAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entidad_propietaria_id", nullable = false)
    private Long ownerEntityId;

    @Column(name = "nombre", nullable = false, length = 180)
    private String name;

    @Column(name = "descripcion", length = 500)
    private String description;

    @Column(name = "color_predeterminado", length = 20)
    private String defaultColor;

    @Column(nullable = false, length = 80)
    private String zona_horaria = "Europe/Madrid";

    @Column(name = "activo", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public Long getOwnerEntityId() { return ownerEntityId; }
    public void setOwnerEntityId(Long ownerEntityId) { this.ownerEntityId = ownerEntityId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDefaultColor() { return defaultColor; }
    public void setDefaultColor(String defaultColor) { this.defaultColor = defaultColor; }
    public String getTimezone() { return zona_horaria; }
    public void setTimezone(String zona_horaria) { this.zona_horaria = zona_horaria; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
