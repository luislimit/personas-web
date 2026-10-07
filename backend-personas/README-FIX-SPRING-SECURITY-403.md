# Fix Spring Security v2.6 - 403 de Method Security

## Problema

`@PreAuthorize` lanza `org.springframework.security.access.AccessDeniedException`
en la capa de Method Security.

Ese error ocurre después de la `SecurityFilterChain`, por lo que el
`accessDeniedHandler` configurado en `ConfiguracionSeguridad` no es quien genera
la respuesta.

El `@RestControllerAdvice` existente tenía un handler genérico de `Exception`,
por lo que la denegación terminaba convertida en HTTP 500.

## Solución

`ManejadorExcepcionesApi` trata explícitamente:

- `AccessDeniedException` -> HTTP 403
- `AuthenticationException` -> HTTP 401

Se conserva el mismo formato `ErrorApi` usado por el resto de la aplicación.

No hay cambios de base de datos.
