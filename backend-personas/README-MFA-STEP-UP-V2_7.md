# Gestión de Personas v2.7 - MFA / autenticación reforzada bajo demanda

- MFA no se liga a un dominio concreto. Se define mediante `politica_mfa`.
- Una política puede aplicar por contexto, rol de seguridad, permiso o combinación (AND).
- La sesión empieza en `BASICO`; al validar MFA pasa a `MFA`.
- Mientras la verificación siga vigente no se vuelve a pedir MFA en otros puntos que también lo requieran.
- Canales: EMAIL (correo de acceso de la cuenta) y SMS (teléfono preferido/activo de la persona asociada).
- Mecanismos: CODIGO y ENLACE de un solo uso.
- Los desafíos guardan solo SHA-256 del secreto; nunca el código/token en claro.
- Caducidad por defecto del desafío: 10 min; máximo 5 intentos; elevación por defecto 480 min.
- El proveedor actual `ProveedorEntregaMfaRegistro` es deliberadamente de desarrollo: escribe el código/enlace en el log. Antes de producción debe sustituirse por proveedor real de email/SMS.
- API MFA: `/api/autenticacion/mfa/estado`, `/desafios`, `/verificar-codigo`, `/verificar-enlace`.
- Gestión políticas: `/api/seguridad/mfa/politicas`.
- Error funcional cuando falta MFA: HTTP 403 con `validacion.codigo=MFA_REQUERIDA`.

La BD v2.7 es un script de recreación completo, no una migración.
