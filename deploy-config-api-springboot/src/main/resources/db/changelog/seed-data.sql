zh-- Affect Versions
INSERT INTO affect_version (id, name, status, release_date, description) VALUES
('v-2504', '24.04', 'IN_PROGRESS', '2025-02-14', 'Sprint courant — Interac v2 + Camunda stabilisation'),
('v-2503', '24.03', 'RELEASED',    '2025-02-03', 'Paiements batch + notifications WebSocket'),
('v-2502', '24.02', 'RELEASED',    '2025-01-20', 'AccèsD dark mode + API Comptes WebFlux'),
('v-2501', '24.01', 'RELEASED',    '2025-01-06', 'Réclamations auto-approbation + OAuth2'),
('v-2413', '23.13', 'ARCHIVED',    '2024-12-20', 'Stabilisation fin d''année'),
('v-2505', '24.05', 'PLANNED',     '2025-02-28', 'Prochain sprint — Multi-devises + A11y');

-- Squads
INSERT INTO squad (id, name, color, board_id) VALUES
('bp', 'BPMN Paiements',   '#20977C', 'BPMN-42'),
('dm', 'DMN/Rules',         '#DCB433', 'DMN-18'),
('cc', 'Core Comptes',      '#00874E', 'CPT-07'),
('ux', 'UX/Angular',        '#DD0031', 'UX-33'),
('a1', 'Accessibilité',     '#45B398', 'A11Y-05'),
('dc', 'Data & Analytics',  '#1EA5B4', 'DATA-21');

-- Members
INSERT INTO squad_member (id, squad_id, name, role, employee_code, username, github) VALUES
('tk', 'bp', 'Thomas K.',    'LEAD',   'EMP-1042', 'tkowalski',   'tkowalski-desj'),
('nb', 'bp', 'Nadia B.',     'DEV',    'EMP-1187', 'nbouchard',   'nadia-b'),
('ml', 'bp', 'Martin L.',    'DEV',    'EMP-1203', 'mlafrance',   'mlafrance'),
('sg', 'bp', 'Sarah G.',     'QA',     'EMP-0981', 'sgagnon',     'sarah-qa'),
('jf', 'dm', 'Jean-F B.',    'LEAD',   'EMP-0877', 'jfbergeron',  'jf-berg'),
('ab', 'dm', 'Amélie B.',    'DEV',    'EMP-1322', 'abrosseau',   'amelie-br'),
('rp', 'dm', 'Robert P.',    'QA',     'EMP-0764', 'rpelletier',  'rpelletier'),
('sr', 'cc', 'Sophie R.',    'LEAD',   'EMP-0652', 'srichard',    'sophie-r'),
('at', 'cc', 'Alex T.',      'DEV',    'EMP-1410', 'atremblay',   'alex-tremb'),
('lb', 'cc', 'Louis B.',     'DEV',    'EMP-1455', 'lbeaulieu',   'louis-beau'),
('ck', 'cc', 'Caroline K.',  'QA',     'EMP-1102', 'ckhoury',     'ckhoury'),
('gm', 'cc', 'Gabriel M.',   'DEVOPS', 'EMP-0899', 'gmartin',     'gab-ops'),
('im', 'ux', 'Isabelle M.',  'LEAD',   'EMP-0543', 'imoreau',     'isa-moreau'),
('fd', 'ux', 'Félix D.',     'DEV',    'EMP-1378', 'fdumont',     'felix-d'),
('op', 'ux', 'Olivier P.',   'UX',     'EMP-1290', 'opaquin',     'opaquin-ux'),
('rt', 'ux', 'Raphaël T.',   'QA',     'EMP-1156', 'rtrudeau',    'raph-t'),
('ng', 'a1', 'Nathalie G.',  'LEAD',   'EMP-0488', 'ngirard',     'nat-girard'),
('sd', 'a1', 'Sylvie D.',    'UX',     'EMP-1067', 'sdeschenes',  'sylvie-ux'),
('md', 'dc', 'Marc D.',      'LEAD',   'EMP-0721', 'mdubois',     'marc-data'),
('nl', 'dc', 'Nicolas L.',   'DEV',    'EMP-1501', 'nleblanc',    'nico-lb');
