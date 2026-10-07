package es.gestionpersonas.seguridad.servicio;
public interface ProveedorEntregaMfa { void enviarCodigo(String canal,String destino,String codigo,int minutos); void enviarEnlace(String canal,String destino,String enlace,int minutos); }
