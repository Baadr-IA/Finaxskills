-- Flyway style migration to create evaluations table
-- If Flyway is enabled this file will be applied automatically.

CREATE TABLE IF NOT EXISTS evaluations (
    id BIGSERIAL PRIMARY KEY,
    collaborator_id BIGINT NOT NULL REFERENCES collaborator(id),
    evaluation_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    date_assigned DATE NOT NULL,
    date_planned DATE,
    date_validated DATE,
    level_declared VARCHAR(100),
    level_validated VARCHAR(100),
    score VARCHAR(20),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_evaluations_date_assigned ON evaluations(date_assigned DESC);

