package es.gestionpersonas.seguridad.repositorio;
import es.gestionpersonas.seguridad.modelo.PoliticaMfa; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface RepositorioPoliticaMfa extends JpaRepository<PoliticaMfa,Long>{ List<PoliticaMfa> findByActivoTrueOrderByIdAsc(); }
