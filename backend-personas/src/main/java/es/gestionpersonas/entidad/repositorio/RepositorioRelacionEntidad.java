package es.gestionpersonas.entidad.repositorio;
import es.gestionpersonas.entidad.modelo.RelacionEntidad;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface RepositorioRelacionEntidad extends JpaRepository<RelacionEntidad,Long>{
 @Query("select r from RelacionEntidad r join fetch r.relationType where r.sourceEntityId = :entityId or r.targetEntityId = :entityId order by r.id")
 List<RelacionEntidad> findAllForEntity(@Param("entityId") Long entityId);
 @Query("select r from RelacionEntidad r join fetch r.relationType where r.id = :id")
 Optional<RelacionEntidad> findDetailedById(@Param("id") Long id);
}
