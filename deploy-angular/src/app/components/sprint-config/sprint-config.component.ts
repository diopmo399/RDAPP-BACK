import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, timer } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil, switchMap, tap } from 'rxjs/operators';
import {
  AffectVersion, SquadConfig, SquadMember, MemberRole, VersionStatus,
  VERSION_STATUS_CONFIG, ROLE_CONFIG, PALETTE,
  JiraSprintResponse, JiraSprintIssue, JiraStatusResponse,
  ISSUE_STATUS_COLORS, PRIORITY_ICONS, JIRA_BASE_URL,
} from '../../models/deploy.models';
import { DeployService } from '../../services/deploy.service';

@Component({
  selector: 'app-sprint-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sprint-config.component.html',
  styleUrls: ['./sprint-config.component.scss'],
})
export class SprintConfigComponent implements OnInit, OnDestroy {

  readonly VS = VERSION_STATUS_CONFIG;
  readonly RC = ROLE_CONFIG;
  readonly P = PALETTE;
  readonly ISC = ISSUE_STATUS_COLORS;
  readonly PI = PRIORITY_ICONS;
  readonly JIRA_URL = JIRA_BASE_URL;

  // UI state
  showJiraStatus = true;
  showVersions = true;
  showSquads = true;
  expandedSquad: string | null = null;
  showSprintIssues: Record<string, boolean> = {};
  showClosedSprints: Record<string, boolean> = {};
  verPage = 1;
  perPage = 3;
  loading = false;
  syncingAll = false;

  // GHA async polling state
  ghaPending: Record<string, boolean> = {};        // squad ID → GHA triggered, waiting
  ghaPollingCount: Record<string, number> = {};     // squad ID → poll attempts
  ghaLastSyncAt: Record<string, string | null> = {}; // squad ID → syncedAt before trigger
  readonly GHA_POLL_INTERVAL = 8000;  // 8s between polls
  readonly GHA_MAX_POLLS = 30;        // ~4 minutes max

  // Version form
  newVerName = '';
  newVerStatus: VersionStatus = 'planned';
  newVerDate = '';
  newVerDesc = '';

  // Squad form
  newSquadName = '';
  newSquadBoard = '';

  // Member form (per squad)
  newMemberName: Record<string, string> = {};
  newMemberRole: Record<string, MemberRole> = {};
  newMemberCode: Record<string, string> = {};
  newMemberUsername: Record<string, string> = {};
  newMemberGithub: Record<string, string> = {};

  // Board sync state
  syncingBoard: Record<string, 'idle' | 'syncing' | 'gha-pending' | 'synced' | 'error'> = {};
  syncMessage: Record<string, string> = {};

  // Board ID debounce
  private boardIdChange$ = new Subject<{ squadId: string; boardId: string }>();
  private destroy$ = new Subject<void>();

  // Saving state per entity
  saving: Record<string, boolean> = {};

  constructor(readonly svc: DeployService, private cdr: ChangeDetectorRef) {}

  async ngOnInit(): Promise<void> {
    this.boardIdChange$.pipe(
      debounceTime(600),
      distinctUntilChanged((a, b) => a.squadId === b.squadId && a.boardId === b.boardId),
      takeUntil(this.destroy$),
    ).subscribe(async ({ squadId, boardId }) => {
      await this.svc.updateSquadBoardId(squadId, boardId);
      this.cdr.markForCheck();
    });

    this.loading = true;
    this.cdr.markForCheck();

    await Promise.all([
      this.svc.loadConfig(),
      this.svc.loadJiraStatus(),
    ]);

    // Charger les sprints pour toutes les escouades avec boardId
    for (const sq of this.svc.squadConfigs) {
      if (sq.boardId) {
        this.svc.loadSprintForSquad(sq.id).then(() => this.cdr.markForCheck());
      }
    }

    this.loading = false;
    this.cdr.markForCheck();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Jira Status ──

  get jiraStatus(): JiraStatusResponse | null { return this.svc.jiraStatus; }

  // ── Versions ──

  get allVersions(): AffectVersion[] { return this.svc.allVersionsSorted; }
  get verTotalPages(): number { return Math.ceil(this.allVersions.length / this.perPage); }
  get verSlice(): AffectVersion[] {
    if (this.verPage > this.verTotalPages) this.verPage = Math.max(1, this.verTotalPages);
    return this.allVersions.slice((this.verPage - 1) * this.perPage, this.verPage * this.perPage);
  }
  get verPages(): number[] { return Array.from({ length: this.verTotalPages }, (_, i) => i + 1); }

  isPastVersion(v: AffectVersion): boolean {
    return v.status === 'released' || v.status === 'archived';
  }

  async updateVersionStatus(v: AffectVersion, status: VersionStatus): Promise<void> {
    this.saving[v.id] = true; this.cdr.markForCheck();
    await this.svc.updateAffectVersion(v.id, { status });
    this.saving[v.id] = false; this.cdr.markForCheck();
  }

  async updateVersionDate(v: AffectVersion, date: string): Promise<void> {
    await this.svc.updateAffectVersion(v.id, { releaseDate: date });
    this.cdr.markForCheck();
  }

  async removeVersion(id: string): Promise<void> {
    this.saving[id] = true; this.cdr.markForCheck();
    await this.svc.removeAffectVersion(id);
    delete this.saving[id]; this.cdr.markForCheck();
  }

  async addVersion(): Promise<void> {
    if (!this.newVerName.trim()) return;
    this.saving['new-ver'] = true; this.cdr.markForCheck();
    await this.svc.addAffectVersion(this.newVerName.trim(), this.newVerStatus, this.newVerDate, this.newVerDesc);
    this.newVerName = ''; this.newVerDesc = ''; this.newVerDate = '';
    this.saving['new-ver'] = false; this.cdr.markForCheck();
  }

  // ── Squads ──

  get totalMembers(): number {
    return this.svc.squadConfigs.reduce((sum, sq) => sum + sq.members.length, 0);
  }

  async toggleSquad(id: string): Promise<void> {
    this.expandedSquad = this.expandedSquad === id ? null : id;
    if (this.expandedSquad === id && !this.svc.activeSprintBySquad[id] && !this.svc.sprintLoading[id]) {
      await this.loadSprintData(id);
    }
  }

  async loadSprintData(squadId: string): Promise<void> {
    this.cdr.markForCheck();
    await this.svc.loadSprintForSquad(squadId);
    this.cdr.markForCheck();
  }

  async removeSquad(id: string): Promise<void> {
    this.saving[id] = true; this.cdr.markForCheck();
    await this.svc.removeSquad(id);
    delete this.saving[id]; this.cdr.markForCheck();
  }

  async addSquad(): Promise<void> {
    if (!this.newSquadName.trim()) return;
    this.saving['new-sq'] = true; this.cdr.markForCheck();
    await this.svc.addSquad(this.newSquadName.trim(), this.newSquadBoard.trim());
    this.newSquadName = ''; this.newSquadBoard = '';
    this.saving['new-sq'] = false; this.cdr.markForCheck();
  }

  onBoardIdChange(squadId: string, boardId: string): void {
    const sq = this.svc.squadConfigs.find(s => s.id === squadId);
    if (sq) sq.boardId = boardId;
    this.boardIdChange$.next({ squadId, boardId });
  }

  // ── Members ──

  initials(name: string): string {
    return name.split(/[\s.-]+/).filter(w => w.length > 0).map(w => w[0].toUpperCase()).join('').slice(0, 2);
  }

  async addMember(squadId: string): Promise<void> {
    const name = (this.newMemberName[squadId] || '').trim();
    if (!name) return;
    this.saving['new-m-' + squadId] = true; this.cdr.markForCheck();
    await this.svc.addMember(squadId, {
      name,
      role: this.newMemberRole[squadId] || 'dev',
      employeeCode: (this.newMemberCode[squadId] || '').trim(),
      username: (this.newMemberUsername[squadId] || '').trim(),
      github: (this.newMemberGithub[squadId] || '').trim(),
    });
    this.newMemberName[squadId] = '';
    this.newMemberCode[squadId] = '';
    this.newMemberUsername[squadId] = '';
    this.newMemberGithub[squadId] = '';
    this.saving['new-m-' + squadId] = false; this.cdr.markForCheck();
  }

  async removeMember(squadId: string, memberId: string): Promise<void> {
    this.saving[memberId] = true; this.cdr.markForCheck();
    await this.svc.removeMember(squadId, memberId);
    delete this.saving[memberId]; this.cdr.markForCheck();
  }

  // ══════════════════════════════════════════
  // Board Sync — GHA dispatch + polling
  // ══════════════════════════════════════════

  async syncBoard(squadId: string): Promise<void> {
    this.syncingBoard[squadId] = 'syncing';
    this.syncMessage[squadId] = '';
    this.cdr.markForCheck();

    // Capturer le syncedAt actuel pour détecter les nouvelles données
    const currentSprint = this.svc.activeSprintBySquad[squadId];
    this.ghaLastSyncAt[squadId] = currentSprint?.syncedAt ?? null;

    try {
      const result = await this.svc.syncBoardToBackend(squadId);

      // Détecter si c'est un trigger GHA (async) vs sync direct
      const isGha = result.message?.includes('GitHub Actions') || result.message?.includes('async');

      if (isGha && result.success) {
        // GHA lancé → polling async
        this.syncingBoard[squadId] = 'gha-pending';
        this.syncMessage[squadId] = result.message;
        this.ghaPending[squadId] = true;
        this.ghaPollingCount[squadId] = 0;
        this.startPolling(squadId);
      } else if (result.success) {
        // Sync direct réussi
        this.syncingBoard[squadId] = 'synced';
        this.syncMessage[squadId] = result.message;
        await this.loadSprintData(squadId);
        this.autoResetState(squadId, 5000);
      } else {
        this.syncingBoard[squadId] = 'error';
        this.syncMessage[squadId] = result.message;
        this.autoResetState(squadId, 5000);
      }
    } catch {
      this.syncingBoard[squadId] = 'error';
      this.syncMessage[squadId] = 'Erreur de connexion';
      this.autoResetState(squadId, 5000);
    }
    this.cdr.markForCheck();
  }

  private startPolling(squadId: string): void {
    const pollFn = async () => {
      if (!this.ghaPending[squadId]) return;
      this.ghaPollingCount[squadId]++;

      await this.svc.loadSprintForSquad(squadId);
      const newSprint = this.svc.activeSprintBySquad[squadId];
      const oldSyncAt = this.ghaLastSyncAt[squadId];

      // Vérifier si de nouvelles données sont arrivées
      const hasNewData = newSprint?.syncedAt && newSprint.syncedAt !== oldSyncAt;

      if (hasNewData) {
        // GHA terminé, nouvelles données reçues!
        this.ghaPending[squadId] = false;
        this.syncingBoard[squadId] = 'synced';
        this.syncMessage[squadId] = `Synchronisé — ${newSprint!.name} (${newSprint!.doneIssues}/${newSprint!.totalIssues} issues)`;
        this.autoResetState(squadId, 6000);
        this.cdr.markForCheck();
        return;
      }

      if (this.ghaPollingCount[squadId] >= this.GHA_MAX_POLLS) {
        // Timeout
        this.ghaPending[squadId] = false;
        this.syncingBoard[squadId] = 'idle';
        this.syncMessage[squadId] = 'Sync GHA en cours... rafraîchir manuellement';
        this.cdr.markForCheck();
        return;
      }

      // Continuer le polling
      this.cdr.markForCheck();
      setTimeout(() => pollFn(), this.GHA_POLL_INTERVAL);
    };

    // Premier poll après un délai (laisser GHA démarrer)
    setTimeout(() => pollFn(), 5000);
  }

  async syncAllSquads(): Promise<void> {
    this.syncingAll = true;
    this.cdr.markForCheck();
    await this.svc.triggerJiraSyncAll();

    // Marquer toutes les escouades comme GHA pending
    for (const sq of this.svc.squadConfigs) {
      if (sq.boardId) {
        this.ghaPending[sq.id] = true;
        this.ghaPollingCount[sq.id] = 0;
        this.ghaLastSyncAt[sq.id] = this.svc.activeSprintBySquad[sq.id]?.syncedAt ?? null;
        this.syncingBoard[sq.id] = 'gha-pending';
        this.startPolling(sq.id);
      }
    }

    this.syncingAll = false;
    this.cdr.markForCheck();
  }

  /** Rafraîchir manuellement les données sprint */
  async refreshSprintData(squadId: string): Promise<void> {
    await this.loadSprintData(squadId);
    // Si polling en cours et données fraîches → arrêter le poll
    const sprint = this.svc.activeSprintBySquad[squadId];
    if (sprint?.syncedAt && sprint.syncedAt !== this.ghaLastSyncAt[squadId]) {
      this.ghaPending[squadId] = false;
      this.syncingBoard[squadId] = 'synced';
      this.syncMessage[squadId] = `Synchronisé — ${sprint.name}`;
      this.autoResetState(squadId, 5000);
    }
    this.cdr.markForCheck();
  }

  private autoResetState(squadId: string, delay: number): void {
    setTimeout(() => {
      if (this.syncingBoard[squadId] !== 'gha-pending') {
        this.syncingBoard[squadId] = 'idle';
        this.syncMessage[squadId] = '';
        this.cdr.markForCheck();
      }
    }, delay);
  }

  getSyncState(squadId: string): string {
    return this.syncingBoard[squadId] || 'idle';
  }

  isGhaPending(squadId: string): boolean {
    return !!this.ghaPending[squadId];
  }

  getPollingCount(squadId: string): number {
    return this.ghaPollingCount[squadId] || 0;
  }

  get anyGhaPending(): boolean {
    return Object.values(this.ghaPending).some(v => v);
  }

  // ── Sprint helpers ──

  getActiveSprint(squadId: string): JiraSprintResponse | null {
    return this.svc.activeSprintBySquad[squadId] ?? null;
  }

  getClosedSprints(squadId: string): JiraSprintResponse[] {
    return this.svc.closedSprintsBySquad[squadId] ?? [];
  }

  isSprintLoading(squadId: string): boolean {
    return !!this.svc.sprintLoading[squadId];
  }

  issueStatusColor(cat: string): { bg: string; color: string } {
    return this.ISC[cat] || this.ISC['new'];
  }

  priorityIcon(p: string): string {
    return this.PI[p] || '➡️';
  }

  donePercent(sprint: JiraSprintResponse): number {
    return sprint.completionPercent ?? (sprint.totalIssues > 0 ? (sprint.doneIssues / sprint.totalIssues) * 100 : 0);
  }

  spPercent(sprint: JiraSprintResponse): number {
    return sprint.totalStoryPoints > 0 ? (sprint.doneStoryPoints / sprint.totalStoryPoints) * 100 : 0;
  }

  issueUrl(key: string): string {
    return `${this.JIRA_URL}/${key}`;
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    try {
      return new Date(iso).toLocaleDateString('fr-CA', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch { return iso; }
  }

  timeAgo(iso: string | null): string {
    if (!iso) return '';
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'à l\'instant';
    if (mins < 60) return `il y a ${mins}min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `il y a ${hours}h`;
    return `il y a ${Math.floor(hours / 24)}j`;
  }

  // ── Helpers ──

  isSaving(key: string): boolean { return !!this.saving[key]; }
  statusEntries() { return Object.entries(this.VS) as [VersionStatus, typeof this.VS[VersionStatus]][]; }
  roleEntries() { return Object.entries(this.RC) as [MemberRole, typeof this.RC[MemberRole]][]; }
}
