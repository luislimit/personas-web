# Incremental 02 — Personas y entidades

Primer módulo funcional del frontend.

## Incluye

- Activación del módulo `Personas y entidades` en navegación.
- Listado real contra `GET /api/entidades`.
- Búsqueda por nombre.
- Filtros por tipo y estado.
- Detalle real contra `GET /api/entidades/{id}`.
- Alta con `POST /api/entidades`.
- Edición con `PATCH /api/entidades/{id}`.
- Archivado con `DELETE /api/entidades/{id}`.
- Formulario específico para PERSONA con:
  - nombre;
  - primer apellido;
  - segundo apellido;
  - fecha de nacimiento.
- Creación básica de ORGANIZACION, GRUPO e INSTALACION.
- Respeto de privacidad: si el backend oculta `persona`, la ficha no intenta
  reconstruir datos personales.
- Caché separada por contexto activo mediante TanStack Query.
- Manejo de 403/401/MFA reutilizando el cliente API existente.

## Importante sobre permisos

El frontend usa los permisos efectivos de sesión para decidir si muestra
acciones generales como Crear/Editar/Archivar.

Con el modelo jerárquico v2.9 esos permisos son una unión de asignaciones.
Por ello una acción puede mostrarse y posteriormente ser rechazada por backend
si el permiso no alcanza la entidad concreta. El backend continúa siendo la
fuente de verdad de autorización.

En una futura iteración se puede añadir una capacidad por entidad para que la
UI oculte también esas acciones con precisión por fila.

## No incluido todavía

- Identificadores/documentos de identidad.
- Contactos.
- Direcciones.
- Relaciones.
- Roles funcionales.
- Paginación servidor.

Estos bloques se incorporarán en incrementales posteriores.

No requiere cambios de backend ni de base de datos.
