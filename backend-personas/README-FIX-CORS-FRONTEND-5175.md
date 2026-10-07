# Fix CORS frontend 5175

Spring Security tenía `.cors(...)` activado, pero no existía una política CORS
explícita que autorizase el origen del frontend.

Por defecto se permiten:

- http://localhost:5175
- http://127.0.0.1:5175

Cabeceras admitidas:

- Authorization
- Content-Type
- Accept
- X-Contexto-Id

Métodos:

GET, POST, PUT, PATCH, DELETE y OPTIONS.

En producción se puede configurar sin cambiar código:

APP_CORS_ALLOWED_ORIGINS=https://app.ejemplo.es,https://admin.ejemplo.es

No requiere cambios de base de datos.
