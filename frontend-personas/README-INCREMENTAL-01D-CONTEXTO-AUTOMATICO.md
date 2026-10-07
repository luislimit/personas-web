# Incremental 01d — contexto automático por equipo/navegador

Se elimina la selección de contexto del flujo normal de login.

## Comportamiento

1. El usuario inicia sesión.
2. Si este navegador/equipo ya recuerda un último contexto válido para esa cuenta,
   se activa automáticamente.
3. Si es la primera vez o ese contexto ya no existe para la cuenta, se activa
   el primer contexto disponible.
4. El cambio manual sigue disponible desde `Conectado a:` en la cabecera.
5. Cada cambio desde la cabecera pasa a ser el último contexto recordado para
   esa cuenta en este navegador.
6. Al cerrar sesión se elimina token/contexto/permisos de sesión, pero se conserva
   la preferencia del último contexto.
7. La pantalla `/seleccionar-contexto` se mantiene únicamente como fallback
   excepcional si una cuenta no puede activar automáticamente un contexto.

La preferencia es local al navegador/equipo. No requiere backend ni cambios de BD.

Una futura opción `contexto por defecto` persistida en backend podría añadirse
si se desea sincronizar la preferencia entre varios equipos/dispositivos.
