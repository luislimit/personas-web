package es.gestionpersonas.solicitud.repositorio;

import es.gestionpersonas.solicitud.modelo.Solicitud;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RepositorioSolicitud extends JpaRepository<Solicitud, Long> {
    @Query("select r from Solicitud r join fetch r.requestType order by r.requestDate desc, r.id desc")
    List<Solicitud> findAllDetailed();

    @Query("select r from Solicitud r join fetch r.requestType where r.id = :id")
    Optional<Solicitud> findDetailedById(@Param("id") Long id);
}
