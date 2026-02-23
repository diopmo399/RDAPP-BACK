import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeployService } from '../../services/deploy.service';
import {
  AppConfig, EnvKey, Commit,
  ENV_KEYS, PALETTE, JIRA_BASE_URL, GITHUB_BASE_URL,
  getEnvMeta, getStatusMeta, getCommitTypeMeta,
} from '../../models/deploy.models';
import { TruncatePipe } from '../../pipes/truncate.pipe';

@Component({
  selector: 'app-pipeline',
  standalone: true,
  imports: [CommonModule, TruncatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pipeline.component.html',
  styleUrls: ['./pipeline.component.scss'],
})
export class PipelineComponent {
  @Input({ required: true }) app!: AppConfig;

  readonly envKeys = ENV_KEYS;
  readonly palette = PALETTE;
  readonly jiraUrl = JIRA_BASE_URL;
  readonly githubUrl = GITHUB_BASE_URL;

  /** Lookups type-safe — fonctions importées du modèle. */
  readonly envMeta = getEnvMeta;
  readonly statusMeta = getStatusMeta;
  readonly commitTypeMeta = getCommitTypeMeta;

  constructor(readonly svc: DeployService) {}

  get selectedEnv(): EnvKey | null { return this.svc.selectedEnv; }

  hasSameVersionAsPrevious(index: number, envKey: EnvKey): boolean {
    return index > 0 && this.app.envs[ENV_KEYS[index - 1]].version === this.app.envs[envKey].version;
  }

  selectEnv(envKey: EnvKey, event: Event): void {
    event.stopPropagation();
    this.svc.selectEnv(envKey);
  }

  toggleVersion(version: string, event: Event): void {
    event.stopPropagation();
    this.svc.toggleVersion(version);
  }

  trackByEnvKey(_: number, envKey: EnvKey): string { return envKey; }
  trackByVersion(_: number, version: string): string { return version; }
  trackByCommitSha(_: number, commit: Commit): string { return commit.sha; }
}
