import { Component, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeployService } from '../../services/deploy.service';
import { PipelineComponent } from '../pipeline/pipeline.component';
import { SprintPanelComponent } from '../sprint-panel/sprint-panel.component';
import { TruncatePipe } from '../../pipes/truncate.pipe';
import {
  AppConfig, TabType,
  ENV_KEYS, PALETTE,
  getEnvMeta, getStatusMeta, getTechMeta,
} from '../../models/deploy.models';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [CommonModule, PipelineComponent, SprintPanelComponent, TruncatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app-card.component.html',
  styleUrls: ['./app-card.component.scss'],
})
export class AppCardComponent {
  @Input({ required: true }) app!: AppConfig;

  readonly envKeys = ENV_KEYS;
  readonly palette = PALETTE;

  readonly envMeta = getEnvMeta;
  readonly statusMeta = getStatusMeta;
  readonly techMeta = getTechMeta;

  constructor(readonly svc: DeployService, private cdr: ChangeDetectorRef) {}

  get isExpanded(): boolean { return this.svc.expandedAppId === this.app.id; }
  get activeTab(): TabType  { return this.svc.activeTab; }
  get isApiMode(): boolean  { return this.svc.dataSource === 'api'; }
  get isSprintLoading(): boolean { return this.svc.isSprintLoading(this.app.id); }

  get notStartedTickets()  { return this.svc.getNotStarted(this.app.id); }
  get inProgressTickets()  { return this.svc.getInProgress(this.app.id); }
  get pullRequestTickets() { return this.svc.getInPR(this.app.id); }

  get notStartedGroups()  { return this.svc.groupTicketsBySquad(this.notStartedTickets, this.app.id); }
  get inProgressGroups()  { return this.svc.groupTicketsBySquad(this.inProgressTickets, this.app.id); }
  get pullRequestGroups() { return this.svc.groupTicketsBySquad(this.pullRequestTickets, this.app.id); }

  toggleCard(): void {
    this.svc.toggleApp(this.app.id);
    // Forcer refresh quand on ouvre une card en mode API
    if (this.svc.expandedAppId === this.app.id && this.isApiMode) {
      setTimeout(() => this.cdr.markForCheck(), 500);
    }
  }

  switchTab(tab: TabType, event: Event): void {
    event.stopPropagation();
    this.svc.switchTab(tab);
    if (tab === 'sprint' && this.isApiMode) {
      setTimeout(() => this.cdr.markForCheck(), 500);
    }
  }

  refreshApp(event: Event): void {
    event.stopPropagation();
    this.svc.refreshApp(this.app.id);
  }
}
