-- ============================================================
-- GESTIÓN DE PERSONAS - SCRIPT COMPLETO MYSQL 8.0.36
-- CORE genérico + demo Club + demo Residencia
-- Versión ES-2.4 - auditoría transversal y normalización de fechas
--
-- ATENCIÓN:
-- Este script ELIMINA completamente la base gestion_personas
-- si ya existe y la recrea desde cero.
-- ============================================================

DROP DATABASE IF EXISTS gestion_personas;

CREATE DATABASE gestion_personas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE gestion_personas;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;



-- ============================================================
-- NOTA SOBRE PREFERENCIAS
-- ============================================================
-- contacto_entidad:
--   una entidad puede tener múltiples CORREO, TELEFONO, etc.
--   activo indica si el contacto está operativo.
--   preferido indica cuál es el preferido.
--
-- cuenta_usuario:
--   una PERSONA puede tener múltiples cuentas.
--   estado controla ACTIVO / BLOQUEADA / DESHABILITADA.
--   preferido indica la cuenta principal.
--
-- La regla "solo un preferido activo por tipo/persona" se aplicará
-- transaccionalmente en el API Spring Boot. No se fuerza mediante
-- columnas generadas.


-- ============================================================
-- ESQUEMA
-- ============================================================


SET NAMES utf8mb4;

CREATE TABLE tipo_entidad (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tipo_entidad_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE entidad (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tipo_entidad_id BIGINT UNSIGNED NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_entidad_tipo (tipo_entidad_id),
    KEY idx_entidad_estado (estado),
    CONSTRAINT fk_entidad_tipo FOREIGN KEY (tipo_entidad_id) REFERENCES tipo_entidad(id),
    CONSTRAINT chk_entidad_estado CHECK (estado IN ('ACTIVO','INACTIVO','ARCHIVADO'))
) ENGINE=InnoDB;

CREATE TABLE persona (
    entidad_id BIGINT UNSIGNED NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    primer_apellido VARCHAR(150) NOT NULL,
    segundo_apellido VARCHAR(150) NULL,
    fecha_nacimiento DATE NULL,
    genero VARCHAR(30) NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (entidad_id),
    CONSTRAINT fk_persona_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE organizacion (
    entidad_id BIGINT UNSIGNED NOT NULL,
    razon_social VARCHAR(200) NULL,
    identificador_fiscal VARCHAR(50) NULL,
    tipo_organizacion VARCHAR(50) NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (entidad_id),
    UNIQUE KEY uk_organizacion_identificador_fiscal (identificador_fiscal),
    CONSTRAINT fk_organizacion_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE instalacion (
    entidad_id BIGINT UNSIGNED NOT NULL,
    tipo_instalacion VARCHAR(50) NULL,
    capacidad INT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (entidad_id),
    CONSTRAINT fk_instalacion_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE identificador_entidad (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entidad_id BIGINT UNSIGNED NOT NULL,
    tipo_identificador VARCHAR(50) NOT NULL,
    valor_identificador VARCHAR(150) NOT NULL,
    emisor VARCHAR(150) NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    valido_desde DATE NULL,
    valido_hasta DATE NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_identificador_tipo_valor (tipo_identificador, valor_identificador),
    KEY idx_identificador_entidad (entidad_id),
    CONSTRAINT fk_identificador_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE contacto_entidad (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entidad_id BIGINT UNSIGNED NOT NULL,
    tipo_contacto VARCHAR(50) NOT NULL,
    valor_contacto VARCHAR(255) NOT NULL,
    etiqueta VARCHAR(100) NULL,
    preferido BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_verificacion DATE NULL,
    valido_desde DATE NULL,
    valido_hasta DATE NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contacto_valor_por_entidad (entidad_id, tipo_contacto, valor_contacto),
    KEY idx_contacto_entidad (entidad_id),
    KEY idx_contacto_entidad_tipo_activo (entidad_id, tipo_contacto, activo),
    CONSTRAINT fk_contacto_entidad
        FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE,
    CONSTRAINT chk_contacto_fechas
        CHECK (valido_hasta IS NULL OR valido_desde IS NULL OR valido_hasta >= valido_desde)
) ENGINE=InnoDB;

CREATE TABLE direccion_entidad (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entidad_id BIGINT UNSIGNED NOT NULL,
    tipo_direccion VARCHAR(50) NOT NULL DEFAULT 'PRINCIPAL',
    direccion VARCHAR(200) NOT NULL,
    codigo_postal VARCHAR(20) NULL,
    municipio VARCHAR(100) NOT NULL,
    provincia VARCHAR(100) NULL,
    codigo_pais CHAR(2) NOT NULL DEFAULT 'ES',
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_direccion_entidad (entidad_id),
    CONSTRAINT fk_direccion_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE tipo_relacion (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(60) NOT NULL,
    nombre_directo VARCHAR(100) NOT NULL,
    nombre_inverso VARCHAR(100) NOT NULL,
    simetrica BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_relacion_tipo_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE relacion_entidad (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entidad_origen_id BIGINT UNSIGNED NOT NULL,
    entidad_destino_id BIGINT UNSIGNED NOT NULL,
    tipo_relacion_id BIGINT UNSIGNED NOT NULL,
    valido_desde DATE NULL,
    valido_hasta DATE NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entidad_relacion_activo (entidad_origen_id, entidad_destino_id, tipo_relacion_id, valido_desde),
    KEY idx_relacion_origen (entidad_origen_id),
    KEY idx_relacion_destino (entidad_destino_id),
    KEY idx_relacion_tipo (tipo_relacion_id),
    CONSTRAINT fk_relacion_origen FOREIGN KEY (entidad_origen_id) REFERENCES entidad(id),
    CONSTRAINT fk_relacion_destino FOREIGN KEY (entidad_destino_id) REFERENCES entidad(id),
    CONSTRAINT fk_relacion_tipo FOREIGN KEY (tipo_relacion_id) REFERENCES tipo_relacion(id),
    CONSTRAINT chk_relacion_estado CHECK (estado IN ('ACTIVO','INACTIVO','FINALIZADO'))
) ENGINE=InnoDB;

CREATE TABLE rol_funcional (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(60) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entidad_rol_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE rol_funcional_entidad_contexto (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entidad_id BIGINT UNSIGNED NOT NULL,
    entidad_contexto_id BIGINT UNSIGNED NOT NULL,
    rol_funcional_id BIGINT UNSIGNED NOT NULL,
    valido_desde DATE NULL,
    valido_hasta DATE NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rol_funcional_entidad_contexto (entidad_id, entidad_contexto_id, rol_funcional_id, valido_desde),
    KEY idx_ecr_entidad (entidad_id),
    KEY idx_ecr_contexto (entidad_contexto_id),
    CONSTRAINT fk_ecr_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id),
    CONSTRAINT fk_ecr_contexto FOREIGN KEY (entidad_contexto_id) REFERENCES entidad(id),
    CONSTRAINT fk_ecr_rol FOREIGN KEY (rol_funcional_id) REFERENCES rol_funcional(id),
    CONSTRAINT chk_ecr_estado CHECK (estado IN ('ACTIVO','INACTIVO','FINALIZADO'))
) ENGINE=InnoDB;

CREATE TABLE cuenta_usuario (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    persona_entidad_id BIGINT UNSIGNED NULL,
    nombre_usuario VARCHAR(150) NOT NULL,
    correo_acceso VARCHAR(255) NULL,
    hash_contrasena VARCHAR(255) NOT NULL,
    proveedor_autenticacion VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    sujeto_externo VARCHAR(255) NULL,
    preferido BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ultimo_acceso DATETIME NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_username (nombre_usuario),
    UNIQUE KEY uk_usuario_proveedor_sujeto (proveedor_autenticacion, sujeto_externo),
    KEY idx_usuario_persona (persona_entidad_id),
    KEY idx_usuario_acceso_correo (correo_acceso),
    KEY idx_usuario_persona_estado_preferido (persona_entidad_id, estado, preferido),
    CONSTRAINT fk_usuario_persona
        FOREIGN KEY (persona_entidad_id) REFERENCES entidad(id),
    CONSTRAINT chk_usuario_estado CHECK (estado IN ('ACTIVO','BLOQUEADA','DESHABILITADA'))
) ENGINE=InnoDB;


CREATE TABLE sesion_autenticacion (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cuenta_id BIGINT UNSIGNED NOT NULL,
    hash_token CHAR(64) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion DATETIME NOT NULL,
    ultimo_uso DATETIME NULL,
    fecha_revocacion DATETIME NULL,
    nivel_autenticacion VARCHAR(20) NOT NULL DEFAULT 'BASICO',
    mfa_verificada_en DATETIME NULL,
    mfa_valida_hasta DATETIME NULL,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_autenticacion_sesion_token_hash (hash_token),
    KEY idx_autenticacion_sesion_cuenta (cuenta_id),
    KEY idx_autenticacion_sesion_expiracion (fecha_expiracion, fecha_revocacion),
    CONSTRAINT fk_autenticacion_sesion_cuenta
        FOREIGN KEY (cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE CASCADE,
    CONSTRAINT chk_sesion_nivel_autenticacion CHECK (nivel_autenticacion IN ('BASICO','MFA'))
) ENGINE=InnoDB;

CREATE TABLE desafio_mfa (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sesion_id BIGINT UNSIGNED NOT NULL, cuenta_id BIGINT UNSIGNED NOT NULL,
    canal VARCHAR(20) NOT NULL, mecanismo VARCHAR(20) NOT NULL, destino_enmascarado VARCHAR(255) NOT NULL,
    hash_secreto CHAR(64) NOT NULL, fecha_expiracion DATETIME NOT NULL, intentos INT NOT NULL DEFAULT 0,
    maximo_intentos INT NOT NULL DEFAULT 5, fecha_consumo DATETIME NULL, fecha_invalidacion DATETIME NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL, auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL, auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id), KEY idx_desafio_mfa_sesion (sesion_id,fecha_expiracion), KEY idx_desafio_mfa_cuenta (cuenta_id),
    CONSTRAINT fk_desafio_mfa_sesion FOREIGN KEY (sesion_id) REFERENCES sesion_autenticacion(id) ON DELETE CASCADE,
    CONSTRAINT fk_desafio_mfa_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE CASCADE,
    CONSTRAINT chk_desafio_mfa_canal CHECK (canal IN ('EMAIL','SMS')),
    CONSTRAINT chk_desafio_mfa_mecanismo CHECK (mecanismo IN ('CODIGO','ENLACE')),
    CONSTRAINT chk_desafio_mfa_intentos CHECK (intentos >= 0 AND maximo_intentos > 0)
) ENGINE=InnoDB;

CREATE TABLE rol_seguridad (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(80) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rol_seguridad_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE permiso (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(255) NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permiso_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE rol_seguridad_permiso (
    rol_seguridad_id BIGINT UNSIGNED NOT NULL,
    permiso_id BIGINT UNSIGNED NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (rol_seguridad_id, permiso_id),
    CONSTRAINT fk_rp_rol FOREIGN KEY (rol_seguridad_id) REFERENCES rol_seguridad(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permiso FOREIGN KEY (permiso_id) REFERENCES permiso(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE contexto_cuenta (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cuenta_id BIGINT UNSIGNED NOT NULL,
    entidad_contexto_id BIGINT UNSIGNED NOT NULL,
    rol_seguridad_id BIGINT UNSIGNED NOT NULL,
    alcance VARCHAR(255) NULL,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE,
    valido_desde DATE NULL,
    valido_hasta DATE NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cuenta_contexto (cuenta_id, entidad_contexto_id, rol_seguridad_id),
    KEY idx_cuenta_contexto_contexto (entidad_contexto_id),
    CONSTRAINT fk_ac_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_ac_contexto FOREIGN KEY (entidad_contexto_id) REFERENCES entidad(id),
    CONSTRAINT fk_ac_rol FOREIGN KEY (rol_seguridad_id) REFERENCES rol_seguridad(id)
) ENGINE=InnoDB;


CREATE TABLE regla_alcance_acceso (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    contexto_cuenta_id BIGINT UNSIGNED NOT NULL,
    tipo_regla VARCHAR(30) NOT NULL,
    tipo_relacion_id BIGINT UNSIGNED NULL,
    rol_funcional_id BIGINT UNSIGNED NULL,
    tipo_entidad_objetivo_id BIGINT UNSIGNED NULL,
    incluir_descendientes BOOLEAN NOT NULL DEFAULT FALSE,
    efecto VARCHAR(10) NOT NULL,
    prioridad INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_regla_alcance_contexto (contexto_cuenta_id, activo, prioridad),
    KEY idx_regla_alcance_relacion (tipo_relacion_id),
    KEY idx_regla_alcance_rol_funcional (rol_funcional_id),
    KEY idx_regla_alcance_tipo_entidad (tipo_entidad_objetivo_id),
    CONSTRAINT fk_regla_alcance_contexto FOREIGN KEY (contexto_cuenta_id) REFERENCES contexto_cuenta(id) ON DELETE CASCADE,
    CONSTRAINT fk_regla_alcance_relacion FOREIGN KEY (tipo_relacion_id) REFERENCES tipo_relacion(id),
    CONSTRAINT fk_regla_alcance_rol_funcional FOREIGN KEY (rol_funcional_id) REFERENCES rol_funcional(id),
    CONSTRAINT fk_regla_alcance_tipo_entidad FOREIGN KEY (tipo_entidad_objetivo_id) REFERENCES tipo_entidad(id),
    CONSTRAINT chk_regla_alcance_tipo CHECK (tipo_regla IN ('RELACION','ROL_FUNCIONAL','TIPO_ENTIDAD','COMPUESTA')),
    CONSTRAINT chk_regla_alcance_efecto CHECK (efecto IN ('PERMITIR','DENEGAR')),
    CONSTRAINT chk_regla_alcance_criterio CHECK (tipo_relacion_id IS NOT NULL OR rol_funcional_id IS NOT NULL OR tipo_entidad_objetivo_id IS NOT NULL),
    CONSTRAINT chk_regla_alcance_descendientes CHECK (incluir_descendientes = FALSE OR tipo_relacion_id IS NOT NULL),
    CONSTRAINT chk_regla_alcance_coherencia CHECK (
        (tipo_regla='RELACION' AND tipo_relacion_id IS NOT NULL AND rol_funcional_id IS NULL AND tipo_entidad_objetivo_id IS NULL) OR
        (tipo_regla='ROL_FUNCIONAL' AND tipo_relacion_id IS NULL AND rol_funcional_id IS NOT NULL AND tipo_entidad_objetivo_id IS NULL) OR
        (tipo_regla='TIPO_ENTIDAD' AND tipo_relacion_id IS NULL AND rol_funcional_id IS NULL AND tipo_entidad_objetivo_id IS NOT NULL) OR
        (tipo_regla='COMPUESTA' AND ((tipo_relacion_id IS NOT NULL) + (rol_funcional_id IS NOT NULL) + (tipo_entidad_objetivo_id IS NOT NULL)) >= 2)
    )
) ENGINE=InnoDB;

CREATE TABLE excepcion_permiso_acceso (
    contexto_cuenta_id BIGINT UNSIGNED NOT NULL,
    permiso_id BIGINT UNSIGNED NOT NULL,
    efecto VARCHAR(10) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (contexto_cuenta_id, permiso_id),
    CONSTRAINT fk_apo_ac FOREIGN KEY (contexto_cuenta_id) REFERENCES contexto_cuenta(id) ON DELETE CASCADE,
    CONSTRAINT fk_apo_permiso FOREIGN KEY (permiso_id) REFERENCES permiso(id) ON DELETE CASCADE,
    CONSTRAINT chk_apo_efecto CHECK (efecto IN ('PERMITIR','DENEGAR'))
) ENGINE=InnoDB;

CREATE TABLE politica_mfa (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entidad_contexto_id BIGINT UNSIGNED NULL, rol_seguridad_id BIGINT UNSIGNED NULL, permiso_id BIGINT UNSIGNED NULL,
    minutos_validez INT NOT NULL DEFAULT 480, activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL, auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL, auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY(id), KEY idx_politica_mfa_contexto(entidad_contexto_id), KEY idx_politica_mfa_rol(rol_seguridad_id), KEY idx_politica_mfa_permiso(permiso_id),
    CONSTRAINT fk_politica_mfa_contexto FOREIGN KEY(entidad_contexto_id) REFERENCES entidad(id) ON DELETE CASCADE,
    CONSTRAINT fk_politica_mfa_rol FOREIGN KEY(rol_seguridad_id) REFERENCES rol_seguridad(id) ON DELETE CASCADE,
    CONSTRAINT fk_politica_mfa_permiso FOREIGN KEY(permiso_id) REFERENCES permiso(id) ON DELETE CASCADE,
    CONSTRAINT chk_politica_mfa_objetivo CHECK (entidad_contexto_id IS NOT NULL OR rol_seguridad_id IS NOT NULL OR permiso_id IS NOT NULL),
    CONSTRAINT chk_politica_mfa_minutos CHECK (minutos_validez BETWEEN 1 AND 10080)
) ENGINE=InnoDB;

CREATE TABLE calendario (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entidad_propietaria_id BIGINT UNSIGNED NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(500) NULL,
    color_predeterminado VARCHAR(20) NULL,
    zona_horaria VARCHAR(80) NOT NULL DEFAULT 'Europe/Madrid',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_calendario_propietario (entidad_propietaria_id),
    CONSTRAINT fk_calendario_propietario FOREIGN KEY (entidad_propietaria_id) REFERENCES entidad(id)
) ENGINE=InnoDB;

CREATE TABLE preferencia_calendario (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    calendario_id BIGINT UNSIGNED NOT NULL,
    entidad_id BIGINT UNSIGNED NULL,
    cuenta_id BIGINT UNSIGNED NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    color_personalizado VARCHAR(20) NULL,
    notificaciones_habilitadas BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_calendario_preferencia_entidad (calendario_id, entidad_id),
    UNIQUE KEY uk_calendario_preferencia_cuenta (calendario_id, cuenta_id),
    CONSTRAINT fk_cp_calendario FOREIGN KEY (calendario_id) REFERENCES calendario(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE CASCADE,
    CONSTRAINT chk_cp_propietario CHECK (
        (entidad_id IS NOT NULL AND cuenta_id IS NULL) OR
        (entidad_id IS NULL AND cuenta_id IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE evento_calendario (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    calendario_id BIGINT UNSIGNED NOT NULL,
    titulo VARCHAR(220) NOT NULL,
    descripcion TEXT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NULL,
    dia_completo BOOLEAN NOT NULL DEFAULT FALSE,
    zona_horaria VARCHAR(80) NULL,
    entidad_ubicacion_id BIGINT UNSIGNED NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADO',
    creado_por_cuenta_id BIGINT UNSIGNED NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_evento_calendario_inicio (calendario_id, fecha_inicio),
    KEY idx_evento_ubicacion (entidad_ubicacion_id),
    KEY idx_evento_creado_por (creado_por_cuenta_id),
    CONSTRAINT fk_evento_calendario FOREIGN KEY (calendario_id) REFERENCES calendario(id) ON DELETE CASCADE,
    CONSTRAINT fk_evento_ubicacion FOREIGN KEY (entidad_ubicacion_id) REFERENCES entidad(id),
    CONSTRAINT fk_evento_creado_por FOREIGN KEY (creado_por_cuenta_id) REFERENCES cuenta_usuario(id),
    CONSTRAINT chk_evento_estado CHECK (estado IN ('PROGRAMADO','CANCELADO','COMPLETADO'))
) ENGINE=InnoDB;

-- Regla de recurrencia asociada a un evento maestro.
-- El evento maestro contiene título, horario base, ubicación, etc.
CREATE TABLE recurrencia_evento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    evento_id BIGINT UNSIGNED NOT NULL,
    frecuencia VARCHAR(20) NOT NULL,
    intervalo INT NOT NULL DEFAULT 1,
    por_dia_semana VARCHAR(80) NULL COMMENT 'Lista RFC5545 simplificada: MO,TU,WE,TH,FR,SA,SU',
    por_dia_mes VARCHAR(120) NULL COMMENT 'Días del mes separados por coma, por ejemplo 1,15',
    hasta_fecha DATETIME NULL,
    numero_ocurrencias INT NULL,
    zona_horaria VARCHAR(80) NOT NULL DEFAULT 'Europe/Madrid',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recurrencia_evento (evento_id),
    CONSTRAINT fk_recurrencia_evento FOREIGN KEY (evento_id) REFERENCES evento_calendario(id) ON DELETE CASCADE,
    CONSTRAINT chk_recurrencia_frecuencia CHECK (frecuencia IN ('DIARIA','SEMANAL','MENSUAL','ANUAL')),
    CONSTRAINT chk_recurrencia_intervalo CHECK (intervalo >= 1),
    CONSTRAINT chk_recurrencia_conteo CHECK (numero_ocurrencias IS NULL OR numero_ocurrencias >= 1),
    CONSTRAINT chk_recurrencia_fin CHECK (NOT (hasta_fecha IS NOT NULL AND numero_ocurrencias IS NOT NULL))
) ENGINE=InnoDB;

-- Excepciones de una ocurrencia concreta de una serie.
-- MODIFICADA apunta a un evento_calendario de sustitución; CANCELADO omite la ocurrencia.
-- Nota MySQL 8.0:
-- La coherencia entre tipo_excepcion y evento_sustitucion_id se valida en backend.
-- MySQL no permite usar en un CHECK una columna implicada en una FK con ON DELETE SET NULL.
CREATE TABLE excepcion_recurrencia_evento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    recurrencia_id BIGINT UNSIGNED NOT NULL,
    fecha_inicio_original DATETIME NOT NULL,
    tipo_excepcion VARCHAR(20) NOT NULL,
    evento_sustitucion_id BIGINT UNSIGNED NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recurrencia_excepcion (recurrencia_id, fecha_inicio_original),
    KEY idx_recurrencia_excepcion_sustitucion (evento_sustitucion_id),
    CONSTRAINT fk_recurrencia_excepcion_serie FOREIGN KEY (recurrencia_id) REFERENCES recurrencia_evento(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurrencia_excepcion_sustitucion FOREIGN KEY (evento_sustitucion_id) REFERENCES evento_calendario(id) ON DELETE SET NULL,
    CONSTRAINT chk_recurrencia_excepcion_tipo CHECK (tipo_excepcion IN ('MODIFICADA','CANCELADA'))
) ENGINE=InnoDB;

CREATE TABLE participante_evento (
    evento_id BIGINT UNSIGNED NOT NULL,
    entidad_id BIGINT UNSIGNED NOT NULL,
    rol_participante VARCHAR(60) NOT NULL DEFAULT 'ASISTENTE',
    estado VARCHAR(30) NOT NULL DEFAULT 'INVITADO',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (evento_id, entidad_id, rol_participante),
    CONSTRAINT fk_ep_evento FOREIGN KEY (evento_id) REFERENCES evento_calendario(id) ON DELETE CASCADE,
    CONSTRAINT fk_ep_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id) ON DELETE CASCADE,
    CONSTRAINT chk_ep_estado CHECK (estado IN ('INVITADO','ACEPTADO','RECHAZADO','TENTATIVO','ASISTIO','AUSENTE'))
) ENGINE=InnoDB;

CREATE TABLE clasificacion_dato (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    nivel_sensibilidad INT NOT NULL DEFAULT 0,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_clasificacion_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE categoria_sensible (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(60) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sensible_categoria_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE tipo_documento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(60) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    clasificacion_predeterminada_id BIGINT UNSIGNED NOT NULL,
    categoria_sensible_predeterminada_id BIGINT UNSIGNED NULL,
    maximo_accesos INT NULL COMMENT 'NULL = accesos ilimitados; 1 = acceso único; N = máximo N accesos',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tipo_documento_codigo (codigo),
    KEY idx_tipo_documento_clasificacion (clasificacion_predeterminada_id),
    KEY idx_tipo_documento_categoria_sensible (categoria_sensible_predeterminada_id),
    CONSTRAINT fk_tipo_documento_clasificacion
        FOREIGN KEY (clasificacion_predeterminada_id) REFERENCES clasificacion_dato(id),
    CONSTRAINT fk_tipo_documento_categoria_sensible
        FOREIGN KEY (categoria_sensible_predeterminada_id) REFERENCES categoria_sensible(id),
    CONSTRAINT chk_tipo_documento_maximo_accesos
        CHECK (maximo_accesos IS NULL OR maximo_accesos >= 1)
) ENGINE=InnoDB;

-- Nota MySQL 8.0:
-- La prevención de autosustitución (documento que se sustituye a sí mismo)
-- se valida en backend. MySQL no permite que un CHECK haga referencia
-- a una columna AUTO_INCREMENT de la misma tabla.
CREATE TABLE documento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tipo_documento_id BIGINT UNSIGNED NOT NULL,
    entidad_emisora_id BIGINT UNSIGNED NOT NULL,
    entidad_destinataria_id BIGINT UNSIGNED NOT NULL,
    clasificacion_id BIGINT UNSIGNED NOT NULL,
    categoria_sensible_id BIGINT UNSIGNED NULL,
    nombre VARCHAR(220) NOT NULL,
    descripcion VARCHAR(500) NULL,
    referencia_archivo VARCHAR(500) NOT NULL,
    fecha_documento DATE NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR',
    sustituye_a_documento_id BIGINT UNSIGNED NULL,
    creado_por_cuenta_id BIGINT UNSIGNED NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    fecha_disponibilidad DATE NULL,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_documento_sustituye_a (sustituye_a_documento_id),
    KEY idx_documento_tipo (tipo_documento_id),
    KEY idx_documento_emisora (entidad_emisora_id),
    KEY idx_documento_destinataria (entidad_destinataria_id),
    KEY idx_documento_clasificacion (clasificacion_id),
    KEY idx_documento_categoria_sensible (categoria_sensible_id),
    KEY idx_documento_estado (estado),
    KEY idx_documento_creado_por (creado_por_cuenta_id),
    CONSTRAINT fk_documento_tipo FOREIGN KEY (tipo_documento_id) REFERENCES tipo_documento(id),
    CONSTRAINT fk_documento_emisora FOREIGN KEY (entidad_emisora_id) REFERENCES entidad(id),
    CONSTRAINT fk_documento_destinataria FOREIGN KEY (entidad_destinataria_id) REFERENCES entidad(id),
    CONSTRAINT fk_documento_clasificacion FOREIGN KEY (clasificacion_id) REFERENCES clasificacion_dato(id),
    CONSTRAINT fk_documento_categoria_sensible FOREIGN KEY (categoria_sensible_id) REFERENCES categoria_sensible(id),
    CONSTRAINT fk_documento_creado_por FOREIGN KEY (creado_por_cuenta_id) REFERENCES cuenta_usuario(id),
    CONSTRAINT fk_documento_sustituye_a FOREIGN KEY (sustituye_a_documento_id) REFERENCES documento(id),
    CONSTRAINT chk_documento_estado CHECK (estado IN ('BORRADOR','DISPONIBLE','RETIRADO','SUSTITUIDO'))
) ENGINE=InnoDB;

-- Un acceso significa que el contenido del documento se ha puesto a disposición
-- de una cuenta. Desde ese momento el documento es inmutable y no eliminable.
CREATE TABLE acceso_documento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    documento_id BIGINT UNSIGNED NOT NULL,
    cuenta_usuario_id BIGINT UNSIGNED NOT NULL,
    fecha_acceso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_acceso_documento_documento (documento_id, fecha_acceso),
    KEY idx_acceso_documento_cuenta (cuenta_usuario_id, fecha_acceso),
    CONSTRAINT fk_acceso_documento_documento
        FOREIGN KEY (documento_id) REFERENCES documento(id) ON DELETE RESTRICT,
    CONSTRAINT fk_acceso_documento_cuenta
        FOREIGN KEY (cuenta_usuario_id) REFERENCES cuenta_usuario(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE tipo_solicitud (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(60) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    puede_generar_evento BOOLEAN NOT NULL DEFAULT FALSE,
    puede_generar_documento BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solicitud_tipo_codigo (codigo)
) ENGINE=InnoDB;

CREATE TABLE solicitud (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tipo_solicitud_id BIGINT UNSIGNED NOT NULL,
    asunto VARCHAR(220) NOT NULL,
    descripcion TEXT NULL,
    creado_por_entidad_id BIGINT UNSIGNED NOT NULL,
    fecha_solicitud DATE NOT NULL DEFAULT (CURRENT_DATE),
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_global VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
    fecha_resolucion DATE NULL,
    fecha_cierre DATE NULL,
    solicitud_padre_id BIGINT UNSIGNED NULL,
    evento_generado_id BIGINT UNSIGNED NULL,
    documento_generado_id BIGINT UNSIGNED NULL,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_solicitud_creador (creado_por_entidad_id),
    KEY idx_solicitud_estado (estado_global),
    CONSTRAINT fk_solicitud_tipo FOREIGN KEY (tipo_solicitud_id) REFERENCES tipo_solicitud(id),
    CONSTRAINT fk_solicitud_creador FOREIGN KEY (creado_por_entidad_id) REFERENCES entidad(id),
    CONSTRAINT fk_solicitud_padre FOREIGN KEY (solicitud_padre_id) REFERENCES solicitud(id),
    CONSTRAINT fk_solicitud_evento FOREIGN KEY (evento_generado_id) REFERENCES evento_calendario(id),
    CONSTRAINT fk_solicitud_documento FOREIGN KEY (documento_generado_id) REFERENCES documento(id),
    CONSTRAINT chk_solicitud_estado CHECK (estado_global IN ('ABIERTA','RESUELTA','CANCELADO','CERRADA'))
) ENGINE=InnoDB;

CREATE TABLE participante_solicitud (
    solicitud_id BIGINT UNSIGNED NOT NULL,
    entidad_id BIGINT UNSIGNED NOT NULL,
    rol_participante VARCHAR(50) NOT NULL,
    estado_participante VARCHAR(30) NOT NULL,
    fecha_lectura DATE NULL,
    fecha_respuesta DATE NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (solicitud_id, entidad_id, rol_participante),
    CONSTRAINT fk_rp_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_entidad FOREIGN KEY (entidad_id) REFERENCES entidad(id),
    CONSTRAINT chk_rp_estado CHECK (estado_participante IN ('EN_ESPERA','PENDIENTE','COMPLETADA','CANCELADO'))
) ENGINE=InnoDB;

CREATE TABLE mensaje_solicitud (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    solicitud_id BIGINT UNSIGNED NOT NULL,
    entidad_remitente_id BIGINT UNSIGNED NOT NULL,
    mensaje TEXT NOT NULL,
    fecha_mensaje DATE NOT NULL DEFAULT (CURRENT_DATE),
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_solicitud_mensaje_solicitud (solicitud_id, fecha_creacion),
    CONSTRAINT fk_rm_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud(id) ON DELETE CASCADE,
    CONSTRAINT fk_rm_remitente FOREIGN KEY (entidad_remitente_id) REFERENCES entidad(id)
) ENGINE=InnoDB;

CREATE TABLE accion_solicitud (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    solicitud_id BIGINT UNSIGNED NOT NULL,
    entidad_actor_id BIGINT UNSIGNED NULL,
    tipo_accion VARCHAR(60) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    datos JSON NULL,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_solicitud_accion_solicitud (solicitud_id, fecha_creacion),
    CONSTRAINT fk_ra_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitud(id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_actor FOREIGN KEY (entidad_actor_id) REFERENCES entidad(id)
) ENGINE=InnoDB;

CREATE TABLE auditoria_acceso (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cuenta_id BIGINT UNSIGNED NULL,
    entidad_contexto_id BIGINT UNSIGNED NULL,
    tipo_recurso VARCHAR(60) NOT NULL,
    recurso_id BIGINT UNSIGNED NULL,
    entidad_destino_id BIGINT UNSIGNED NULL,
    accion VARCHAR(60) NOT NULL,
    codigo_clasificacion VARCHAR(50) NULL,
    ip_origen VARCHAR(45) NULL,
    fecha_acceso DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_acceso_auditoria_cuenta (cuenta_id, fecha_acceso),
    KEY idx_acceso_auditoria_destino (entidad_destino_id, fecha_acceso),
    CONSTRAINT fk_acceso_auditoria_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta_usuario(id),
    CONSTRAINT fk_acceso_auditoria_contexto FOREIGN KEY (entidad_contexto_id) REFERENCES entidad(id),
    CONSTRAINT fk_acceso_auditoria_destino FOREIGN KEY (entidad_destino_id) REFERENCES entidad(id)
) ENGINE=InnoDB;

CREATE TABLE auditoria_cambio (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cuenta_id BIGINT UNSIGNED NULL,
    entidad_contexto_id BIGINT UNSIGNED NULL,
    tipo_recurso VARCHAR(60) NOT NULL,
    recurso_id BIGINT UNSIGNED NULL,
    accion VARCHAR(60) NOT NULL,
    valor_anterior JSON NULL,
    valor_nuevo JSON NULL,
    ip_origen VARCHAR(45) NULL,
    fecha_cambio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    auditoria_creado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_creacion VARCHAR(45) NULL,
    auditoria_modificado_por_cuenta_id BIGINT UNSIGNED NULL,
    auditoria_ip_modificacion VARCHAR(45) NULL,
    PRIMARY KEY (id),
    KEY idx_cambio_auditoria_cuenta (cuenta_id, fecha_cambio),
    CONSTRAINT fk_cambio_auditoria_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta_usuario(id),
    CONSTRAINT fk_cambio_auditoria_contexto FOREIGN KEY (entidad_contexto_id) REFERENCES entidad(id)
) ENGINE=InnoDB;


-- ============================================================
-- CLAVES FORÁNEAS DE AUDITORÍA ESTÁNDAR
-- ============================================================

ALTER TABLE tipo_entidad
    ADD CONSTRAINT fk_aud_1_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_1_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE entidad
    ADD CONSTRAINT fk_aud_2_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_2_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE persona
    ADD CONSTRAINT fk_aud_3_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_3_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE organizacion
    ADD CONSTRAINT fk_aud_4_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_4_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE instalacion
    ADD CONSTRAINT fk_aud_5_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_5_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE identificador_entidad
    ADD CONSTRAINT fk_aud_6_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_6_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE contacto_entidad
    ADD CONSTRAINT fk_aud_7_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_7_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE direccion_entidad
    ADD CONSTRAINT fk_aud_8_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_8_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE tipo_relacion
    ADD CONSTRAINT fk_aud_9_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_9_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE relacion_entidad
    ADD CONSTRAINT fk_aud_10_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_10_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE rol_funcional
    ADD CONSTRAINT fk_aud_11_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_11_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE rol_funcional_entidad_contexto
    ADD CONSTRAINT fk_aud_12_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_12_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE cuenta_usuario
    ADD CONSTRAINT fk_aud_13_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_13_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE sesion_autenticacion
    ADD CONSTRAINT fk_aud_14_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_14_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE rol_seguridad
    ADD CONSTRAINT fk_aud_15_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_15_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE permiso
    ADD CONSTRAINT fk_aud_16_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_16_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE rol_seguridad_permiso
    ADD CONSTRAINT fk_aud_17_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_17_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE contexto_cuenta
    ADD CONSTRAINT fk_aud_18_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_18_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE excepcion_permiso_acceso
    ADD CONSTRAINT fk_aud_19_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_19_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE calendario
    ADD CONSTRAINT fk_aud_20_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_20_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE preferencia_calendario
    ADD CONSTRAINT fk_aud_21_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_21_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE evento_calendario
    ADD CONSTRAINT fk_aud_22_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_22_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE recurrencia_evento
    ADD CONSTRAINT fk_aud_23_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_23_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE excepcion_recurrencia_evento
    ADD CONSTRAINT fk_aud_24_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_24_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE participante_evento
    ADD CONSTRAINT fk_aud_25_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_25_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE clasificacion_dato
    ADD CONSTRAINT fk_aud_26_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_26_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE categoria_sensible
    ADD CONSTRAINT fk_aud_27_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_27_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE tipo_documento
    ADD CONSTRAINT fk_aud_28_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_28_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE documento
    ADD CONSTRAINT fk_aud_29_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_29_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE acceso_documento
    ADD CONSTRAINT fk_aud_30_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_30_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE tipo_solicitud
    ADD CONSTRAINT fk_aud_31_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_31_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE solicitud
    ADD CONSTRAINT fk_aud_32_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_32_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE participante_solicitud
    ADD CONSTRAINT fk_aud_33_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_33_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE mensaje_solicitud
    ADD CONSTRAINT fk_aud_34_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_34_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE accion_solicitud
    ADD CONSTRAINT fk_aud_35_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_35_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE auditoria_acceso
    ADD CONSTRAINT fk_aud_36_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_36_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;
ALTER TABLE auditoria_cambio
    ADD CONSTRAINT fk_aud_37_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_37_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;



ALTER TABLE regla_alcance_acceso
    ADD CONSTRAINT fk_aud_38_crea FOREIGN KEY (auditoria_creado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_aud_38_mod FOREIGN KEY (auditoria_modificado_por_cuenta_id) REFERENCES cuenta_usuario(id) ON DELETE SET NULL;


-- ============================================================
-- CATÁLOGOS Y PERMISOS
-- ============================================================


INSERT INTO tipo_entidad(codigo,nombre) VALUES
('PERSONA','Persona'),
('ORGANIZACION','Organización'),
('INSTALACION','Instalación'),
('GRUPO','Grupo o unidad organizativa');

INSERT INTO tipo_relacion(codigo,nombre_directo,nombre_inverso,simetrica) VALUES
('PADRE_HIJO','PADRE_MADRE_DE','HIJO_HIJA_DE',FALSE),
('TUTOR_TUTELADO','TUTOR_DE','TUTELADO_POR',FALSE),
('PERTENENCIA','TIENE_MIEMBRO','MIEMBRO_DE',FALSE),
('PROPIEDAD','POSEE','PROPIEDAD_DE',FALSE),
('GESTION','GESTIONA','GESTIONADO_POR',FALSE),
('HERMANOS','HERMANO_DE','HERMANO_DE',TRUE);

INSERT INTO rol_funcional(codigo,nombre) VALUES
('JUGADOR','Jugador'),
('ENTRENADOR','Entrenador'),
('PADRE_MADRE','Padre/Madre'),
('TUTOR','Tutor'),
('MIEMBRO','Miembro'),
('RESIDENTE','Residente'),
('ENFERMERIA','Enfermería'),
('MEDICO','Médico'),
('ADMINISTRACION','Administración'),
('DIRECCION','Dirección');

INSERT INTO rol_seguridad(codigo,nombre) VALUES
('ADMIN_PLATAFORMA','Administrador plataforma'),
('ADMIN_ORGANIZACION','Administrador organización'),
('GESTOR_CONTEXTO','Gestor de contexto'),
('AUTOSERVICIO','Autoservicio'),
('PERSONAL_ORGANIZACION','Personal de organización'),
('GESTOR_INFORMACION_SENSIBLE','Gestor de información sensible');

INSERT INTO permiso(codigo,descripcion) VALUES
('ENTIDAD.LEER','Consultar entidades'),
('ENTIDAD.CREAR','Crear entidades'),
('ENTIDAD.MODIFICAR','Modificar entidades'),
('ENTIDAD.ELIMINAR','Eliminar entidades'),
('DATOS_PERSONALES.LEER','Consultar datos personales de terceros'),
('DATOS_PERSONALES.MODIFICAR','Modificar datos personales'),
('CALENDARIO.LEER','Consultar calendarios'),
('CALENDARIO.CREAR','Crear calendarios'),
('CALENDARIO.MODIFICAR','Modificar calendarios'),
('CALENDARIO.ELIMINAR','Eliminar calendarios'),
('EVENTO.LEER','Consultar eventos'),
('EVENTO.CREAR','Crear eventos'),
('EVENTO.MODIFICAR','Modificar eventos'),
('EVENTO.ELIMINAR','Eliminar eventos'),
('DOCUMENTO.LEER','Consultar documentos normales'),
('DOCUMENTO.CREAR','Crear documentos'),
('DOCUMENTO.MODIFICAR','Modificar documentos'),
('DOCUMENTO.ELIMINAR','Eliminar documentos'),
('INFORMACION_SENSIBLE.LEER','Consultar información sensible'),
('INFORMACION_SENSIBLE.MODIFICAR','Modificar información sensible'),
('SOLICITUD.LEER','Consultar solicitudes'),
('SOLICITUD.CREAR','Crear solicitudes'),
('SOLICITUD.MODIFICAR','Gestionar solicitudes'),
('SOLICITUD.CANCELAR','Cancelar solicitudes'),
('CUENTA.LEER','Consultar cuentas de usuario'),
('CUENTA.CREAR','Crear cuentas de usuario'),
('CUENTA.MODIFICAR','Modificar cuentas de usuario'),
('CUENTA.ELIMINAR','Deshabilitar cuentas de usuario'),
('CONTEXTO.LEER','Consultar contextos de acceso'),
('CONTEXTO.CREAR','Asignar contextos y roles'),
('CONTEXTO.MODIFICAR','Modificar contextos y roles'),
('CONTEXTO.ELIMINAR','Revocar contextos y roles'),
('MFA.POLITICA.LEER','Consultar políticas de autenticación multifactor'),
('MFA.POLITICA.MODIFICAR','Gestionar políticas de autenticación multifactor');

INSERT INTO rol_seguridad_permiso(rol_seguridad_id, permiso_id)
SELECT r.id,p.id FROM rol_seguridad r JOIN permiso p
WHERE r.codigo='ADMIN_PLATAFORMA';

INSERT INTO rol_seguridad_permiso(rol_seguridad_id, permiso_id)
SELECT r.id,p.id FROM rol_seguridad r JOIN permiso p
WHERE r.codigo='ADMIN_ORGANIZACION' AND p.codigo IN (
'ENTIDAD.LEER','ENTIDAD.CREAR','ENTIDAD.MODIFICAR','ENTIDAD.ELIMINAR',
'DATOS_PERSONALES.LEER','DATOS_PERSONALES.MODIFICAR',
'CALENDARIO.LEER','CALENDARIO.CREAR','CALENDARIO.MODIFICAR','CALENDARIO.ELIMINAR',
'EVENTO.LEER','EVENTO.CREAR','EVENTO.MODIFICAR','EVENTO.ELIMINAR',
'DOCUMENTO.LEER','DOCUMENTO.CREAR','DOCUMENTO.MODIFICAR','DOCUMENTO.ELIMINAR',
'SOLICITUD.LEER','SOLICITUD.CREAR','SOLICITUD.MODIFICAR','SOLICITUD.CANCELAR',
'CUENTA.LEER','CUENTA.CREAR','CUENTA.MODIFICAR','CUENTA.ELIMINAR',
'CONTEXTO.LEER','CONTEXTO.CREAR','CONTEXTO.MODIFICAR','CONTEXTO.ELIMINAR',
'MFA.POLITICA.LEER','MFA.POLITICA.MODIFICAR');

INSERT INTO rol_seguridad_permiso(rol_seguridad_id, permiso_id)
SELECT r.id,p.id FROM rol_seguridad r JOIN permiso p
WHERE r.codigo='GESTOR_CONTEXTO' AND p.codigo IN (
'ENTIDAD.LEER','CALENDARIO.LEER','EVENTO.LEER','EVENTO.CREAR','EVENTO.MODIFICAR',
'SOLICITUD.LEER','SOLICITUD.CREAR','SOLICITUD.MODIFICAR');

INSERT INTO rol_seguridad_permiso(rol_seguridad_id, permiso_id)
SELECT r.id,p.id FROM rol_seguridad r JOIN permiso p
WHERE r.codigo='AUTOSERVICIO' AND p.codigo IN (
'ENTIDAD.LEER','CALENDARIO.LEER','EVENTO.LEER','DOCUMENTO.LEER',
'SOLICITUD.LEER','SOLICITUD.CREAR','SOLICITUD.CANCELAR');

INSERT INTO rol_seguridad_permiso(rol_seguridad_id, permiso_id)
SELECT r.id,p.id FROM rol_seguridad r JOIN permiso p
WHERE r.codigo='PERSONAL_ORGANIZACION' AND p.codigo IN (
'ENTIDAD.LEER','DATOS_PERSONALES.LEER','CALENDARIO.LEER','EVENTO.LEER','EVENTO.CREAR','EVENTO.MODIFICAR',
'DOCUMENTO.LEER','DOCUMENTO.CREAR','SOLICITUD.LEER','SOLICITUD.CREAR','SOLICITUD.MODIFICAR');

INSERT INTO rol_seguridad_permiso(rol_seguridad_id, permiso_id)
SELECT r.id,p.id FROM rol_seguridad r JOIN permiso p
WHERE r.codigo='GESTOR_INFORMACION_SENSIBLE' AND p.codigo IN (
'ENTIDAD.LEER','DATOS_PERSONALES.LEER','CALENDARIO.LEER','EVENTO.LEER','EVENTO.CREAR','EVENTO.MODIFICAR',
'DOCUMENTO.LEER','DOCUMENTO.CREAR','DOCUMENTO.MODIFICAR','DOCUMENTO.ELIMINAR',
'INFORMACION_SENSIBLE.LEER','INFORMACION_SENSIBLE.MODIFICAR',
'SOLICITUD.LEER','SOLICITUD.CREAR','SOLICITUD.MODIFICAR');

INSERT INTO clasificacion_dato(codigo,nombre,nivel_sensibilidad) VALUES
('NORMAL','Normal',0),
('PERSONAL','Datos personales',10),
('SENSIBLE','Información sensible',20);

INSERT INTO categoria_sensible(codigo,nombre,descripcion,activo) VALUES
('SALUD','Salud','Información sanitaria o clínica',TRUE),
('FINANCIERA','Económica / financiera','Información económica, bancaria, salarial o financiera',TRUE),
('JURIDICA','Legal / jurídica','Información jurídica, judicial o contractual sensible',TRUE),
('SEGURIDAD','Seguridad','Información de seguridad, policial, operativa o de acceso restringido',TRUE),
('DISCIPLINARIA','Disciplinaria','Información disciplinaria o de incidencias sensibles',TRUE),
('OTRA','Otra','Otra información sensible no incluida en las categorías anteriores',TRUE);

INSERT INTO tipo_documento(
    codigo,nombre,clasificacion_predeterminada_id,categoria_sensible_predeterminada_id,maximo_accesos,activo
)
SELECT 'IDENTIDAD','Identidad',dc.id,NULL,NULL,TRUE
FROM clasificacion_dato dc WHERE dc.codigo='PERSONAL';

INSERT INTO tipo_documento(
    codigo,nombre,clasificacion_predeterminada_id,categoria_sensible_predeterminada_id,maximo_accesos,activo
)
SELECT 'LICENCIA','Licencia',dc.id,NULL,NULL,TRUE
FROM clasificacion_dato dc WHERE dc.codigo='PERSONAL';

INSERT INTO tipo_documento(
    codigo,nombre,clasificacion_predeterminada_id,categoria_sensible_predeterminada_id,maximo_accesos,activo
)
SELECT 'CONTRATO','Contrato',dc.id,NULL,NULL,TRUE
FROM clasificacion_dato dc WHERE dc.codigo='PERSONAL';

INSERT INTO tipo_documento(
    codigo,nombre,clasificacion_predeterminada_id,categoria_sensible_predeterminada_id,maximo_accesos,activo
)
SELECT 'FACTURA','Factura',dc.id,cs.id,NULL,TRUE
FROM clasificacion_dato dc JOIN categoria_sensible cs
WHERE dc.codigo='SENSIBLE' AND cs.codigo='FINANCIERA';

INSERT INTO tipo_documento(
    codigo,nombre,clasificacion_predeterminada_id,categoria_sensible_predeterminada_id,maximo_accesos,activo
)
SELECT 'INFORME_MEDICO','Informe médico',dc.id,cs.id,NULL,TRUE
FROM clasificacion_dato dc JOIN categoria_sensible cs
WHERE dc.codigo='SENSIBLE' AND cs.codigo='SALUD';

INSERT INTO tipo_documento(
    codigo,nombre,clasificacion_predeterminada_id,categoria_sensible_predeterminada_id,maximo_accesos,activo
)
SELECT 'ACCESO_UNICO','Documento de acceso único',dc.id,cs.id,1,TRUE
FROM clasificacion_dato dc JOIN categoria_sensible cs
WHERE dc.codigo='SENSIBLE' AND cs.codigo='OTRA';

INSERT INTO tipo_solicitud(codigo,nombre,puede_generar_evento,puede_generar_documento) VALUES
('REUNION','Solicitud de reunión',TRUE,FALSE),
('CITA','Solicitud de cita',TRUE,FALSE),
('SOLICITUD_DOCUMENTO','Solicitud de documento',FALSE,TRUE),
('CONSULTA_GENERAL','Consulta general',FALSE,FALSE);



-- ============================================================
-- DATOS DEMO - CLUB
-- ============================================================


-- Organización / grupos / instalación
INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'CB Tres Cantos' FROM tipo_entidad WHERE codigo='ORGANIZACION';
SET @club = LAST_INSERT_ID();
INSERT INTO organizacion(entidad_id,razon_social,identificador_fiscal,tipo_organizacion)
VALUES (@club,'Club Baloncesto Tres Cantos','G28000001','CLUB_DEPORTIVO');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Infantil A' FROM tipo_entidad WHERE codigo='GRUPO';
SET @infantil = LAST_INSERT_ID();

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Junior A' FROM tipo_entidad WHERE codigo='GRUPO';
SET @junior = LAST_INSERT_ID();

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Polideportivo Municipal' FROM tipo_entidad WHERE codigo='INSTALACION';
SET @poli = LAST_INSERT_ID();
INSERT INTO instalacion(entidad_id,tipo_instalacion,capacidad) VALUES (@poli,'PABELLON_DEPORTIVO',800);

INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @club,@infantil,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @club,@junior,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @club,@poli,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PROPIEDAD';

-- Personas
INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Carlos Martín' FROM tipo_entidad WHERE codigo='PERSONA';
SET @carlos = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@carlos,'Carlos','Martín','1982-04-12');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'María Martín' FROM tipo_entidad WHERE codigo='PERSONA';
SET @maria = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@maria,'María','Martín','1984-09-21');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Diego Martín' FROM tipo_entidad WHERE codigo='PERSONA';
SET @diego = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@diego,'Diego','Martín','2013-02-15');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Laura Sánchez' FROM tipo_entidad WHERE codigo='PERSONA';
SET @laura = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@laura,'Laura','Sánchez','2013-07-10');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Pablo Gómez' FROM tipo_entidad WHERE codigo='PERSONA';
SET @pablo = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@pablo,'Pablo','Gómez','2012-11-02');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Sergio Romero' FROM tipo_entidad WHERE codigo='PERSONA';
SET @sergio = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@sergio,'Sergio','Romero','1986-01-09');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Álvaro Pérez' FROM tipo_entidad WHERE codigo='PERSONA';
SET @alvaro = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@alvaro,'Álvaro','Pérez','2009-03-18');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Daniel López' FROM tipo_entidad WHERE codigo='PERSONA';
SET @daniel = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@daniel,'Daniel','López','2009-08-05');

-- Contactos / identificadores
INSERT INTO identificador_entidad(entidad_id,tipo_identificador,valor_identificador,principal)
VALUES (@carlos,'DNI','12345678Z',TRUE),
(@maria,'DNI','23456789D',TRUE),
(@diego,'NIE','Y1234567X',TRUE);

INSERT INTO contacto_entidad(entidad_id,tipo_contacto,valor_contacto,preferido,activo)
VALUES (@carlos,'CORREO','carlos.martin@example.test',TRUE,TRUE),
(@carlos,'CORREO','carlos.trabajo@example.test',FALSE,TRUE),
(@carlos,'TELEFONO','600111222',TRUE,TRUE),
(@maria,'CORREO','maria.martin@example.test',TRUE,TRUE),
(@maria,'TELEFONO','600333444',TRUE,TRUE);

-- Familia
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @carlos,@diego,id,'2013-02-15' FROM tipo_relacion WHERE codigo='PADRE_HIJO';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @maria,@diego,id,'2013-02-15' FROM tipo_relacion WHERE codigo='PADRE_HIJO';

-- Miembros y roles
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @infantil,@carlos,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @infantil,@diego,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @infantil,@laura,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @infantil,@pablo,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';

INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @junior,@sergio,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @junior,@alvaro,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @junior,@daniel,id,'2026-09-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';

INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @carlos,@infantil,id,'2026-09-01' FROM rol_funcional WHERE codigo='ENTRENADOR';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @diego,@infantil,id,'2026-09-01' FROM rol_funcional WHERE codigo='JUGADOR';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @laura,@infantil,id,'2026-09-01' FROM rol_funcional WHERE codigo='JUGADOR';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @pablo,@infantil,id,'2026-09-01' FROM rol_funcional WHERE codigo='JUGADOR';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @sergio,@junior,id,'2026-09-01' FROM rol_funcional WHERE codigo='ENTRENADOR';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @alvaro,@junior,id,'2026-09-01' FROM rol_funcional WHERE codigo='JUGADOR';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @daniel,@junior,id,'2026-09-01' FROM rol_funcional WHERE codigo='JUGADOR';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @carlos,@club,id,'2026-09-01' FROM rol_funcional WHERE codigo='PADRE_MADRE';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @maria,@club,id,'2026-09-01' FROM rol_funcional WHERE codigo='PADRE_MADRE';

-- Cuentas y contextos (hashes de prueba, NO contraseñas reales)
INSERT INTO cuenta_usuario(persona_entidad_id,nombre_usuario,correo_acceso,hash_contrasena,preferido) VALUES
(@carlos,'carlos','carlos.martin@example.test','{noop}demo',TRUE),
(@carlos,'carlos.secundaria','carlos.trabajo@example.test','{noop}demo',FALSE),
(@maria,'maria','maria.martin@example.test','{noop}demo',TRUE),
(@sergio,'sergio','sergio.romero@example.test','{noop}demo',TRUE);

SET @acc_carlos = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='carlos');
SET @acc_maria = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='maria');
SET @acc_sergio = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='sergio');

INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_carlos,@infantil,id,'CONTEXTO',CURRENT_DATE FROM rol_seguridad WHERE codigo='GESTOR_CONTEXTO';
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_carlos,@club,id,'RELACIONADOS',CURRENT_DATE FROM rol_seguridad WHERE codigo='AUTOSERVICIO';
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_maria,@club,id,'RELACIONADOS',CURRENT_DATE FROM rol_seguridad WHERE codigo='AUTOSERVICIO';
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_sergio,@junior,id,'CONTEXTO',CURRENT_DATE FROM rol_seguridad WHERE codigo='GESTOR_CONTEXTO';

-- Calendarios y eventos
INSERT INTO calendario(entidad_propietaria_id,nombre,descripcion,color_predeterminado)
VALUES (@club,'Club - General','Calendario general del club','#1F4E79'),
(@infantil,'Infantil A','Calendario del equipo Infantil A','#3366CC'),
(@junior,'Junior A','Calendario del equipo Junior A','#7F6000'),
(@carlos,'Carlos Martín','Calendario personal','#666666');

SET @cal_club = (SELECT id FROM calendario WHERE entidad_propietaria_id=@club ORDER BY id LIMIT 1);
SET @cal_inf = (SELECT id FROM calendario WHERE entidad_propietaria_id=@infantil ORDER BY id LIMIT 1);
SET @cal_jun = (SELECT id FROM calendario WHERE entidad_propietaria_id=@junior ORDER BY id LIMIT 1);

INSERT INTO evento_calendario(calendario_id,titulo,descripcion,fecha_inicio,fecha_fin,zona_horaria,entidad_ubicacion_id,estado,creado_por_cuenta_id)
VALUES
(@cal_inf,'Entrenamiento Infantil A','Entrenamiento de pista','2026-10-05 18:00:00','2026-10-05 19:30:00','Europe/Madrid',@poli,'PROGRAMADO',@acc_carlos),
(@cal_inf,'Partido Infantil A','Partido de liga','2026-10-10 12:00:00','2026-10-10 13:30:00','Europe/Madrid',@poli,'PROGRAMADO',@acc_carlos);

SET @evt1 = (SELECT id FROM evento_calendario WHERE calendario_id=@cal_inf AND titulo='Entrenamiento Infantil A' ORDER BY id LIMIT 1);
INSERT INTO recurrencia_evento(evento_id,frecuencia,intervalo,por_dia_semana,hasta_fecha,zona_horaria)
VALUES (@evt1,'SEMANAL',1,'MO,WE','2026-12-20 23:59:59','Europe/Madrid');
INSERT INTO participante_evento(evento_id,entidad_id,rol_participante,estado) VALUES
(@evt1,@carlos,'ENTRENADOR','ACEPTADO'),
(@evt1,@diego,'JUGADOR','ACEPTADO'),
(@evt1,@laura,'JUGADOR','ACEPTADO'),
(@evt1,@pablo,'JUGADOR','ACEPTADO');

-- Documentos
INSERT INTO documento(
    tipo_documento_id,entidad_emisora_id,entidad_destinataria_id,clasificacion_id,categoria_sensible_id,
    nombre,referencia_archivo,fecha_documento,estado,creado_por_cuenta_id,fecha_disponibilidad
)
SELECT dt.id,@club,@diego,dt.clasificacion_predeterminada_id,dt.categoria_sensible_predeterminada_id,
       'Licencia federativa 2026/27','demo://club/diego/licencia-2026-27.pdf','2026-09-01','DISPONIBLE',@acc_carlos,'2026-09-01'
FROM tipo_documento dt
WHERE dt.codigo='LICENCIA';
SET @doc_licencia_diego = LAST_INSERT_ID();

-- Carlos ya ha accedido a esta licencia. A partir de aquí no puede modificarse ni eliminarse.
INSERT INTO acceso_documento(documento_id,cuenta_usuario_id,fecha_acceso)
VALUES (@doc_licencia_diego,@acc_carlos,'2026-09-02 09:15:00');



-- ============================================================
-- DATOS DEMO - RESIDENCIA
-- ============================================================


INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Residencia Los Olivos' FROM tipo_entidad WHERE codigo='ORGANIZACION';
SET @residencia = LAST_INSERT_ID();
INSERT INTO organizacion(entidad_id,razon_social,identificador_fiscal,tipo_organizacion)
VALUES (@residencia,'Residencia Los Olivos S.L.','B28000002','RESIDENCIA');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Planta 1' FROM tipo_entidad WHERE codigo='GRUPO';
SET @planta1 = LAST_INSERT_ID();

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Planta 2' FROM tipo_entidad WHERE codigo='GRUPO';
SET @planta2 = LAST_INSERT_ID();

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Servicio médico' FROM tipo_entidad WHERE codigo='GRUPO';
SET @medical = LAST_INSERT_ID();

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Administración' FROM tipo_entidad WHERE codigo='GRUPO';
SET @admin = LAST_INSERT_ID();

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Consulta médica' FROM tipo_entidad WHERE codigo='INSTALACION';
SET @consulta = LAST_INSERT_ID();
INSERT INTO instalacion(entidad_id,tipo_instalacion,capacidad) VALUES (@consulta,'CONSULTA',4);

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Sala de actividades' FROM tipo_entidad WHERE codigo='INSTALACION';
SET @sala = LAST_INSERT_ID();
INSERT INTO instalacion(entidad_id,tipo_instalacion,capacidad) VALUES (@sala,'SALA_ACTIVIDADES',30);

INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @residencia,@planta1,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @residencia,@planta2,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @residencia,@medical,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @residencia,@admin,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';

-- Instalaciones pertenecientes a la residencia.
-- Se relacionan explícitamente para que queden dentro del árbol de contexto
-- y puedan utilizarse como ubicaciones/calendarios sin romper el aislamiento.
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @residencia,@consulta,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PROPIEDAD';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @residencia,@sala,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PROPIEDAD';

-- Personas
INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Ana López' FROM tipo_entidad WHERE codigo='PERSONA';
SET @ana = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@ana,'Ana','López','1944-06-03');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Carmen Martín' FROM tipo_entidad WHERE codigo='PERSONA';
SET @carmen = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@carmen,'Carmen','Martín','1939-10-14');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'José García' FROM tipo_entidad WHERE codigo='PERSONA';
SET @jose = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@jose,'José','García','1941-03-29');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Marta García' FROM tipo_entidad WHERE codigo='PERSONA';
SET @marta = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@marta,'Marta','García','1989-05-04');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Pedro Ruiz' FROM tipo_entidad WHERE codigo='PERSONA';
SET @pedro = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@pedro,'Pedro','Ruiz','1978-12-11');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Elena Martín' FROM tipo_entidad WHERE codigo='PERSONA';
SET @elena = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@elena,'Elena','Martín','1990-01-18');

INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Lucía López' FROM tipo_entidad WHERE codigo='PERSONA';
SET @lucia = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@lucia,'Lucía','López','1972-08-07');

-- Contactos: una entidad puede tener varios del mismo tipo; uno activo puede ser preferido
INSERT INTO contacto_entidad(entidad_id,tipo_contacto,valor_contacto,etiqueta,preferido,activo) VALUES
(@ana,'CORREO','ana.lopez@example.test','Personal',TRUE,TRUE),
(@ana,'TELEFONO','610100100','Móvil',TRUE,TRUE),
(@ana,'TELEFONO','910100100','Habitación',FALSE,TRUE),
(@lucia,'CORREO','lucia.lopez@example.test','Personal',TRUE,TRUE),
(@lucia,'CORREO','lucia.trabajo@example.test','Trabajo',FALSE,TRUE),
(@marta,'CORREO','marta.garcia@example.test','Trabajo',TRUE,TRUE);

-- Residente/familiar
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @lucia,@ana,id,'2000-01-01' FROM tipo_relacion WHERE codigo='PADRE_HIJO';

-- Pertenencias
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @planta1,@ana,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @planta1,@carmen,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @planta2,@jose,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @medical,@marta,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @medical,@pedro,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde)
SELECT @admin,@elena,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';

INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @ana,@residencia,id,'2026-01-01' FROM rol_funcional WHERE codigo='RESIDENTE';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @carmen,@residencia,id,'2026-01-01' FROM rol_funcional WHERE codigo='RESIDENTE';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @jose,@residencia,id,'2026-01-01' FROM rol_funcional WHERE codigo='RESIDENTE';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @marta,@residencia,id,'2026-01-01' FROM rol_funcional WHERE codigo='ENFERMERIA';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @pedro,@residencia,id,'2026-01-01' FROM rol_funcional WHERE codigo='MEDICO';
INSERT INTO rol_funcional_entidad_contexto(entidad_id,entidad_contexto_id,rol_funcional_id,valido_desde)
SELECT @elena,@residencia,id,'2026-01-01' FROM rol_funcional WHERE codigo='ADMINISTRACION';

-- Cuentas
INSERT INTO cuenta_usuario(persona_entidad_id,nombre_usuario,correo_acceso,hash_contrasena,preferido) VALUES
(@marta,'marta','marta.garcia@example.test','{noop}demo',TRUE),
(@pedro,'pedro','pedro.ruiz@example.test','{noop}demo',TRUE),
(@elena,'elena','elena.martin@example.test','{noop}demo',TRUE),
(@lucia,'lucia','lucia.lopez@example.test','{noop}demo',TRUE);

SET @acc_marta = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='marta');
SET @acc_pedro = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='pedro');
SET @acc_elena = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='elena');
SET @acc_lucia = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='lucia');

-- Cuenta técnica/demo de administración de organización para pruebas E2E.
-- Usa una cuenta separada para no mezclar el rol funcional de Elena con su cuenta habitual.
INSERT INTO cuenta_usuario(persona_entidad_id,nombre_usuario,correo_acceso,hash_contrasena,preferido)
VALUES (@elena,'admin.residencia','admin.residencia@example.test','{noop}demo',FALSE);
SET @acc_admin_residencia = LAST_INSERT_ID();

INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_admin_residencia,@residencia,id,'CONTEXTO',CURRENT_DATE
FROM rol_seguridad WHERE codigo='ADMIN_ORGANIZACION';

INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_marta,@residencia,id,'CONTEXTO',CURRENT_DATE FROM rol_seguridad WHERE codigo='GESTOR_INFORMACION_SENSIBLE';
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_pedro,@residencia,id,'CONTEXTO',CURRENT_DATE FROM rol_seguridad WHERE codigo='GESTOR_INFORMACION_SENSIBLE';
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_elena,@residencia,id,'CONTEXTO',CURRENT_DATE FROM rol_seguridad WHERE codigo='PERSONAL_ORGANIZACION';
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_lucia,@residencia,id,'RELACIONADOS',CURRENT_DATE FROM rol_seguridad WHERE codigo='AUTOSERVICIO';

-- Cuenta dedicada a pruebas y demostración del alcance PERSONALIZADO.
-- No se insertan reglas base: por defecto solo puede ver su propia persona y el contexto.
INSERT INTO cuenta_usuario(persona_entidad_id,nombre_usuario,correo_acceso,hash_contrasena,preferido)
VALUES (@elena,'reglas.demo','reglas.demo@example.test','{noop}demo',FALSE);
SET @acc_reglas_demo = LAST_INSERT_ID();
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde)
SELECT @acc_reglas_demo,@residencia,id,'PERSONALIZADO',CURRENT_DATE
FROM rol_seguridad WHERE codigo='PERSONAL_ORGANIZACION';

-- Calendarios
INSERT INTO calendario(entidad_propietaria_id,nombre,descripcion,color_predeterminado)
VALUES (@residencia,'Residencia - General','Calendario general','#548235'),
(@medical,'Servicio médico','Agenda sanitaria','#C00000'),
(@sala,'Sala de actividades','Actividades comunes','#7030A0');

SET @cal_res = (SELECT id FROM calendario WHERE entidad_propietaria_id=@residencia ORDER BY id LIMIT 1);
SET @cal_med = (SELECT id FROM calendario WHERE entidad_propietaria_id=@medical ORDER BY id LIMIT 1);
SET @cal_sala = (SELECT id FROM calendario WHERE entidad_propietaria_id=@sala ORDER BY id LIMIT 1);

INSERT INTO evento_calendario(calendario_id,titulo,descripcion,fecha_inicio,fecha_fin,entidad_ubicacion_id,estado)
VALUES
(@cal_sala,'Taller de memoria','Actividad grupal','2026-10-05 11:00:00','2026-10-05 12:00:00',@sala,'PROGRAMADO'),
(@cal_res,'Fisioterapia grupal','Sesión de movilidad','2026-10-06 10:00:00','2026-10-06 11:00:00',@sala,'PROGRAMADO');

-- Documentos de Ana
INSERT INTO documento(
    tipo_documento_id,entidad_emisora_id,entidad_destinataria_id,clasificacion_id,categoria_sensible_id,
    nombre,referencia_archivo,fecha_documento,estado,creado_por_cuenta_id,fecha_disponibilidad
)
SELECT dt.id,@residencia,@ana,dt.clasificacion_predeterminada_id,dt.categoria_sensible_predeterminada_id,
       'Contrato de residencia','demo://residencia/ana/contrato.pdf','2026-01-01','DISPONIBLE',@acc_elena,'2026-01-01'
FROM tipo_documento dt WHERE dt.codigo='CONTRATO';

INSERT INTO documento(
    tipo_documento_id,entidad_emisora_id,entidad_destinataria_id,clasificacion_id,categoria_sensible_id,
    nombre,referencia_archivo,fecha_documento,estado,creado_por_cuenta_id,fecha_disponibilidad
)
SELECT dt.id,@residencia,@ana,dt.clasificacion_predeterminada_id,dt.categoria_sensible_predeterminada_id,
       'Factura septiembre 2026','demo://residencia/ana/factura-2026-09.pdf','2026-09-30','DISPONIBLE',@acc_elena,'2026-09-30'
FROM tipo_documento dt WHERE dt.codigo='FACTURA';

INSERT INTO documento(
    tipo_documento_id,entidad_emisora_id,entidad_destinataria_id,clasificacion_id,categoria_sensible_id,
    nombre,referencia_archivo,fecha_documento,estado,creado_por_cuenta_id,fecha_disponibilidad
)
SELECT dt.id,@medical,@ana,dt.clasificacion_predeterminada_id,dt.categoria_sensible_predeterminada_id,
       'Informe médico septiembre 2026','demo://residencia/ana/informe-medico-2026-09.pdf','2026-09-28','DISPONIBLE',@acc_pedro,'2026-09-28'
FROM tipo_documento dt WHERE dt.codigo='INFORME_MEDICO';


-- ============================================================
-- DATOS DEMO - WORKFLOWS
-- ============================================================


-- IDs necesarios
SET @club = (SELECT e.id FROM entidad e JOIN organizacion o ON o.entidad_id=e.id WHERE o.identificador_fiscal='G28000001');
SET @infantil = (SELECT id FROM entidad WHERE nombre='Infantil A' ORDER BY id LIMIT 1);
SET @poli = (SELECT id FROM entidad WHERE nombre='Polideportivo Municipal' ORDER BY id LIMIT 1);
SET @carlos = (SELECT id FROM entidad WHERE nombre='Carlos Martín' ORDER BY id LIMIT 1);
SET @maria = (SELECT id FROM entidad WHERE nombre='María Martín' ORDER BY id LIMIT 1);
SET @diego = (SELECT id FROM entidad WHERE nombre='Diego Martín' ORDER BY id LIMIT 1);

SET @residencia = (SELECT e.id FROM entidad e JOIN organizacion o ON o.entidad_id=e.id WHERE o.identificador_fiscal='B28000002');
SET @ana = (SELECT id FROM entidad WHERE nombre='Ana López' ORDER BY id LIMIT 1);
SET @lucia = (SELECT id FROM entidad WHERE nombre='Lucía López' ORDER BY id LIMIT 1);
SET @medical = (SELECT id FROM entidad WHERE nombre='Servicio médico' ORDER BY id LIMIT 1);
SET @admin = (SELECT id FROM entidad WHERE nombre='Administración' ORDER BY id LIMIT 1);
SET @consulta = (SELECT id FROM entidad WHERE nombre='Consulta médica' ORDER BY id LIMIT 1);

-- Solicitud 1: reunión en club, ya resuelta y con evento
INSERT INTO solicitud(tipo_solicitud_id,asunto,descripcion,creado_por_entidad_id,fecha_solicitud,estado_global,fecha_resolucion)
SELECT id,'Reunión con entrenador','Revisión de evolución deportiva de Diego',@maria,'2026-10-01','RESUELTA','2026-10-02'
FROM tipo_solicitud WHERE codigo='REUNION';
SET @req_club = LAST_INSERT_ID();

INSERT INTO participante_solicitud(solicitud_id,entidad_id,rol_participante,estado_participante,fecha_lectura,fecha_respuesta) VALUES
(@req_club,@maria,'REQUESTER','COMPLETADA','2026-10-01','2026-10-02'),
(@req_club,@carlos,'RECIPIENT','COMPLETADA','2026-10-01','2026-10-01');

INSERT INTO mensaje_solicitud(solicitud_id,entidad_remitente_id,mensaje,fecha_mensaje,fecha_creacion)
VALUES
(@req_club,@maria,'Querría comentar la evolución de Diego.','2026-10-01','2026-10-01 17:00:00'),
(@req_club,@carlos,'Podemos vernos el martes a las 19:00.','2026-10-01','2026-10-01 18:00:00'),
(@req_club,@maria,'Perfecto, gracias.','2026-10-02','2026-10-02 10:30:00');

SET @cal_inf = (SELECT id FROM calendario WHERE entidad_propietaria_id=@infantil ORDER BY id LIMIT 1);
INSERT INTO evento_calendario(calendario_id,titulo,descripcion,fecha_inicio,fecha_fin,entidad_ubicacion_id,estado)
VALUES (@cal_inf,'Reunión familia Diego Martín','Generado desde solicitud','2026-10-06 19:00:00','2026-10-06 19:30:00',@poli,'PROGRAMADO');
SET @evt_meeting = LAST_INSERT_ID();

UPDATE solicitud SET evento_generado_id=@evt_meeting WHERE id=@req_club;

INSERT INTO accion_solicitud(solicitud_id,entidad_actor_id,tipo_accion,fecha_creacion,datos) VALUES
(@req_club,@maria,'CREATED','2026-10-01 17:00:00',JSON_OBJECT('requesterStatus','EN_ESPERA','recipientStatus','PENDIENTE')),
(@req_club,@carlos,'PROPOSED_TIME','2026-10-01 18:00:00',JSON_OBJECT('proposedAt','2026-10-06T19:00:00')),
(@req_club,@maria,'ACEPTADO','2026-10-02 10:30:00',JSON_OBJECT('eventId',@evt_meeting));

-- Solicitud 2: factura en residencia, abierta
INSERT INTO solicitud(tipo_solicitud_id,asunto,descripcion,creado_por_entidad_id,fecha_solicitud,estado_global)
SELECT id,'Factura de septiembre','Solicito copia de la factura de septiembre de Ana López',@lucia,'2026-10-01','ABIERTA'
FROM tipo_solicitud WHERE codigo='SOLICITUD_DOCUMENTO';
SET @req_invoice = LAST_INSERT_ID();

INSERT INTO participante_solicitud(solicitud_id,entidad_id,rol_participante,estado_participante) VALUES
(@req_invoice,@lucia,'REQUESTER','EN_ESPERA'),
(@req_invoice,@admin,'RECIPIENT','PENDIENTE');

INSERT INTO accion_solicitud(solicitud_id,entidad_actor_id,tipo_accion,datos)
VALUES (@req_invoice,@lucia,'CREATED',JSON_OBJECT('requesterStatus','EN_ESPERA','recipientStatus','PENDIENTE'));

-- Solicitud 3: cita médica, pendiente del servicio médico
INSERT INTO solicitud(tipo_solicitud_id,asunto,descripcion,creado_por_entidad_id,fecha_solicitud,estado_global)
SELECT id,'Consulta médica','Solicito revisión por molestias persistentes',@ana,'2026-10-03','ABIERTA'
FROM tipo_solicitud WHERE codigo='CITA';
SET @req_med = LAST_INSERT_ID();

INSERT INTO participante_solicitud(solicitud_id,entidad_id,rol_participante,estado_participante) VALUES
(@req_med,@ana,'REQUESTER','EN_ESPERA'),
(@req_med,@medical,'RECIPIENT','PENDIENTE');

INSERT INTO accion_solicitud(solicitud_id,entidad_actor_id,tipo_accion,datos)
VALUES (@req_med,@ana,'CREATED',JSON_OBJECT('requesterStatus','EN_ESPERA','recipientStatus','PENDIENTE'));

-- Preferencias de calendario de ejemplo
SET @acc_carlos = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='carlos');
INSERT INTO preferencia_calendario(calendario_id,cuenta_id,visible,notificaciones_habilitadas)
SELECT id,@acc_carlos,TRUE,TRUE FROM calendario WHERE entidad_propietaria_id=@infantil LIMIT 1;

SET @acc_marta = (SELECT id FROM cuenta_usuario WHERE nombre_usuario='marta');
INSERT INTO preferencia_calendario(calendario_id,cuenta_id,visible,notificaciones_habilitadas)
SELECT id,@acc_marta,TRUE,TRUE FROM calendario WHERE entidad_propietaria_id=@medical LIMIT 1;



-- ============================================================
-- DEMO MFA AISLADA (no afecta a las baterías existentes)
-- ============================================================
INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Residencia MFA Demo' FROM tipo_entidad WHERE codigo='ORGANIZACION';
SET @residencia_mfa = LAST_INSERT_ID();
INSERT INTO organizacion(entidad_id,razon_social,identificador_fiscal,tipo_organizacion) VALUES (@residencia_mfa,'Residencia MFA Demo','B28999991','RESIDENCIA');
INSERT INTO entidad(tipo_entidad_id,nombre) SELECT id,'Usuario MFA Demo' FROM tipo_entidad WHERE codigo='PERSONA';
SET @persona_mfa = LAST_INSERT_ID();
INSERT INTO persona(entidad_id,nombre,primer_apellido,fecha_nacimiento) VALUES (@persona_mfa,'Usuario','MFA Demo','1980-01-01');
INSERT INTO contacto_entidad(entidad_id,tipo_contacto,valor_contacto,etiqueta,preferido,activo) VALUES
(@persona_mfa,'CORREO','mfa.demo@example.test','MFA',TRUE,TRUE),(@persona_mfa,'TELEFONO','600999888','Móvil',TRUE,TRUE);
INSERT INTO relacion_entidad(entidad_origen_id,entidad_destino_id,tipo_relacion_id,valido_desde) SELECT @residencia_mfa,@persona_mfa,id,'2026-01-01' FROM tipo_relacion WHERE codigo='PERTENENCIA';
INSERT INTO cuenta_usuario(persona_entidad_id,nombre_usuario,correo_acceso,hash_contrasena,preferido) VALUES (@persona_mfa,'mfa.demo','mfa.demo@example.test','{noop}demo',TRUE);
SET @acc_mfa = LAST_INSERT_ID();
INSERT INTO contexto_cuenta(cuenta_id,entidad_contexto_id,rol_seguridad_id,alcance,valido_desde) SELECT @acc_mfa,@residencia_mfa,id,'CONTEXTO','2026-01-01' FROM rol_seguridad WHERE codigo='ADMIN_ORGANIZACION';
INSERT INTO politica_mfa(entidad_contexto_id,minutos_validez,activo) VALUES (@residencia_mfa,480,TRUE);

-- ============================================================
-- CONSULTAS DE COMPROBACIÓN
-- ============================================================


-- 1) Entidades del club con roles contextuales
SELECT e.nombre AS persona, c.nombre AS contexto, er.codigo AS rol
FROM rol_funcional_entidad_contexto ecr
JOIN entidad e ON e.id=ecr.entidad_id
JOIN entidad c ON c.id=ecr.entidad_contexto_id
JOIN rol_funcional er ON er.id=ecr.rol_funcional_id
WHERE c.nombre IN ('CB Tres Cantos','Infantil A','Junior A')
ORDER BY c.nombre, er.codigo, e.nombre;

-- 2) Residentes y personal
SELECT e.nombre AS persona, c.nombre AS contexto, er.codigo AS rol
FROM rol_funcional_entidad_contexto ecr
JOIN entidad e ON e.id=ecr.entidad_id
JOIN entidad c ON c.id=ecr.entidad_contexto_id
JOIN rol_funcional er ON er.id=ecr.rol_funcional_id
WHERE c.nombre='Residencia Los Olivos'
ORDER BY er.codigo,e.nombre;

-- 3) Relaciones familiares en ambos dominios
SELECT s.nombre AS origen, rt.nombre_directo, t.nombre AS destino
FROM relacion_entidad r
JOIN entidad s ON s.id=r.entidad_origen_id
JOIN entidad t ON t.id=r.entidad_destino_id
JOIN tipo_relacion rt ON rt.id=r.tipo_relacion_id
WHERE rt.codigo IN ('PADRE_HIJO','TUTOR_TUTELADO')
ORDER BY s.nombre,t.nombre;

-- 4) Calendario Infantil A
SELECT ce.id, ce.titulo, ce.fecha_inicio, ce.fecha_fin, loc.nombre AS lugar
FROM evento_calendario ce
JOIN calendario c ON c.id=ce.calendario_id
LEFT JOIN entidad loc ON loc.id=ce.entidad_ubicacion_id
WHERE c.entidad_propietaria_id=(SELECT id FROM entidad WHERE nombre='Infantil A' LIMIT 1)
ORDER BY ce.fecha_inicio;

-- 5) Solicitudes y estado de cada participante
SELECT r.id,r.asunto,r.estado_global,e.nombre,rp.rol_participante,rp.estado_participante
FROM solicitud r
JOIN participante_solicitud rp ON rp.solicitud_id=r.id
JOIN entidad e ON e.id=rp.entidad_id
ORDER BY r.id,rp.rol_participante;

-- 6) Documentos de Ana y clasificación
SELECT d.nombre, em.nombre AS emisor, des.nombre AS destinatario,
       dt.codigo AS tipo, dc.codigo AS clasificacion, cs.codigo AS categoria_sensible,
       d.estado, d.sustituye_a_documento_id
FROM documento d
JOIN entidad em ON em.id=d.entidad_emisora_id
JOIN entidad des ON des.id=d.entidad_destinataria_id
JOIN tipo_documento dt ON dt.id=d.tipo_documento_id
JOIN clasificacion_dato dc ON dc.id=d.clasificacion_id
LEFT JOIN categoria_sensible cs ON cs.id=d.categoria_sensible_id
WHERE des.nombre='Ana López'
ORDER BY dc.nivel_sensibilidad,d.fecha_documento;



-- ============================================================
-- COMPROBACIÓN FINAL
-- ============================================================

SELECT 'Carga completada correctamente' AS resultado;

SELECT COUNT(*) AS total_entity_types FROM tipo_entidad;
SELECT COUNT(*) AS total_entidades FROM entidad;
SELECT COUNT(*) AS total_personas FROM persona;
SELECT COUNT(*) AS total_organizaciones FROM organizacion;
SELECT COUNT(*) AS total_instalaciones FROM instalacion;
SELECT COUNT(*) AS total_contactos FROM contacto_entidad;
SELECT COUNT(*) AS total_relaciones FROM relacion_entidad;
SELECT COUNT(*) AS total_roles_contextuales FROM rol_funcional_entidad_contexto;
SELECT COUNT(*) AS total_cuentas FROM cuenta_usuario;
SELECT COUNT(*) AS total_contextos_cuenta FROM contexto_cuenta;
SELECT COUNT(*) AS total_reglas_alcance FROM regla_alcance_acceso;
SELECT COUNT(*) AS total_politicas_mfa FROM politica_mfa;
SELECT COUNT(*) AS total_desafios_mfa FROM desafio_mfa;
SELECT COUNT(*) AS total_calendarios FROM calendario;
SELECT COUNT(*) AS total_eventos FROM evento_calendario;
SELECT COUNT(*) AS total_solicitudes FROM solicitud;
SELECT COUNT(*) AS total_documentos FROM documento;
SELECT COUNT(*) AS total_accesos_documento FROM acceso_documento;

SELECT
    e.nombre AS entidad,
    ec.tipo_contacto,
    ec.valor_contacto,
    ec.etiqueta,
    ec.preferido,
    ec.activo
FROM contacto_entidad ec
JOIN entidad e ON e.id = ec.entidad_id
ORDER BY e.nombre, ec.tipo_contacto, ec.preferido DESC, ec.id;

SELECT
    e.nombre AS persona,
    ua.nombre_usuario,
    ua.correo_acceso,
    ua.proveedor_autenticacion,
    ua.preferido,
    ua.estado
FROM cuenta_usuario ua
LEFT JOIN entidad e ON e.id = ua.persona_entidad_id
ORDER BY e.nombre, ua.preferido DESC, ua.id;
