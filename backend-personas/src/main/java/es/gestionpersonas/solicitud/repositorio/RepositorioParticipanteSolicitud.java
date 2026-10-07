package es.gestionpersonas.solicitud.repositorio;
import es.gestionpersonas.solicitud.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RepositorioParticipanteSolicitud extends JpaRepository<ParticipanteSolicitud, ParticipanteSolicitudId> {
    List<ParticipanteSolicitud> findByIdRequestIdOrderByIdRoleAsc(Long requestId);
}
