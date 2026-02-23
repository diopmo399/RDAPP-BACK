/**
 * ═══════════════════════════════════════════════════════
 * API Response Interfaces
 * Matching backend DTOs from Spring Boot REST API
 * ═══════════════════════════════════════════════════════
 */

// ── Pipeline / Applications ─────────────────

export interface ApiAppResponse {
  id: string;
  name: string;
  icon: string;
  description: string;
  repo: string;
  tech: string;
  color: string;
  envs: Record<string, ApiEnvResponse>;
  commits?: Record<string, ApiCommitResponse[]>;
}

export interface ApiEnvResponse {
  version: string;
  status: string;
  lastDeploy: string;
  branch: string;
  instances: number;
  uptime: string;
}

export interface ApiCommitResponse {
  sha: string;
  message: string;
  ticket: string;
  ticketTitle: string;
  type: string;
  author: string;
  date: string;
}

// ── Squads (from existing backend) ──────────

export interface ApiSquadResponse {
  id: string;
  name: string;
  color: string;
  boardId?: number;
  projectKey?: string;
  members: ApiMemberResponse[];
}

export interface ApiMemberResponse {
  id: string;
  fullName: string;
  username: string;
  email: string;
  role: string;
  avatarUrl?: string;
}

// ── Sprint / Jira sync ─────────────────────

export interface ApiSprintResponse {
  id: string;
  jiraSprintId: number;
  squadId: string;
  name: string;
  state: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  completeDate?: string;
  boardId: number;
  projectKey: string;
  totalIssues: number;
  doneIssues: number;
  totalStoryPoints: number;
  doneStoryPoints: number;
  completionPercent: number;
  issues: ApiIssueResponse[];
  syncedAt: string;
}

export interface ApiIssueResponse {
  issueKey: string;
  summary: string;
  issueType: string;
  statusName: string;
  statusCategory: string;   // new | indeterminate | done
  priority: string;
  storyPoints: number;
  assigneeName: string;
  assigneeUsername: string;
  fixVersion?: string;
}

export interface ApiSyncResultResponse {
  squadId: string;
  squadName: string;
  boardId: number;
  boardName: string;
  activeSprint?: ApiSprintResponse;
  closedSprints: ApiSprintResponse[];
  futureSprints: ApiSprintResponse[];
  error?: string;
  syncedAt: string;
}

export interface ApiJiraStatusResponse {
  configured: boolean;
  connected: boolean;
  baseUrl: string;
  authType: string;
  error?: string;
}

// ── Batch ingest (GHA) ─────────────────────

export interface ApiBatchIngestResponse {
  message: string;
  sprintsProcessed: number;
  issuesProcessed: number;
}
