# Corrección PruebasAislamientoE2E

El `@BeforeAll` intentaba obtener la licencia de Diego usando a `carlos`
en el contexto `Infantil A`.

Carlos tiene `GESTOR_CONTEXTO`, que no incluye `DOCUMENTO.LEER`, por lo
que la preparación fallaba antes de ejecutar las 10 pruebas.

La corrección obtiene el id del documento usando a `maria` en
`CB Tres Cantos`:

- `maria` tiene rol `AUTOSERVICIO`
- dispone de `DOCUMENTO.LEER`
- su alcance es `RELACIONADOS`
- está relacionada con Diego mediante `PADRE_HIJO`

Así la preparación usa un acceso legítimo y las pruebas de aislamiento
pueden ejecutarse realmente.
