// ══════════════════════════════════════════════════════
// ANGULAR INTEGRATION — Ajouter dans deploy.models.ts
// ══════════════════════════════════════════════════════

export interface SprintGlobalResponse {
  sprint: SprintMetadata;
  notStarted: SprintTicketDto[];
  inProgress: SprintTicketDto[];
  done: SprintTicketDto[];
  squads: Record<string, SquadDto>;
  totalPoints: number;
  donePoints: number;
  lastSync: string;
}

export interface SprintMetadata {
  name: string;
  startDate: string;
  endDate: string;
  state: string;
}

export interface SprintTicketDto {
  ticket: string;
  title: string;
  squad: string;
  storyPoints: number;
  priority: 'critical' | 'high' | 'medium' | 'low';
  author: string;
  app: string | null;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE';
  progress?: number;
  branch?: string;
  completedDate?: string;
  version?: string;
}

export interface SquadDto {
  id: string;
  name: string;
  color: string;
}
