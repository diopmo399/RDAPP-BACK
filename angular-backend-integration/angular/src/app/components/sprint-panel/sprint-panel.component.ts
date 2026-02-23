import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  SprintTicket, InProgressTicket, PullRequestTicket,
  SquadGroup, PALETTE, JIRA_BASE_URL, GITHUB_BASE_URL,
  getPriorityMeta,
} from '../../models/deploy.models';
import { DeployService } from '../../services/deploy.service';

@Component({
  selector: 'app-sprint-panel',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sprint-panel.component.html',
  styleUrls: ['./sprint-panel.component.scss'],
})
export class SprintPanelComponent {
  @Input() panelTitle = '';
  @Input() icon = '';
  @Input() accentColor = '';
  @Input() groups: readonly SquadGroup<any>[] = [];
  @Input() tickets: readonly SprintTicket[] = [];
  @Input() appColor = '';
  @Input() appRepo = '';
  @Input() mode: 'ns' | 'ip' | 'pr' = 'ns';

  readonly palette = PALETTE;
  readonly jiraUrl = JIRA_BASE_URL;
  readonly githubUrl = GITHUB_BASE_URL;
  readonly priorityMeta = getPriorityMeta;

  constructor(readonly svc: DeployService) {}

  get totalPoints(): number {
    return this.svc.sumStoryPoints(this.tickets);
  }

  asInProgress(ticket: SprintTicket): InProgressTicket {
    return ticket as InProgressTicket;
  }

  asPullRequest(ticket: SprintTicket): PullRequestTicket {
    return ticket as PullRequestTicket;
  }

  checksToArray(checks: Record<string, boolean>): { name: string; pass: boolean }[] {
    return Object.entries(checks).map(([name, pass]) => ({ name, pass }));
  }

  trackBySquadId(_: number, group: SquadGroup): string { return group.squad.id; }
  trackByTicketId(_: number, ticket: SprintTicket): string { return ticket.ticket; }
}
