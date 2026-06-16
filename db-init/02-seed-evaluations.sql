-- Optional seed data to mirror frontend static mock data
-- These INSERTs use subqueries to find collaborators by first/last name.
-- They will succeed only if collaborators with matching names exist in table `collaborator`.

INSERT INTO evaluations (collabon n  rator_id, evaluation_name, status, date_assigned, date_planned, date_validated, level_declared, level_validated, score)
VALUES
((SELECT id FROM collaborator WHERE first_name='Jean' AND last_name='Dupont' LIMIT 1), 'Test Java Complet', 'en attente', '2026-03-20', NULL, NULL, NULL, NULL, NULL),
((SELECT id FROM collaborator WHERE first_name='Jean' AND last_name='Dupont' LIMIT 1), 'Test Java Complet', 'completé', '2026-02-10', '2026-02-15', '2026-02-15', 'NIVEAU 2', 'NIVEAU 2', '100%'),
((SELECT id FROM collaborator WHERE first_name='Marie' AND last_name='Martin' LIMIT 1), 'Test Fondamentaux Python', 'en cours', '2026-03-15', '2026-03-26', NULL, NULL, NULL, NULL),
((SELECT id FROM collaborator WHERE first_name='Pierre' AND last_name='Bernard' LIMIT 1), 'Test Java Complet', 'completé', '2026-01-05', '2026-01-10', '2026-01-10', 'NIVEAU 3', 'NIVEAU 3', '100%');

