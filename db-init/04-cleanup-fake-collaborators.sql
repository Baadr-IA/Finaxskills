-- Cleanup script for fake collaborators previously seeded in local environments.
-- Usage:
--   1) Run as-is for DRY-RUN (no delete)
--   2) Set DO_DELETE to true to apply deletion

BEGIN;

-- Toggle this value to true when you want to actually delete rows.
WITH params AS (
    SELECT false::boolean AS do_delete
),
fake_collaborators AS (
    SELECT c.id, c.email, c.keycloak_id
    FROM collaborator c
    WHERE c.email IN (
        'jean.dupont@example.com',
        'marie.martin@example.com',
        'pierre.bernard@example.com',
        'sophie.lefebvre@example.com',
        'thomas.garcia@example.com',
        'luc.martinez@example.com',
        'isabelle.dubois@example.com',
        'paul.michel@example.com'
    )
       OR c.keycloak_id IN (
        'kc-jean-dupont',
        'kc-marie-martin',
        'kc-pierre-bernard',
        'kc-sophie-lefebvre',
        'kc-thomas-garcia',
        'kc-luc-martinez',
        'kc-isabelle-dubois',
        'kc-paul-michel'
    )
)
SELECT * FROM fake_collaborators ORDER BY id;

-- Impact preview per table
WITH fake_ids AS (
    SELECT c.id
    FROM collaborator c
    WHERE c.email IN (
        'jean.dupont@example.com',
        'marie.martin@example.com',
        'pierre.bernard@example.com',
        'sophie.lefebvre@example.com',
        'thomas.garcia@example.com',
        'luc.martinez@example.com',
        'isabelle.dubois@example.com',
        'paul.michel@example.com'
    )
       OR c.keycloak_id IN (
        'kc-jean-dupont',
        'kc-marie-martin',
        'kc-pierre-bernard',
        'kc-sophie-lefebvre',
        'kc-thomas-garcia',
        'kc-luc-martinez',
        'kc-isabelle-dubois',
        'kc-paul-michel'
    )
)
SELECT 'collaborator_answer' AS table_name, COUNT(*) AS rows_to_delete
FROM collaborator_answer ca
JOIN fake_ids f ON f.id = ca.collaborator_id
UNION ALL
SELECT 'assessment_session', COUNT(*)
FROM assessment_session s
JOIN fake_ids f ON f.id = s.collaborator_id
UNION ALL
SELECT 'collaborator_skill', COUNT(*)
FROM collaborator_skill cs
JOIN fake_ids f ON f.id = cs.collaborator_id
UNION ALL
SELECT 'test_collab', COUNT(*)
FROM test_collab tc
JOIN fake_ids f ON f.id = tc.collaborator_id
UNION ALL
SELECT 'evaluations', COUNT(*)
FROM evaluations e
JOIN fake_ids f ON f.id = e.collaborator_id
UNION ALL
SELECT 'collaborator', COUNT(*)
FROM collaborator c
JOIN fake_ids f ON f.id = c.id;

-- Apply deletion only when do_delete=true.
WITH params AS (
    SELECT false::boolean AS do_delete
),
fake_ids AS (
    SELECT c.id
    FROM collaborator c
    WHERE c.email IN (
        'jean.dupont@example.com',
        'marie.martin@example.com',
        'pierre.bernard@example.com',
        'sophie.lefebvre@example.com',
        'thomas.garcia@example.com',
        'luc.martinez@example.com',
        'isabelle.dubois@example.com',
        'paul.michel@example.com'
    )
       OR c.keycloak_id IN (
        'kc-jean-dupont',
        'kc-marie-martin',
        'kc-pierre-bernard',
        'kc-sophie-lefebvre',
        'kc-thomas-garcia',
        'kc-luc-martinez',
        'kc-isabelle-dubois',
        'kc-paul-michel'
    )
),
del_ca AS (
    DELETE FROM collaborator_answer ca
    USING fake_ids f, params p
    WHERE p.do_delete AND ca.collaborator_id = f.id
    RETURNING ca.id
),
del_as AS (
    DELETE FROM assessment_session s
    USING fake_ids f, params p
    WHERE p.do_delete AND s.collaborator_id = f.id
    RETURNING s.id
),
del_cs AS (
    DELETE FROM collaborator_skill cs
    USING fake_ids f, params p
    WHERE p.do_delete AND cs.collaborator_id = f.id
    RETURNING cs.collaborator_id
),
del_tc AS (
    DELETE FROM test_collab tc
    USING fake_ids f, params p
    WHERE p.do_delete AND tc.collaborator_id = f.id
    RETURNING tc.id
),
del_ev AS (
    DELETE FROM evaluations e
    USING fake_ids f, params p
    WHERE p.do_delete AND e.collaborator_id = f.id
    RETURNING e.id
),
del_c AS (
    DELETE FROM collaborator c
    USING fake_ids f, params p
    WHERE p.do_delete AND c.id = f.id
    RETURNING c.id
)
SELECT
    (SELECT do_delete FROM params) AS deletion_applied,
    (SELECT COUNT(*) FROM del_ca) AS deleted_collaborator_answer,
    (SELECT COUNT(*) FROM del_as) AS deleted_assessment_session,
    (SELECT COUNT(*) FROM del_cs) AS deleted_collaborator_skill,
    (SELECT COUNT(*) FROM del_tc) AS deleted_test_collab,
    (SELECT COUNT(*) FROM del_ev) AS deleted_evaluations,
    (SELECT COUNT(*) FROM del_c) AS deleted_collaborators;

COMMIT;

