package es.gestionpersonas.entidad.repositorio;
import es.gestionpersonas.entidad.modelo.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RepositorioPersona extends JpaRepository<Persona, Long> {}
