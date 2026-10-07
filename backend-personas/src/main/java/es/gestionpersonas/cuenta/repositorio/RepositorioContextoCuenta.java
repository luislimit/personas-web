package es.gestionpersonas.cuenta.repositorio;

import es.gestionpersonas.cuenta.modelo.ContextoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RepositorioContextoCuenta extends JpaRepository<ContextoCuenta,Long> {
    List<ContextoCuenta> findByAccountIdOrderByIdAsc(Long accountId);
    Optional<ContextoCuenta> findByIdAndAccountId(Long id,Long accountId);
    Optional<ContextoCuenta> findByAccountIdAndContextEntityId(Long accountId,Long contextId);
    boolean existsByAccountIdAndContextEntityId(Long accountId,Long contextId);
}
