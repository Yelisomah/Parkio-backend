-- ============================================================================
-- V6__collections_and_enforcement.sql
-- Collections reconciliation (tasks.md task 9) and Admin/Enforcement module
-- (tasks.md task 11 — Fine, Ad).
-- ============================================================================

CREATE TABLE daily_collections_summary (
    id                  BIGSERIAL PRIMARY KEY,
    space_id            BIGINT NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    staff_id            BIGINT REFERENCES staff(id) ON DELETE SET NULL,
    summary_date        DATE NOT NULL,
    expected_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    digital_amount      NUMERIC(12,2) NOT NULL DEFAULT 0,
    cash_logged_amount  NUMERIC(12,2) NOT NULL DEFAULT 0,
    discrepancy_flag    BOOLEAN NOT NULL DEFAULT FALSE,
    discrepancy_note    VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_collections_space_staff_date UNIQUE (space_id, staff_id, summary_date)
);
CREATE INDEX idx_collections_space_date ON daily_collections_summary(space_id, summary_date);
CREATE INDEX idx_collections_flagged ON daily_collections_summary(discrepancy_flag);

CREATE TABLE fines (
    id                 BIGSERIAL PRIMARY KEY,
    plate_number       VARCHAR(20) NOT NULL,
    space_id           BIGINT NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    issued_by_staff_id BIGINT NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    reason             VARCHAR(500) NOT NULL,
    fine_amount        NUMERIC(10,2) NOT NULL,
    is_paid            BOOLEAN NOT NULL DEFAULT FALSE,
    disputed           BOOLEAN NOT NULL DEFAULT FALSE,
    dispute_reason     VARCHAR(1000),
    issued_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_fines_plate ON fines(plate_number);
CREATE INDEX idx_fines_space ON fines(space_id);
CREATE INDEX idx_fines_disputed ON fines(disputed);

CREATE TABLE ads (
    id            BIGSERIAL PRIMARY KEY,
    space_id      BIGINT REFERENCES parking_lots(id) ON DELETE CASCADE,
    organization_id BIGINT REFERENCES organizations(id) ON DELETE CASCADE,
    image_url     VARCHAR(500) NOT NULL,
    business_name VARCHAR(150) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ads_space ON ads(space_id);
CREATE INDEX idx_ads_organization ON ads(organization_id);
