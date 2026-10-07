package es.gestionpersonas.cuenta.repositorio;

import es.gestionpersonas.cuenta.modelo.CuentaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RepositorioCuentaUsuario extends JpaRepository<CuentaUsuario,Long> {
    List<CuentaUsuario> findByPersonEntityIdOrderByPreferredDescIdAsc(Long personEntityId);
    List<CuentaUsuario> findByPersonEntityIdAndStatusAndPreferredTrue(Long personEntityId,String status);
    boolean existsByUsername(String username);
    Optional<CuentaUsuario> findByUsername(String username);
}
