/**
 * Mock Data — 6 Applications Rdapp
 * 2 Camunda · 3 Spring Boot · 1 Angular
 *
 * Replace with API calls in production.
 */

import {
  AppConfig, Squad, SprintTicket, InProgressTicket, PullRequestTicket, PALETTE,
} from '../models/deploy.models';

const P = PALETTE;

// ════════════════════════════════════════════
// Applications
// ════════════════════════════════════════════

export const APPS: readonly AppConfig[] = [
  {
    id: 'orch-paiements', name: 'Orchestrateur Paiements', icon: '⚙️',
    description: 'Workflow BPMN traitement paiements', repo: 'orch-paiements',
    tech: 'camunda', color: P.greenLight,
    envs: {
      dev:  { version: '2.5.0-dev.9',  status: 'deployed', lastDeploy: 'il y a 20 min', branch: 'feat/bpmn-virement-batch', instances: 2, uptime: '4h 15m' },
      qa:   { version: '2.4.1-rc.2',   status: 'deployed', lastDeploy: 'il y a 6h',     branch: 'release/2.4.1',            instances: 2, uptime: '28h 10m' },
      test: { version: '2.4.0',        status: 'deployed', lastDeploy: 'il y a 3 jours', branch: 'main',                     instances: 3, uptime: '72h 00m' },
      prod: { version: '2.3.2',        status: 'deployed', lastDeploy: 'il y a 9 jours', branch: 'main',                     instances: 6, uptime: '216h 30m' },
    },
    commits: {
      '2.5.0-dev.9': [
        { sha: 'a1f2e3b', message: 'feat: BPMN subprocess virement batch',    ticket: 'ORC-312', ticketTitle: 'Subprocess virement batch', type: 'feat', author: 'Thomas K.',  date: '17 fév 14:10' },
        { sha: 'c4d5a6f', message: 'fix: timer boundary event retry',         ticket: 'ORC-310', ticketTitle: 'Timer boundary retry',      type: 'fix',  author: 'Nadia B.',   date: '17 fév 10:30' },
      ],
      '2.4.1-rc.2': [
        { sha: 'e7f8b9c', message: 'feat: DMN table scoring risque',          ticket: 'ORC-305', ticketTitle: 'DMN scoring risque',        type: 'feat', author: 'Thomas K.',  date: '15 fév 15:00' },
        { sha: 'd1a2e3f', message: 'fix: incident handler DLQ',               ticket: 'ORC-302', ticketTitle: 'Incident handler DLQ',      type: 'fix',  author: 'Nadia B.',   date: '14 fév 11:20' },
      ],
      '2.4.0': [
        { sha: 'g4h5i6j', message: 'feat: compensation handler remboursement', ticket: 'ORC-295', ticketTitle: 'Compensation remboursement', type: 'feat', author: 'Thomas K.', date: '11 fév 14:00' },
      ],
      '2.3.2': [
        { sha: 'k7l8m9n', message: 'fix: race condition parallel gateway',    ticket: 'ORC-288', ticketTitle: 'Race condition gateway',    type: 'fix',  author: 'Nadia B.',   date: '5 fév 09:30' },
      ],
    },
  },
  {
    id: 'wf-reclamations', name: 'Workflow Réclamations', icon: '🔄',
    description: 'Workflow BPMN gestion réclamations', repo: 'wf-reclamations',
    tech: 'camunda', color: P.goldLight,
    envs: {
      dev:  { version: '1.8.0-dev.4',  status: 'deployed',  lastDeploy: 'il y a 1h',      branch: 'feat/bpmn-auto-approbation', instances: 1, uptime: '6h 00m' },
      qa:   { version: '1.7.2-rc.1',   status: 'deploying', lastDeploy: 'en cours...',     branch: 'release/1.7.2',              instances: 2, uptime: '—' },
      test: { version: '1.7.1',        status: 'deployed',  lastDeploy: 'il y a 5 jours',  branch: 'main',                       instances: 2, uptime: '120h 15m' },
      prod: { version: '1.7.0',        status: 'deployed',  lastDeploy: 'il y a 14 jours', branch: 'main',                       instances: 4, uptime: '336h 45m' },
    },
    commits: {
      '1.8.0-dev.4': [
        { sha: 'o1p2q3r', message: 'feat: BPMN auto-approbation < 5000$', ticket: 'WFR-198', ticketTitle: 'Auto-approbation',         type: 'feat', author: 'Camille L.', date: '17 fév 13:00' },
        { sha: 's4t5u6v', message: 'fix: user task assignation expert',   ticket: 'WFR-196', ticketTitle: 'Assignation expert',       type: 'fix',  author: 'David M.',   date: '17 fév 09:15' },
      ],
      '1.7.2-rc.1': [
        { sha: 'w7x8y9z', message: 'feat: signal event rappel client',    ticket: 'WFR-190', ticketTitle: 'Signal rappel',            type: 'feat', author: 'Camille L.', date: '14 fév 14:30' },
        { sha: 'a2b3c4d', message: 'perf: optimisation requêtes',         ticket: 'WFR-188', ticketTitle: 'Perf requêtes historique',  type: 'perf', author: 'David M.',   date: '13 fév 16:00' },
      ],
      '1.7.1': [
        { sha: 'e5f6g7h', message: 'fix: message correlation dupliquée',  ticket: 'WFR-182', ticketTitle: 'Correlation duplicata',    type: 'fix',  author: 'Camille L.', date: '9 fév 10:00' },
      ],
      '1.7.0': [
        { sha: 'i8j9k0l', message: 'feat: multi-instance subprocess docs', ticket: 'WFR-175', ticketTitle: 'Subprocess validation',   type: 'feat', author: 'David M.',   date: '1 fév 14:00' },
      ],
    },
  },
  {
    id: 'api-comptes', name: 'API Comptes', icon: '🏦',
    description: 'Microservice gestion comptes & soldes', repo: 'api-comptes',
    tech: 'spring', color: P.green,
    envs: {
      dev:  { version: '5.2.0-dev.14', status: 'deployed', lastDeploy: 'il y a 8 min',   branch: 'feat/compte-multi-devises', instances: 2,  uptime: '2h 40m' },
      qa:   { version: '5.1.3-rc.1',   status: 'deployed', lastDeploy: 'il y a 3h',      branch: 'release/5.1.3',             instances: 3,  uptime: '18h 20m' },
      test: { version: '5.1.2',        status: 'deployed', lastDeploy: 'il y a 4 jours', branch: 'main',                      instances: 4,  uptime: '96h 05m' },
      prod: { version: '5.1.1',        status: 'deployed', lastDeploy: 'il y a 7 jours', branch: 'main',                      instances: 10, uptime: '168h 50m' },
    },
    commits: {
      '5.2.0-dev.14': [
        { sha: 'm1n2o3p', message: 'feat: endpoint compte multi-devises', ticket: 'CPT-892', ticketTitle: 'Multi-devises',     type: 'feat', author: 'Sophie R.', date: '17 fév 14:30' },
        { sha: 'q4r5s6t', message: 'test: tests intégration JPA',         ticket: 'CPT-890', ticketTitle: 'Tests JPA',         type: 'test', author: 'Alex T.',   date: '17 fév 11:00' },
      ],
      '5.1.3-rc.1': [
        { sha: 'u7v8w9x', message: 'fix: pagination Spring Data',         ticket: 'CPT-885', ticketTitle: 'Pagination relevés', type: 'fix',  author: 'Sophie R.', date: '15 fév 15:30' },
        { sha: 'y0z1a2b', message: 'perf: cache Redis solde temps réel',  ticket: 'CPT-882', ticketTitle: 'Cache Redis solde',  type: 'perf', author: 'Alex T.',   date: '14 fév 16:00' },
      ],
      '5.1.2': [
        { sha: 'c3d4e5f', message: 'fix: transaction isolation',          ticket: 'CPT-878', ticketTitle: 'Isolation transaction', type: 'fix', author: 'Sophie R.', date: '10 fév 09:00' },
      ],
      '5.1.1': [
        { sha: 'g6h7i8j', message: 'fix: ObjectMapper date serialization', ticket: 'CPT-870', ticketTitle: 'Serialization date', type: 'fix', author: 'Alex T.',   date: '7 fév 10:00' },
      ],
    },
  },
  {
    id: 'api-paiements', name: 'API Paiements', icon: '💳',
    description: 'Microservice traitement paiements', repo: 'api-paiements',
    tech: 'spring', color: P.teal,
    envs: {
      dev:  { version: '8.1.0-dev.23', status: 'deployed', lastDeploy: 'il y a 45 min', branch: 'feat/paiement-temps-reel', instances: 2,  uptime: '6h 45m' },
      qa:   { version: '8.0.4-rc.1',   status: 'deployed', lastDeploy: 'il y a 8h',     branch: 'release/8.0.4',            instances: 3,  uptime: '32h 10m' },
      test: { version: '8.0.3',        status: 'blocked',  lastDeploy: 'il y a 1 jour', branch: 'main',                     instances: 4,  uptime: '26h 00m' },
      prod: { version: '8.0.2',        status: 'deployed', lastDeploy: 'il y a 8 jours', branch: 'main',                    instances: 16, uptime: '192h 20m' },
    },
    commits: {
      '8.1.0-dev.23': [
        { sha: 'aa1bb2c', message: 'feat: paiement temps réel RTP',  ticket: 'PAY-2105', ticketTitle: 'Paiements RTP',   type: 'feat', author: 'Patrick L.',   date: '17 fév 13:00' },
        { sha: 'dd3ee4f', message: 'test: load test 50K tx/sec',     ticket: 'PAY-2102', ticketTitle: 'Load test 50K',    type: 'test', author: 'Véronique S.', date: '17 fév 09:30' },
      ],
      '8.0.4-rc.1': [
        { sha: 'gg5hh6i', message: 'fix: retry Interac e-Transfer',  ticket: 'PAY-2095', ticketTitle: 'Timeout Interac',  type: 'fix',  author: 'Patrick L.',   date: '15 fév 14:00' },
      ],
      '8.0.3': [
        { sha: 'pp1qq2r', message: 'fix: deadlock @Transactional',   ticket: 'PAY-2080', ticketTitle: 'Deadlock',         type: 'fix',  author: 'Patrick L.',   date: '12 fév 11:00' },
      ],
      '8.0.2': [
        { sha: 'vv5ww6x', message: 'fix: PCI DSS token vault',      ticket: 'PAY-2070', ticketTitle: 'PCI DSS tokens',   type: 'fix',  author: 'Véronique S.', date: '7 fév 10:00' },
      ],
    },
  },
  {
    id: 'svc-notifications', name: 'Service Notifications', icon: '🔔',
    description: 'Service notifications multicanal', repo: 'svc-notifications',
    tech: 'spring', color: P.tealLight,
    envs: {
      dev:  { version: '3.4.0-dev.6',  status: 'deployed', lastDeploy: 'il y a 25 min',   branch: 'feat/push-notification-v2', instances: 1, uptime: '3h 50m' },
      qa:   { version: '3.3.1-rc.1',   status: 'deployed', lastDeploy: 'il y a 5h',       branch: 'release/3.3.1',             instances: 2, uptime: '20h 30m' },
      test: { version: '3.3.0',        status: 'deployed', lastDeploy: 'il y a 6 jours',  branch: 'main',                      instances: 2, uptime: '144h 10m' },
      prod: { version: '3.2.4',        status: 'deployed', lastDeploy: 'il y a 11 jours', branch: 'main',                      instances: 4, uptime: '264h 20m' },
    },
    commits: {
      '3.4.0-dev.6': [
        { sha: 'bb1cc2d', message: 'feat: push Firebase v2',     ticket: 'NTF-445', ticketTitle: 'Push Firebase v2',  type: 'feat', author: 'Karine B.',  date: '17 fév 14:00' },
        { sha: 'ee3ff4g', message: 'fix: template Thymeleaf',    ticket: 'NTF-443', ticketTitle: 'Template courriel',  type: 'fix',  author: 'Francis T.', date: '17 fév 10:20' },
      ],
      '3.3.1-rc.1': [
        { sha: 'hh5ii6j', message: 'feat: webhook Slack alertes', ticket: 'NTF-438', ticketTitle: 'Webhook Slack',    type: 'feat', author: 'Karine B.',  date: '14 fév 15:30' },
      ],
      '3.3.0': [
        { sha: 'nn9oo0p', message: 'feat: SMS Twilio API',       ticket: 'NTF-428', ticketTitle: 'Intégration Twilio', type: 'feat', author: 'Karine B.',  date: '8 fév 14:00' },
      ],
      '3.2.4': [
        { sha: 'qq1rr2s', message: 'fix: retry DLX RabbitMQ',   ticket: 'NTF-420', ticketTitle: 'Retry DLX',          type: 'fix',  author: 'Francis T.', date: '3 fév 10:00' },
      ],
    },
  },
  {
    id: 'accesd-web', name: 'AccèsD Web', icon: '🌐',
    description: 'Portail bancaire en ligne', repo: 'accesd-web',
    tech: 'angular', color: '#DD0031',
    envs: {
      dev:  { version: '4.12.0-dev.18', status: 'deployed',  lastDeploy: 'il y a 12 min',  branch: 'feat/virement-interac-v2', instances: 1, uptime: '3h 02m' },
      qa:   { version: '4.11.3-rc.2',   status: 'deploying', lastDeploy: 'en cours...',     branch: 'release/4.11.3',           instances: 2, uptime: '—' },
      test: { version: '4.11.2',        status: 'deployed',  lastDeploy: 'il y a 2 jours', branch: 'main',                     instances: 4, uptime: '48h 12m' },
      prod: { version: '4.11.1',        status: 'deployed',  lastDeploy: 'il y a 6 jours', branch: 'main',                     instances: 8, uptime: '144h 30m' },
    },
    commits: {
      '4.12.0-dev.18': [
        { sha: 'a1b2c3d', message: 'feat: composant virement Interac v2', ticket: 'ACD-1847', ticketTitle: 'Virement v2',      type: 'feat', author: 'Isabelle M.', date: '17 fév 14:20' },
        { sha: 'e4f5a6b', message: 'fix: reactive form validation IBAN',  ticket: 'ACD-1843', ticketTitle: 'Validation IBAN',   type: 'fix',  author: 'Félix D.',    date: '17 fév 11:05' },
      ],
      '4.11.3-rc.2': [
        { sha: '7c8d9e0', message: 'feat: NgRx store dark mode',          ticket: 'ACD-1830', ticketTitle: 'Dark mode NgRx',    type: 'feat', author: 'Isabelle M.', date: '15 fév 16:30' },
        { sha: 'f1a2b3c', message: 'perf: lazy loading modules',          ticket: 'ACD-1825', ticketTitle: 'Lazy loading',      type: 'perf', author: 'Félix D.',    date: '15 fév 10:00' },
      ],
      '4.11.2': [
        { sha: 'b7c8d9e', message: 'fix: pipe currency fr-CA',            ticket: 'ACD-1810', ticketTitle: 'Pipe currency',     type: 'fix',  author: 'Isabelle M.', date: '12 fév 09:30' },
      ],
      '4.11.1': [
        { sha: 'c4d5e6f', message: 'fix: XSS DomSanitizer',              ticket: 'ACD-1798', ticketTitle: 'DomSanitizer fix',  type: 'fix',  author: 'Félix D.',    date: '8 fév 10:00' },
      ],
    },
  },
];

// ════════════════════════════════════════════
// Squads
// ════════════════════════════════════════════

export const SQUADS: Readonly<Record<string, readonly Squad[]>> = {
  'orch-paiements':   [{ id: 'bpmn-pay', name: 'BPMN Paiements', color: P.greenLight }, { id: 'dmn', name: 'DMN/Rules', color: P.gold }],
  'wf-reclamations':  [{ id: 'bpmn-recl', name: 'BPMN Réclamations', color: P.goldLight }, { id: 'sinistres', name: 'Sinistres', color: P.tealLight }],
  'api-comptes':      [{ id: 'core-cpt', name: 'Core Comptes', color: P.green }, { id: 'data-cpt', name: 'Data Comptes', color: P.greenSoft }],
  'api-paiements':    [{ id: 'core-pay', name: 'Core Paiements', color: P.teal }, { id: 'interac', name: 'Interac', color: P.gold }],
  'svc-notifications': [{ id: 'notif', name: 'Notifications', color: P.tealLight }],
  'accesd-web':       [{ id: 'ux-web', name: 'UX/Angular', color: '#DD0031' }, { id: 'a11y', name: 'Accessibilité', color: P.greenSoft }],
};

// ════════════════════════════════════════════
// Sprint Tickets
// ════════════════════════════════════════════

export const NOT_STARTED: Readonly<Record<string, readonly SprintTicket[]>> = {
  'orch-paiements':   [{ ticket: 'ORC-318', title: 'BPMN error event paiement refusé', squad: 'bpmn-pay', storyPoints: 8, priority: 'high', author: 'Thomas K.' }, { ticket: 'ORC-316', title: 'DMN table limite virement international', squad: 'dmn', storyPoints: 5, priority: 'medium', author: 'Jean-F. B.' }, { ticket: 'ORC-315', title: 'Timer event relance paiement', squad: 'bpmn-pay', storyPoints: 5, priority: 'high', author: 'Nadia B.' }],
  'wf-reclamations':  [{ ticket: 'WFR-205', title: 'BPMN subprocess expertise terrain', squad: 'bpmn-recl', storyPoints: 13, priority: 'critical', author: 'Camille L.' }, { ticket: 'WFR-203', title: 'User task assignation auto', squad: 'sinistres', storyPoints: 8, priority: 'high', author: 'Stéphane G.' }, { ticket: 'WFR-201', title: 'Signal event fermeture expirée', squad: 'bpmn-recl', storyPoints: 5, priority: 'medium', author: 'David M.' }],
  'api-comptes':      [{ ticket: 'CPT-900', title: 'Solde consolidé multi-institutions', squad: 'core-cpt', storyPoints: 13, priority: 'critical', author: 'Sophie R.' }, { ticket: 'CPT-898', title: 'OAuth2 resource server', squad: 'core-cpt', storyPoints: 8, priority: 'high', author: 'Alex T.' }, { ticket: 'CPT-896', title: 'Flyway V48 table devises', squad: 'data-cpt', storyPoints: 3, priority: 'medium', author: 'Marc D.' }],
  'api-paiements':    [{ ticket: 'PAY-2115', title: 'Batch paiements @Scheduled', squad: 'core-pay', storyPoints: 13, priority: 'critical', author: 'Patrick L.' }, { ticket: 'PAY-2112', title: 'Webhook statut paiement', squad: 'core-pay', storyPoints: 5, priority: 'high', author: 'Véronique S.' }, { ticket: 'PAY-2110', title: 'Interac autodeposit endpoint', squad: 'interac', storyPoints: 8, priority: 'high', author: 'Jean-M. R.' }],
  'svc-notifications': [{ ticket: 'NTF-452', title: 'Template virement confirmé', squad: 'notif', storyPoints: 3, priority: 'medium', author: 'Karine B.' }, { ticket: 'NTF-450', title: 'Spring Batch nettoyage', squad: 'notif', storyPoints: 5, priority: 'low', author: 'Francis T.' }],
  'accesd-web':       [{ ticket: 'ACD-1855', title: 'ng-select filtrable comptes', squad: 'ux-web', storyPoints: 5, priority: 'high', author: 'Isabelle M.' }, { ticket: 'ACD-1853', title: 'ARIA live region alertes', squad: 'a11y', storyPoints: 8, priority: 'critical', author: 'Nathalie G.' }, { ticket: 'ACD-1850', title: 'Material theming dark mode', squad: 'ux-web', storyPoints: 5, priority: 'medium', author: 'Félix D.' }],
};

export const IN_PROGRESS: Readonly<Record<string, readonly InProgressTicket[]>> = {
  'orch-paiements':   [{ ticket: 'ORC-314', title: 'External task KYC enrichissement', squad: 'bpmn-pay', storyPoints: 8, priority: 'high', author: 'Thomas K.', progress: 60, branch: 'feat/external-task-kyc' }, { ticket: 'ORC-313', title: 'Message start event externe', squad: 'bpmn-pay', storyPoints: 5, priority: 'medium', author: 'Nadia B.', progress: 35, branch: 'feat/msg-start-ext' }],
  'wf-reclamations':  [{ ticket: 'WFR-200', title: 'Escalation event délai expert', squad: 'bpmn-recl', storyPoints: 8, priority: 'high', author: 'Camille L.', progress: 75, branch: 'feat/escalation-delai' }, { ticket: 'WFR-199', title: 'Service task notification assuré', squad: 'sinistres', storyPoints: 5, priority: 'medium', author: 'Marie-Ève P.', progress: 40, branch: 'feat/svc-task-notif' }, { ticket: 'WFR-197', title: 'DMN couverture sinistre auto', squad: 'bpmn-recl', storyPoints: 5, priority: 'medium', author: 'David M.', progress: 20, branch: 'feat/dmn-couverture' }],
  'api-comptes':      [{ ticket: 'CPT-895', title: 'WebFlux reactive endpoint solde', squad: 'core-cpt', storyPoints: 8, priority: 'high', author: 'Sophie R.', progress: 55, branch: 'feat/webflux-solde' }, { ticket: 'CPT-893', title: 'Liquibase rollback comptes joints', squad: 'data-cpt', storyPoints: 5, priority: 'medium', author: 'Marc D.', progress: 80, branch: 'feat/liquibase-rollback' }],
  'api-paiements':    [{ ticket: 'PAY-2108', title: 'Circuit Breaker Interac', squad: 'core-pay', storyPoints: 8, priority: 'critical', author: 'Patrick L.', progress: 45, branch: 'feat/circuit-breaker' }, { ticket: 'PAY-2106', title: 'Kafka producer événements', squad: 'core-pay', storyPoints: 8, priority: 'high', author: 'Véronique S.', progress: 70, branch: 'feat/kafka-events' }, { ticket: 'PAY-2104', title: 'Interac Request-to-Pay', squad: 'interac', storyPoints: 13, priority: 'high', author: 'Jean-M. R.', progress: 30, branch: 'feat/interac-rtp' }],
  'svc-notifications': [{ ticket: 'NTF-448', title: 'WebSocket notifications live', squad: 'notif', storyPoints: 8, priority: 'high', author: 'Karine B.', progress: 65, branch: 'feat/websocket-live' }, { ticket: 'NTF-446', title: 'DLQ retry backoff RabbitMQ', squad: 'notif', storyPoints: 5, priority: 'medium', author: 'Francis T.', progress: 85, branch: 'fix/dlq-backoff' }],
  'accesd-web':       [{ ticket: 'ACD-1852', title: 'CDK virtual scroll relevés', squad: 'ux-web', storyPoints: 8, priority: 'high', author: 'Félix D.', progress: 50, branch: 'feat/cdk-virtual-scroll' }, { ticket: 'ACD-1849', title: 'NgRx effects comptes', squad: 'ux-web', storyPoints: 5, priority: 'medium', author: 'Isabelle M.', progress: 70, branch: 'feat/ngrx-effects' }, { ticket: 'ACD-1845', title: 'Cypress E2E virement', squad: 'a11y', storyPoints: 5, priority: 'medium', author: 'Nathalie G.', progress: 90, branch: 'test/cypress-virement' }],
};

export const IN_PR: Readonly<Record<string, readonly PullRequestTicket[]>> = {
  'orch-paiements':   [{ ticket: 'ORC-308', title: 'Message correlation externe', squad: 'bpmn-pay', storyPoints: 8, priority: 'high', author: 'Nadia B.', pr: '#234', branch: 'feat/msg-correlation', checks: { ci: true, lint: true, tests: true, sonar: true }, comments: 5, created: 'il y a 4h' }, { ticket: 'ORC-306', title: 'External task enrichissement', squad: 'bpmn-pay', storyPoints: 5, priority: 'medium', author: 'Thomas K.', pr: '#232', branch: 'feat/ext-task-enrich', checks: { ci: true, lint: true, tests: false, sonar: true }, comments: 2, created: 'il y a 1h' }],
  'wf-reclamations':  [{ ticket: 'WFR-195', title: 'Call activity validation docs', squad: 'bpmn-recl', storyPoints: 8, priority: 'high', author: 'David M.', pr: '#156', branch: 'feat/call-activity', checks: { ci: true, lint: true, tests: true, sonar: true }, comments: 7, created: 'il y a 6h' }, { ticket: 'WFR-193', title: 'Service task courriel expert', squad: 'sinistres', storyPoints: 3, priority: 'medium', author: 'Marie-Ève P.', pr: '#154', branch: 'feat/svc-task-email', checks: { ci: true, lint: true, tests: true, sonar: false }, comments: 1, created: 'il y a 2h' }],
  'api-comptes':      [{ ticket: 'CPT-888', title: 'JPA Specification filtres', squad: 'core-cpt', storyPoints: 8, priority: 'high', author: 'Alex T.', pr: '#567', branch: 'feat/jpa-spec', checks: { ci: true, lint: true, tests: true, sonar: true }, comments: 4, created: 'il y a 3h' }, { ticket: 'CPT-886', title: 'MapStruct mapper DTO', squad: 'core-cpt', storyPoints: 3, priority: 'low', author: 'Sophie R.', pr: '#565', branch: 'refactor/mapstruct', checks: { ci: true, lint: true, tests: true, sonar: true }, comments: 8, created: 'il y a 1 jour' }],
  'api-paiements':    [{ ticket: 'PAY-2100', title: 'Vault encryption at rest', squad: 'core-pay', storyPoints: 13, priority: 'critical', author: 'Patrick L.', pr: '#892', branch: 'feat/vault-encrypt', checks: { ci: true, lint: true, tests: true, sonar: false }, comments: 12, created: 'il y a 1 jour' }, { ticket: 'PAY-2098', title: 'Interac multi-devises', squad: 'interac', storyPoints: 8, priority: 'high', author: 'Jean-M. R.', pr: '#890', branch: 'feat/interac-multi', checks: { ci: true, lint: true, tests: true, sonar: true }, comments: 5, created: 'il y a 4h' }],
  'svc-notifications': [{ ticket: 'NTF-440', title: 'WebFlux SSE live', squad: 'notif', storyPoints: 8, priority: 'high', author: 'Karine B.', pr: '#178', branch: 'feat/webflux-sse', checks: { ci: true, lint: true, tests: true, sonar: true }, comments: 6, created: 'il y a 8h' }],
  'accesd-web':       [{ ticket: 'ACD-1842', title: 'DatePicker ControlValueAccessor', squad: 'ux-web', storyPoints: 5, priority: 'medium', author: 'Félix D.', pr: '#1284', branch: 'feat/datepicker-cva', checks: { ci: true, lint: true, tests: true, sonar: false }, comments: 3, created: 'il y a 2h' }, { ticket: 'ACD-1838', title: 'HttpInterceptor retry 401', squad: 'ux-web', storyPoints: 8, priority: 'high', author: 'Isabelle M.', pr: '#1282', branch: 'fix/interceptor-retry', checks: { ci: true, lint: true, tests: true, sonar: true }, comments: 7, created: 'il y a 5h' }, { ticket: 'ACD-1835', title: 'cdkTrapFocus accessibilité', squad: 'a11y', storyPoints: 5, priority: 'critical', author: 'Nathalie G.', pr: '#1280', branch: 'feat/cdk-trap-focus', checks: { ci: true, lint: true, tests: false, sonar: true }, comments: 1, created: 'il y a 1h' }],
};
