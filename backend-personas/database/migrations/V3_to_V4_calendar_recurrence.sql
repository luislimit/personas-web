-- ============================================================
-- GESTIÓN DE PERSONAS
-- Migración V3 -> V4: recurrencia de calendario
-- MySQL 8.0.36
-- ============================================================

USE gestion_personas;

ALTER TABLE calendar_event
    ADD COLUMN timezone VARCHAR(80) NULL AFTER all_day,
    ADD COLUMN created_by_account_id BIGINT UNSIGNED NULL AFTER status,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD KEY idx_event_created_by (created_by_account_id),
    ADD CONSTRAINT fk_event_created_by
        FOREIGN KEY (created_by_account_id) REFERENCES user_account(id);

UPDATE calendar_event ce
JOIN calendar c ON c.id = ce.calendar_id
SET ce.timezone = c.timezone
WHERE ce.timezone IS NULL;

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
    CONSTRAINT fk_recurrence_exception_series
        FOREIGN KEY (recurrence_id) REFERENCES event_recurrence(id) ON DELETE CASCADE,
    CONSTRAINT fk_recurrence_exception_replacement
        FOREIGN KEY (replacement_event_id) REFERENCES calendar_event(id) ON DELETE SET NULL,
    CONSTRAINT chk_recurrence_exception_type CHECK (exception_type IN ('MODIFIED','CANCELLED')),
    CONSTRAINT chk_recurrence_exception_replacement CHECK (
        (exception_type='MODIFIED' AND replacement_event_id IS NOT NULL)
        OR (exception_type='CANCELLED' AND replacement_event_id IS NULL)
    )
) ENGINE=InnoDB;
