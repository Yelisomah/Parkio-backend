-- ============================================================================
-- V2__add_geospatial_location.sql
--
-- Adds PostGIS-backed radius search (Kiro design.md requirement 7) without
-- touching the JPA entity model:
--   - `location` is NOT a Hibernate-mapped field. It's maintained entirely by
--     the trigger below, driven off the existing latitude/longitude columns
--     that the entity already owns.
--   - This means ddl-auto=validate in prod never needs to know this column
--     exists, and the H2 test profile (which uses Hibernate to generate its
--     own schema from the entity, and has no PostGIS/JTS support at all)
--     is completely unaffected — no test infra changes needed to add this.
--
-- Deliberate trade-off: nearby search (ParkingLotRepository.findNearby) only
-- works against real Postgres+PostGIS. It is NOT exercised by the existing
-- H2 test suite — see ParkingLotNearbySearchIT, which uses Testcontainers
-- against a real postgis/postgis image instead, per tech-standards.md's own
-- testing rule for geospatial logic. That test requires Docker + network and
-- was NOT run in this sandbox (no Docker/registry access) — please run it
-- yourself before relying on this in prod.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE parking_lots ADD COLUMN location geography(Point, 4326);

-- Backfill existing rows
UPDATE parking_lots
SET location = ST_SetSRID(ST_MakePoint(longitude::float8, latitude::float8), 4326)::geography
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE INDEX idx_parking_lots_location ON parking_lots USING GIST (location);

-- Keep `location` in sync whenever the app writes latitude/longitude via JPA
CREATE OR REPLACE FUNCTION parking_lots_sync_location() RETURNS trigger AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location := ST_SetSRID(ST_MakePoint(NEW.longitude::float8, NEW.latitude::float8), 4326)::geography;
    ELSE
        NEW.location := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_parking_lots_sync_location
    BEFORE INSERT OR UPDATE OF latitude, longitude ON parking_lots
    FOR EACH ROW
    EXECUTE FUNCTION parking_lots_sync_location();
