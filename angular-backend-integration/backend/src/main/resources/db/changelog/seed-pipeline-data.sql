-- ═══════════════════════════════════════════════════════
-- Seed Data — 6 Applications Rdapp
-- 2 Camunda · 3 Spring Boot · 1 Angular
-- ═══════════════════════════════════════════════════════

-- Applications
INSERT INTO application (id, name, icon, description, repo, tech, color, sort_order) VALUES
('orch-paiements',    'Orchestrateur Paiements',  '⚙️', 'Workflow BPMN traitement paiements',  'orch-paiements',    'camunda', '#20977C', 1),
('wf-reclamations',   'Workflow Réclamations',     '🔄', 'Workflow BPMN gestion réclamations',   'wf-reclamations',   'camunda', '#F6C93C', 2),
('api-comptes',       'API Comptes',               '🏦', 'Microservice gestion comptes & soldes','api-comptes',       'spring',  '#00874E', 3),
('api-paiements',     'API Paiements',             '💳', 'Microservice traitement paiements',    'api-paiements',     'spring',  '#1EA5B4', 4),
('svc-notifications', 'Service Notifications',     '🔔', 'Service notifications multicanal',     'svc-notifications', 'spring',  '#69B4C5', 5),
('accesd-web',        'AccèsD Web',                '🌐', 'Portail bancaire en ligne',            'accesd-web',        'angular', '#DD0031', 6);

-- ══════════════════════════════════════════
-- Environment Deployments
-- ══════════════════════════════════════════

-- orch-paiements
INSERT INTO environment_deployment (id, application_id, env_key, version, status, last_deploy, branch, instances, uptime) VALUES
('ed-op-dev',  'orch-paiements', 'dev',  '2.5.0-dev.9',  'deployed', 'il y a 20 min',    'feat/bpmn-virement-batch', 2, '4h 15m'),
('ed-op-qa',   'orch-paiements', 'qa',   '2.4.1-rc.2',   'deployed', 'il y a 6h',        'release/2.4.1',            2, '28h 10m'),
('ed-op-test', 'orch-paiements', 'test', '2.4.0',        'deployed', 'il y a 3 jours',   'main',                     3, '72h 00m'),
('ed-op-prod', 'orch-paiements', 'prod', '2.3.2',        'deployed', 'il y a 9 jours',   'main',                     6, '216h 30m');

-- wf-reclamations
INSERT INTO environment_deployment (id, application_id, env_key, version, status, last_deploy, branch, instances, uptime) VALUES
('ed-wf-dev',  'wf-reclamations', 'dev',  '1.8.0-dev.4',  'deployed',  'il y a 1h',        'feat/bpmn-auto-approbation', 1, '6h 00m'),
('ed-wf-qa',   'wf-reclamations', 'qa',   '1.7.2-rc.1',   'deploying', 'en cours...',      'release/1.7.2',              2, '—'),
('ed-wf-test', 'wf-reclamations', 'test', '1.7.1',        'deployed',  'il y a 5 jours',   'main',                       2, '120h 15m'),
('ed-wf-prod', 'wf-reclamations', 'prod', '1.7.0',        'deployed',  'il y a 14 jours',  'main',                       4, '336h 45m');

-- api-comptes
INSERT INTO environment_deployment (id, application_id, env_key, version, status, last_deploy, branch, instances, uptime) VALUES
('ed-ac-dev',  'api-comptes', 'dev',  '5.2.0-dev.14', 'deployed', 'il y a 8 min',    'feat/compte-multi-devises', 2,  '2h 40m'),
('ed-ac-qa',   'api-comptes', 'qa',   '5.1.3-rc.1',   'deployed', 'il y a 3h',       'release/5.1.3',             3,  '18h 20m'),
('ed-ac-test', 'api-comptes', 'test', '5.1.2',        'deployed', 'il y a 4 jours',  'main',                      4,  '96h 05m'),
('ed-ac-prod', 'api-comptes', 'prod', '5.1.1',        'deployed', 'il y a 7 jours',  'main',                      10, '168h 50m');

-- api-paiements
INSERT INTO environment_deployment (id, application_id, env_key, version, status, last_deploy, branch, instances, uptime) VALUES
('ed-ap-dev',  'api-paiements', 'dev',  '8.1.0-dev.23', 'deployed', 'il y a 45 min',   'feat/paiement-temps-reel', 2,  '6h 45m'),
('ed-ap-qa',   'api-paiements', 'qa',   '8.0.4-rc.1',   'deployed', 'il y a 8h',       'release/8.0.4',            3,  '32h 10m'),
('ed-ap-test', 'api-paiements', 'test', '8.0.3',        'blocked',  'il y a 1 jour',   'main',                     4,  '26h 00m'),
('ed-ap-prod', 'api-paiements', 'prod', '8.0.2',        'deployed', 'il y a 8 jours',  'main',                     16, '192h 20m');

-- svc-notifications
INSERT INTO environment_deployment (id, application_id, env_key, version, status, last_deploy, branch, instances, uptime) VALUES
('ed-sn-dev',  'svc-notifications', 'dev',  '3.4.0-dev.6',  'deployed', 'il y a 25 min',    'feat/push-notification-v2', 1, '3h 50m'),
('ed-sn-qa',   'svc-notifications', 'qa',   '3.3.1-rc.1',   'deployed', 'il y a 5h',        'release/3.3.1',             2, '20h 30m'),
('ed-sn-test', 'svc-notifications', 'test', '3.3.0',        'deployed', 'il y a 6 jours',   'main',                      2, '144h 10m'),
('ed-sn-prod', 'svc-notifications', 'prod', '3.2.4',        'deployed', 'il y a 11 jours',  'main',                      4, '264h 20m');

-- accesd-web
INSERT INTO environment_deployment (id, application_id, env_key, version, status, last_deploy, branch, instances, uptime) VALUES
('ed-aw-dev',  'accesd-web', 'dev',  '4.12.0-dev.18', 'deployed',  'il y a 12 min',   'feat/virement-interac-v2', 1, '3h 02m'),
('ed-aw-qa',   'accesd-web', 'qa',   '4.11.3-rc.2',   'deploying', 'en cours...',     'release/4.11.3',           2, '—'),
('ed-aw-test', 'accesd-web', 'test', '4.11.2',        'deployed',  'il y a 2 jours',  'main',                     4, '48h 12m'),
('ed-aw-prod', 'accesd-web', 'prod', '4.11.1',        'deployed',  'il y a 6 jours',  'main',                     8, '144h 30m');

-- ══════════════════════════════════════════
-- Commits
-- ══════════════════════════════════════════

-- orch-paiements
INSERT INTO commit_record (id, application_id, version_tag, sha, message, ticket, ticket_title, commit_type, author, commit_date) VALUES
('c-op-1', 'orch-paiements', '2.5.0-dev.9', 'a1f2e3b', 'feat: BPMN subprocess virement batch',     'ORC-312', 'Subprocess virement batch', 'feat', 'Thomas K.',  '17 fév 14:10'),
('c-op-2', 'orch-paiements', '2.5.0-dev.9', 'c4d5a6f', 'fix: timer boundary event retry',          'ORC-310', 'Timer boundary retry',      'fix',  'Nadia B.',   '17 fév 10:30'),
('c-op-3', 'orch-paiements', '2.4.1-rc.2',  'e7f8b9c', 'feat: DMN table scoring risque',            'ORC-305', 'DMN scoring risque',        'feat', 'Thomas K.',  '15 fév 15:00'),
('c-op-4', 'orch-paiements', '2.4.1-rc.2',  'd1a2e3f', 'fix: incident handler DLQ',                 'ORC-302', 'Incident handler DLQ',      'fix',  'Nadia B.',   '14 fév 11:20'),
('c-op-5', 'orch-paiements', '2.4.0',       'g4h5i6j', 'feat: compensation handler remboursement',  'ORC-295', 'Compensation remboursement', 'feat', 'Thomas K.',  '11 fév 14:00'),
('c-op-6', 'orch-paiements', '2.3.2',       'k7l8m9n', 'fix: race condition parallel gateway',      'ORC-288', 'Race condition gateway',     'fix',  'Nadia B.',   '5 fév 09:30');

-- wf-reclamations
INSERT INTO commit_record (id, application_id, version_tag, sha, message, ticket, ticket_title, commit_type, author, commit_date) VALUES
('c-wf-1', 'wf-reclamations', '1.8.0-dev.4', 'o1p2q3r', 'feat: BPMN auto-approbation < 5000$',  'WFR-198', 'Auto-approbation',       'feat', 'Camille L.', '17 fév 13:00'),
('c-wf-2', 'wf-reclamations', '1.8.0-dev.4', 's4t5u6v', 'fix: user task assignation expert',     'WFR-196', 'Assignation expert',     'fix',  'David M.',   '17 fév 09:15'),
('c-wf-3', 'wf-reclamations', '1.7.2-rc.1',  'w7x8y9z', 'feat: signal event rappel client',      'WFR-190', 'Signal rappel',          'feat', 'Camille L.', '14 fév 14:30'),
('c-wf-4', 'wf-reclamations', '1.7.2-rc.1',  'a2b3c4d', 'perf: optimisation requêtes',            'WFR-188', 'Perf requêtes historique','perf', 'David M.',   '13 fév 16:00'),
('c-wf-5', 'wf-reclamations', '1.7.1',       'e5f6g7h', 'fix: message correlation dupliquée',     'WFR-182', 'Correlation duplicata',  'fix',  'Camille L.', '9 fév 10:00'),
('c-wf-6', 'wf-reclamations', '1.7.0',       'i8j9k0l', 'feat: multi-instance subprocess docs',   'WFR-175', 'Subprocess validation',  'feat', 'David M.',   '1 fév 14:00');

-- api-comptes
INSERT INTO commit_record (id, application_id, version_tag, sha, message, ticket, ticket_title, commit_type, author, commit_date) VALUES
('c-ac-1', 'api-comptes', '5.2.0-dev.14', 'm1n2o3p', 'feat: endpoint compte multi-devises', 'CPT-892', 'Multi-devises',       'feat', 'Sophie R.', '17 fév 14:30'),
('c-ac-2', 'api-comptes', '5.2.0-dev.14', 'q4r5s6t', 'test: tests intégration JPA',          'CPT-890', 'Tests JPA',           'test', 'Alex T.',   '17 fév 11:00'),
('c-ac-3', 'api-comptes', '5.1.3-rc.1',   'u7v8w9x', 'fix: pagination Spring Data',           'CPT-885', 'Pagination relevés',  'fix',  'Sophie R.', '15 fév 15:30'),
('c-ac-4', 'api-comptes', '5.1.3-rc.1',   'y0z1a2b', 'perf: cache Redis solde temps réel',    'CPT-882', 'Cache Redis solde',   'perf', 'Alex T.',   '14 fév 16:00'),
('c-ac-5', 'api-comptes', '5.1.2',        'c3d4e5f', 'fix: transaction isolation',             'CPT-878', 'Isolation transaction','fix',  'Sophie R.', '10 fév 09:00'),
('c-ac-6', 'api-comptes', '5.1.1',        'g6h7i8j', 'fix: ObjectMapper date serialization',   'CPT-870', 'Serialization date',  'fix',  'Alex T.',   '7 fév 10:00');

-- api-paiements
INSERT INTO commit_record (id, application_id, version_tag, sha, message, ticket, ticket_title, commit_type, author, commit_date) VALUES
('c-ap-1', 'api-paiements', '8.1.0-dev.23', 'aa1bb2c', 'feat: paiement temps réel RTP',  'PAY-2105', 'Paiements RTP',   'feat', 'Patrick L.',   '17 fév 13:00'),
('c-ap-2', 'api-paiements', '8.1.0-dev.23', 'dd3ee4f', 'test: load test 50K tx/sec',      'PAY-2102', 'Load test 50K',   'test', 'Véronique S.', '17 fév 09:30'),
('c-ap-3', 'api-paiements', '8.0.4-rc.1',   'gg5hh6i', 'fix: retry Interac e-Transfer',   'PAY-2095', 'Timeout Interac', 'fix',  'Patrick L.',   '15 fév 14:00'),
('c-ap-4', 'api-paiements', '8.0.3',        'pp1qq2r', 'fix: deadlock @Transactional',     'PAY-2080', 'Deadlock',        'fix',  'Patrick L.',   '12 fév 11:00'),
('c-ap-5', 'api-paiements', '8.0.2',        'vv5ww6x', 'fix: PCI DSS token vault',         'PAY-2070', 'PCI DSS tokens',  'fix',  'Véronique S.', '7 fév 10:00');

-- svc-notifications
INSERT INTO commit_record (id, application_id, version_tag, sha, message, ticket, ticket_title, commit_type, author, commit_date) VALUES
('c-sn-1', 'svc-notifications', '3.4.0-dev.6',  'bb1cc2d', 'feat: push Firebase v2',      'NTF-445', 'Push Firebase v2',   'feat', 'Karine B.',  '17 fév 14:00'),
('c-sn-2', 'svc-notifications', '3.4.0-dev.6',  'ee3ff4g', 'fix: template Thymeleaf',      'NTF-443', 'Template courriel',  'fix',  'Francis T.', '17 fév 10:20'),
('c-sn-3', 'svc-notifications', '3.3.1-rc.1',   'hh5ii6j', 'feat: webhook Slack alertes',  'NTF-438', 'Webhook Slack',      'feat', 'Karine B.',  '14 fév 15:30'),
('c-sn-4', 'svc-notifications', '3.3.0',        'nn9oo0p', 'feat: SMS Twilio API',          'NTF-428', 'Intégration Twilio', 'feat', 'Karine B.',  '8 fév 14:00'),
('c-sn-5', 'svc-notifications', '3.2.4',        'qq1rr2s', 'fix: retry DLX RabbitMQ',       'NTF-420', 'Retry DLX',          'fix',  'Francis T.', '3 fév 10:00');

-- accesd-web
INSERT INTO commit_record (id, application_id, version_tag, sha, message, ticket, ticket_title, commit_type, author, commit_date) VALUES
('c-aw-1', 'accesd-web', '4.12.0-dev.18', 'a1b2c3d', 'feat: composant virement Interac v2', 'ACD-1847', 'Virement v2',     'feat', 'Isabelle M.', '17 fév 14:20'),
('c-aw-2', 'accesd-web', '4.12.0-dev.18', 'e4f5a6b', 'fix: reactive form validation IBAN',   'ACD-1843', 'Validation IBAN', 'fix',  'Félix D.',    '17 fév 11:05'),
('c-aw-3', 'accesd-web', '4.11.3-rc.2',   '7c8d9e0', 'feat: NgRx store dark mode',           'ACD-1830', 'Dark mode NgRx',  'feat', 'Isabelle M.', '15 fév 16:30'),
('c-aw-4', 'accesd-web', '4.11.3-rc.2',   'f1a2b3c', 'perf: lazy loading modules',           'ACD-1825', 'Lazy loading',    'perf', 'Félix D.',    '15 fév 10:00'),
('c-aw-5', 'accesd-web', '4.11.2',        'b7c8d9e', 'fix: pipe currency fr-CA',              'ACD-1810', 'Pipe currency',   'fix',  'Isabelle M.', '12 fév 09:30'),
('c-aw-6', 'accesd-web', '4.11.1',        'c4d5e6f', 'fix: XSS DomSanitizer',                 'ACD-1798', 'DomSanitizer fix','fix',  'Félix D.',    '8 fév 10:00');
