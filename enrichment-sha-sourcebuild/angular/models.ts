// ══════════════════════════════════════════════════════════
// MODIFICATIONS dans apps.data.ts (ou ton fichier de modèles)
// ══════════════════════════════════════════════════════════

// ── Commit : INCHANGÉ ──
export interface Commit {
  sha: string;
  message: string;
  ticket: string;
  ticketTitle: string;
  type: CommitType;
  author: string;
  date: string;
  statusName?: string;
  statusCategory?: string;
  rawDate?: Date;
}

// ── EnvData : ajouter tagCommitSha ──
export interface EnvData {
  version: string;
  status: DeployStatus;
  lastDeploy: string;
  branch: string;
  instances: number;
  uptime: string;
  headCommitDate?: string;
  dateDeploiement?: string;
  tagCommitSha?: string;           // ← AJOUT
}

// ── NOUVEAU : VersionInfo ──
export interface VersionInfo {
  tagCommitSha?: string;           // SHA du tag Git
  sourceBuild?: string;            // ex: "1.5.4-build.20260218T1639Z"
  commits: Commit[];               // commits entre cette version et la précédente
}

// ── AppConfig : MODIFIER commits ──
export interface AppConfig {
  id: string;
  name: string;
  icon: string;
  description: string;
  repo: string;
  tech: TechType;
  color: string;
  envs: Record<EnvKey, EnvData>;
  commits: Record<string, VersionInfo>;   // ← CHANGÉ : Commit[] → VersionInfo
}
