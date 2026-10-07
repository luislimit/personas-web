# Incremental 01f — contextos raíz

El selector superior representa únicamente contextos de conexión independientes.

El backend agrupa las asignaciones internas bajo su raíz, por ejemplo:

CB Tres Cantos
└── Infantil A

En ese caso el frontend recibe solo `CB Tres Cantos`, por lo que no aparece
desplegable si no existe otra raíz independiente.

También se elimina de la cabecera el texto de rol/alcance, porque con permisos
acumulativos una sesión puede estar compuesta por varias asignaciones y un solo
rol mostrado sería engañoso.

Si existen dos raíces independientes, por ejemplo `CB Tres Cantos` y
`Residencia Los Olivos`, el desplegable sigue apareciendo normalmente.
