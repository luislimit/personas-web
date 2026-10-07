package es.gestionpersonas.calendario.repositorio;

import es.gestionpersonas.calendario.modelo.RecurrenciaEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepositorioRecurrenciaEvento extends JpaRepository<RecurrenciaEvento, Long> {
    Optional<RecurrenciaEvento> findByEventId(Long eventId);
    List<RecurrenciaEvento> findByEventIdInAndActiveTrue(Collection<Long> eventIds);
}
