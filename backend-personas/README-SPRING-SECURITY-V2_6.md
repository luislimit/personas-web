# Gestión de Personas - Integración Spring Security v2.6

## Objetivo

Dejar de mantener un sistema HTTP de autorización paralelo al framework y reutilizar Spring Security para las responsabilidades estándar:

- autenticación de cada petición;
- `SecurityContext` y principal actual;
- `GrantedAuthority` con los permisos efectivos del contexto;
- `ROLE_<codigoRol>` como autoridad estándar de Spring;
- `@PreAuthorize` para proteger métodos REST;
- respuestas 401/403 en la capa de seguridad;
- `PasswordEncoder` como bean de Spring.

El CORE propio sigue siendo responsable de lo que Spring Security no puede inferir de forma genérica:

- `contexto_cuenta`;
- resolución del contexto activo mediante `X-Contexto-Id`;
- alcance `PROPIO`, `RELACIONADOS`, `CONTEXTO`, `ARBOL_CONTEXTO`, `PERSONALIZADO`;
- reglas `PERMITIR` / `DENEGAR`;
- relaciones entre entidades;
- datos personales;
- información sensible.

## Flujo nuevo

```text
HTTP
  -> FiltroAutenticacionBearer
      -> valida token opaco existente
      -> resuelve X-Contexto-Id
      -> ServicioControlAcceso.resolve(...)
      -> permisos efectivos de BD
      -> Spring SecurityContext
           principal = PrincipalSeguridad
           authorities = ENTIDAD.LEER, DOCUMENTO.LEER, ...
           role = ROLE_ADMIN_ORGANIZACION, ...
  -> @PreAuthorize
  -> servicios del CORE
      -> alcance / privacidad / sensible / PERSONALIZADO
```

## Compatibilidad

No cambia:

- formato del token;
- tablas de seguridad;
- modelo de roles/permisos;
- cabecera `Authorization: Bearer`;
- cabecera `X-Contexto-Id`;
- contratos REST;
- motor de alcance.

Por ello **no hay cambio de base de datos en v2.6**. Se mantiene la BD completa v2.5.

## Interceptor anterior

`InterceptorSeguridadApi` deja de ser un `HandlerInterceptor` y queda como clase de compatibilidad deprecada. `ConfiguracionWeb` ya no registra interceptores de seguridad.

## Pruebas

Primero:

```powershell
.\mvnw.cmd clean test-compile
```

Reiniciar backend, porque cambia la cadena de filtros de Spring:

```powershell
.\mvnw.cmd spring-boot:run
```

Prueba específica:

```powershell
.\scripts\probar-spring-security-e2e.cmd
```

Regresión completa:

```powershell
.\scripts\probar-todo-e2e.cmd
```

Idempotencia:

```powershell
.\scripts\probar-idempotencia-e2e.cmd
```
