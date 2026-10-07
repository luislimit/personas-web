package es.gestionpersonas.calendario.repositorio;
import es.gestionpersonas.calendario.modelo.Calendario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RepositorioCalendario extends JpaRepository<Calendario, Long> {
    List<Calendario> findAllByOrderByNameAsc();
    List<Calendario> findByOwnerEntityIdOrderByNameAsc(Long ownerEntityId);
}
