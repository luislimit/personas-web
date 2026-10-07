package es.gestionpersonas.seguridad.repositorio;
import es.gestionpersonas.seguridad.modelo.DesafioMfa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RepositorioDesafioMfa extends JpaRepository<DesafioMfa,Long>{ List<DesafioMfa> findBySesionIdOrderByIdDesc(Long sesionId); Optional<DesafioMfa> findByIdAndSesionId(Long id,Long sesionId); }
