package es.gestionpersonas.seguridad.api;
import es.gestionpersonas.seguridad.dto.MfaDtos; import es.gestionpersonas.seguridad.web.PrincipalSeguridad; import org.springframework.security.core.annotation.AuthenticationPrincipal; import es.gestionpersonas.seguridad.servicio.ServicioPoliticaMfa; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/seguridad/mfa/politicas")
public class PoliticaMfaController { private final ServicioPoliticaMfa s; public PoliticaMfaController(ServicioPoliticaMfa s){this.s=s;}
 @GetMapping @PreAuthorize("@autorizacionSegura.permitido(authentication,'MFA.POLITICA.LEER')") public List<MfaDtos.VistaPolitica> listar(@AuthenticationPrincipal PrincipalSeguridad p){return s.listar(p.acceso());}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@autorizacionSegura.permitido(authentication,'MFA.POLITICA.MODIFICAR')") public MfaDtos.VistaPolitica crear(@AuthenticationPrincipal PrincipalSeguridad p,@Valid @RequestBody MfaDtos.SolicitudPolitica d){return s.crear(p.acceso(),d);}
 @PutMapping("/{id}") @PreAuthorize("@autorizacionSegura.permitido(authentication,'MFA.POLITICA.MODIFICAR')") public MfaDtos.VistaPolitica actualizar(@AuthenticationPrincipal PrincipalSeguridad p,@PathVariable Long id,@Valid @RequestBody MfaDtos.SolicitudPolitica d){return s.actualizar(p.acceso(),id,d);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("@autorizacionSegura.permitido(authentication,'MFA.POLITICA.MODIFICAR')") public void eliminar(@AuthenticationPrincipal PrincipalSeguridad p,@PathVariable Long id){s.eliminar(p.acceso(),id);}
}
