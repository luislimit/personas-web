package es.gestionpersonas.documento.repositorio;

import es.gestionpersonas.documento.modelo.AccesoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RepositorioAccesoDocumento extends JpaRepository<AccesoDocumento, Long> {
    long countByDocumentoId(Long documentoId);
    boolean existsByDocumentoId(Long documentoId);
    List<AccesoDocumento> findByDocumentoIdOrderByFechaAccesoDescIdDesc(Long documentoId);
}
