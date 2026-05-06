ALTER TABLE IF EXISTS trip_schedules
    ADD COLUMN IF NOT EXISTS fare_overridden BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS trip_schedules
    ALTER COLUMN fare_overridden SET DEFAULT FALSE;

UPDATE trip_schedules
SET fare_overridden = FALSE
WHERE fare_overridden IS NULL;

ALTER TABLE IF EXISTS trip_schedules
    ALTER COLUMN fare_overridden SET NOT NULL;
