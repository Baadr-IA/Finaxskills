-- Add details column and migrate status values to French labels

ALTER TABLE evaluations
  ADD COLUMN IF NOT EXISTS details TEXT DEFAULT '' NOT NULL;

-- Migrate existing status values to French equivalents
UPDATE evaluations
SET status = CASE
  WHEN lower(status) = 'pending' THEN 'en attente'
  WHEN lower(status) = 'in_progress' THEN 'en cours'
  WHEN lower(status) = 'completed' THEN 'complété'
  ELSE status
END
WHERE status IS NOT NULL;

