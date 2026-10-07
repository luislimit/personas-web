# Gestión de Personas — v2.5 Motor de reglas de alcance PERSONALIZADO

## Objetivo

Añadir un motor genérico de autorización para `contexto_cuenta.alcance = 'PERSONALIZADO'`
sin introducir reglas específicas de Club, Residencia, Escuela, Empresa u otros dominios.

## Regla base

En un contexto `PERSONALIZADO` siempre son accesibles:

- la entidad de contexto activa;
- la persona asociada a la cuenta autenticada.

El resto de entidades requieren al menos una regla `PERMITIR` coincidente.
Si cualquier regla `DENEGAR` activa coincide con la misma entidad, la denegación prevalece.

## Tipos de regla

### RELACION

Permite o deniega entidades alcanzables desde la entidad de contexto usando un único tipo de relación.

- `incluirDescendientes = false`: solo relación directa contexto -> entidad objetivo.
- `incluirDescendientes = true`: recorrido recursivo usando ese mismo tipo de relación.

### ROL_FUNCIONAL

Permite o deniega entidades que tengan un rol funcional concreto dentro de la entidad de contexto activa.

Ejemplo: permitir únicamente personas con rol `RESIDENTE` en una residencia.

### TIPO_ENTIDAD

Filtra por tipo de entidad (`PERSONA`, `INSTALACION`, `GRUPO`, etc.).
Por seguridad, una regla basada únicamente en tipo no abre acceso global: la entidad también debe pertenecer
al árbol estructural del contexto.

### COMPUESTA

Combina dos o más criterios. Todos los criterios informados deben cumplirse (AND).

Ejemplo:

- rol funcional `RESIDENTE`;
- tipo objetivo `PERSONA`;
- efecto `DENEGAR`.

## Efectos

- `PERMITIR`
- `DENEGAR`

`DENEGAR` prevalece sobre cualquier `PERMITIR` coincidente, independientemente de la prioridad.
La prioridad se conserva para ordenar reglas y para futuras ampliaciones del motor.

## API

Base:

`/api/cuentas/{cuentaId}/contextos/{contextoCuentaId}/reglas-alcance`

Operaciones:

- `GET` listar reglas;
- `POST` crear;
- `PUT /{reglaId}` reemplazar;
- `DELETE /{reglaId}` eliminar.

Las reglas solo pueden gestionarse en contextos cuyo alcance sea `PERSONALIZADO`.
La gestión reutiliza los permisos `CONTEXTO.*` existentes.

## Datos demo

Se añade la cuenta dedicada:

- usuario: `reglas.demo`
- contraseña: `demo`
- contexto: `Residencia Los Olivos`
- alcance: `PERSONALIZADO`

Se crea sin reglas iniciales para comprobar que el comportamiento por defecto es restrictivo.

## E2E

Nueva batería:

`PruebasReglasAlcanceE2E`

Valida:

1. comportamiento por defecto;
2. reglas por rol funcional;
3. precedencia de `DENEGAR`;
4. relaciones directas;
5. tipo de entidad sin escape del contexto;
6. reglas inactivas;
7. CRUD de reglas.

El script `probar-todo-e2e.cmd` incluye la nueva batería y, por tanto,
`probar-idempotencia-e2e.cmd` también la ejecuta dos veces consecutivas.

## Aplicación

Esta versión introduce una tabla nueva, por lo que en la fase actual debe recrearse la BD con:

`gestion_personas_completo_es_v2_5.sql`

Después aplicar el incremental de backend, reiniciar el backend y ejecutar:

```powershell
.\scripts\probar-todo-e2e.cmd
```

Y después:

```powershell
.\scripts\probar-idempotencia-e2e.cmd
```
