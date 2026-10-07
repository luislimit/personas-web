package es.gestionpersonas.seguridad.repositorio;

import es.gestionpersonas.seguridad.modelo.ReglaAlcanceAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RepositorioReglaAlcanceAcceso extends JpaRepository<ReglaAlcanceAcceso, Long> {
    List<ReglaAlcanceAcceso> findByContextoCuentaIdOrderByPrioridadDescIdAsc(Long contextoCuentaId);
    List<ReglaAlcanceAcceso> findByContextoCuentaIdAndActivoTrueOrderByPrioridadDescIdAsc(Long contextoCuentaId);
    Optional<ReglaAlcanceAcceso> findByIdAndContextoCuentaId(Long id, Long contextoCuentaId);
}
