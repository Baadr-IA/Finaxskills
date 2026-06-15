-- Add level column to skill with default NIVEAU_1
ALTER TABLE skill
  ADD COLUMN IF NOT EXISTS level VARCHAR(20) DEFAULT 'NIVEAU_1' NOT NULL;

-- If you prefer to store labels like 'niveau 1' instead of enum names, adjust accordingly.

