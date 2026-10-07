package es.gestionpersonas.documento.api;

import es.gestionpersonas.documento.dto.*;
import es.gestionpersonas.documento.servicio.ServicioDocumento;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@Tag(name="Documentos",description="Gestión documental, accesos y sustituciones")
public class DocumentoController {
    private final ServicioDocumento servicio;
    public DocumentoController(ServicioDocumento servicio){this.servicio=servicio;}

    @GetMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'DOCUMENTO.LEER')")
    public DocumentoDto obtener(@PathVariable Long id){return servicio.obtener(id);}

    @GetMapping("/{id}/accesos")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'DOCUMENTO.LEER')")
    public List<DocumentoDtos.Acceso> accesos(@PathVariable Long id){return servicio.accesos(id);}

    @PostMapping
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'DOCUMENTO.CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDto crear(@Valid @RequestBody DocumentoDtos.Crear entrada){return servicio.crear(entrada);}

    @PostMapping("/{id}/acceder")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'DOCUMENTO.LEER')")
    public DocumentoDtos.ResultadoAcceso acceder(@PathVariable Long id){return servicio.acceder(id);}

    @PatchMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'DOCUMENTO.MODIFICAR')")
    public DocumentoDto modificar(@PathVariable Long id,@RequestBody DocumentoDtos.Modificar entrada){return servicio.modificar(id,entrada);}

    @DeleteMapping("/{id}")
    @PreAuthorize("@autorizacionSegura.permitido(authentication,'DOCUMENTO.ELIMINAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id){servicio.eliminar(id);}
}
