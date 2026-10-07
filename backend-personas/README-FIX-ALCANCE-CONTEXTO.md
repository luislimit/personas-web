# Fix alcance de contexto

Este incremental implementa la primera versión real del alcance de contexto sobre ENTIDAD.

## Comportamiento

- `ADMIN_PLATAFORMA`: puede acceder a cualquier entidad.
- `CONTEXTO` / `ARBOL_CONTEXTO`: contexto activo + descendientes estructurales recursivos.
- Relaciones estructurales consideradas:
  - `PERTENENCIA`
  - `PROPIEDAD`
  - `GESTION`
- `RELACIONADOS`: persona asociada a la cuenta + entidades directamente relacionadas.
- `PROPIO`: persona asociada a la cuenta.
- La entidad de contexto siempre es visible.

## Protección aplicada

No se filtra solo el buscador. También se valida el alcance en:

- detalle de entidad;
- modificación y archivado;
- contactos;
- cuentas;
- relaciones.

Así no se puede evitar el filtro llamando directamente a `/api/entidades/{id}`.

## Creación

Una entidad creada desde un contexto no global se vincula automáticamente al contexto
mediante una relación `PERTENENCIA`.

Esto evita crear una entidad y que, inmediatamente después, quede fuera del propio alcance
del usuario que la creó.

## Test de privacidad

La prueba de privacidad usa ahora `Diego Martín`, que pertenece al contexto `Infantil A`.
Con aislamiento real, `Ana López` ni siquiera debe aparecer para Carlos cuando actúa desde
ese contexto.

## Limitación consciente

`PERSONALIZADO` todavía no tiene motor de reglas. Por ahora solo permite persona propia +
entidad de contexto. La futura tabla `regla_alcance_acceso` podrá ampliar este comportamiento.
