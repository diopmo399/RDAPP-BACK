import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, catchError, map, tap, BehaviorSubject, forkJoin, retry, timer } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ApiAppResponse, ApiCommitResponse, ApiEnvResponse,
  ApiSquadResponse, ApiSprintResponse, ApiSyncResultResponse,
  ApiJiraStatusResponse, ApiIssueResponse,
} from '../models/api.models';
import {
  AppConfig, EnvKey, Commit, CommitType, TechType, DeployStatus,
  Squad, SprintTicket, InProgressTicket, PullRequestTicket,
  Priority,
} from '../models/deploy.models';

@Injectable({ providedIn: 'root' })
export class DeployApiService {

  private readonly base = environment.apiBaseUrl;

  // ── État de connexion ────────────────────
  readonly backendAvailable$ = new BehaviorSubject<boolean | null>(null); // null = unknown
  readonly loading$ = new BehaviorSubject<boolean>(false);
  readonly lastError$ = new BehaviorSubject<string | null>(null);

  constructor(private http: HttpClient) {}

  // ═══════════════════════════════════════════
  // Pipeline / Applications
  // ═══════════════════════════════════════════

  /** Liste complète des apps avec commits — pour le dashboard */
  loadApps(): Observable<AppConfig[]> {
    this.loading$.next(true);
    return this.http.get<ApiAppResponse[]>(`${this.base}/v1/apps/full`).pipe(
      retry({ count: 1, delay: (_, retryCount) => timer(retryCount * 1000) }),
      map(apps => apps.map(a => this.mapAppResponse(a))),
      tap(() => {
        this.backendAvailable$.next(true);
        this.loading$.next(false);
        this.lastError$.next(null);
      }),
      catchError(err => this.handleError<AppConfig[]>(err, [])),
    );
  }

  /** Détail d'une app avec commits */
  loadApp(appId: string): Observable<AppConfig | null> {
    return this.http.get<ApiAppResponse>(`${this.base}/v1/apps/${appId}`).pipe(
      map(a => this.mapAppResponse(a)),
      catchError(err => this.handleError<AppConfig | null>(err, null)),
    );
  }

  /** Commits pour une app, groupés par version */
  loadCommits(appId: string): Observable<Record<string, Commit[]>> {
    return this.http.get<Record<string, ApiCommitResponse[]>>(`${this.base}/v1/apps/${appId}/commits`).pipe(
      map(data => {
        const result: Record<string, Commit[]> = {};
        for (const [ver, commits] of Object.entries(data)) {
          result[ver] = commits.map(c => this.mapCommit(c));
        }
        return result;
      }),
      catchError(err => this.handleError<Record<string, Commit[]>>(err, {})),
    );
  }

  /** Mettre à jour un déploiement (webhook CI/CD) */
  updateDeployment(appId: string, envKey: string, data: Partial<ApiEnvResponse>): Observable<ApiEnvResponse | null> {
    return this.http.put<ApiEnvResponse>(`${this.base}/v1/apps/${appId}/envs/${envKey}`, data).pipe(
      catchError(err => this.handleError<ApiEnvResponse | null>(err, null)),
    );
  }

  // ═══════════════════════════════════════════
  // Squads
  // ═══════════════════════════════════════════

  loadSquads(): Observable<ApiSquadResponse[]> {
    return this.http.get<ApiSquadResponse[]>(`${this.base}/v1/squads`).pipe(
      catchError(err => this.handleError<ApiSquadResponse[]>(err, [])),
    );
  }

  // ═══════════════════════════════════════════
  // Sprint / Jira sync
  // ═══════════════════════════════════════════

  /** Sprint actif pour une escouade */
  loadActiveSprint(squadId: string): Observable<ApiSprintResponse | null> {
    return this.http.get<ApiSprintResponse>(`${this.base}/v1/jira/sprints/squad/${squadId}/active`).pipe(
      catchError(err => this.handleError<ApiSprintResponse | null>(err, null)),
    );
  }

  /** Tous les sprints pour une escouade */
  loadAllSprints(squadId: string): Observable<ApiSprintResponse[]> {
    return this.http.get<ApiSprintResponse[]>(`${this.base}/v1/jira/sprints/squad/${squadId}`).pipe(
      catchError(err => this.handleError<ApiSprintResponse[]>(err, [])),
    );
  }

  /** Sprints fermés pour une escouade */
  loadClosedSprints(squadId: string): Observable<ApiSprintResponse[]> {
    return this.http.get<ApiSprintResponse[]>(`${this.base}/v1/jira/sprints/squad/${squadId}/closed`).pipe(
      catchError(err => this.handleError<ApiSprintResponse[]>(err, [])),
    );
  }

  /** Déclencher sync Jira pour une escouade */
  syncSquad(squadId: string): Observable<ApiSyncResultResponse | null> {
    return this.http.post<ApiSyncResultResponse>(`${this.base}/v1/jira/sync/squad/${squadId}`, {}).pipe(
      catchError(err => this.handleError<ApiSyncResultResponse | null>(err, null)),
    );
  }

  /** Sync toutes les escouades */
  syncAll(): Observable<ApiSyncResultResponse[]> {
    return this.http.post<ApiSyncResultResponse[]>(`${this.base}/v1/jira/sync/all`, {}).pipe(
      catchError(err => this.handleError<ApiSyncResultResponse[]>(err, [])),
    );
  }

  /** Statut Jira */
  getJiraStatus(): Observable<ApiJiraStatusResponse | null> {
    return this.http.get<ApiJiraStatusResponse>(`${this.base}/v1/jira/status`).pipe(
      catchError(err => this.handleError<ApiJiraStatusResponse | null>(err, null)),
    );
  }

  /** Sync board via squad controller (peut déclencher GHA) */
  syncBoardToBackend(squadId: string, boardId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.base}/v1/squads/${squadId}/sync`, { boardId }).pipe(
      catchError(err => this.handleError<{ message: string }>(err, { message: 'Erreur de sync' })),
    );
  }

  // ═══════════════════════════════════════════
  // Sprint data → tickets mapping
  // (Convertit les issues Jira en tickets pour le sprint panel)
  // ═══════════════════════════════════════════

  /**
   * Charge les tickets sprint pour une app en combinant les squads et leurs sprints actifs.
   * Retourne les tickets groupés par statut : notStarted, inProgress, inPR
   */
  loadSprintTicketsForApp(appId: string, squads: Squad[]): Observable<{
    notStarted: SprintTicket[];
    inProgress: InProgressTicket[];
    inPR: PullRequestTicket[];
    squads: Squad[];
  }> {
    if (!squads.length) {
      return of({ notStarted: [], inProgress: [], inPR: [], squads: [] });
    }

    const calls = squads.map(sq =>
      this.loadActiveSprint(sq.id).pipe(map(sprint => ({ squad: sq, sprint })))
    );

    return forkJoin(calls).pipe(
      map(results => {
        const notStarted: SprintTicket[] = [];
        const inProgress: InProgressTicket[] = [];
        const inPR: PullRequestTicket[] = [];

        for (const { squad, sprint } of results) {
          if (!sprint?.issues) continue;
          for (const issue of sprint.issues) {
            const base: SprintTicket = {
              ticket: issue.issueKey,
              title: issue.summary,
              squad: squad.id,
              storyPoints: issue.storyPoints || 0,
              priority: this.mapPriority(issue.priority),
              author: issue.assigneeName || 'Non assigné',
            };

            // Catégorisation par statusCategory Jira
            switch (issue.statusCategory?.toLowerCase()) {
              case 'new':
                notStarted.push(base);
                break;
              case 'indeterminate':
                // Si le statut contient "review" ou "PR" → traiter comme PR
                if (this.isReviewStatus(issue.statusName)) {
                  inPR.push({
                    ...base,
                    pr: '#—',
                    branch: `feat/${issue.issueKey.toLowerCase()}`,
                    checks: { ci: true, lint: true, tests: true, sonar: true },
                    comments: 0,
                    created: sprint.syncedAt ? this.timeAgo(sprint.syncedAt) : '—',
                  });
                } else {
                  inProgress.push({
                    ...base,
                    progress: this.estimateProgress(issue.statusName),
                    branch: `feat/${issue.issueKey.toLowerCase()}`,
                  });
                }
                break;
              case 'done':
                // Issues terminées → pas dans les panels sprint
                break;
              default:
                notStarted.push(base);
            }
          }
        }

        return { notStarted, inProgress, inPR, squads };
      }),
      catchError(() => of({ notStarted: [], inProgress: [], inPR: [], squads: [] })),
    );
  }

  // ═══════════════════════════════════════════
  // Mapping helpers
  // ═══════════════════════════════════════════

  private mapAppResponse(a: ApiAppResponse): AppConfig {
    const envs: Record<EnvKey, any> = {} as any;
    for (const ek of ['dev', 'qa', 'test', 'prod'] as EnvKey[]) {
      const env = a.envs?.[ek];
      envs[ek] = env ? {
        version: env.version || '',
        status: (env.status || 'deployed') as DeployStatus,
        lastDeploy: env.lastDeploy || '—',
        branch: env.branch || 'main',
        instances: env.instances || 0,
        uptime: env.uptime || '—',
      } : {
        version: '—', status: 'deployed' as DeployStatus,
        lastDeploy: '—', branch: 'main', instances: 0, uptime: '—',
      };
    }

    const commits: Record<string, Commit[]> = {};
    if (a.commits) {
      for (const [ver, list] of Object.entries(a.commits)) {
        commits[ver] = list.map(c => this.mapCommit(c));
      }
    }

    return {
      id: a.id,
      name: a.name,
      icon: a.icon || '📦',
      description: a.description || '',
      repo: a.repo || '',
      tech: (a.tech || 'spring') as TechType,
      color: a.color || '#00874E',
      envs,
      commits,
    };
  }

  private mapCommit(c: ApiCommitResponse): Commit {
    return {
      sha: c.sha,
      message: c.message,
      ticket: c.ticket,
      ticketTitle: c.ticketTitle,
      type: (c.type || 'chore') as CommitType,
      author: c.author,
      date: c.date,
    };
  }

  private mapPriority(p: string): Priority {
    if (!p) return 'medium';
    const lower = p.toLowerCase();
    if (lower.includes('critical') || lower.includes('blocker') || lower.includes('highest')) return 'critical';
    if (lower.includes('high') || lower.includes('major')) return 'high';
    if (lower.includes('low') || lower.includes('minor') || lower.includes('trivial')) return 'low';
    return 'medium';
  }

  private isReviewStatus(statusName: string): boolean {
    if (!statusName) return false;
    const lower = statusName.toLowerCase();
    return lower.includes('review') || lower.includes('pr')
        || lower.includes('pull request') || lower.includes('code review')
        || lower.includes('validation');
  }

  private estimateProgress(statusName: string): number {
    if (!statusName) return 30;
    const lower = statusName.toLowerCase();
    if (lower.includes('in progress') || lower.includes('en cours')) return 50;
    if (lower.includes('développement') || lower.includes('development')) return 40;
    if (lower.includes('testing') || lower.includes('test')) return 75;
    if (lower.includes('review') || lower.includes('validation')) return 85;
    return 30;
  }

  private timeAgo(isoDate: string): string {
    const now = Date.now();
    const then = new Date(isoDate).getTime();
    const diffMin = Math.floor((now - then) / 60000);
    if (diffMin < 1) return 'à l\'instant';
    if (diffMin < 60) return `il y a ${diffMin}min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `il y a ${diffH}h`;
    const diffD = Math.floor(diffH / 24);
    return `il y a ${diffD}j`;
  }

  // ═══════════════════════════════════════════
  // Error handling
  // ═══════════════════════════════════════════

  private handleError<T>(err: HttpErrorResponse, fallback: T): Observable<T> {
    console.warn('[DeployAPI]', err.status, err.message);
    this.loading$.next(false);

    if (err.status === 0 || err.status >= 500) {
      this.backendAvailable$.next(false);
      this.lastError$.next('Backend indisponible');
    } else {
      this.lastError$.next(err.error?.detail || err.message);
    }

    return of(fallback);
  }
}
