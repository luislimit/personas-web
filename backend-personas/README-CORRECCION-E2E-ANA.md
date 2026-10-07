# Corrección E2E - búsqueda de Ana López

Las dos pruebas documentales fallaban porque la URL ya venía pre-codificada:

`?nombre=Ana%20L%C3%B3pez`

y `RestClient` volvía a codificar el porcentaje al construir la URI.

El backend sí contiene a Ana López; el problema estaba en el test.

La búsqueda se cambia a:

`?nombre=Ana`

y después el test sigue filtrando exactamente por `Ana López` en la respuesta.

Ejecutar:

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-api.cmd
```

o:

```powershell
.\mvnw.cmd "-Dtest=PruebasApiE2E" test
```
