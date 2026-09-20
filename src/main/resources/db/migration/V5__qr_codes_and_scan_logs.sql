-- ============================================================================
-- V5__qr_codes_and_scan_logs.sql
-- Permanent per-space QR codes (tasks.md 4.5) and staff scan logging with
-- offline reconciliation (tasks.md task 8).
-- ============================================================================

ALTER TABLE bookings ADD COLUMN qr_code VARCHAR(40) UNIQUE;

CREATE TABLE space_qr_codes (
    id         BIGSERIAL PRIMARY KEY,
    space_id   BIGINT NOT NULL UNIQUE REFERENCES parking_lots(id) ON DELETE CASCADE,
    code       VARCHAR(40) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE scan_logs (
    id                    BIGSERIAL PRIMARY KEY,
    booking_id            BIGINT REFERENCES bookings(id) ON DELETE CASCADE,
    raw_qr_code           VARCHAR(40) NOT NULL,
    scanned_by_staff_id   BIGINT NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    action                VARCHAR(20) NOT NULL,
    scan_time             TIMESTAMP NOT NULL,
    latitude              NUMERIC(10,7),
    longitude             NUMERIC(10,7),
    offline_synced        BOOLEAN NOT NULL DEFAULT FALSE,
    reconciliation_status VARCHAR(20) NOT NULL DEFAULT 'OK',
    reconciliation_note   VARCHAR(500),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_scan_logs_booking ON scan_logs(booking_id);
CREATE INDEX idx_scan_logs_staff ON scan_logs(scanned_by_staff_id);
CREATE INDEX idx_scan_logs_reconciliation ON scan_logs(reconciliation_status);
