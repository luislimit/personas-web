# PruebasApiE2E - fix consolidado

Este incremental consolida dos correcciones que se habían aplicado por separado y
que podían sobrescribirse mutuamente al copiar ZIPs incrementales:

1. `getLista`: elimina el uso de la variable de pattern matching `map` fuera del
   bloque donde el compilador garantiza su alcance.
2. Búsqueda de Ana López: usa `?nombre=Ana` y filtra después por nombre exacto,
   evitando URI pre-codificada.

IMPORTANTE:
Después de copiar este incremental ejecutar obligatoriamente:

```powershell
.\mvnw.cmd clean test-compile
```

Esto elimina cualquier `.class` viejo de `target/test-classes`.

Después, con el backend arrancado:

```powershell
.\scripts\probar-todo-e2e.cmd
```
