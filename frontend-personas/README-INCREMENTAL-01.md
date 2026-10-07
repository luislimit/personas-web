# Frontend Gestión de Personas - Incremental 01

Primer bloque del frontend administrativo.

## Incluye

- React 19 + TypeScript + Vite.
- MUI + Emotion.
- TanStack Query preparado para server state.
- Login real contra `POST /api/autenticacion/iniciar-sesion`.
- Recuperación de sesión con `GET /api/autenticacion/yo`.
- Selección/cambio de contexto.
- Cabeceras `Authorization: Bearer` y `X-Contexto-Id` centralizadas.
- Shell administrativo inicial.
- Visualización del rol, alcance y permisos efectivos.
- Gestión centralizada de 401 y 403.
- Step-up MFA automático para `MFA_REQUERIDA`.
- MFA mediante código por email/SMS.
- Ruta `/mfa/verificar` para enlaces MFA de un solo uso.

## Desarrollo

Con backend en `http://localhost:8085`:

```powershell
cd C:\Projects\personas-web\frontend-personas
npm install
npm run dev
```

Abrir:

```text
http://localhost:5173
```

Vite redirige `/api` al backend 8085, por lo que en desarrollo no es necesario configurar CORS.

## Variables

Para despliegue con API en otro origen:

```env
VITE_API_URL=https://api.ejemplo.es
```

Para desarrollo local puede dejarse vacío.

## Nota MFA por enlace

El backend v2.7 usa por defecto:

```text
http://localhost:5173/mfa/verificar
```

como base de los enlaces MFA, por lo que esta ruta ya queda implementada.

## Siguiente incremental previsto

`02-entidades-personas`:

- listado/búsqueda de entidades;
- alta de persona/organización/instalación/grupo;
- detalle de entidad;
- edición;
- archivado;
- contactos;
- relaciones.
