package es.gestionpersonas.seguridad.api;
import es.gestionpersonas.seguridad.dto.MfaDtos; import es.gestionpersonas.seguridad.servicio.ServicioMfa; import es.gestionpersonas.seguridad.web.PrincipalSeguridad; import io.swagger.v3.oas.annotations.tags.Tag; import jakarta.validation.Valid; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/autenticacion/mfa") @Tag(name="MFA",description="Autenticación multifactor bajo demanda")
public class MfaController { private final ServicioMfa s; public MfaController(ServicioMfa s){this.s=s;}
 @GetMapping("/estado") public MfaDtos.EstadoMfa estado(@AuthenticationPrincipal PrincipalSeguridad p){return s.estado(p.sesionId());}
 @PostMapping("/desafios") public MfaDtos.RespuestaDesafio desafio(@AuthenticationPrincipal PrincipalSeguridad p,@Valid @RequestBody MfaDtos.SolicitudDesafio d){return s.crear(p.sesionId(),d);}
 @PostMapping("/verificar-codigo") public MfaDtos.RespuestaVerificacion codigo(@AuthenticationPrincipal PrincipalSeguridad p,@Valid @RequestBody MfaDtos.VerificarCodigo d){return s.verificarCodigo(p.sesionId(),d);}
 @PostMapping("/verificar-enlace") public MfaDtos.RespuestaVerificacion enlace(@AuthenticationPrincipal PrincipalSeguridad p,@Valid @RequestBody MfaDtos.VerificarEnlace d){return s.verificarEnlace(p.sesionId(),d);}
}
