package es.gestionpersonas.seguridad.servicio;
import es.gestionpersonas.seguridad.web.PrincipalSeguridad; import org.springframework.security.core.Authentication; import org.springframework.stereotype.Component;
@Component("autorizacionSegura")
public class AutorizacionSegura { private final ServicioMfa mfa; public AutorizacionSegura(ServicioMfa mfa){this.mfa=mfa;} public boolean permitido(Authentication a,String permiso){if(a==null||!a.isAuthenticated())return false;boolean ok=a.getAuthorities().stream().anyMatch(x->permiso.equals(x.getAuthority()));if(!ok)return false;if(a.getPrincipal() instanceof PrincipalSeguridad p)mfa.exigirPermiso(p.acceso(),permiso);return true;} }
