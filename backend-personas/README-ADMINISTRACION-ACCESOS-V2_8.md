# Gestión de Personas - v2.8
## Administración integral de cuentas y accesos

Este bloque cierra el ciclo administrativo del CORE:

`PERSONA -> CUENTA -> CONTEXTO -> ROL -> ALCANCE -> EXCEPCIONES -> REGLAS PERSONALIZADAS -> MFA`

## Funcionalidad incluida

- Búsqueda de cuentas administrables desde el contexto activo.
- Alta de cuentas con contraseña en claro por API; el backend la codifica con `PasswordEncoder`.
- Activar, bloquear y deshabilitar cuentas.
- Vincular y desvincular una cuenta de una PERSONA.
- Asignar un único acceso de cuenta por contexto.
- Cambiar rol de seguridad y alcance del contexto.
- Gestión de excepciones de permiso `PERMITIR` / `DENEGAR`.
- Diagnóstico de permisos efectivos y explicación de la procedencia de cada permiso.
- Información de MFA aplicable por contexto y por permiso.
- Consulta y revocación de sesiones de una cuenta.
- Bloqueo/deshabilitación/cambio de contraseña/vinculación de persona revocan sesiones activas cuando corresponde.
- Catálogos REST de roles de seguridad y permisos.

## Controles contra escalada de privilegios

- Un administrador de organización solo administra cuentas/personas dentro de su contexto activo.
- Solo un `ADMIN_PLATAFORMA` puede asignar `ADMIN_PLATAFORMA`.
- Un administrador no plataforma no puede crear una excepción `PERMITIR` para un permiso que él mismo no posee.
- Los cambios de rol, alcance y overrides se evalúan en cada petición, por lo que afectan inmediatamente a sesiones ya abiertas.

## Cambio de modelo

`contexto_cuenta` pasa a permitir una única fila por `(cuenta_id, entidad_contexto_id)`.
El rol se modifica sobre esa asignación en lugar de crear varias filas del mismo contexto con roles distintos.

## Endpoints principales

- `GET /api/cuentas`
- `POST /api/cuentas`
- `POST /api/cuentas/{id}/activar`
- `POST /api/cuentas/{id}/bloquear`
- `DELETE /api/cuentas/{id}`
- `PUT /api/cuentas/{id}/persona/{personaId}`
- `DELETE /api/cuentas/{id}/persona`
- `GET|POST /api/cuentas/{id}/contextos`
- `PATCH|DELETE /api/cuentas/{id}/contextos/{contextoCuentaId}`
- `GET|PUT|DELETE /api/cuentas/{id}/contextos/{contextoCuentaId}/excepciones-permiso/...`
- `GET /api/cuentas/{id}/contextos/{contextoCuentaId}/acceso-efectivo`
- `GET /api/cuentas/{id}/contextos/{contextoCuentaId}/permisos/{codigo}/explicacion`
- `GET /api/cuentas/{id}/sesiones`
- `DELETE /api/cuentas/{id}/sesiones/{sesionId}`
- `POST /api/cuentas/{id}/sesiones/revocar-todas`
- `GET /api/seguridad/catalogos/roles`
- `GET /api/seguridad/catalogos/permisos`

## Pruebas

Ejecutar primero:

`./scripts/probar-administracion-accesos-e2e.cmd`

Después ejecutar la batería completa y la doble pasada idempotente.
