# Incremental 01b — separación Login / Contexto

Corrección del primer bloque frontend.

## Problema
Si una cuenta tenía un único contexto, el frontend intentaba activarlo dentro
del propio flujo de login. Si la activación devolvía 403 (por política, alcance
o MFA), la pantalla de login mostraba ese 403 como si usuario/contraseña fueran
incorrectos.

## Cambio
- Login y selección de contexto quedan desacoplados.
- Tras un login correcto siempre se navega a `/seleccionar-contexto`.
- El contexto se activa únicamente al pulsar `Entrar`.
- Se elimina cualquier contexto anterior al iniciar una nueva sesión.
- Los errores HTTP ahora muestran también respuestas de texto no JSON.

No requiere cambios de backend ni de base de datos.
