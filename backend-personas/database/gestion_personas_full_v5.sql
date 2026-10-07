-- ============================================================
-- GESTIÓN DE PERSONAS - SCRIPT COMPLETO MYSQL 8.0.36
-- CORE genérico + demo Club + demo Residencia
-- Versión V4 - recurrencia de calendario
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
-- entity_contact:
--   una entidad puede tener múltiples EMAIL, PHONE, etc.
--   is_active indica si el contacto está operativo.
--   is_preferred indica cuál es el preferido.
--
-- user_account:
--   una PERSON puede tener múltiples cuentas.
--   status controla ACTIVE / LOCKED / DISABLED.
--   is_preferred indica la cuenta principal.
--
-- La regla "solo un preferido activo por tipo/persona" se aplicará
-- transaccionalmente en el API Spring Boot. No se fuerza mediante
-- columnas generadas.


-- ============================================================
-- ESQUEMA
-- ============================================================


SET NAMES utf8mb4;

CREATE TABLE entity_type (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entity_type_code (code)
) ENGINE=InnoDB;

CREATE TABLE entity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entity_type_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_entity_type (entity_type_id),
    KEY idx_entity_status (status),
    CONSTRAINT fk_entity_type FOREIGN KEY (entity_type_id) REFERENCES entity_type(id),
    CONSTRAINT chk_entity_status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED'))
) ENGINE=InnoDB;

CREATE TABLE person (
    entity_id BIGINT UNSIGNED NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(150) NOT NULL,
    second_last_name VARCHAR(150) NULL,
    birth_date DATE NULL,
    gender VARCHAR(30) NULL,
    PRIMARY KEY (entity_id),
    CONSTRAINT fk_person_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE organization (
    entity_id BIGINT UNSIGNED NOT NULL,
    legal_name VARCHAR(200) NULL,
    tax_id VARCHAR(50) NULL,
    organization_kind VARCHAR(50) NULL,
    PRIMARY KEY (entity_id),
    UNIQUE KEY uk_organization_tax_id (tax_id),
    CONSTRAINT fk_organization_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE facility (
    entity_id BIGINT UNSIGNED NOT NULL,
    facility_kind VARCHAR(50) NULL,
    capacity INT NULL,
    PRIMARY KEY (entity_id),
    CONSTRAINT fk_facility_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE entity_identifier (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entity_id BIGINT UNSIGNED NOT NULL,
    identifier_type VARCHAR(50) NOT NULL,
    identifier_value VARCHAR(150) NOT NULL,
    issuer VARCHAR(150) NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from DATE NULL,
    valid_to DATE NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_identifier_type_value (identifier_type, identifier_value),
    KEY idx_identifier_entity (entity_id),
    CONSTRAINT fk_identifier_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE entity_contact (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entity_id BIGINT UNSIGNED NOT NULL,
    contact_type VARCHAR(50) NOT NULL,
    contact_value VARCHAR(255) NOT NULL,
    label VARCHAR(100) NULL,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    verified_at DATETIME(6) NULL,
    valid_from DATETIME(6) NULL,
    valid_to DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_contact_value_per_entity (entity_id, contact_type, contact_value),
    KEY idx_contact_entity (entity_id),
    KEY idx_contact_entity_type_active (entity_id, contact_type, is_active),
    CONSTRAINT fk_contact_entity
        FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT chk_contact_dates
        CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)
) ENGINE=InnoDB;

CREATE TABLE entity_address (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entity_id BIGINT UNSIGNED NOT NULL,
    address_type VARCHAR(50) NOT NULL DEFAULT 'MAIN',
    street VARCHAR(200) NOT NULL,
    postal_code VARCHAR(20) NULL,
    city VARCHAR(100) NOT NULL,
    province VARCHAR(100) NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'ES',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_address_entity (entity_id),
    CONSTRAINT fk_address_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE relation_type (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(60) NOT NULL,
    forward_name VARCHAR(100) NOT NULL,
    reverse_name VARCHAR(100) NOT NULL,
    is_symmetric BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_relation_type_code (code)
) ENGINE=InnoDB;

CREATE TABLE entity_relation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_entity_id BIGINT UNSIGNED NOT NULL,
    target_entity_id BIGINT UNSIGNED NOT NULL,
    relation_type_id BIGINT UNSIGNED NOT NULL,
    valid_from DATE NULL,
    valid_to DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_entity_relation_active (source_entity_id, target_entity_id, relation_type_id, valid_from),
    KEY idx_relation_source (source_entity_id),
    KEY idx_relation_target (target_entity_id),
    KEY idx_relation_type (relation_type_id),
    CONSTRAINT fk_relation_source FOREIGN KEY (source_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_relation_target FOREIGN KEY (target_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_relation_type FOREIGN KEY (relation_type_id) REFERENCES relation_type(id),
    CONSTRAINT chk_relation_status CHECK (status IN ('ACTIVE','INACTIVE','ENDED'))
) ENGINE=InnoDB;

CREATE TABLE entity_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_entity_role_code (code)
) ENGINE=InnoDB;

CREATE TABLE entity_context_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    entity_id BIGINT UNSIGNED NOT NULL,
    context_entity_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    valid_from DATE NULL,
    valid_to DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_entity_context_role (entity_id, context_entity_id, role_id, valid_from),
    KEY idx_ecr_entity (entity_id),
    KEY idx_ecr_context (context_entity_id),
    CONSTRAINT fk_ecr_entity FOREIGN KEY (entity_id) REFERENCES entity(id),
    CONSTRAINT fk_ecr_context FOREIGN KEY (context_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_ecr_role FOREIGN KEY (role_id) REFERENCES entity_role(id),
    CONSTRAINT chk_ecr_status CHECK (status IN ('ACTIVE','INACTIVE','ENDED'))
) ENGINE=InnoDB;

CREATE TABLE user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    person_entity_id BIGINT UNSIGNED NULL,
    username VARCHAR(150) NOT NULL,
    login_email VARCHAR(255) NULL,
    password_hash VARCHAR(255) NOT NULL,
    auth_provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    external_subject VARCHAR(255) NULL,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_provider_subject (auth_provider, external_subject),
    KEY idx_user_person (person_entity_id),
    KEY idx_user_login_email (login_email),
    KEY idx_user_person_status_preferred (person_entity_id, status, is_preferred),
    CONSTRAINT fk_user_person
        FOREIGN KEY (person_entity_id) REFERENCES entity(id),
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE','LOCKED','DISABLED'))
) ENGINE=InnoDB;


CREATE TABLE auth_session (
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

CREATE TABLE role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB;

CREATE TABLE permission (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(120) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB;

CREATE TABLE role_permission (
    role_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE account_context (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    context_entity_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    scope VARCHAR(255) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATETIME(6) NULL,
    valid_to DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_context (account_id, context_entity_id, role_id),
    KEY idx_account_context_context (context_entity_id),
    CONSTRAINT fk_ac_account FOREIGN KEY (account_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_ac_context FOREIGN KEY (context_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_ac_role FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB;

CREATE TABLE access_permission_override (
    account_context_id BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    effect VARCHAR(10) NOT NULL,
    PRIMARY KEY (account_context_id, permission_id),
    CONSTRAINT fk_apo_ac FOREIGN KEY (account_context_id) REFERENCES account_context(id) ON DELETE CASCADE,
    CONSTRAINT fk_apo_permission FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE,
    CONSTRAINT chk_apo_effect CHECK (effect IN ('ALLOW','DENY'))
) ENGINE=InnoDB;

CREATE TABLE calendar (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_entity_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(500) NULL,
    default_color VARCHAR(20) NULL,
    timezone VARCHAR(80) NOT NULL DEFAULT 'Europe/Madrid',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    KEY idx_calendar_owner (owner_entity_id),
    CONSTRAINT fk_calendar_owner FOREIGN KEY (owner_entity_id) REFERENCES entity(id)
) ENGINE=InnoDB;

CREATE TABLE calendar_preference (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    calendar_id BIGINT UNSIGNED NOT NULL,
    entity_id BIGINT UNSIGNED NULL,
    account_id BIGINT UNSIGNED NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    color_override VARCHAR(20) NULL,
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_calendar_pref_entity (calendar_id, entity_id),
    UNIQUE KEY uk_calendar_pref_account (calendar_id, account_id),
    CONSTRAINT fk_cp_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT fk_cp_account FOREIGN KEY (account_id) REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT chk_cp_owner CHECK (
        (entity_id IS NOT NULL AND account_id IS NULL) OR
        (entity_id IS NULL AND account_id IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE calendar_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    calendar_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(220) NOT NULL,
    description TEXT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NULL,
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    timezone VARCHAR(80) NULL,
    location_entity_id BIGINT UNSIGNED NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_by_account_id BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_event_calendar_start (calendar_id, starts_at),
    KEY idx_event_location (location_entity_id),
    KEY idx_event_created_by (created_by_account_id),
    CONSTRAINT fk_event_calendar FOREIGN KEY (calendar_id) REFERENCES calendar(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_location FOREIGN KEY (location_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_event_created_by FOREIGN KEY (created_by_account_id) REFERENCES user_account(id),
    CONSTRAINT chk_event_status CHECK (status IN ('SCHEDULED','CANCELLED','COMPLETED'))
) ENGINE=InnoDB;

-- Regla de recurrencia asociada a un evento maestro.
-- El evento maestro contiene título, horario base, ubicación, etc.
CREATE TABLE event_recurrence (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    event_id BIGINT UNSIGNED NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    interval_value INT NOT NULL DEFAULT 1,
    by_day VARCHAR(80) NULL COMMENT 'Lista RFC5545 simplificada: MO,TU,WE,TH,FR,SA,SU',
    by_month_day VARCHAR(120) NULL COMMENT 'Días del mes separados por coma, por ejemplo 1,15',
    until_at DATETIME(6) NULL,
    occurrence_count INT NULL,
    timezone VARCHAR(80) NOT NULL DEFAULT 'Europe/Madrid',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recurrence_event (event_id),
    CONSTRAINT fk_recurrence_event FOREIGN KEY (event_id) REFERENCES calendar_event(id) ON DELETE CASCADE,
    CONSTRAINT chk_recurrence_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),
    CONSTRAINT chk_recurrence_interval CHECK (interval_value >= 1),
    CONSTRAINT chk_recurrence_count CHECK (occurrence_count IS NULL OR occurrence_count >= 1),
    CONSTRAINT chk_recurrence_end CHECK (NOT (until_at IS NOT NULL AND occurrence_count IS NOT NULL))
) ENGINE=InnoDB;

-- Excepciones de una ocurrencia concreta de una serie.
-- MODIFIED apunta a un calendar_event de sustitución; CANCELLED omite la ocurrencia.
CREATE TABLE event_recurrence_exception (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    recurrence_id BIGINT UNSIGNED NOT NULL,
    original_starts_at DATETIME(6) NOT NULL,
    exception_type VARCHAR(20) NOT NULL,
    replacement_event_id BIGINT UNSIGNED NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recurrence_exception (recurrence_id, original_starts_at),
    KEY idx_recurrence_exception_replacement (replacement_event_id),
    CONSTRAINT fk_recurrence_exception_series FOREIGN KEY (recurrence_id) REFERENCES event_recurrence(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurrence_exception_replacement FOREIGN KEY (replacement_event_id) REFERENCES calendar_event(id) ON DELETE SET NULL,
    CONSTRAINT chk_recurrence_exception_type CHECK (exception_type IN ('MODIFIED','CANCELLED')),
    CONSTRAINT chk_recurrence_exception_replacement CHECK (
        (exception_type='MODIFIED' AND replacement_event_id IS NOT NULL)
        OR (exception_type='CANCELLED' AND replacement_event_id IS NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE event_participant (
    event_id BIGINT UNSIGNED NOT NULL,
    entity_id BIGINT UNSIGNED NOT NULL,
    participant_role VARCHAR(60) NOT NULL DEFAULT 'ATTENDEE',
    status VARCHAR(30) NOT NULL DEFAULT 'INVITED',
    PRIMARY KEY (event_id, entity_id, participant_role),
    CONSTRAINT fk_ep_event FOREIGN KEY (event_id) REFERENCES calendar_event(id) ON DELETE CASCADE,
    CONSTRAINT fk_ep_entity FOREIGN KEY (entity_id) REFERENCES entity(id) ON DELETE CASCADE,
    CONSTRAINT chk_ep_status CHECK (status IN ('INVITED','ACCEPTED','DECLINED','TENTATIVE','ATTENDED','ABSENT'))
) ENGINE=InnoDB;

CREATE TABLE data_classification (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    sensitivity_rank INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_classification_code (code)
) ENGINE=InnoDB;

CREATE TABLE document_type (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_type_code (code)
) ENGINE=InnoDB;

CREATE TABLE document (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_entity_id BIGINT UNSIGNED NOT NULL,
    document_type_id BIGINT UNSIGNED NOT NULL,
    classification_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(220) NOT NULL,
    description VARCHAR(500) NULL,
    file_reference VARCHAR(500) NOT NULL,
    document_date DATE NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT UNSIGNED NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    KEY idx_document_owner (owner_entity_id),
    KEY idx_document_classification (classification_id),
    CONSTRAINT fk_document_owner FOREIGN KEY (owner_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_document_type FOREIGN KEY (document_type_id) REFERENCES document_type(id),
    CONSTRAINT fk_document_class FOREIGN KEY (classification_id) REFERENCES data_classification(id),
    CONSTRAINT fk_document_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    CONSTRAINT chk_document_status CHECK (status IN ('ACTIVE','ARCHIVED','DELETED'))
) ENGINE=InnoDB;

CREATE TABLE request_type (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    can_generate_event BOOLEAN NOT NULL DEFAULT FALSE,
    can_generate_document BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_type_code (code)
) ENGINE=InnoDB;

CREATE TABLE request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_type_id BIGINT UNSIGNED NOT NULL,
    subject VARCHAR(220) NOT NULL,
    description TEXT NULL,
    created_by_entity_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    global_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolved_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL,
    parent_request_id BIGINT UNSIGNED NULL,
    generated_event_id BIGINT UNSIGNED NULL,
    generated_document_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id),
    KEY idx_request_creator (created_by_entity_id),
    KEY idx_request_status (global_status),
    CONSTRAINT fk_request_type FOREIGN KEY (request_type_id) REFERENCES request_type(id),
    CONSTRAINT fk_request_creator FOREIGN KEY (created_by_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_request_parent FOREIGN KEY (parent_request_id) REFERENCES request(id),
    CONSTRAINT fk_request_event FOREIGN KEY (generated_event_id) REFERENCES calendar_event(id),
    CONSTRAINT fk_request_document FOREIGN KEY (generated_document_id) REFERENCES document(id),
    CONSTRAINT chk_request_status CHECK (global_status IN ('OPEN','RESOLVED','CANCELLED','CLOSED'))
) ENGINE=InnoDB;

CREATE TABLE request_participant (
    request_id BIGINT UNSIGNED NOT NULL,
    entity_id BIGINT UNSIGNED NOT NULL,
    role VARCHAR(50) NOT NULL,
    participant_status VARCHAR(30) NOT NULL,
    read_at DATETIME(6) NULL,
    responded_at DATETIME(6) NULL,
    PRIMARY KEY (request_id, entity_id, role),
    CONSTRAINT fk_rp_request FOREIGN KEY (request_id) REFERENCES request(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_entity FOREIGN KEY (entity_id) REFERENCES entity(id),
    CONSTRAINT chk_rp_status CHECK (participant_status IN ('WAITING','PENDING','DONE','CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE request_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    sender_entity_id BIGINT UNSIGNED NOT NULL,
    message TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_request_message_request (request_id, created_at),
    CONSTRAINT fk_rm_request FOREIGN KEY (request_id) REFERENCES request(id) ON DELETE CASCADE,
    CONSTRAINT fk_rm_sender FOREIGN KEY (sender_entity_id) REFERENCES entity(id)
) ENGINE=InnoDB;

CREATE TABLE request_action (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id BIGINT UNSIGNED NOT NULL,
    actor_entity_id BIGINT UNSIGNED NULL,
    action_type VARCHAR(60) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    data JSON NULL,
    PRIMARY KEY (id),
    KEY idx_request_action_request (request_id, created_at),
    CONSTRAINT fk_ra_request FOREIGN KEY (request_id) REFERENCES request(id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_actor FOREIGN KEY (actor_entity_id) REFERENCES entity(id)
) ENGINE=InnoDB;

CREATE TABLE access_audit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NULL,
    context_entity_id BIGINT UNSIGNED NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id BIGINT UNSIGNED NULL,
    target_entity_id BIGINT UNSIGNED NULL,
    action VARCHAR(60) NOT NULL,
    classification_code VARCHAR(50) NULL,
    accessed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_access_audit_account (account_id, accessed_at),
    KEY idx_access_audit_target (target_entity_id, accessed_at),
    CONSTRAINT fk_access_audit_account FOREIGN KEY (account_id) REFERENCES user_account(id),
    CONSTRAINT fk_access_audit_context FOREIGN KEY (context_entity_id) REFERENCES entity(id),
    CONSTRAINT fk_access_audit_target FOREIGN KEY (target_entity_id) REFERENCES entity(id)
) ENGINE=InnoDB;

CREATE TABLE change_audit (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NULL,
    context_entity_id BIGINT UNSIGNED NULL,
    resource_type VARCHAR(60) NOT NULL,
    resource_id BIGINT UNSIGNED NULL,
    action VARCHAR(60) NOT NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_change_audit_account (account_id, changed_at),
    CONSTRAINT fk_change_audit_account FOREIGN KEY (account_id) REFERENCES user_account(id),
    CONSTRAINT fk_change_audit_context FOREIGN KEY (context_entity_id) REFERENCES entity(id)
) ENGINE=InnoDB;



-- ============================================================
-- CATÁLOGOS Y PERMISOS
-- ============================================================


INSERT INTO entity_type(code,name) VALUES
('PERSON','Persona'),
('ORGANIZATION','Organización'),
('FACILITY','Instalación'),
('GROUP','Grupo o unidad organizativa');

INSERT INTO relation_type(code,forward_name,reverse_name,is_symmetric) VALUES
('PARENT_CHILD','PARENT_OF','CHILD_OF',FALSE),
('GUARDIAN_CHILD','GUARDIAN_OF','UNDER_GUARDIANSHIP_OF',FALSE),
('MEMBERSHIP','HAS_MEMBER','MEMBER_OF',FALSE),
('OWNERSHIP','OWNS','OWNED_BY',FALSE),
('MANAGEMENT','MANAGES','MANAGED_BY',FALSE),
('SIBLING','SIBLING_OF','SIBLING_OF',TRUE);

INSERT INTO entity_role(code,name) VALUES
('PLAYER','Jugador'),
('COACH','Entrenador'),
('PARENT','Padre/Madre'),
('GUARDIAN','Tutor'),
('MEMBER','Miembro'),
('RESIDENT','Residente'),
('NURSE','Enfermería'),
('DOCTOR','Médico'),
('ADMINISTRATION','Administración'),
('DIRECTOR','Dirección');

INSERT INTO role(code,name) VALUES
('PLATFORM_ADMIN','Administrador plataforma'),
('ORG_ADMIN','Administrador organización'),
('TEAM_MANAGER','Gestor de equipo'),
('SELF_SERVICE','Autoservicio'),
('RESIDENCE_STAFF','Personal residencia'),
('HEALTH_STAFF','Personal sanitario');

INSERT INTO permission(code,description) VALUES
('ENTITY.READ','Consultar entidades'),
('ENTITY.CREATE','Crear entidades'),
('ENTITY.UPDATE','Modificar entidades'),
('ENTITY.DELETE','Eliminar entidades'),
('PERSONAL_DATA.READ','Consultar datos personales de terceros'),
('PERSONAL_DATA.UPDATE','Modificar datos personales'),
('CALENDAR.READ','Consultar calendarios'),
('CALENDAR.CREATE','Crear calendarios'),
('CALENDAR.UPDATE','Modificar calendarios'),
('CALENDAR.DELETE','Eliminar calendarios'),
('EVENT.READ','Consultar eventos'),
('EVENT.CREATE','Crear eventos'),
('EVENT.UPDATE','Modificar eventos'),
('EVENT.DELETE','Eliminar eventos'),
('DOCUMENT.READ','Consultar documentos normales'),
('DOCUMENT.CREATE','Crear documentos'),
('DOCUMENT.UPDATE','Modificar documentos'),
('DOCUMENT.DELETE','Eliminar documentos'),
('HEALTH_DATA.READ','Consultar datos/documentos sanitarios'),
('HEALTH_DATA.UPDATE','Modificar datos/documentos sanitarios'),
('REQUEST.READ','Consultar solicitudes'),
('REQUEST.CREATE','Crear solicitudes'),
('REQUEST.UPDATE','Gestionar solicitudes'),
('REQUEST.CANCEL','Cancelar solicitudes'),
('ACCOUNT.READ','Consultar cuentas de usuario'),
('ACCOUNT.CREATE','Crear cuentas de usuario'),
('ACCOUNT.UPDATE','Modificar cuentas de usuario'),
('ACCOUNT.DELETE','Deshabilitar cuentas de usuario'),
('CONTEXT.READ','Consultar contextos de acceso'),
('CONTEXT.CREATE','Asignar contextos y roles'),
('CONTEXT.UPDATE','Modificar contextos y roles'),
('CONTEXT.DELETE','Revocar contextos y roles');

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p
WHERE r.code='PLATFORM_ADMIN';

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p
WHERE r.code='ORG_ADMIN' AND p.code IN (
'ENTITY.READ','ENTITY.CREATE','ENTITY.UPDATE',
'PERSONAL_DATA.READ','CALENDAR.READ','CALENDAR.CREATE','CALENDAR.UPDATE',
'EVENT.READ','EVENT.CREATE','EVENT.UPDATE',
'DOCUMENT.READ','DOCUMENT.CREATE','DOCUMENT.UPDATE',
'REQUEST.READ','REQUEST.CREATE','REQUEST.UPDATE','REQUEST.CANCEL',
'ACCOUNT.READ','ACCOUNT.CREATE','ACCOUNT.UPDATE','ACCOUNT.DELETE',
'CONTEXT.READ','CONTEXT.CREATE','CONTEXT.UPDATE','CONTEXT.DELETE');

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p
WHERE r.code='TEAM_MANAGER' AND p.code IN (
'ENTITY.READ','CALENDAR.READ','EVENT.READ','EVENT.CREATE','EVENT.UPDATE',
'REQUEST.READ','REQUEST.CREATE','REQUEST.UPDATE');

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p
WHERE r.code='SELF_SERVICE' AND p.code IN (
'ENTITY.READ','CALENDAR.READ','EVENT.READ','DOCUMENT.READ',
'REQUEST.READ','REQUEST.CREATE','REQUEST.CANCEL');

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p
WHERE r.code='RESIDENCE_STAFF' AND p.code IN (
'ENTITY.READ','PERSONAL_DATA.READ','CALENDAR.READ','EVENT.READ','EVENT.CREATE','EVENT.UPDATE',
'DOCUMENT.READ','DOCUMENT.CREATE','REQUEST.READ','REQUEST.CREATE','REQUEST.UPDATE');

INSERT INTO role_permission(role_id, permission_id)
SELECT r.id,p.id FROM role r JOIN permission p
WHERE r.code='HEALTH_STAFF' AND p.code IN (
'ENTITY.READ','PERSONAL_DATA.READ','CALENDAR.READ','EVENT.READ','EVENT.CREATE','EVENT.UPDATE',
'DOCUMENT.READ','DOCUMENT.CREATE','HEALTH_DATA.READ','HEALTH_DATA.UPDATE',
'REQUEST.READ','REQUEST.CREATE','REQUEST.UPDATE');

INSERT INTO data_classification(code,name,sensitivity_rank) VALUES
('NORMAL','Normal',0),
('PERSONAL','Datos personales',10),
('SENSITIVE','Sensible',20),
('HEALTH','Salud',30);

INSERT INTO document_type(code,name) VALUES
('IDENTITY','Identidad'),
('LICENSE','Licencia'),
('CONTRACT','Contrato'),
('INVOICE','Factura'),
('MEDICAL_REPORT','Informe médico');

INSERT INTO request_type(code,name,can_generate_event,can_generate_document) VALUES
('MEETING','Solicitud de reunión',TRUE,FALSE),
('APPOINTMENT','Solicitud de cita',TRUE,FALSE),
('DOCUMENT_REQUEST','Solicitud de documento',FALSE,TRUE),
('GENERAL_QUERY','Consulta general',FALSE,FALSE);



-- ============================================================
-- DATOS DEMO - CLUB
-- ============================================================


-- Organización / grupos / instalación
INSERT INTO entity(entity_type_id,name) SELECT id,'CB Tres Cantos' FROM entity_type WHERE code='ORGANIZATION';
SET @club = LAST_INSERT_ID();
INSERT INTO organization(entity_id,legal_name,tax_id,organization_kind)
VALUES (@club,'Club Baloncesto Tres Cantos','G28000001','SPORTS_CLUB');

INSERT INTO entity(entity_type_id,name) SELECT id,'Infantil A' FROM entity_type WHERE code='GROUP';
SET @infantil = LAST_INSERT_ID();

INSERT INTO entity(entity_type_id,name) SELECT id,'Junior A' FROM entity_type WHERE code='GROUP';
SET @junior = LAST_INSERT_ID();

INSERT INTO entity(entity_type_id,name) SELECT id,'Polideportivo Municipal' FROM entity_type WHERE code='FACILITY';
SET @poli = LAST_INSERT_ID();
INSERT INTO facility(entity_id,facility_kind,capacity) VALUES (@poli,'SPORTS_HALL',800);

INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @club,@infantil,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @club,@junior,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @club,@poli,id,'2026-09-01' FROM relation_type WHERE code='OWNERSHIP';

-- Personas
INSERT INTO entity(entity_type_id,name) SELECT id,'Carlos Martín' FROM entity_type WHERE code='PERSON';
SET @carlos = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@carlos,'Carlos','Martín','1982-04-12');

INSERT INTO entity(entity_type_id,name) SELECT id,'María Martín' FROM entity_type WHERE code='PERSON';
SET @maria = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@maria,'María','Martín','1984-09-21');

INSERT INTO entity(entity_type_id,name) SELECT id,'Diego Martín' FROM entity_type WHERE code='PERSON';
SET @diego = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@diego,'Diego','Martín','2013-02-15');

INSERT INTO entity(entity_type_id,name) SELECT id,'Laura Sánchez' FROM entity_type WHERE code='PERSON';
SET @laura = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@laura,'Laura','Sánchez','2013-07-10');

INSERT INTO entity(entity_type_id,name) SELECT id,'Pablo Gómez' FROM entity_type WHERE code='PERSON';
SET @pablo = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@pablo,'Pablo','Gómez','2012-11-02');

INSERT INTO entity(entity_type_id,name) SELECT id,'Sergio Romero' FROM entity_type WHERE code='PERSON';
SET @sergio = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@sergio,'Sergio','Romero','1986-01-09');

INSERT INTO entity(entity_type_id,name) SELECT id,'Álvaro Pérez' FROM entity_type WHERE code='PERSON';
SET @alvaro = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@alvaro,'Álvaro','Pérez','2009-03-18');

INSERT INTO entity(entity_type_id,name) SELECT id,'Daniel López' FROM entity_type WHERE code='PERSON';
SET @daniel = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@daniel,'Daniel','López','2009-08-05');

-- Contactos / identificadores
INSERT INTO entity_identifier(entity_id,identifier_type,identifier_value,is_primary)
VALUES (@carlos,'DNI','12345678Z',TRUE),
(@maria,'DNI','23456789D',TRUE),
(@diego,'NIE','Y1234567X',TRUE);

INSERT INTO entity_contact(entity_id,contact_type,contact_value,is_preferred,is_active)
VALUES (@carlos,'EMAIL','carlos.martin@example.test',TRUE,TRUE),
(@carlos,'EMAIL','carlos.trabajo@example.test',FALSE,TRUE),
(@carlos,'PHONE','600111222',TRUE,TRUE),
(@maria,'EMAIL','maria.martin@example.test',TRUE,TRUE),
(@maria,'PHONE','600333444',TRUE,TRUE);

-- Familia
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @carlos,@diego,id,'2013-02-15' FROM relation_type WHERE code='PARENT_CHILD';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @maria,@diego,id,'2013-02-15' FROM relation_type WHERE code='PARENT_CHILD';

-- Miembros y roles
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @infantil,@carlos,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @infantil,@diego,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @infantil,@laura,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @infantil,@pablo,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';

INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @junior,@sergio,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @junior,@alvaro,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @junior,@daniel,id,'2026-09-01' FROM relation_type WHERE code='MEMBERSHIP';

INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @carlos,@infantil,id,'2026-09-01' FROM entity_role WHERE code='COACH';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @diego,@infantil,id,'2026-09-01' FROM entity_role WHERE code='PLAYER';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @laura,@infantil,id,'2026-09-01' FROM entity_role WHERE code='PLAYER';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @pablo,@infantil,id,'2026-09-01' FROM entity_role WHERE code='PLAYER';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @sergio,@junior,id,'2026-09-01' FROM entity_role WHERE code='COACH';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @alvaro,@junior,id,'2026-09-01' FROM entity_role WHERE code='PLAYER';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @daniel,@junior,id,'2026-09-01' FROM entity_role WHERE code='PLAYER';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @carlos,@club,id,'2026-09-01' FROM entity_role WHERE code='PARENT';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @maria,@club,id,'2026-09-01' FROM entity_role WHERE code='PARENT';

-- Cuentas y contextos (hashes de prueba, NO passwords reales)
INSERT INTO user_account(person_entity_id,username,login_email,password_hash,is_preferred) VALUES
(@carlos,'carlos','carlos.martin@example.test','{noop}demo',TRUE),
(@carlos,'carlos.secundaria','carlos.trabajo@example.test','{noop}demo',FALSE),
(@maria,'maria','maria.martin@example.test','{noop}demo',TRUE),
(@sergio,'sergio','sergio.romero@example.test','{noop}demo',TRUE);

SET @acc_carlos = (SELECT id FROM user_account WHERE username='carlos');
SET @acc_maria = (SELECT id FROM user_account WHERE username='maria');
SET @acc_sergio = (SELECT id FROM user_account WHERE username='sergio');

INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_carlos,@infantil,id,'TEAM',NOW(6) FROM role WHERE code='TEAM_MANAGER';
INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_carlos,@club,id,'SELF_AND_CHILDREN',NOW(6) FROM role WHERE code='SELF_SERVICE';
INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_maria,@club,id,'SELF_AND_CHILDREN',NOW(6) FROM role WHERE code='SELF_SERVICE';
INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_sergio,@junior,id,'TEAM',NOW(6) FROM role WHERE code='TEAM_MANAGER';

-- Calendarios y eventos
INSERT INTO calendar(owner_entity_id,name,description,default_color)
VALUES (@club,'Club - General','Calendario general del club','#1F4E79'),
(@infantil,'Infantil A','Calendario del equipo Infantil A','#3366CC'),
(@junior,'Junior A','Calendario del equipo Junior A','#7F6000'),
(@carlos,'Carlos Martín','Calendario personal','#666666');

SET @cal_club = (SELECT id FROM calendar WHERE owner_entity_id=@club ORDER BY id LIMIT 1);
SET @cal_inf = (SELECT id FROM calendar WHERE owner_entity_id=@infantil ORDER BY id LIMIT 1);
SET @cal_jun = (SELECT id FROM calendar WHERE owner_entity_id=@junior ORDER BY id LIMIT 1);

INSERT INTO calendar_event(calendar_id,title,description,starts_at,ends_at,timezone,location_entity_id,status,created_by_account_id)
VALUES
(@cal_inf,'Entrenamiento Infantil A','Entrenamiento de pista','2026-10-05 18:00:00','2026-10-05 19:30:00','Europe/Madrid',@poli,'SCHEDULED',@acc_carlos),
(@cal_inf,'Partido Infantil A','Partido de liga','2026-10-10 12:00:00','2026-10-10 13:30:00','Europe/Madrid',@poli,'SCHEDULED',@acc_carlos);

SET @evt1 = (SELECT id FROM calendar_event WHERE calendar_id=@cal_inf AND title='Entrenamiento Infantil A' ORDER BY id LIMIT 1);
INSERT INTO event_recurrence(event_id,frequency,interval_value,by_day,until_at,timezone)
VALUES (@evt1,'WEEKLY',1,'MO,WE','2026-12-20 23:59:59','Europe/Madrid');
INSERT INTO event_participant(event_id,entity_id,participant_role,status) VALUES
(@evt1,@carlos,'COACH','ACCEPTED'),
(@evt1,@diego,'PLAYER','ACCEPTED'),
(@evt1,@laura,'PLAYER','ACCEPTED'),
(@evt1,@pablo,'PLAYER','ACCEPTED');

-- Documentos
INSERT INTO document(owner_entity_id,document_type_id,classification_id,name,file_reference,document_date)
SELECT @diego,dt.id,dc.id,'Licencia federativa 2026/27','demo://club/diego/licencia-2026-27.pdf','2026-09-01'
FROM document_type dt JOIN data_classification dc
WHERE dt.code='LICENSE' AND dc.code='PERSONAL';



-- ============================================================
-- DATOS DEMO - RESIDENCIA
-- ============================================================


INSERT INTO entity(entity_type_id,name) SELECT id,'Residencia Los Olivos' FROM entity_type WHERE code='ORGANIZATION';
SET @residencia = LAST_INSERT_ID();
INSERT INTO organization(entity_id,legal_name,tax_id,organization_kind)
VALUES (@residencia,'Residencia Los Olivos S.L.','B28000002','CARE_HOME');

INSERT INTO entity(entity_type_id,name) SELECT id,'Planta 1' FROM entity_type WHERE code='GROUP';
SET @planta1 = LAST_INSERT_ID();

INSERT INTO entity(entity_type_id,name) SELECT id,'Planta 2' FROM entity_type WHERE code='GROUP';
SET @planta2 = LAST_INSERT_ID();

INSERT INTO entity(entity_type_id,name) SELECT id,'Servicio médico' FROM entity_type WHERE code='GROUP';
SET @medical = LAST_INSERT_ID();

INSERT INTO entity(entity_type_id,name) SELECT id,'Administración' FROM entity_type WHERE code='GROUP';
SET @admin = LAST_INSERT_ID();

INSERT INTO entity(entity_type_id,name) SELECT id,'Consulta médica' FROM entity_type WHERE code='FACILITY';
SET @consulta = LAST_INSERT_ID();
INSERT INTO facility(entity_id,facility_kind,capacity) VALUES (@consulta,'MEDICAL_ROOM',4);

INSERT INTO entity(entity_type_id,name) SELECT id,'Sala de actividades' FROM entity_type WHERE code='FACILITY';
SET @sala = LAST_INSERT_ID();
INSERT INTO facility(entity_id,facility_kind,capacity) VALUES (@sala,'ACTIVITY_ROOM',30);

INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @residencia,@planta1,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @residencia,@planta2,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @residencia,@medical,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @residencia,@admin,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';

-- Personas
INSERT INTO entity(entity_type_id,name) SELECT id,'Ana López' FROM entity_type WHERE code='PERSON';
SET @ana = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@ana,'Ana','López','1944-06-03');

INSERT INTO entity(entity_type_id,name) SELECT id,'Carmen Martín' FROM entity_type WHERE code='PERSON';
SET @carmen = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@carmen,'Carmen','Martín','1939-10-14');

INSERT INTO entity(entity_type_id,name) SELECT id,'José García' FROM entity_type WHERE code='PERSON';
SET @jose = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@jose,'José','García','1941-03-29');

INSERT INTO entity(entity_type_id,name) SELECT id,'Marta García' FROM entity_type WHERE code='PERSON';
SET @marta = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@marta,'Marta','García','1989-05-04');

INSERT INTO entity(entity_type_id,name) SELECT id,'Pedro Ruiz' FROM entity_type WHERE code='PERSON';
SET @pedro = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@pedro,'Pedro','Ruiz','1978-12-11');

INSERT INTO entity(entity_type_id,name) SELECT id,'Elena Martín' FROM entity_type WHERE code='PERSON';
SET @elena = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@elena,'Elena','Martín','1990-01-18');

INSERT INTO entity(entity_type_id,name) SELECT id,'Lucía López' FROM entity_type WHERE code='PERSON';
SET @lucia = LAST_INSERT_ID();
INSERT INTO person(entity_id,first_name,last_name,birth_date) VALUES (@lucia,'Lucía','López','1972-08-07');

-- Contactos: una entidad puede tener varios del mismo tipo; uno activo puede ser preferido
INSERT INTO entity_contact(entity_id,contact_type,contact_value,label,is_preferred,is_active) VALUES
(@ana,'EMAIL','ana.lopez@example.test','Personal',TRUE,TRUE),
(@ana,'PHONE','610100100','Móvil',TRUE,TRUE),
(@ana,'PHONE','910100100','Habitación',FALSE,TRUE),
(@lucia,'EMAIL','lucia.lopez@example.test','Personal',TRUE,TRUE),
(@lucia,'EMAIL','lucia.trabajo@example.test','Trabajo',FALSE,TRUE),
(@marta,'EMAIL','marta.garcia@example.test','Trabajo',TRUE,TRUE);

-- Residente/familiar
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @lucia,@ana,id,'2000-01-01' FROM relation_type WHERE code='PARENT_CHILD';

-- Pertenencias
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @planta1,@ana,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @planta1,@carmen,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @planta2,@jose,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @medical,@marta,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @medical,@pedro,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';
INSERT INTO entity_relation(source_entity_id,target_entity_id,relation_type_id,valid_from)
SELECT @admin,@elena,id,'2026-01-01' FROM relation_type WHERE code='MEMBERSHIP';

INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @ana,@residencia,id,'2026-01-01' FROM entity_role WHERE code='RESIDENT';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @carmen,@residencia,id,'2026-01-01' FROM entity_role WHERE code='RESIDENT';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @jose,@residencia,id,'2026-01-01' FROM entity_role WHERE code='RESIDENT';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @marta,@residencia,id,'2026-01-01' FROM entity_role WHERE code='NURSE';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @pedro,@residencia,id,'2026-01-01' FROM entity_role WHERE code='DOCTOR';
INSERT INTO entity_context_role(entity_id,context_entity_id,role_id,valid_from)
SELECT @elena,@residencia,id,'2026-01-01' FROM entity_role WHERE code='ADMINISTRATION';

-- Cuentas
INSERT INTO user_account(person_entity_id,username,login_email,password_hash,is_preferred) VALUES
(@marta,'marta','marta.garcia@example.test','{noop}demo',TRUE),
(@pedro,'pedro','pedro.ruiz@example.test','{noop}demo',TRUE),
(@elena,'elena','elena.martin@example.test','{noop}demo',TRUE),
(@lucia,'lucia','lucia.lopez@example.test','{noop}demo',TRUE);

SET @acc_marta = (SELECT id FROM user_account WHERE username='marta');
SET @acc_pedro = (SELECT id FROM user_account WHERE username='pedro');
SET @acc_elena = (SELECT id FROM user_account WHERE username='elena');
SET @acc_lucia = (SELECT id FROM user_account WHERE username='lucia');

INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_marta,@residencia,id,'RESIDENCE',NOW(6) FROM role WHERE code='HEALTH_STAFF';
INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_pedro,@residencia,id,'RESIDENCE',NOW(6) FROM role WHERE code='HEALTH_STAFF';
INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_elena,@residencia,id,'RESIDENCE',NOW(6) FROM role WHERE code='RESIDENCE_STAFF';
INSERT INTO account_context(account_id,context_entity_id,role_id,scope,valid_from)
SELECT @acc_lucia,@residencia,id,'SELF_AND_RELATED',NOW(6) FROM role WHERE code='SELF_SERVICE';

-- Calendarios
INSERT INTO calendar(owner_entity_id,name,description,default_color)
VALUES (@residencia,'Residencia - General','Calendario general','#548235'),
(@medical,'Servicio médico','Agenda sanitaria','#C00000'),
(@sala,'Sala de actividades','Actividades comunes','#7030A0');

SET @cal_res = (SELECT id FROM calendar WHERE owner_entity_id=@residencia ORDER BY id LIMIT 1);
SET @cal_med = (SELECT id FROM calendar WHERE owner_entity_id=@medical ORDER BY id LIMIT 1);
SET @cal_sala = (SELECT id FROM calendar WHERE owner_entity_id=@sala ORDER BY id LIMIT 1);

INSERT INTO calendar_event(calendar_id,title,description,starts_at,ends_at,location_entity_id,status)
VALUES
(@cal_sala,'Taller de memoria','Actividad grupal','2026-10-05 11:00:00','2026-10-05 12:00:00',@sala,'SCHEDULED'),
(@cal_res,'Fisioterapia grupal','Sesión de movilidad','2026-10-06 10:00:00','2026-10-06 11:00:00',@sala,'SCHEDULED');

-- Documentos de Ana
INSERT INTO document(owner_entity_id,document_type_id,classification_id,name,file_reference,document_date,created_by)
SELECT @ana,dt.id,dc.id,'Contrato de residencia','demo://residencia/ana/contrato.pdf','2026-01-01',@acc_elena
FROM document_type dt JOIN data_classification dc
WHERE dt.code='CONTRACT' AND dc.code='PERSONAL';

INSERT INTO document(owner_entity_id,document_type_id,classification_id,name,file_reference,document_date,created_by)
SELECT @ana,dt.id,dc.id,'Factura septiembre 2026','demo://residencia/ana/factura-2026-09.pdf','2026-09-30',@acc_elena
FROM document_type dt JOIN data_classification dc
WHERE dt.code='INVOICE' AND dc.code='PERSONAL';

INSERT INTO document(owner_entity_id,document_type_id,classification_id,name,file_reference,document_date,created_by)
SELECT @ana,dt.id,dc.id,'Informe médico septiembre 2026','demo://residencia/ana/informe-medico-2026-09.pdf','2026-09-28',@acc_pedro
FROM document_type dt JOIN data_classification dc
WHERE dt.code='MEDICAL_REPORT' AND dc.code='HEALTH';



-- ============================================================
-- DATOS DEMO - WORKFLOWS
-- ============================================================


-- IDs necesarios
SET @club = (SELECT e.id FROM entity e JOIN organization o ON o.entity_id=e.id WHERE o.tax_id='G28000001');
SET @infantil = (SELECT id FROM entity WHERE name='Infantil A' ORDER BY id LIMIT 1);
SET @poli = (SELECT id FROM entity WHERE name='Polideportivo Municipal' ORDER BY id LIMIT 1);
SET @carlos = (SELECT id FROM entity WHERE name='Carlos Martín' ORDER BY id LIMIT 1);
SET @maria = (SELECT id FROM entity WHERE name='María Martín' ORDER BY id LIMIT 1);
SET @diego = (SELECT id FROM entity WHERE name='Diego Martín' ORDER BY id LIMIT 1);

SET @residencia = (SELECT e.id FROM entity e JOIN organization o ON o.entity_id=e.id WHERE o.tax_id='B28000002');
SET @ana = (SELECT id FROM entity WHERE name='Ana López' ORDER BY id LIMIT 1);
SET @lucia = (SELECT id FROM entity WHERE name='Lucía López' ORDER BY id LIMIT 1);
SET @medical = (SELECT id FROM entity WHERE name='Servicio médico' ORDER BY id LIMIT 1);
SET @admin = (SELECT id FROM entity WHERE name='Administración' ORDER BY id LIMIT 1);
SET @consulta = (SELECT id FROM entity WHERE name='Consulta médica' ORDER BY id LIMIT 1);

-- Solicitud 1: reunión en club, ya resuelta y con evento
INSERT INTO request(request_type_id,subject,description,created_by_entity_id,global_status,resolved_at)
SELECT id,'Reunión con entrenador','Revisión de evolución deportiva de Diego',@maria,'RESOLVED','2026-10-02 10:30:00'
FROM request_type WHERE code='MEETING';
SET @req_club = LAST_INSERT_ID();

INSERT INTO request_participant(request_id,entity_id,role,participant_status,read_at,responded_at) VALUES
(@req_club,@maria,'REQUESTER','DONE','2026-10-01 17:00:00','2026-10-02 10:30:00'),
(@req_club,@carlos,'RECIPIENT','DONE','2026-10-01 17:20:00','2026-10-01 18:00:00');

INSERT INTO request_message(request_id,sender_entity_id,message,created_at)
VALUES
(@req_club,@maria,'Querría comentar la evolución de Diego.','2026-10-01 17:00:00'),
(@req_club,@carlos,'Podemos vernos el martes a las 19:00.','2026-10-01 18:00:00'),
(@req_club,@maria,'Perfecto, gracias.','2026-10-02 10:30:00');

SET @cal_inf = (SELECT id FROM calendar WHERE owner_entity_id=@infantil ORDER BY id LIMIT 1);
INSERT INTO calendar_event(calendar_id,title,description,starts_at,ends_at,location_entity_id,status)
VALUES (@cal_inf,'Reunión familia Diego Martín','Generado desde solicitud','2026-10-06 19:00:00','2026-10-06 19:30:00',@poli,'SCHEDULED');
SET @evt_meeting = LAST_INSERT_ID();

UPDATE request SET generated_event_id=@evt_meeting WHERE id=@req_club;

INSERT INTO request_action(request_id,actor_entity_id,action_type,created_at,data) VALUES
(@req_club,@maria,'CREATED','2026-10-01 17:00:00',JSON_OBJECT('requesterStatus','WAITING','recipientStatus','PENDING')),
(@req_club,@carlos,'PROPOSED_TIME','2026-10-01 18:00:00',JSON_OBJECT('proposedAt','2026-10-06T19:00:00')),
(@req_club,@maria,'ACCEPTED','2026-10-02 10:30:00',JSON_OBJECT('eventId',@evt_meeting));

-- Solicitud 2: factura en residencia, abierta
INSERT INTO request(request_type_id,subject,description,created_by_entity_id,global_status)
SELECT id,'Factura de septiembre','Solicito copia de la factura de septiembre de Ana López',@lucia,'OPEN'
FROM request_type WHERE code='DOCUMENT_REQUEST';
SET @req_invoice = LAST_INSERT_ID();

INSERT INTO request_participant(request_id,entity_id,role,participant_status) VALUES
(@req_invoice,@lucia,'REQUESTER','WAITING'),
(@req_invoice,@admin,'RECIPIENT','PENDING');

INSERT INTO request_action(request_id,actor_entity_id,action_type,data)
VALUES (@req_invoice,@lucia,'CREATED',JSON_OBJECT('requesterStatus','WAITING','recipientStatus','PENDING'));

-- Solicitud 3: cita médica, pendiente del servicio médico
INSERT INTO request(request_type_id,subject,description,created_by_entity_id,global_status)
SELECT id,'Consulta médica','Solicito revisión por molestias persistentes',@ana,'OPEN'
FROM request_type WHERE code='APPOINTMENT';
SET @req_med = LAST_INSERT_ID();

INSERT INTO request_participant(request_id,entity_id,role,participant_status) VALUES
(@req_med,@ana,'REQUESTER','WAITING'),
(@req_med,@medical,'RECIPIENT','PENDING');

INSERT INTO request_action(request_id,actor_entity_id,action_type,data)
VALUES (@req_med,@ana,'CREATED',JSON_OBJECT('requesterStatus','WAITING','recipientStatus','PENDING'));

-- Preferencias de calendario de ejemplo
SET @acc_carlos = (SELECT id FROM user_account WHERE username='carlos');
INSERT INTO calendar_preference(calendar_id,account_id,visible,notification_enabled)
SELECT id,@acc_carlos,TRUE,TRUE FROM calendar WHERE owner_entity_id=@infantil LIMIT 1;

SET @acc_marta = (SELECT id FROM user_account WHERE username='marta');
INSERT INTO calendar_preference(calendar_id,account_id,visible,notification_enabled)
SELECT id,@acc_marta,TRUE,TRUE FROM calendar WHERE owner_entity_id=@medical LIMIT 1;



-- ============================================================
-- CONSULTAS DE COMPROBACIÓN
-- ============================================================


-- 1) Entidades del club con roles contextuales
SELECT e.name AS persona, c.name AS contexto, er.code AS rol
FROM entity_context_role ecr
JOIN entity e ON e.id=ecr.entity_id
JOIN entity c ON c.id=ecr.context_entity_id
JOIN entity_role er ON er.id=ecr.role_id
WHERE c.name IN ('CB Tres Cantos','Infantil A','Junior A')
ORDER BY c.name, er.code, e.name;

-- 2) Residentes y personal
SELECT e.name AS persona, c.name AS contexto, er.code AS rol
FROM entity_context_role ecr
JOIN entity e ON e.id=ecr.entity_id
JOIN entity c ON c.id=ecr.context_entity_id
JOIN entity_role er ON er.id=ecr.role_id
WHERE c.name='Residencia Los Olivos'
ORDER BY er.code,e.name;

-- 3) Relaciones familiares en ambos dominios
SELECT s.name AS origen, rt.forward_name, t.name AS destino
FROM entity_relation r
JOIN entity s ON s.id=r.source_entity_id
JOIN entity t ON t.id=r.target_entity_id
JOIN relation_type rt ON rt.id=r.relation_type_id
WHERE rt.code IN ('PARENT_CHILD','GUARDIAN_CHILD')
ORDER BY s.name,t.name;

-- 4) Calendario Infantil A
SELECT ce.id, ce.title, ce.starts_at, ce.ends_at, loc.name AS lugar
FROM calendar_event ce
JOIN calendar c ON c.id=ce.calendar_id
LEFT JOIN entity loc ON loc.id=ce.location_entity_id
WHERE c.owner_entity_id=(SELECT id FROM entity WHERE name='Infantil A' LIMIT 1)
ORDER BY ce.starts_at;

-- 5) Solicitudes y estado de cada participante
SELECT r.id,r.subject,r.global_status,e.name,rp.role,rp.participant_status
FROM request r
JOIN request_participant rp ON rp.request_id=r.id
JOIN entity e ON e.id=rp.entity_id
ORDER BY r.id,rp.role;

-- 6) Documentos de Ana y clasificación
SELECT d.name, dt.code AS tipo, dc.code AS clasificacion, d.file_reference
FROM document d
JOIN entity e ON e.id=d.owner_entity_id
JOIN document_type dt ON dt.id=d.document_type_id
JOIN data_classification dc ON dc.id=d.classification_id
WHERE e.name='Ana López'
ORDER BY dc.sensitivity_rank,d.document_date;



-- ============================================================
-- COMPROBACIÓN FINAL
-- ============================================================

SELECT 'Carga completada correctamente' AS resultado;

SELECT COUNT(*) AS total_entity_types FROM entity_type;
SELECT COUNT(*) AS total_entidades FROM entity;
SELECT COUNT(*) AS total_personas FROM person;
SELECT COUNT(*) AS total_organizaciones FROM organization;
SELECT COUNT(*) AS total_instalaciones FROM facility;
SELECT COUNT(*) AS total_contactos FROM entity_contact;
SELECT COUNT(*) AS total_relaciones FROM entity_relation;
SELECT COUNT(*) AS total_roles_contextuales FROM entity_context_role;
SELECT COUNT(*) AS total_cuentas FROM user_account;
SELECT COUNT(*) AS total_contextos_cuenta FROM account_context;
SELECT COUNT(*) AS total_calendarios FROM calendar;
SELECT COUNT(*) AS total_eventos FROM calendar_event;
SELECT COUNT(*) AS total_solicitudes FROM request;
SELECT COUNT(*) AS total_documentos FROM document;

SELECT
    e.name AS entidad,
    ec.contact_type,
    ec.contact_value,
    ec.label,
    ec.is_preferred,
    ec.is_active
FROM entity_contact ec
JOIN entity e ON e.id = ec.entity_id
ORDER BY e.name, ec.contact_type, ec.is_preferred DESC, ec.id;

SELECT
    e.name AS persona,
    ua.username,
    ua.login_email,
    ua.auth_provider,
    ua.is_preferred,
    ua.status
FROM user_account ua
LEFT JOIN entity e ON e.id = ua.person_entity_id
ORDER BY e.name, ua.is_preferred DESC, ua.id;
