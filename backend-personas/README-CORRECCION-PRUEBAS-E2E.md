# Corrección pruebas E2E

Corrige un error de compilación de Java 21 en `PruebasApiE2E` relacionado
con el alcance de una variable de pattern matching después de `fail(...)`.

Además añade:

`./scripts/probar-api.cmd`

para poder ejecutar la batería sin modificar la ExecutionPolicy de PowerShell.

## Uso

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-api.cmd
```

O directamente:

```powershell
.\mvnw.cmd "-Dtest=PruebasApiE2E" test
```

No es necesario cambiar la política global de PowerShell.
