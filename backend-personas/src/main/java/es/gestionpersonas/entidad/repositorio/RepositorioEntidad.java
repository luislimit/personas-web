package es.gestionpersonas.entidad.repositorio;

import es.gestionpersonas.entidad.modelo.Entidad;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RepositorioEntidad extends JpaRepository<Entidad, Long> {
    @Query("""
        select e from Entidad e
        join fetch e.entityType t
        where (:type is null or t.code = :type)
          and (:status is null or e.status = :status)
          and (:name is null or lower(e.name) like lower(concat('%', :name, '%')))
        order by e.name
        """)
    List<Entidad> search(@Param("type") String type,
                            @Param("status") String status,
                            @Param("name") String name);

    @Query("select e from Entidad e join fetch e.entityType where e.id = :id")
    java.util.Optional<Entidad> findDetailedById(@Param("id") Long id);
}
