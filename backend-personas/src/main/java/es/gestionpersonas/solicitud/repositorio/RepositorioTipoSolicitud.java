package es.gestionpersonas.solicitud.repositorio;
import es.gestionpersonas.solicitud.modelo.TipoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RepositorioTipoSolicitud extends JpaRepository<TipoSolicitud, Long> {
    Optional<TipoSolicitud> findByCode(String code);
}
