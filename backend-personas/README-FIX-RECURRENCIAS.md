# Corrección recurrencias: cancelación y división de serie

La batería `PruebasCalendariosE2E` detectó dos defectos reales.

## 1. SOLO_ESTE cancelado seguía apareciendo

La excepción se guarda como:

`CANCELADA`

pero al expandir/consultar la serie se comprobaba:

`CANCELADO`

Por eso la excepción existía en BD, pero no se omitía de la agenda.

Se corrigen las dos comprobaciones para usar `CANCELADA`.

## 2. ESTE_Y_SIGUIENTES conservaba una ocurrencia de más en la serie original

Para cortar la serie se hacía:

`occurrence.minusNanos(1)`

La columna MySQL `DATETIME(6)` solo conserva microsegundos. Ese nanosegundo puede
redondearse/truncarse de forma que el límite termine persistiendo prácticamente
en la misma fecha de la ocurrencia que queremos mover a la nueva serie.

Se cambia a:

`occurrence.minusNanos(1_000)`

es decir, un microsegundo, que coincide con la precisión real de `DATETIME(6)`.

La misma corrección se aplica al borrado `ESTE_Y_SIGUIENTES`.

## Verificación

Después de aplicar el incremental, reinicia el backend y ejecuta:

```powershell
.\mvnw.cmd "-Dtest=PruebasCalendariosE2E" test
```

Resultado esperado: `Tests run: 8, Failures: 0, Errors: 0`.
