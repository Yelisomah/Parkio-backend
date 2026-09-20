-- ============================================================================
-- V10__phone_verified_registration.sql
-- Generalizes the guest-checkout-only OTP table into a shared one, and adds
-- phone verification to regular account registration (both OTP-gated) —
-- built at explicit request, extending Phase 2's guest-checkout OTP work
-- rather than duplicating it.
-- ============================================================================

ALTER TABLE guest_otps RENAME TO otps;

ALTER TABLE otps ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'GUEST_CHECKOUT';
ALTER TABLE otps ALTER COLUMN purpose DROP DEFAULT;

ALTER TABLE users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
