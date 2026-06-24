-- Flyway migration: create test_collab table to track evaluations assigned to collaborators

CREATE TABLE IF NOT EXISTS test_collab (
    id BIGSERIAL PRIMARY KEY,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    collaborator_id BIGINT NOT NULL REFERENCES collaborator(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL,
    progress INTEGER DEFAULT 0,
    due_date DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now() NOT NULL,
    CONSTRAINT uk_test_collab_evaluation_collaborator UNIQUE (evaluation_id, collaborator_id),
    CONSTRAINT chk_test_collab_progress CHECK (progress >= 0 AND progress <= 100)
);

CREATE INDEX IF NOT EXISTS idx_test_collab_collaborator ON test_collab(collaborator_id);
CREATE INDEX IF NOT EXISTS idx_test_collab_assigned_at ON test_collab(assigned_at DESC);
CREATE INDEX IF NOT EXISTS idx_test_collab_status ON test_collab(status);
