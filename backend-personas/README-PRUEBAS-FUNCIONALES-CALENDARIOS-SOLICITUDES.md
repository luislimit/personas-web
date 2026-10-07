# Pruebas funcionales E2E: Calendarios y Solicitudes

## Calendarios

Incluye 8 casos:

1. CRUD lógico de calendario.
2. Ciclo de evento simple.
3. Expansión de recurrencia semanal.
4. Modificación `SOLO_ESTE`.
5. Eliminación `SOLO_ESTE`.
6. Modificación `ESTE_Y_SIGUIENTES`.
7. Modificación `TODA_LA_SERIE`.
8. Validación de fechas incorrectas.

## Solicitudes

Incluye 10 casos:

1. Creación + participantes + acción inicial.
2. Modificación de asunto/descripción.
3. Mensajes + trazabilidad.
4. Añadir/modificar/eliminar participante.
5. Acción que cambia estados.
6. Flujo `ABIERTA -> RESUELTA -> CERRADA`.
7. Estado global inválido.
8. Estado de participante inválido.
9. Participante duplicado idempotente.
10. Listado.

## Ejecutar

Con backend arrancado en 8085:

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-funcional-core.cmd
```

O por separado:

```powershell
.\mvnw.cmd "-Dtest=PruebasCalendariosE2E" test
.\mvnw.cmd "-Dtest=PruebasSolicitudesE2E" test
```

Estas pruebas crean datos E2E deliberadamente. Como la BD sigue siendo de desarrollo,
la limpieza global continúa siendo recrear la BD con el script completo cuando sea necesario.
