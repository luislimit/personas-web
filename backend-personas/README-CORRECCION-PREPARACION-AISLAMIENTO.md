# Corrección preparación PruebasAislamientoE2E

Tras ejecutar la batería cuando el aislamiento todavía fallaba, la prueba:

`residenciaNoCreaCalendarioEnClub`

llegó a crear realmente el calendario:

`E2E calendario indebido`

porque el backend aún permitía la operación.

La preparación posterior seleccionaba el primer calendario de Infantil A con:

`calendarios.get(0)`

y, al ordenarse por nombre, podía seleccionar `E2E calendario indebido`,
que no tiene eventos. Por eso `eventos.get(0)` terminaba con
`IndexOutOfBoundsException`.

La prueba se hace ahora idempotente y robusta:

- selecciona exactamente el calendario `Infantil A`;
- selecciona exactamente `Entrenamiento Infantil A` o, como alternativa,
  `Partido Infantil A`;
- no depende del orden ni de residuos de ejecuciones anteriores.

No es necesario cambiar el backend por este error.
