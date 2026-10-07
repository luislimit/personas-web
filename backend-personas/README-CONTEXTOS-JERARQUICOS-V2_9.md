# Gestión de Personas — Contextos jerárquicos y permisos acumulativos v2.9

## Objetivo

Separar el contexto de conexión de las asignaciones internas de seguridad.

Ejemplo:

- CB Tres Cantos (raíz de conexión)
  - Infantil A (asignación interna)

Si una cuenta tiene ambas asignaciones, `/api/autenticacion/contextos` devuelve
solo `CB Tres Cantos`. `Infantil A` sigue existiendo en `contexto_cuenta` y
contribuye a la autorización dentro de la raíz.

## Reglas

- Dos asignaciones relacionadas estructuralmente no generan dos opciones en el selector.
- Dos raíces independientes sí aparecen por separado (por ejemplo club + residencia).
- Los permisos se acumulan por entidad objetivo.
- Una asignación específica puede conceder capacidades adicionales únicamente en su ámbito.
- Una excepción explícita `DENEGAR` aplicable a la entidad prevalece sobre concesiones del mismo permiso.
- Spring Security recibe la unión de permisos del contexto raíz como filtro técnico inicial.
- El CORE realiza después la comprobación fina de permiso + entidad.

## Ejemplo

CB Tres Cantos:
- ENTIDAD.LEER

Infantil A:
- ENTIDAD.MODIFICAR

Resultado:
- cualquier entidad alcanzada por la asignación del club: puede leerse;
- entidades de Infantil A: además pueden modificarse;
- otras ramas del club no reciben ENTIDAD.MODIFICAR por la asignación de Infantil A.

## Base de datos

No hay cambios de esquema ni es necesario recrear la base de datos.
`contexto_cuenta` pasa a interpretarse como asignación de acceso, mientras que
los contextos de conexión se derivan automáticamente de las raíces de esas asignaciones.

## Prueba

Ejecutar:

    .\scripts\probar-contextos-jerarquicos-e2e.cmd

Después ejecutar las baterías completas e idempotencia.
