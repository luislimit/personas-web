package es.gestionpersonas.calendario.repositorio;

import es.gestionpersonas.calendario.modelo.ExcepcionRecurrenciaEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepositorioExcepcionRecurrenciaEvento extends JpaRepository<ExcepcionRecurrenciaEvento, Long> {
    List<ExcepcionRecurrenciaEvento> findByRecurrenceIdIn(Collection<Long> recurrenceIds);
    Optional<ExcepcionRecurrenciaEvento> findByRecurrenceIdAndOriginalStartsAt(Long recurrenceId, LocalDateTime originalStartsAt);
}
