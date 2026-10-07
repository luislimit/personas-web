# Fix PruebasApiE2E - selección idempotente del documento demo

La batería `PruebasDocumentosE2E` crea documentos `INFORME_MEDICO` en distintos
estados, incluido `BORRADOR`, y los deja en la BD de desarrollo.

Por tanto, una ejecución posterior de `PruebasApiE2E` no puede seleccionar el
primer documento cuyo tipo sea `INFORME_MEDICO`, porque puede recuperar uno de
los documentos E2E creados en una ejecución anterior.

Se corrige la prueba para identificar explícitamente el dato demo estable:

`Informe médico septiembre 2026`

Así la prueba deja de depender del orden de los documentos y del residuo dejado
por baterías anteriores.

Este incremental conserva también las correcciones previas de:

- búsqueda de Ana mediante `?nombre=Ana`;
- `getLista` sin el problema de alcance de la variable `map`.

No requiere recrear la BD ni reiniciar el backend.
