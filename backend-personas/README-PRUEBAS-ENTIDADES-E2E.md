# Batería E2E de Entidades

Requiere recrear la BD con:

`gestion_personas_completo_es_v2_2.sql`

La nueva versión añade la cuenta demo:

- usuario: `admin.residencia`
- contraseña: `demo`
- contexto: `Residencia Los Olivos`
- rol: `ADMIN_ORGANIZACION`

## Casos incluidos

1. Diagnóstico de permisos.
2. Crear persona.
3. Modificar persona.
4. Crear contacto.
5. Mantener un solo contacto preferido por tipo.
6. Eliminar contacto = desactivar.
7. Relación directa e inversa.
8. Bloquear autorelación.
9. Finalizar relación sin borrarla.
10. Archivar entidad.
11. Privacidad de datos personales.
12. Aislamiento por contexto.

La prueba 12 está diseñada como prueba de arquitectura:
un administrador de Residencia no debería recuperar entidades del Club.
Si falla, habremos confirmado que la autorización actual comprueba permisos
pero todavía no restringe el conjunto de recursos por el alcance del contexto.

## Ejecutar

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-entidades.cmd
```

o:

```powershell
.\mvnw.cmd "-Dtest=PruebasEntidadesE2E" test
```
