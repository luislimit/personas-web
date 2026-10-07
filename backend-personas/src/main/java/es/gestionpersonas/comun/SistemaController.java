package es.gestionpersonas.comun;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/sistema")
@Tag(name = "Sistema", description = "Estado técnico del servicio")
public class SistemaController {
    private final EntityManager em;
    public SistemaController(EntityManager em) { this.em = em; }

    @GetMapping("/salud")
    @Operation(summary = "Comprobar API y conexión con MySQL")
    public Map<String, Object> salud() {
        Object baseDatos = em.createNativeQuery("select database()").getSingleResult();
        Object version = em.createNativeQuery("select version()").getSingleResult();
        return Map.of("estado", "ACTIVO", "baseDatos", baseDatos, "versionMySQL", version);
    }
}
