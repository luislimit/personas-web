package es.gestionpersonas.comun;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class ManejadorExcepcionesApi {
    public record ErrorApi(LocalDateTime fechaHora, int estadoHttp, String error, String mensaje,
                           Map<String,String> validacion) {}

    @ExceptionHandler(ExcepcionNoAutorizado.class)
    public ResponseEntity<ErrorApi> noAutorizado(ExcepcionNoAutorizado ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorApi(LocalDateTime.now(),401,"No autorizado",ex.getMessage(),Map.of()));
    }

    @ExceptionHandler(ExcepcionMfaRequerida.class)
    public ResponseEntity<ErrorApi> mfaRequerida(ExcepcionMfaRequerida ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorApi(
            LocalDateTime.now().withNano(0), 403, "MFA requerida", ex.getMessage(),
            Map.of("codigo", "MFA_REQUERIDA")
        ));
    }

    @ExceptionHandler(ExcepcionProhibido.class)
    public ResponseEntity<ErrorApi> prohibido(ExcepcionProhibido ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorApi(LocalDateTime.now(),403,"Prohibido",ex.getMessage(),Map.of()));
    }

    /**
     * Las denegaciones producidas por @PreAuthorize / Method Security ocurren
     * dentro de la invocación MVC y, por tanto, no siempre llegan al
     * AccessDeniedHandler de la SecurityFilterChain. Se normalizan aquí al
     * mismo contrato 403 que utiliza el resto de la API.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> prohibidoSpringSecurity(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ErrorApi(
                LocalDateTime.now().withNano(0),
                403,
                "Prohibido",
                "Permiso insuficiente",
                Map.of()
            )
        );
    }

    /**
     * Equivalente para errores de autenticación que alcancen la capa MVC.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorApi> noAutorizadoSpringSecurity(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            new ErrorApi(
                LocalDateTime.now().withNano(0),
                401,
                "No autorizado",
                "Autenticación requerida",
                Map.of()
            )
        );
    }

    @ExceptionHandler(ExcepcionNoEncontrado.class)
    public ResponseEntity<ErrorApi> noEncontrado(ExcepcionNoEncontrado ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorApi(LocalDateTime.now(),404,"No encontrado",ex.getMessage(),Map.of()));
    }

    @ExceptionHandler(ExcepcionConflicto.class)
    public ResponseEntity<ErrorApi> conflicto(ExcepcionConflicto ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorApi(LocalDateTime.now(),409,"Conflicto",ex.getMessage(),Map.of()));
    }

    @ExceptionHandler({IllegalArgumentException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorApi> peticionIncorrecta(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorApi(LocalDateTime.now(),400,"Petición incorrecta",ex.getMessage(),Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException ex) {
        Map<String,String> campos=new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> campos.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorApi(LocalDateTime.now(),400,"Petición incorrecta","Datos no válidos",campos));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> generico(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorApi(LocalDateTime.now(),500,"Error interno del servidor",ex.getMessage(),Map.of()));
    }
}
