package es.gestionpersonas.solicitud.repositorio;
import es.gestionpersonas.solicitud.modelo.MensajeSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RepositorioMensajeSolicitud extends JpaRepository<MensajeSolicitud, Long> {
    List<MensajeSolicitud> findByRequestIdOrderByMessageDateAscIdAsc(Long requestId);
}
