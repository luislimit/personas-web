# Pruebas E2E de aislamiento transversal

Esta batería no modifica el backend. Su objetivo es localizar qué módulos
siguen validando únicamente el permiso funcional y no el alcance del contexto.

## Requisitos

- `gestion_personas_completo_es_v2_2.sql`
- fix de alcance de entidades ya aplicado
- backend arrancado

## Casos

1. Residencia no lista calendarios del Club.
2. Residencia no abre directamente un calendario del Club.
3. Residencia no abre directamente un evento del Club.
4. Residencia no crea calendario cuyo propietario es Infantil A.
5. Residencia no abre documento personal de Diego.
6. Residencia no crea documento destinado a Diego.
7. Residencia no lista solicitudes del Club.
8. Residencia no abre directamente una solicitud del Club.
9. Residencia no escribe mensajes en solicitudes del Club.
10. Residencia no crea una solicitud suplantando una entidad del Club.

## Ejecutar

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-aislamiento.cmd
```

o:

```powershell
.\mvnw.cmd "-Dtest=PruebasAislamientoE2E" test
```

Es esperable que varias pruebas fallen en la primera ejecución.
Cada fallo identifica un punto concreto donde debemos aplicar el alcance
de contexto al recurso.
