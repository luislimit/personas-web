-- ============================================================
-- V4 -> V5: autenticación por sesión + permisos de cuentas/contextos
-- ============================================================

CREATE TABLE IF NOT EXISTS auth_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    last_seen_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_session_token_hash (token_hash),
    KEY idx_auth_session_account (account_id),
    KEY idx_auth_session_expiry (expires_at, revoked_at),
    CONSTRAINT fk_auth_session_account
        FOREIGN KEY (account_id) REFERENCES user_account(id) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT IGNORE INTO permission(code,description) VALUES
('ACCOUNT.READ','Consultar cuentas de usuario'),
('ACCOUNT.CREATE','Crear cuentas de usuario'),
('ACCOUNT.UPDATE','Modificar cuentas de usuario'),
('ACCOUNT.DELETE','Deshabilitar cuentas de usuario'),
('CONTEXT.READ','Consultar contextos de acceso'),
('CONTEXT.CREATE','Asignar contextos y roles'),
('CONTEXT.UPDATE','Modificar contextos y roles'),
('CONTEXT.DELETE','Revocar contextos y roles');

-- PLATFORM_ADMIN recibe todos los nuevos permisos.
INSERT IGNORE INTO role_permission(role_id,permission_id)
SELECT r.id,p.id
  FROM role r
  JOIN permission p
 WHERE r.code='PLATFORM_ADMIN'
   AND p.code IN ('ACCOUNT.READ','ACCOUNT.CREATE','ACCOUNT.UPDATE','ACCOUNT.DELETE',
                  'CONTEXT.READ','CONTEXT.CREATE','CONTEXT.UPDATE','CONTEXT.DELETE');

-- ORG_ADMIN puede administrar cuentas y contextos.
INSERT IGNORE INTO role_permission(role_id,permission_id)
SELECT r.id,p.id
  FROM role r
  JOIN permission p
 WHERE r.code='ORG_ADMIN'
   AND p.code IN ('ACCOUNT.READ','ACCOUNT.CREATE','ACCOUNT.UPDATE','ACCOUNT.DELETE',
                  'CONTEXT.READ','CONTEXT.CREATE','CONTEXT.UPDATE','CONTEXT.DELETE');
