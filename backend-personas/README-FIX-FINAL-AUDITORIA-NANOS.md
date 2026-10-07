# Fix final compilación PruebasAuditoriaE2E

Se corrige la diferencia entre:

- `LocalDateTime#getNano()`
- `Timestamp#getNanos()`

El error anterior sustituyó también la llamada correspondiente a `java.sql.Timestamp`.

No cambia backend productivo ni base de datos.
No es necesario reiniciar el backend por este ajuste de test.
