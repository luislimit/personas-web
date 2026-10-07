package es.gestionpersonas.calendario.repositorio;

import es.gestionpersonas.calendario.modelo.EventoCalendario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RepositorioEventoCalendario extends JpaRepository<EventoCalendario, Long> {
    List<EventoCalendario> findByCalendarIdOrderByStartsAtAsc(Long calendarId);
    List<EventoCalendario> findByCalendarIdAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
        Long calendarId, LocalDateTime to, LocalDateTime from);
    List<EventoCalendario> findByCalendarIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
        Long calendarId, LocalDateTime from, LocalDateTime to);
}
