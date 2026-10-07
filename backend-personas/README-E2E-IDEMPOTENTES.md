# Limpieza e idempotencia de las baterías E2E

Objetivo: que cada batería pueda ejecutarse sola, después de otra batería y varias veces seguidas sin recrear la BD.

## Criterios aplicados

- Los datos creados por tests mutantes llevan un identificador de ejecución + sufijo aleatorio.
- Los datos demo se buscan por identidad estable (nombre/asunto/tipo esperado), no por posición ni por «primer resultado».
- `PruebasApiE2E` selecciona explícitamente el calendario demo `Infantil A`.
- La prueba de solicitudes verifica la solicitud demo `Reunión con entrenador`, no simplemente que haya cualquier solicitud residual.
- `PruebasAislamientoE2E` selecciona explícitamente `Licencia federativa 2026/27`.
- Las recurrencias no dependen del orden de la respuesta: se ordenan por fecha antes de validar posiciones.
- La auditoría comprueba que solo existe una relación automática `PERTENENCIA` para la entidad recién creada.
- No se hace cleanup destructivo global: algunos recursos son deliberadamente inmutables o usan borrado lógico. La independencia se consigue por identidad única, no por confiar en que una limpieza previa haya funcionado.

## Prueba definitiva de idempotencia

Con backend arrancado y sin recrear la BD entre medias:

```powershell
.\scripts\probar-idempotencia-e2e.cmd
```

El script ejecuta dos veces consecutivas `probar-todo-e2e.cmd`. La segunda ejecución debe pasar sobre el residuo dejado por la primera.
