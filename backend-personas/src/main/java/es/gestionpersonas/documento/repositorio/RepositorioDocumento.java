package es.gestionpersonas.documento.repositorio;

import es.gestionpersonas.documento.modelo.Documento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RepositorioDocumento extends JpaRepository<Documento, Long> {
    List<Documento> findByEntidadDestinatariaIdOrderByFechaDocumentoDescIdDesc(Long entidadDestinatariaId);
    boolean existsBySustituyeADocumentoId(Long documentoId);
    boolean existsBySustituyeADocumentoIdAndIdNot(Long documentoId, Long idExcluir);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Documento d where d.id=:id")
    Optional<Documento> buscarPorIdParaActualizar(@Param("id") Long id);
}
