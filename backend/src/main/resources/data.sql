-- Keep only idempotent schema/data migrations here.
-- Demo tours and logs should be created through the app/API, not reinserted on every backend start.

ALTER TABLE IF EXISTS tour DROP CONSTRAINT IF EXISTS tour_estimated_time_check;
ALTER TABLE IF EXISTS tour DROP CONSTRAINT IF EXISTS tour_distance_check;
ALTER TABLE IF EXISTS tour DROP CONSTRAINT IF EXISTS tour_transport_type_check;

UPDATE tour SET transport_type = 'VEHICLE' WHERE transport_type = 'VACATION';

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
