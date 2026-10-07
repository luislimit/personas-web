# Corrección PruebasEntidadesE2E - privacidad de datos personales

La prueba de privacidad usaba `Ana López` mientras la sesión activa era:

- usuario: `carlos`
- contexto: `Infantil A`

Tras implantar el aislamiento por contexto, Ana pertenece a `Residencia Los Olivos`
y no debe ser visible desde `Infantil A`. Por tanto la prueba fallaba antes de poder
comprobar la privacidad de datos personales.

Se sustituye Ana por `Laura Sánchez`, que sí pertenece a Infantil A y es un tercero
respecto de Carlos.

La prueba sigue validando exactamente lo que pretende:

- Carlos puede ver la identidad básica de Laura porque está dentro de su contexto.
- Carlos no tiene `DATOS_PERSONALES.LEER`.
- La respuesta no debe contener el bloque `persona`.

No requiere ningún cambio de BD ni de backend productivo.
