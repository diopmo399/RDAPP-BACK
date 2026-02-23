import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { DeployApiService } from './deploy-api.service';
import {
  AppConfig, EnvKey, Commit, Squad, SprintTicket,
  InProgressTicket, PullRequestTicket, SquadGroup,
  TabType, ENV_KEYS, PALETTE,
} from '../models/deploy.models';
import { APPS, SQUADS, NOT_STARTED, IN_PROGRESS, IN_PR } from '../data/apps.data';

export type DataSource = 'api' | 'mock' | 'loading';

@Injectable({ providedIn: 'root' })
export class DeployService {

  constructor(private api: DeployApiService) {}

  // ── Source de données ─────────────────────
  dataSource: DataSource = 'loading';
  appsLoaded = false;

  // ── Données ───────────────────────────────
  private _apps: AppConfig[] = [];
  get apps(): readonly AppConfig[] { return this._apps; }

  // ── Données sprint par app (depuis API) ───
  private _sprintData: Record<string, {
    notStarted: SprintTicket[];
    inProgress: InProgressTicket[];
    inPR: PullRequestTicket[];
    squads: Squad[];
  }> = {};
  sprintLoading: Record<string, boolean> = {};

  // ── État UI ───────────────────────────────
  expandedAppId: string | null = null;
  activeTab: TabType = 'pipeline';
  selectedEnv: EnvKey | null = null;
  expandedVersion: string | null = null;

  // ── Observable pour refresh ───────────────
  readonly apps$ = new BehaviorSubject<readonly AppConfig[]>([]);
  readonly dataSource$ = new BehaviorSubject<DataSource>('loading');

  // ═══════════════════════════════════════════
  // Initialisation — appelé par le dashboard
  // ═══════════════════════════════════════════

  init(): void {
    if (this.appsLoaded) return;
    this.dataSource = 'loading';
    this.dataSource$.next('loading');

    this.api.loadApps().subscribe({
      next: (apps) => {
        if (apps.length > 0) {
          this._apps = apps;
          this.dataSource = 'api';
          console.log('[Deploy] ✓ Données chargées depuis le backend API —', apps.length, 'apps');
        } else {
          // Fallback données mock
          this._apps = [...APPS];
          this.dataSource = 'mock';
          console.log('[Deploy] ⚠ Backend vide ou indisponible — données mock utilisées');
        }
        this.appsLoaded = true;
        this.apps$.next(this._apps);
        this.dataSource$.next(this.dataSource);
      },
      error: () => {
        this._apps = [...APPS];
        this.dataSource = 'mock';
        this.appsLoaded = true;
        this.apps$.next(this._apps);
        this.dataSource$.next(this.dataSource);
        console.log('[Deploy] ⚠ Erreur API — fallback mock data');
      },
    });
  }

  /** Force un rechargement depuis l'API */
  refresh(): void {
    this.appsLoaded = false;
    this.init();
  }

  /** Recharge une seule app depuis l'API */
  refreshApp(appId: string): void {
    this.api.loadApp(appId).subscribe(app => {
      if (!app) return;
      const idx = this._apps.findIndex(a => a.id === appId);
      if (idx !== -1) {
        this._apps[idx] = app;
        this.apps$.next(this._apps);
      }
    });
  }

  // ═══════════════════════════════════════════
  // Compteurs globaux
  // ═══════════════════════════════════════════

  get totalDeployed(): number {
    let n = 0;
    for (const app of this._apps)
      for (const ek of ENV_KEYS)
        if (app.envs[ek]?.status === 'deployed') n++;
    return n;
  }

  get totalNotStarted(): number {
    if (this.dataSource === 'api') {
      return Object.values(this._sprintData).reduce((n, d) => n + d.notStarted.length, 0);
    }
    return this.countAll(NOT_STARTED);
  }

  get totalInProgress(): number {
    if (this.dataSource === 'api') {
      return Object.values(this._sprintData).reduce((n, d) => n + d.inProgress.length, 0);
    }
    return this.countAll(IN_PROGRESS);
  }

  get totalInPR(): number {
    if (this.dataSource === 'api') {
      return Object.values(this._sprintData).reduce((n, d) => n + d.inPR.length, 0);
    }
    return this.countAll(IN_PR);
  }

  // ═══════════════════════════════════════════
  // Actions UI
  // ═══════════════════════════════════════════

  toggleApp(appId: string): void {
    this.expandedAppId = this.expandedAppId === appId ? null : appId;
    this.activeTab = 'pipeline';
    this.selectedEnv = null;
    this.expandedVersion = null;

    // Si on ouvre une app et qu'on est en mode API, charger les sprint data
    if (this.expandedAppId && this.dataSource === 'api') {
      this.loadSprintDataForApp(appId);
    }
  }

  switchTab(tab: TabType): void {
    this.activeTab = tab;
    this.selectedEnv = null;
    this.expandedVersion = null;

    // Charger les sprint data quand on switch sur sprint tab
    if (tab === 'sprint' && this.expandedAppId && this.dataSource === 'api') {
      this.loadSprintDataForApp(this.expandedAppId);
    }
  }

  selectEnv(envKey: EnvKey): void {
    this.selectedEnv = this.selectedEnv === envKey ? null : envKey;
    this.expandedVersion = null;
  }

  toggleVersion(version: string): void {
    this.expandedVersion = this.expandedVersion === version ? null : version;
  }

  // ═══════════════════════════════════════════
  // Sprint data — chargement API
  // ═══════════════════════════════════════════

  loadSprintDataForApp(appId: string): void {
    if (this._sprintData[appId] || this.sprintLoading[appId]) return;

    // Charger les squads depuis l'API, puis les sprints
    this.sprintLoading[appId] = true;

    this.api.loadSquads().subscribe(apiSquads => {
      // Trouver les squads liées à cette app (par convention de nommage ou config)
      // Pour l'instant, on utilise les squads existantes de l'app
      const appSquads = this.getSquads(appId);

      if (appSquads.length === 0) {
        this._sprintData[appId] = { notStarted: [], inProgress: [], inPR: [], squads: [] };
        this.sprintLoading[appId] = false;
        return;
      }

      this.api.loadSprintTicketsForApp(appId, [...appSquads]).subscribe(data => {
        this._sprintData[appId] = data;
        this.sprintLoading[appId] = false;
      });
    });
  }

  // ═══════════════════════════════════════════
  // Accès données sprint (API ou mock)
  // ═══════════════════════════════════════════

  getNotStarted(appId: string): readonly SprintTicket[] {
    if (this.dataSource === 'api' && this._sprintData[appId]) {
      return this._sprintData[appId].notStarted;
    }
    return NOT_STARTED[appId] ?? [];
  }

  getInProgress(appId: string): readonly InProgressTicket[] {
    if (this.dataSource === 'api' && this._sprintData[appId]) {
      return this._sprintData[appId].inProgress;
    }
    return IN_PROGRESS[appId] ?? [];
  }

  getInPR(appId: string): readonly PullRequestTicket[] {
    if (this.dataSource === 'api' && this._sprintData[appId]) {
      return this._sprintData[appId].inPR;
    }
    return IN_PR[appId] ?? [];
  }

  getSquads(appId: string): readonly Squad[] {
    if (this.dataSource === 'api' && this._sprintData[appId]?.squads?.length) {
      return this._sprintData[appId].squads;
    }
    return SQUADS[appId] ?? [];
  }

  isSprintLoading(appId: string): boolean {
    return !!this.sprintLoading[appId];
  }

  // ═══════════════════════════════════════════
  // Pipeline : versions
  // ═══════════════════════════════════════════

  getVersionsForEnv(app: AppConfig, envKey: EnvKey): string[] {
    if (!app.commits) return [];
    const versions = Object.keys(app.commits);
    const envVer = app.envs[envKey]?.version || '';
    const idx = versions.findIndex(v => envVer.includes(v.replace(/-.*/, '')) || v === envVer);
    return idx !== -1 ? versions.slice(idx) : versions;
  }

  getEnvsContainingVersion(app: AppConfig, version: string): EnvKey[] {
    if (!app.commits) return [];
    const versions = Object.keys(app.commits);
    const vIdx = versions.indexOf(version);
    return ENV_KEYS.filter(ek => {
      const envVer = app.envs[ek]?.version || '';
      const eIdx = versions.findIndex(v => envVer.includes(v.replace(/-.*/, '')) || v === envVer);
      return eIdx !== -1 && eIdx <= vIdx;
    });
  }

  isVersionExpanded(version: string, index: number): boolean {
    return this.expandedVersion ? this.expandedVersion === version : index === 0;
  }

  getCommitTypeStats(commits: readonly Commit[]): { type: string; count: number }[] {
    const map: Record<string, number> = {};
    for (const c of commits) map[c.type] = (map[c.type] || 0) + 1;
    return Object.entries(map).map(([type, count]) => ({ type, count }));
  }

  // ═══════════════════════════════════════════
  // Sprint : helpers
  // ═══════════════════════════════════════════

  groupTicketsBySquad<T extends SprintTicket>(tickets: readonly T[], appId: string): SquadGroup<T>[] {
    const squads = this.getSquads(appId);
    const grouped: Record<string, { squad: Squad; tickets: T[] }> = {};
    for (const sq of squads) grouped[sq.id] = { squad: sq, tickets: [] };
    for (const t of tickets) grouped[t.squad]?.tickets.push(t);
    return Object.values(grouped).filter(g => g.tickets.length > 0);
  }

  sumStoryPoints(tickets: readonly SprintTicket[]): number {
    return tickets.reduce((sum, t) => sum + t.storyPoints, 0);
  }

  getProgressColor(progress: number): string {
    if (progress > 70) return PALETTE.green;
    if (progress > 40) return PALETTE.gold;
    return PALETTE.teal;
  }

  areAllChecksPassing(checks: Record<string, boolean>): boolean {
    return Object.values(checks).every(Boolean);
  }

  // ═══════════════════════════════════════════
  // Sync actions (API)
  // ═══════════════════════════════════════════

  /** Sync Jira pour une escouade */
  triggerSync(squadId: string): void {
    this.api.syncSquad(squadId).subscribe(result => {
      if (result?.error) {
        console.warn('[Sync] Erreur:', result.error);
      } else {
        console.log('[Sync] ✓', result?.squadName, '— sprint:', result?.activeSprint?.name);
        // Rafraîchir les données sprint pour l'app ouverte
        if (this.expandedAppId) {
          delete this._sprintData[this.expandedAppId];
          this.sprintLoading[this.expandedAppId] = false;
          this.loadSprintDataForApp(this.expandedAppId);
        }
      }
    });
  }

  /** Sync toutes les escouades */
  triggerSyncAll(): void {
    this.api.syncAll().subscribe(results => {
      console.log('[Sync] ✓ All squads synced —', results.length, 'résultats');
      // Refresh sprint data for expanded app
      if (this.expandedAppId) {
        delete this._sprintData[this.expandedAppId];
        this.sprintLoading[this.expandedAppId] = false;
        this.loadSprintDataForApp(this.expandedAppId);
      }
    });
  }

  // ═══════════════════════════════════════════
  // Privé
  // ═══════════════════════════════════════════

  private countAll(map: Record<string, readonly unknown[]>): number {
    let n = 0;
    for (const arr of Object.values(map)) n += arr.length;
    return n;
  }
}
