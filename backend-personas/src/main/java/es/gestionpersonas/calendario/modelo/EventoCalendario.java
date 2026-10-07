package es.gestionpersonas.calendario.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evento_calendario")
public class EventoCalendario extends EntidadAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calendario_id", nullable = false)
    private Long calendarId;

    @Column(name = "titulo", nullable = false, length = 220)
    private String title;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "fecha_fin")
    private LocalDateTime endsAt;

    @Column(name = "dia_completo", nullable = false)
    private boolean allDay;

    @Column(length = 80)
    private String zona_horaria;

    @Column(name = "entidad_ubicacion_id")
    private Long locationEntityId;

    @Column(name = "estado", nullable = false, length = 30)
    private String status = "PROGRAMADO";

    @Column(name = "creado_por_cuenta_id")
    private Long createdByAccountId;

    @Column(name = "fecha_creacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fecha_modificacion", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getCalendarId() { return calendarId; }
    public void setCalendarId(Long calendarId) { this.calendarId = calendarId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public boolean isAllDay() { return allDay; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }
    public String getTimezone() { return zona_horaria; }
    public void setTimezone(String zona_horaria) { this.zona_horaria = zona_horaria; }
    public Long getLocationEntityId() { return locationEntityId; }
    public void setLocationEntityId(Long locationEntityId) { this.locationEntityId = locationEntityId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedByAccountId() { return createdByAccountId; }
    public void setCreatedByAccountId(Long createdByAccountId) { this.createdByAccountId = createdByAccountId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
