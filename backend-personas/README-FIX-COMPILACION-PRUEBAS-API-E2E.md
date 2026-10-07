# Corrección compilación PruebasApiE2E

El compilador no mantiene visible la variable de pattern matching `map`
fuera del `if (!(item instanceof Map<?, ?> map))`, porque la llamada a
`fail(...)` no se considera terminación de flujo a efectos del análisis
de alcance.

Se sustituye por una comprobación explícita y un cast posterior:

```java
if (!(item instanceof Map<?, ?>)) {
    fail(...);
}

Map<?, ?> map = (Map<?, ?>) item;
```

No cambia ninguna lógica funcional del test ni del backend.
