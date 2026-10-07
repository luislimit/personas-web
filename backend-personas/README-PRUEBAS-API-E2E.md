# Pruebas automáticas E2E de la API

Este incremental añade una batería inicial que llama a la API REST real.

No sustituye a los tests unitarios. Su objetivo es comprobar de extremo a extremo:

- backend arrancado;
- conexión MySQL;
- autenticación;
- contextos;
- permisos;
- entidades;
- calendarios;
- eventos;
- solicitudes;
- documentos sensibles;
- contrato OpenAPI.

## Precondición

La BD debe haberse recreado con los datos demo de:

`gestion_personas_completo_es_v2_corregido.sql`

y el backend debe estar arrancado.

## Ejecución

Desde `backend-personas`:

```powershell
.\scripts\probar-api.ps1
```

Por defecto prueba:

`http://localhost:8080`

Si el backend usa otro puerto:

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-api.ps1
```

También puede ejecutarse JUnit directamente:

```powershell
.\mvnw.cmd "-Dtest=PruebasApiE2E" test
```

## Por qué no se ejecuta con cada `mvn test`

La clase se llama `PruebasApiE2E`, por lo que no entra en el patrón normal de Surefire.
Esto evita que `clean test` dependa de que haya otro backend arrancado.

## Alcance inicial

Estas pruebas son deliberadamente no destructivas: no crean ni eliminan datos.
Cuando esta batería esté estable, el siguiente nivel será una batería funcional CRUD
que cree sus propios datos de prueba, compruebe reglas y los limpie al terminar.
