# Gestión documental V2

Cambios principales:

- `documento` separa entidad emisora y entidad destinataria.
- `tipo_documento` define clasificación predeterminada, categoría sensible y `maximo_accesos`.
- estados: `BORRADOR`, `DISPONIBLE`, `RETIRADO`, `SUSTITUIDO`.
- nuevo `acceso_documento`: cuenta y fecha/hora de cada acceso al contenido.
- el GET de metadatos ya no devuelve `referenciaArchivo`; la referencia solo sale en `POST /api/documentos/{id}/acceder`.
- cualquier acceso vuelve el documento inmutable y no eliminable.
- un sustituto solo puede apuntar a un documento ya accedido, del mismo tipo y del mismo destinatario.
- un documento solo puede tener un sustituto directo.
- eliminación y acceso usan bloqueo pesimista (`SELECT ... FOR UPDATE` vía JPA) sobre la misma fila.

## Concurrencia

`acceder` y `eliminar` bloquean la fila del documento con `PESSIMISTIC_WRITE` dentro de transacción.

- Si el acceso obtiene el bloqueo primero, la eliminación espera y al continuar encuentra el acceso y devuelve conflicto.
- Si la eliminación obtiene el bloqueo primero, elimina y el acceso posterior ya no encuentra el documento.
- Además `acceso_documento.documento_id` usa FK `ON DELETE RESTRICT`, por lo que la BD tampoco permite eliminar un documento que tenga accesos.

## BD

Durante esta fase se usa el script completo:

`database/gestion_personas_completo_es_v2.sql`
