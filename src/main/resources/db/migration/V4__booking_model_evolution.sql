-- ============================================================================
-- V4__booking_model_evolution.sql
-- Kiro Phase C: booking initiation source, payment timing, warden-created
-- bookings, booking extensions, and per-space management mode.
-- ============================================================================

ALTER TABLE bookings ADD COLUMN initiated_by VARCHAR(20) NOT NULL DEFAULT 'DRIVER_APP';
ALTER TABLE bookings ADD COLUMN payment_timing VARCHAR(20) NOT NULL DEFAULT 'PREPAID';
ALTER TABLE bookings ADD COLUMN created_by_staff_id BIGINT REFERENCES staff(id) ON DELETE SET NULL;
ALTER TABLE bookings ADD COLUMN extended_from_booking_id BIGINT REFERENCES bookings(id) ON DELETE SET NULL;

CREATE INDEX idx_bookings_created_by_staff ON bookings(created_by_staff_id);
CREATE INDEX idx_bookings_extended_from ON bookings(extended_from_booking_id);

ALTER TABLE parking_lots ADD COLUMN management_mode VARCHAR(20) NOT NULL DEFAULT 'BOOKING_ONLY';
