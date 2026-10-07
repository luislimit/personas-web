package es.gestionpersonas.seguridad.repositorio;

import es.gestionpersonas.seguridad.modelo.SesionAutenticacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RepositorioSesionAutenticacion extends JpaRepository<SesionAutenticacion, Long> {
    Optional<SesionAutenticacion> findByTokenHash(String tokenHash);
    List<SesionAutenticacion> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
