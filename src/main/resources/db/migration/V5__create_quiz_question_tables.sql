ALTER TABLE test_collab
    ADD COLUMN IF NOT EXISTS generated_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS score INTEGER;

CREATE TABLE IF NOT EXISTS question (
    id BIGSERIAL PRIMARY KEY,
    test_collab_id BIGINT NOT NULL REFERENCES test_collab(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    position_order INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_question_test_collab ON question(test_collab_id);

CREATE TABLE IF NOT EXISTS answer_option (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    option_code VARCHAR(1) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_answer_option_question ON answer_option(question_id);

CREATE TABLE IF NOT EXISTS collaborator_answer (
    id BIGSERIAL PRIMARY KEY,
    collaborator_id BIGINT NOT NULL REFERENCES collaborator(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    answer_option_id BIGINT NOT NULL REFERENCES answer_option(id) ON DELETE CASCADE,
    answered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_collaborator_answer_collab ON collaborator_answer(collaborator_id);
CREATE INDEX IF NOT EXISTS idx_collaborator_answer_question ON collaborator_answer(question_id);
