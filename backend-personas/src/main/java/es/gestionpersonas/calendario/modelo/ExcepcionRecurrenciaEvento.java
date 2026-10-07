package es.gestionpersonas.calendario.modelo;


import es.gestionpersonas.auditoria.EntidadAuditable;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "excepcion_recurrencia_evento")
public class ExcepcionRecurrenciaEvento extends EntidadAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recurrencia_id", nullable = false)
    private Long recurrenceId;

    @Column(name = "fecha_inicio_original", nullable = false)
    private LocalDateTime originalStartsAt;

    @Column(name = "tipo_excepcion", nullable = false, length = 20)
    private String exceptionType;

    @Column(name = "evento_sustitucion_id")
    private Long replacementEventId;

    public Long getId() { return id; }
    public Long getRecurrenceId() { return recurrenceId; }
    public void setRecurrenceId(Long recurrenceId) { this.recurrenceId = recurrenceId; }
    public LocalDateTime getOriginalStartsAt() { return originalStartsAt; }
    public void setOriginalStartsAt(LocalDateTime originalStartsAt) { this.originalStartsAt = originalStartsAt; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public Long getReplacementEventId() { return replacementEventId; }
    public void setReplacementEventId(Long replacementEventId) { this.replacementEventId = replacementEventId; }
}
