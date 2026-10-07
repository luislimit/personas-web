package es.gestionpersonas.calendario.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurrencia_evento")
public class RecurrenciaEvento extends EntidadAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, unique = true)
    private Long eventId;

    @Column(nullable = false, length = 20)
    private String frecuencia;

    @Column(name = "intervalo", nullable = false)
    private int intervalValue = 1;

    @Column(name = "por_dia_semana", length = 80)
    private String byDay;

    @Column(name = "por_dia_mes", length = 120)
    private String byMonthDay;

    @Column(name = "hasta_fecha")
    private LocalDateTime untilAt;

    @Column(name = "numero_ocurrencias")
    private Integer occurrenceCount;

    @Column(nullable = false, length = 80)
    private String zona_horaria = "Europe/Madrid";

    @Column(name = "activo", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getFrequency() { return frecuencia; }
    public void setFrequency(String frecuencia) { this.frecuencia = frecuencia; }
    public int getIntervalValue() { return intervalValue; }
    public void setIntervalValue(int intervalValue) { this.intervalValue = intervalValue; }
    public String getByDay() { return byDay; }
    public void setByDay(String byDay) { this.byDay = byDay; }
    public String getByMonthDay() { return byMonthDay; }
    public void setByMonthDay(String byMonthDay) { this.byMonthDay = byMonthDay; }
    public LocalDateTime getUntilAt() { return untilAt; }
    public void setUntilAt(LocalDateTime untilAt) { this.untilAt = untilAt; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public String getTimezone() { return zona_horaria; }
    public void setTimezone(String zona_horaria) { this.zona_horaria = zona_horaria; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
