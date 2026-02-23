import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AffectVersion, AffectVersionCreate, AffectVersionUpdate,
  SquadConfig, SquadCreate, SquadUpdate,
  SquadMember, MemberCreate, MemberUpdate,
  BoardSyncRequest, BoardSyncResponse,
  JiraSprintResponse, JiraSyncResult, JiraStatusResponse,
} from '../models/deploy.models';

@Injectable({ providedIn: 'root' })
export class ConfigApiService {

  private readonly base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  // ══════════════════════════════════════════
  // Affect Versions
  // ══════════════════════════════════════════

  getVersions(): Observable<AffectVersion[]> {
    return this.http.get<AffectVersion[]>(`${this.base}/versions`);
  }

  createVersion(dto: AffectVersionCreate): Observable<AffectVersion> {
    return this.http.post<AffectVersion>(`${this.base}/versions`, dto);
  }

  updateVersion(id: string, dto: AffectVersionUpdate): Observable<AffectVersion> {
    return this.http.patch<AffectVersion>(`${this.base}/versions/${id}`, dto);
  }

  deleteVersion(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/versions/${id}`);
  }

  // ══════════════════════════════════════════
  // Squads
  // ══════════════════════════════════════════

  getSquads(): Observable<SquadConfig[]> {
    return this.http.get<SquadConfig[]>(`${this.base}/squads`);
  }

  createSquad(dto: SquadCreate): Observable<SquadConfig> {
    return this.http.post<SquadConfig>(`${this.base}/squads`, dto);
  }

  updateSquad(id: string, dto: SquadUpdate): Observable<SquadConfig> {
    return this.http.patch<SquadConfig>(`${this.base}/squads/${id}`, dto);
  }

  deleteSquad(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/squads/${id}`);
  }

  // ══════════════════════════════════════════
  // Members
  // ══════════════════════════════════════════

  addMember(squadId: string, dto: MemberCreate): Observable<SquadMember> {
    return this.http.post<SquadMember>(`${this.base}/squads/${squadId}/members`, dto);
  }

  updateMember(squadId: string, memberId: string, dto: MemberUpdate): Observable<SquadMember> {
    return this.http.patch<SquadMember>(`${this.base}/squads/${squadId}/members/${memberId}`, dto);
  }

  removeMember(squadId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/squads/${squadId}/members/${memberId}`);
  }

  // ══════════════════════════════════════════
  // Board Sync
  // ══════════════════════════════════════════

  syncBoard(squadId: string, request: BoardSyncRequest): Observable<BoardSyncResponse> {
    return this.http.post<BoardSyncResponse>(`${this.base}/squads/${squadId}/sync`, request);
  }

  // ══════════════════════════════════════════
  // Jira Sync
  // ══════════════════════════════════════════

  /** Trigger Jira sync pour une escouade (GHA dispatch ou direct) */
  syncJiraSquad(squadId: string): Observable<JiraSyncResult> {
    return this.http.post<JiraSyncResult>(`${this.base}/jira/sync/squad/${squadId}`, {});
  }

  /** Trigger Jira sync pour toutes les escouades */
  syncJiraAll(): Observable<JiraSyncResult[]> {
    return this.http.post<JiraSyncResult[]>(`${this.base}/jira/sync/all`, {});
  }

  /** Sync Affect Versions depuis Jira project */
  syncJiraVersions(projectKey: string): Observable<{ projectKey: string; totalFromJira: number; created: number; updated: number }> {
    return this.http.post<any>(`${this.base}/jira/sync/versions/${projectKey}`, {});
  }

  // ══════════════════════════════════════════
  // Jira Sprint Data (read)
  // ══════════════════════════════════════════

  /** Sprint actif d'une escouade (avec issues) */
  getActiveSprint(squadId: string): Observable<JiraSprintResponse | null> {
    return this.http.get<JiraSprintResponse | null>(`${this.base}/jira/sprints/squad/${squadId}/active`);
  }

  /** Sprints fermés d'une escouade */
  getClosedSprints(squadId: string): Observable<JiraSprintResponse[]> {
    return this.http.get<JiraSprintResponse[]>(`${this.base}/jira/sprints/squad/${squadId}/closed`);
  }

  /** Tous les sprints d'une escouade */
  getAllSprints(squadId: string): Observable<JiraSprintResponse[]> {
    return this.http.get<JiraSprintResponse[]>(`${this.base}/jira/sprints/squad/${squadId}`);
  }

  // ══════════════════════════════════════════
  // Jira Status
  // ══════════════════════════════════════════

  getJiraStatus(): Observable<JiraStatusResponse> {
    return this.http.get<JiraStatusResponse>(`${this.base}/jira/status`);
  }
}
