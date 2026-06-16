-- Script to seed fake data into collaborator and evaluations tables
-- This script inserts sample collaborators and evaluations for testing

BEGIN;

-- Insert fake collaborators (if they don't already exist)
INSERT INTO collaborator (first_name, last_name, email, job_title, keycloak_id)
VALUES
  ('Jean', 'Dupont', 'jean.dupont@example.com', 'Developeur', 'kc-jean-dupont'),
  ('Marie', 'Martin', 'marie.martin@example.com', 'Ingenieur Devops', 'kc-marie-martin'),
  ('Pierre', 'Bernard', 'pierre.bernard@example.com', 'Product Owner', 'kc-pierre-bernard'),
  ('Sophie', 'Lefebvre', 'sophie.lefebvre@example.com', 'Architecte Solutions', 'kc-sophie-lefebvre'),
  ('Thomas', 'Garcia', 'thomas.garcia@example.com', 'Developeur', 'kc-thomas-garcia'),
  ('Luc', 'Martinez', 'luc.martinez@example.com', 'Lead Developer', 'kc-luc-martinez'),
  ('Isabelle', 'Dubois', 'isabelle.dubois@example.com', 'QA Engineer', 'kc-isabelle-dubois'),
  ('Paul', 'Michel', 'paul.michel@example.com', 'DevOps Engineer', 'kc-paul-michel')
ON CONFLICT (email) DO NOTHING;

-- Insert fake evaluations
INSERT INTO evaluations (collaborator_id, evaluation_name, status, date_assigned, date_planned, date_validated, level_declared, level_validated, score, details, created_at, updated_at)
VALUES
  -- Jean Dupont evaluations
  ((SELECT id FROM collaborator WHERE email='jean.dupont@example.com' LIMIT 1), 'Test Java Complet', 'en attente', '2026-03-20', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='jean.dupont@example.com' LIMIT 1), 'Test Java Complet', 'completé', '2026-02-10', '2026-02-15', '2026-02-15', 'NIVEAU 2', 'NIVEAU 2', '100%', 'Tres bonne performance', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='jean.dupont@example.com' LIMIT 1), 'Test Fondamentaux Python', 'en attente', '2026-05-15', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='jean.dupont@example.com' LIMIT 1), 'Test SQL Avance', 'en cours', '2026-06-01', '2026-06-10', NULL, NULL, NULL, NULL, 'En cours depuis le 1er juin', NOW(), NOW()),

  -- Marie Martin evaluations
  ((SELECT id FROM collaborator WHERE email='marie.martin@example.com' LIMIT 1), 'Test Fondamentaux Python', 'en cours', '2026-03-15', '2026-03-26', NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='marie.martin@example.com' LIMIT 1), 'Test Java Complet', 'en attente', '2026-04-05', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='marie.martin@example.com' LIMIT 1), 'Test Kubernetes', 'completé', '2026-01-01', '2026-01-15', '2026-01-15', 'NIVEAU 3', 'NIVEAU 3', '95%', 'Excellent', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='marie.martin@example.com' LIMIT 1), 'Test Docker', 'completé', '2026-02-20', '2026-02-28', '2026-02-28', 'NIVEAU 2', 'NIVEAU 3', '88%', 'Bonne comprehension des concepts', NOW(), NOW()),

  -- Pierre Bernard evaluations
  ((SELECT id FROM collaborator WHERE email='pierre.bernard@example.com' LIMIT 1), 'Test Java Complet', 'completé', '2026-01-05', '2026-01-10', '2026-01-10', 'NIVEAU 3', 'NIVEAU 3', '100%', 'Excellent score', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='pierre.bernard@example.com' LIMIT 1), 'Test Agile et Scrum', 'completé', '2026-03-01', '2026-03-10', '2026-03-10', 'NIVEAU 2', 'NIVEAU 2', '90%', '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='pierre.bernard@example.com' LIMIT 1), 'Test Leadership', 'en attente', '2026-05-20', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),

  -- Sophie Lefebvre evaluations
  ((SELECT id FROM collaborator WHERE email='sophie.lefebvre@example.com' LIMIT 1), 'Test Architecture Microservices', 'en cours', '2026-04-10', '2026-04-30', NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='sophie.lefebvre@example.com' LIMIT 1), 'Test Cloud AWS', 'completé', '2026-02-01', '2026-02-15', '2026-02-15', 'NIVEAU 3', 'NIVEAU 3', '92%', 'Connaissances approfondies', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='sophie.lefebvre@example.com' LIMIT 1), 'Test Java Complet', 'en attente', '2026-06-01', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),

  -- Thomas Garcia evaluations
  ((SELECT id FROM collaborator WHERE email='thomas.garcia@example.com' LIMIT 1), 'Test Java Complet', 'en attente', '2026-04-15', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='thomas.garcia@example.com' LIMIT 1), 'Test React', 'completé', '2026-01-20', '2026-02-01', '2026-02-01', 'NIVEAU 1', 'NIVEAU 2', '85%', 'Progression notable', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='thomas.garcia@example.com' LIMIT 1), 'Test Angular', 'en cours', '2026-05-01', '2026-05-15', NULL, NULL, NULL, NULL, '', NOW(), NOW()),

  -- Luc Martinez evaluations
  ((SELECT id FROM collaborator WHERE email='luc.martinez@example.com' LIMIT 1), 'Test Java Complet', 'completé', '2025-12-01', '2025-12-15', '2025-12-15', 'NIVEAU 3', 'NIVEAU 3', '100%', 'Maitrise totale', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='luc.martinez@example.com' LIMIT 1), 'Test Code Review', 'completé', '2026-02-10', '2026-02-20', '2026-02-20', 'NIVEAU 3', 'NIVEAU 3', '98%', 'Tres bon mentoring', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='luc.martinez@example.com' LIMIT 1), 'Test Patterns Design', 'en attente', '2026-06-05', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),

  -- Isabelle Dubois evaluations
  ((SELECT id FROM collaborator WHERE email='isabelle.dubois@example.com' LIMIT 1), 'Test QA Automation', 'en cours', '2026-03-01', '2026-03-20', NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='isabelle.dubois@example.com' LIMIT 1), 'Test Selenium', 'completé', '2026-01-15', '2026-01-31', '2026-01-31', 'NIVEAU 2', 'NIVEAU 2', '87%', '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='isabelle.dubois@example.com' LIMIT 1), 'Test Performance Testing', 'en attente', '2026-05-10', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW()),

  -- Paul Michel evaluations
  ((SELECT id FROM collaborator WHERE email='paul.michel@example.com' LIMIT 1), 'Test Docker', 'completé', '2026-02-01', '2026-02-10', '2026-02-10', 'NIVEAU 2', 'NIVEAU 3', '91%', 'Bonne maitrise', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='paul.michel@example.com' LIMIT 1), 'Test Kubernetes', 'en cours', '2026-04-20', '2026-05-20', NULL, NULL, NULL, NULL, '', NOW(), NOW()),
  ((SELECT id FROM collaborator WHERE email='paul.michel@example.com' LIMIT 1), 'Test CI/CD Pipeline', 'en attente', '2026-06-10', NULL, NULL, NULL, NULL, NULL, '', NOW(), NOW())
ON CONFLICT DO NOTHING;

COMMIT;




