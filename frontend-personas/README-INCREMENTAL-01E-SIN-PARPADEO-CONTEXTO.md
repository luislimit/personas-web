# Incremental 01e — eliminar parpadeo al entrar

El proveedor de autenticación ya no publica `usuario` como autenticado antes de
haber resuelto el contexto inicial.

Antes:
login correcto -> usuario autenticado -> router veía contexto nulo ->
/seleccionar-contexto -> activación automática -> inicio.

Ahora:
login correcto -> resolver/activar contexto -> publicar sesión autenticada -> inicio.

La pantalla de selección se conserva únicamente como fallback real si no puede
activarse ningún contexto.

No requiere cambios de backend ni de base de datos.
