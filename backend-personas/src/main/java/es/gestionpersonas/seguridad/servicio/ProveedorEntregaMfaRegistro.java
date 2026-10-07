package es.gestionpersonas.seguridad.servicio;
import org.slf4j.*; import org.springframework.stereotype.Component;
@Component
public class ProveedorEntregaMfaRegistro implements ProveedorEntregaMfa {
 private static final Logger log=LoggerFactory.getLogger(ProveedorEntregaMfaRegistro.class);
 public void enviarCodigo(String canal,String destino,String codigo,int minutos){log.warn("MFA proveedor REGISTRO (solo desarrollo): canal={} destino={} codigo={} caduca={}min",canal,destino,codigo,minutos);}
 public void enviarEnlace(String canal,String destino,String enlace,int minutos){log.warn("MFA proveedor REGISTRO (solo desarrollo): canal={} destino={} enlace={} caduca={}min",canal,destino,enlace,minutos);}
}
