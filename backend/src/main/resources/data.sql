-- Keep only idempotent schema/data migrations here.
-- Demo tours and logs should be created through the app/API, not reinserted on every backend start.

ALTER TABLE IF EXISTS tour DROP CONSTRAINT IF EXISTS tour_estimated_time_check;
ALTER TABLE IF EXISTS tour DROP CONSTRAINT IF EXISTS tour_distance_check;
ALTER TABLE IF EXISTS tour DROP CONSTRAINT IF EXISTS tour_transport_type_check;

UPDATE tour SET transport_type = 'VEHICLE' WHERE transport_type = 'VACATION';

ALTER TABLE IF EXISTS tour ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE IF EXISTS tour ADD COLUMN IF NOT EXISTS creator_name VARCHAR(255);

UPDATE tour
SET owner_user_id = app_user.id,
    creator_name = app_user.name
FROM app_user
WHERE app_user.email = 'demo@tourplanner.local'
  AND tour.owner_user_id IS NULL;

DELETE FROM tour_log
WHERE tourid IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM tour
      WHERE tour.id = tour_log.tourid
  );

ALTER TABLE IF EXISTS tour_log DROP CONSTRAINT IF EXISTS fk_tour_log_tour;

ALTER TABLE IF EXISTS tour_log
    ADD CONSTRAINT fk_tour_log_tour
    FOREIGN KEY (tourid) REFERENCES tour(id) ON DELETE CASCADE;

ALTER TABLE IF EXISTS tour DROP CONSTRAINT IF EXISTS fk_tour_owner;

ALTER TABLE IF EXISTS tour
    ADD CONSTRAINT fk_tour_owner
    FOREIGN KEY (owner_user_id) REFERENCES app_user(id) ON DELETE CASCADE;
