/**
 * ═══════════════════════════════════════════════════════
 * Rdapp Deploy + Sprint Dashboard — Models
 * Types, interfaces, constantes et lookups centralisés.
 * ═══════════════════════════════════════════════════════
 */

// ── Palette ─────────────────────────────────

export const PALETTE = {
  green:      '#00874E',
  greenLight: '#20977C',
  greenSoft:  '#45B398',
  greenPale:  '#8EC9A9',
  teal:       '#1EA5B4',
  tealLight:  '#69B4C5',
  red:        '#E05256',
  gold:       '#DCB433',
  goldLight:  '#F6C93C',
  orange:     '#FC5D0D',
  darkBlue:   '#1E4D5D',
  darkSlate:  '#414A55',
} as const;

// ── Environnements ──────────────────────────

export type EnvKey = 'dev' | 'qa' | 'test' | 'prod';
export const ENV_KEYS: readonly EnvKey[] = ['dev', 'qa', 'test', 'prod'];

export interface EnvMeta { label: string; fullName: string; color: string; }

const _ENV: Record<EnvKey, EnvMeta> = {
  dev:  { label: 'DEV',  fullName: 'Développement',    color: PALETTE.teal },
  qa:   { label: 'QA',   fullName: 'Assurance Qualité', color: PALETTE.gold },
  test: { label: 'TEST', fullName: 'Tests Intégration', color: PALETTE.greenSoft },
  prod: { label: 'PROD', fullName: 'Production',        color: PALETTE.green },
};
export function getEnvMeta(key: EnvKey): EnvMeta { return _ENV[key]; }

// ── Statut Déploiement ──────────────────────

export type DeployStatus = 'deployed' | 'deploying' | 'blocked';
export interface StatusMeta { label: string; icon: string; color: string; }

const _STATUS: Record<DeployStatus, StatusMeta> = {
  deployed:  { label: 'Déployé',  icon: '✓', color: PALETTE.green },
  deploying: { label: 'En cours', icon: '◉', color: PALETTE.gold },
  blocked:   { label: 'Bloqué',   icon: '⚠', color: PALETTE.red },
};
export function getStatusMeta(status: DeployStatus): StatusMeta { return _STATUS[status]; }

// ── Types de Commit ─────────────────────────

export type CommitType = 'feat' | 'fix' | 'perf' | 'test' | 'docs' | 'chore';
export interface CommitTypeMeta { label: string; bg: string; color: string; }

const _CTYPE: Record<CommitType, CommitTypeMeta> = {
  feat:  { label: 'FEAT',  bg: PALETTE.green,     color: '#fff' },
  fix:   { label: 'FIX',   bg: PALETTE.red,       color: '#fff' },
  perf:  { label: 'PERF',  bg: PALETTE.teal,      color: '#fff' },
  test:  { label: 'TEST',  bg: PALETTE.darkBlue,  color: '#fff' },
  docs:  { label: 'DOCS',  bg: PALETTE.gold,      color: '#1a1a1a' },
  chore: { label: 'CHORE', bg: PALETTE.darkSlate,  color: '#fff' },
};
export function getCommitTypeMeta(type: string): CommitTypeMeta {
  return _CTYPE[type as CommitType] ?? _CTYPE.chore;
}

// ── Priorités ───────────────────────────────

export type Priority = 'critical' | 'high' | 'medium' | 'low';
export interface PriorityMeta { label: string; bg: string; color: string; }

const _PRIO: Record<Priority, PriorityMeta> = {
  critical: { label: 'CRITIQUE', bg: '#FEE2E2', color: '#DC2626' },
  high:     { label: 'HAUTE',    bg: '#FEF3C7', color: '#D97706' },
  medium:   { label: 'MOYENNE',  bg: '#E0F2FE', color: '#0284C7' },
  low:      { label: 'BASSE',    bg: '#F0FDF4', color: '#16A34A' },
};
export function getPriorityMeta(priority: Priority): PriorityMeta { return _PRIO[priority]; }

// ── Technologies ────────────────────────────

export type TechType = 'camunda' | 'spring' | 'angular';
export interface TechMeta { label: string; bg: string; color: string; border: string; }

const _TECH: Record<TechType, TechMeta> = {
  camunda: { label: 'Camunda',     bg: '#FC5D0D', color: '#fff', border: '#FC5D0D55' },
  spring:  { label: 'Spring Boot', bg: '#6DB33F', color: '#fff', border: '#6DB33F55' },
  angular: { label: 'Angular',     bg: '#DD0031', color: '#fff', border: '#DD003155' },
};
export function getTechMeta(tech: TechType): TechMeta { return _TECH[tech]; }

// ── URLs ────────────────────────────────────

export const JIRA_BASE_URL   = 'https://jira.rdapp.com/browse';
export const GITHUB_BASE_URL = 'https://github.com/rdapp';

// ── Interfaces ──────────────────────────────

export interface Commit {
  sha: string; message: string; ticket: string; ticketTitle: string;
  type: CommitType; author: string; date: string;
}
export interface EnvData {
  version: string; status: DeployStatus; lastDeploy: string;
  branch: string; instances: number; uptime: string;
}
export interface AppConfig {
  id: string; name: string; icon: string; description: string;
  repo: string; tech: TechType; color: string;
  envs: Record<EnvKey, EnvData>; commits: Record<string, Commit[]>;
}
export interface Squad { id: string; name: string; color: string; }

export interface SprintTicket {
  ticket: string; title: string; squad: string;
  storyPoints: number; priority: Priority; author: string;
}
export interface InProgressTicket extends SprintTicket {
  progress: number; branch: string;
}
export interface PullRequestTicket extends SprintTicket {
  pr: string; branch: string; checks: Record<string, boolean>;
  comments: number; created: string;
}
export interface SquadGroup<T extends SprintTicket = SprintTicket> {
  squad: Squad; tickets: T[];
}
export type TabType = 'pipeline' | 'sprint';
