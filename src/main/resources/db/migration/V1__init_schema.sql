-- ============================================================================
-- V1__init_schema.sql
-- Baseline schema matching the JPA entity model as of this migration.
--
-- NOTE: this was generated from the entity classes because the project had
-- spring.flyway.enabled=true with an empty db/migration folder and
-- ddl-auto=validate — i.e. the app could not start against a fresh database
-- at all. Before relying on this in a real environment, run it against a
-- throwaway Postgres instance and diff it with a Hibernate schema export
-- (spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create)
-- to catch any naming mismatch.
-- ============================================================================

-- ── roles ────────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- ── users ────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    phone      VARCHAR(20),
    enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── vehicles ─────────────────────────────────────────────────────────────────
CREATE TABLE vehicles (
    id            BIGSERIAL PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    make          VARCHAR(50) NOT NULL,
    model         VARCHAR(50) NOT NULL,
    year          VARCHAR(10) NOT NULL,
    color         VARCHAR(30),
    type          VARCHAR(20) NOT NULL,
    owner_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_vehicles_owner ON vehicles(owner_id);

-- ── parking_lots ─────────────────────────────────────────────────────────────
CREATE TABLE parking_lots (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    zip_code    VARCHAR(20),
    latitude    NUMERIC(10,7),
    longitude   NUMERIC(10,7),
    hourly_rate NUMERIC(10,2) NOT NULL,
    daily_rate  NUMERIC(10,2),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    open_time   TIME,
    close_time  TIME,
    description VARCHAR(500),
    amenities   VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_parking_lots_city ON parking_lots(city);

-- ── parking_spots ────────────────────────────────────────────────────────────
CREATE TABLE parking_spots (
    id             BIGSERIAL PRIMARY KEY,
    spot_number    VARCHAR(20) NOT NULL,
    type           VARCHAR(20) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    notes          VARCHAR(200),
    parking_lot_id BIGINT NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_spot_lot_number UNIQUE (parking_lot_id, spot_number)
);
CREATE INDEX idx_parking_spots_lot ON parking_spots(parking_lot_id);
CREATE INDEX idx_parking_spots_status ON parking_spots(status);

-- ── pricing_rules ────────────────────────────────────────────────────────────
CREATE TABLE pricing_rules (
    id              BIGSERIAL PRIMARY KEY,
    parking_lot_id  BIGINT NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    applicable_days VARCHAR(200),
    start_time      TIME,
    end_time        TIME,
    multiplier      NUMERIC(5,2) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pricing_rules_lot ON pricing_rules(parking_lot_id);

-- ── bookings ─────────────────────────────────────────────────────────────────
CREATE TABLE bookings (
    id                BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(50) NOT NULL UNIQUE,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vehicle_id        BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    spot_id           BIGINT NOT NULL REFERENCES parking_spots(id) ON DELETE CASCADE,
    start_time        TIMESTAMP NOT NULL,
    end_time          TIMESTAMP NOT NULL,
    actual_check_in   TIMESTAMP,
    actual_check_out  TIMESTAMP,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount      NUMERIC(10,2) NOT NULL,
    discount_amount   NUMERIC(10,2),
    promo_code        VARCHAR(30),
    reminder_sent     BOOLEAN NOT NULL DEFAULT FALSE,
    notes             VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_spot ON bookings(spot_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_created ON bookings(created_at);
CREATE INDEX idx_bookings_start_time ON bookings(start_time);

-- ── payments ─────────────────────────────────────────────────────────────────
CREATE TABLE payments (
    id                BIGSERIAL PRIMARY KEY,
    transaction_id    VARCHAR(100) NOT NULL UNIQUE,
    booking_id        BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    amount            NUMERIC(10,2) NOT NULL,
    payment_method    VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_reference VARCHAR(200),
    failure_reason    VARCHAR(500),
    paid_at           TIMESTAMP,
    refunded_at       TIMESTAMP,
    refund_amount     NUMERIC(10,2),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway_ref ON payments(gateway_reference);

-- ── reviews ──────────────────────────────────────────────────────────────────
CREATE TABLE reviews (
    id             BIGSERIAL PRIMARY KEY,
    booking_id     BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parking_lot_id BIGINT NOT NULL REFERENCES parking_lots(id) ON DELETE CASCADE,
    rating         INTEGER NOT NULL,
    comment        VARCHAR(1000),
    visible        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reviews_lot ON reviews(parking_lot_id);

-- ── notifications ────────────────────────────────────────────────────────────
CREATE TABLE notifications (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type           VARCHAR(50) NOT NULL,
    title          VARCHAR(200) NOT NULL,
    message        VARCHAR(1000) NOT NULL,
    is_read        BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id   BIGINT,
    reference_type VARCHAR(50),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at        TIMESTAMP
);
CREATE INDEX idx_notification_user ON notifications(user_id);
CREATE INDEX idx_notification_read ON notifications(is_read);

-- ── password_reset_tokens ────────────────────────────────────────────────────
CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    token      VARCHAR(100) NOT NULL UNIQUE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);

-- ── audit_logs ───────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_email VARCHAR(150) NOT NULL,
    action      VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80),
    entity_id   BIGINT,
    description VARCHAR(1000),
    ip_address  VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_actor  ON audit_logs(actor_email);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_time   ON audit_logs(created_at);
