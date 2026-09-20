-- ============================================================================
-- V3__organizations_and_staff.sql
-- Organizations & Staff module (Kiro design.md / tasks.md task 3).
-- Replaces the flat ROLE_ATTENDANT concept removed earlier — see Role.java.
-- ============================================================================

CREATE TABLE organizations (
    id             BIGSERIAL PRIMARY KEY,
    owner_user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name           VARCHAR(150) NOT NULL,
    contact_phone  VARCHAR(20),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_organizations_owner ON organizations(owner_user_id);

CREATE TABLE staff (
    id                 BIGSERIAL PRIMARY KEY,
    organization_id    BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role               VARCHAR(20) NOT NULL,
    reports_to_staff_id BIGINT REFERENCES staff(id) ON DELETE SET NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_staff_org_user UNIQUE (organization_id, user_id)
);
CREATE INDEX idx_staff_org ON staff(organization_id);
CREATE INDEX idx_staff_user ON staff(user_id);
CREATE INDEX idx_staff_reports_to ON staff(reports_to_staff_id);

CREATE TABLE staff_assignments (
    id          BIGSERIAL PRIMARY KEY,
    staff_id    BIGINT NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    space_id    BIGINT NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_staff_assignments_staff ON staff_assignments(staff_id);
CREATE INDEX idx_staff_assignments_space ON staff_assignments(space_id);

CREATE TABLE shifts (
    id                BIGSERIAL PRIMARY KEY,
    staff_id          BIGINT NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    space_id          BIGINT NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    start_time        TIMESTAMP NOT NULL,
    end_time          TIMESTAMP NOT NULL,
    attendance_status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    check_in_time     TIMESTAMP,
    check_out_time    TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_shifts_staff ON shifts(staff_id);
CREATE INDEX idx_shifts_space ON shifts(space_id);
CREATE INDEX idx_shifts_status ON shifts(attendance_status);
CREATE INDEX idx_shifts_start_time ON shifts(start_time);
