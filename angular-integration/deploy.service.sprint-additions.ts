// ══════════════════════════════════════════════════════
// ANGULAR INTEGRATION — Ajouter dans deploy.service.ts
// ══════════════════════════════════════════════════════
//
// Importer les types :
//   import { SprintGlobalResponse, SprintTicketDto, SquadDto } from './sprint.models';
//
// Ajouter dans DeployService :

/*

  // ── Sprint Signals ───────────────────────────────

  sprintData = signal<SprintGlobalResponse | null>(null);
  sprintLoading = signal(false);
  sprintError = signal<string | null>(null);

  // ── Computed ─────────────────────────────────────

  readonly sprintInfo = computed(() => this.sprintData()?.sprint ?? null);
  readonly sprintNotStarted = computed(() => this.sprintData()?.notStarted ?? []);
  readonly sprintInProgress = computed(() => this.sprintData()?.inProgress ?? []);
  readonly sprintDone = computed(() => this.sprintData()?.done ?? []);
  readonly sprintSquads = computed(() => this.sprintData()?.squads ?? {});
  readonly sprintTotalPoints = computed(() => this.sprintData()?.totalPoints ?? 0);
  readonly sprintDonePoints = computed(() => this.sprintData()?.donePoints ?? 0);
  readonly sprintLastSync = computed(() => this.sprintData()?.lastSync ?? null);

  // Tickets filtrés par app (pour le sprint panel dans une app card)
  sprintForApp(appId: string) {
    return computed(() => {
      const data = this.sprintData();
      if (!data) return { notStarted: [], inProgress: [], done: [] };
      return {
        notStarted: data.notStarted.filter(t => t.app === appId),
        inProgress: data.inProgress.filter(t => t.app === appId),
        done: data.done.filter(t => t.app === appId),
      };
    });
  }

  // ── Load Sprint ──────────────────────────────────

  loadSprint(): void {
    this.sprintLoading.set(true);
    this.sprintError.set(null);

    this.http.get<SprintGlobalResponse>(
      `${environment.apiUrl}/sprint/global`
    ).subscribe({
      next: data => {
        this.sprintData.set(data);
        this.sprintLoading.set(false);
      },
      error: err => {
        console.error('Failed to load sprint data', err);
        this.sprintError.set('Erreur chargement sprint');
        this.sprintLoading.set(false);
      },
    });
  }

  // ── Force Refresh ────────────────────────────────

  forceRefreshSprint(): void {
    this.http.post<void>(
      `${environment.apiUrl}/sprint/refresh`, {}
    ).subscribe({
      next: () => {
        // Wait a bit then reload
        setTimeout(() => this.loadSprint(), 1000);
      },
      error: err => console.error('Failed to force refresh', err),
    });
  }

  // ── Init (ajouter dans constructor) ──────────────
  //
  // constructor(private http: HttpClient) {
  //   this.loadPullRequests();
  //   this.loadSprint();           // ← AJOUTER
  //   this.startPRPolling();
  //   this.startSprintPolling();    // ← AJOUTER
  // }

  private sprintPollInterval: any;

  startSprintPolling(): void {
    this.sprintPollInterval = setInterval(() => {
      this.loadSprint();
    }, 300_000); // 5 min
  }

  stopSprintPolling(): void {
    if (this.sprintPollInterval) {
      clearInterval(this.sprintPollInterval);
    }
  }

*/
