-- ============================================================================
-- V7__promo_codes.sql
-- Real promo codes with usage caps (tasks.md task 10), replacing the
-- hardcoded two-entry table that shipped in the original DiscountService.
-- ============================================================================

CREATE TABLE promo_codes (
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(30) NOT NULL UNIQUE,
    discount_percent NUMERIC(5,4) NOT NULL,
    max_uses         INTEGER,
    uses_count       INTEGER NOT NULL DEFAULT 0,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at       TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed the two codes the old hardcoded DiscountService recognized, so
-- anything already relying on them (test data, docs, in-flight testing)
-- keeps working after the cutover.
INSERT INTO promo_codes (code, discount_percent, max_uses) VALUES
    ('PARKIO10', 0.10, NULL),
    ('WELCOME15', 0.15, NULL);
