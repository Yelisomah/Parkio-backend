-- ============================================================================
-- V8__guest_otps.sql
-- OTP-at-payment verification for the QR-signage guest checkout flow
-- (Phase 2, tasks.md 13.2).
-- ============================================================================

CREATE TABLE guest_otps (
    id         BIGSERIAL PRIMARY KEY,
    phone      VARCHAR(20) NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_guest_otps_phone ON guest_otps(phone);
