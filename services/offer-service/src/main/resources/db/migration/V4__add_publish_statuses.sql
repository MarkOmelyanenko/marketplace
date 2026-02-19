-- Add new status values for publish flow
-- Note: PostgreSQL doesn't support ALTER TYPE ADD VALUE in a transaction
-- So we'll just ensure the constraint allows these values
-- The status column is VARCHAR, so we can add new values without migration issues
-- But we should update any CHECK constraints if they exist

-- If there's a CHECK constraint, we might need to drop and recreate it
-- For now, since status is VARCHAR, we can just use the new values
