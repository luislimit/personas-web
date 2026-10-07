package es.gestionpersonas.seguridad.dto;
import jakarta.validation.constraints.*; import java.time.LocalDateTime; import java.util.*;
public final class MfaDtos { private MfaDtos(){}
 public record OpcionCanal(String canal,String destinoEnmascarado,List<String> mecanismos){}
 public record EstadoMfa(String nivel,LocalDateTime verificadaEn,LocalDateTime validaHasta,List<OpcionCanal> canales){}
 public record SolicitudDesafio(@NotBlank String canal,@NotBlank String mecanismo){}
 public record RespuestaDesafio(Long desafioId,String canal,String mecanismo,String destinoEnmascarado,LocalDateTime fechaExpiracion){}
 public record VerificarCodigo(@NotNull Long desafioId,@NotBlank String codigo){}
 public record VerificarEnlace(@NotNull Long desafioId,@NotBlank String token){}
 public record RespuestaVerificacion(boolean verificada,String nivel,LocalDateTime verificadaEn,LocalDateTime validaHasta){}
 public record SolicitudPolitica(Long entidadContextoId,Long rolSeguridadId,String codigoPermiso,@Min(1) @Max(10080) Integer minutosValidez,Boolean activo){}
 public record VistaPolitica(Long id,Long entidadContextoId,Long rolSeguridadId,String codigoRol,String codigoPermiso,Integer minutosValidez,boolean activo){}
}
