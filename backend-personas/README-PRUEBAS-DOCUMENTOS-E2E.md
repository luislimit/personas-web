# Batería funcional E2E de documentos

Añade `PruebasDocumentosE2E`.

## Qué valida

1. Diagnóstico de permisos del gestor documental.
2. Creación en BORRADOR.
3. Un BORRADOR no se puede acceder.
4. Publicación BORRADOR -> DISPONIBLE.
5. Registro de acceso con cuenta, persona y fecha/hora.
6. Inmutabilidad después del primer acceso.
7. Prohibición de eliminar después del primer acceso.
8. Eliminación permitida antes del primer acceso.
9. Solo se sustituye un documento ya accedido.
10. El sustituto debe ser del mismo tipo.
11. Publicar el sustituto marca el anterior como SUSTITUIDO.
12. Un documento solo puede tener un sustituto directo.
13. Documento con maximo_accesos=1.
14. Carrera concurrente ACCEDER vs ELIMINAR.

## Ejecución

```powershell
$env:API_PRUEBAS_URL="http://localhost:8085"
.\scripts\probar-documentos.cmd
```

o:

```powershell
.\mvnw.cmd "-Dtest=PruebasDocumentosE2E" test
```

## Nota importante sobre permisos

La primera prueba comprueba que `pedro` tenga en su contexto:

- DOCUMENTO.LEER
- DOCUMENTO.CREAR
- DOCUMENTO.MODIFICAR
- DOCUMENTO.ELIMINAR
- INFORMACION_SENSIBLE.LEER
- INFORMACION_SENSIBLE.MODIFICAR

Si esa prueba falla, las siguientes se omiten deliberadamente.
Eso significa que hemos detectado primero una carencia de configuración/autorización
que impide probar correctamente las reglas funcionales.

## Datos

La batería crea documentos de prueba con nombres `E2E - ...`.

Los documentos accedidos no se eliminan al finalizar porque precisamente estamos
probando que sean inmutables y no eliminables. Como la BD todavía es de desarrollo,
puede recrearse con el script completo cuando se quiera limpiar todos los datos E2E.
