# Fix aislamiento transversal: Calendarios, Eventos y Solicitudes

La batería `PruebasAislamientoE2E` detectó 8 fallos de 10.

Los dos casos documentales ya estaban correctamente aislados. Los fallos reales estaban en:

- Calendarios: listado, acceso directo y creación.
- Eventos: acceso directo.
- Solicitudes: listado, acceso directo, mensajes y creación.

## Calendarios/Eventos

Se valida ahora que `calendario.entidad_propietaria_id` esté dentro del alcance
del contexto activo.

Esto se aplica en:

- listar calendarios;
- obtener calendario;
- crear/modificar/desactivar calendario;
- listar eventos de calendario;
- obtener evento;
- crear/modificar/eliminar evento.

Por tanto, conocer un ID ya no permite saltarse el aislamiento.

## Solicitudes

Una solicitud pertenece al alcance si:

- su entidad creadora está dentro del alcance; o
- al menos uno de sus participantes está dentro del alcance.

Además:

- el creador de una nueva solicitud debe estar dentro del contexto;
- todos los participantes iniciales deben estar dentro del contexto;
- remitentes de mensajes, actores y participantes modificados deben estar dentro del contexto;
- los listados se filtran;
- el acceso directo por ID devuelve 403 fuera del alcance.

## Resultado esperado

Tras aplicar este incremental:

```powershell
.\mvnw.cmd clean test
$env:API_PRUEBAS_URL="http://localhost:8085"
.\mvnw.cmd "-Dtest=PruebasAislamientoE2E" test
```

La batería debería quedar 10/10.
