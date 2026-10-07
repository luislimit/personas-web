# Fix compilación PruebasAuditoriaE2E

`java.time.LocalDateTime` expone `getNano()`, no `getNanos()`.

Se corrigen 4 llamadas del test.

No cambia backend productivo ni base de datos.
No es necesario reiniciar el backend por este cambio de test.
