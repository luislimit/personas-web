# Gestión de Personas - V2.4

## Normalización temporal

Regla aplicada:

- `DATE`: datos de negocio cuando sólo importa el día.
- `DATETIME`: eventos donde la hora forma parte del negocio y datos técnicos/auditoría.
- Sin microsegundos: se elimina `DATETIME(6)` y se trabaja como máximo hasta segundos.

Cambios principales a `DATE`:

- contacto: verificación y validez;
- contexto de cuenta: validez;
- documento: disponibilidad;
- solicitud: fecha de solicitud, resolución y cierre;
- participante de solicitud: lectura y respuesta;
- mensaje de solicitud: fecha funcional del mensaje.

Calendarios/eventos y recurrencias conservan `DATETIME` porque la hora sí es funcional.
La división de recurrencias usa ahora `minusSeconds(1)`.

## Auditoría transversal

Todas las tablas incorporan:

- `fecha_creacion DATETIME`
- `fecha_modificacion DATETIME`
- `auditoria_creado_por_cuenta_id`
- `auditoria_ip_creacion`
- `auditoria_modificado_por_cuenta_id`
- `auditoria_ip_modificacion`

Las fechas técnicas se conservan hasta segundos.
Las tablas históricas `auditoria_acceso` y `auditoria_cambio` incorporan además `ip_origen`.

En backend, las entidades JPA heredan de `EntidadAuditable`. El usuario autenticado y la IP
remota se toman del contexto de la petición. Por seguridad se usa `request.getRemoteAddr()`;
no se confía automáticamente en `X-Forwarded-For` hasta configurar proxies de confianza.

## Solicitudes y mensajes

Se separa la fecha funcional de la auditoría:

- `solicitud.fecha_solicitud DATE`
- `mensaje_solicitud.fecha_mensaje DATE`
- `fecha_creacion` queda reservada para la trazabilidad técnica con hora/segundo.

## Pruebas

1. Recrear la BD con `database/gestion_personas_completo_es_v2_4.sql`.
2. Reiniciar el backend.
3. Ejecutar:

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-todo-e2e.cmd
```

El script ejecuta secuencialmente las seis baterías E2E y se detiene en el primer fallo.
