# Corrección regresión PruebasApiE2E - búsqueda de Ana López

Este problema ya se había corregido en una versión anterior del E2E.

La versión v2.4 volvió a introducir:

```java
"/api/entidades?nombre=Ana%20L%C3%B3pez"
```

`RestClient` vuelve a tratar/codificar el URI, por lo que esa búsqueda no es fiable.

Se restaura la estrategia que ya funcionaba:

```java
"/api/entidades?nombre=Ana"
```

y después el test selecciona exactamente:

```java
"Ana López".equals(e.get("nombre"))
```

Esto corrige tanto:

- `documentosAna`
- `metadatosNoConsumenAcceso`

No requiere cambios en BD ni en backend de producción.
