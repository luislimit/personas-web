# Siguiente paso: validación E2E de auditoría y fechas

La funcionalidad principal ya pasa todas las baterías E2E. Antes de seguir
añadiendo módulos, esta batería cierra el cambio transversal introducido en v2.4.

## Qué valida

1. Todas las tablas tienen las seis columnas estándar de auditoría.
2. No existe ningún `DATETIME` con precisión fraccionaria.
3. Las fechas funcionales seleccionadas son `DATE`.
4. Los eventos y recurrencias conservan `DATETIME`.
5. Crear una entidad por API registra:
   - cuenta creadora;
   - cuenta modificadora;
   - IP de creación;
   - IP de modificación;
   - timestamps hasta segundos.
6. Modificar una entidad conserva auditoría de creación y actualiza la de modificación.
7. La relación `PERTENENCIA` creada automáticamente también queda auditada.

## Ejecutar solo auditoría

```powershell
.\mvnw.cmd "-Dtest=PruebasAuditoriaE2E" test
```

## Ejecutar todo

```powershell
.\scripts\probar-todo-e2e.cmd
```

No cambia la BD ni el código productivo.
