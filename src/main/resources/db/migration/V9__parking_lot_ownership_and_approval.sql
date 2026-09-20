-- ============================================================================
-- V9__parking_lot_ownership_and_approval.sql
-- Closes a gap flagged repeatedly since Phase B: ParkingLot had no owner at
-- all, so every lot was implicitly platform-managed and StaffAssignment
-- could assign staff to spaces their organization didn't actually own.
-- ============================================================================

ALTER TABLE parking_lots ADD COLUMN owner_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE parking_lots ADD COLUMN organization_id BIGINT REFERENCES organizations(id) ON DELETE SET NULL;

-- Defense in depth — the real enforcement is in ParkingLotService (this
-- constraint can't express "both null is fine, exactly one set is fine,
-- both set is not", so it's a backstop, not the primary mechanism). Also:
-- H2 (the test profile) never sees this migration at all — its schema comes
-- from Hibernate directly off the entity, so this constraint only ever
-- applies against real Postgres.
ALTER TABLE parking_lots ADD CONSTRAINT chk_lot_single_owner
    CHECK (NOT (owner_user_id IS NOT NULL AND organization_id IS NOT NULL));

CREATE INDEX idx_parking_lots_owner_user ON parking_lots(owner_user_id);
CREATE INDEX idx_parking_lots_organization ON parking_lots(organization_id);

-- Existing lots were already live under the old admin-only model — backfill
-- them to APPROVED so this migration doesn't silently take working lots
-- offline. New lots default to PENDING_APPROVAL from the application layer.
ALTER TABLE parking_lots ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
CREATE INDEX idx_parking_lots_approval_status ON parking_lots(approval_status);
