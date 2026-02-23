import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeployService } from '../../services/deploy.service';
import { ENV_KEYS, PALETTE, getEnvMeta } from '../../models/deploy.models';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header>
      <div class="top">
        <div class="left">
          <svg width="32" height="32" viewBox="0 0 100 100">
            <polygon points="50,3 97,25 97,75 50,97 3,75 3,25" fill="#00874E"/>
          </svg>
          <div>
            <h1>Déploiements &amp; Sprint</h1>
            <p class="sub">{{ svc.apps.length }} applications · Sprint 24.04</p>
          </div>
        </div>
        <div class="stats">
          <div class="stat" *ngFor="let s of stats" [style.border-color]="s.color + '20'">
            <span class="num" [style.color]="s.color">{{ s.value }}</span>
            <span class="label">{{ s.label }}</span>
          </div>
        </div>
      </div>
      <div class="legend">
        <div class="env-item" *ngFor="let k of envKeys">
          <span class="dot" [style.background]="envMeta(k).color"></span>
          <span class="tag" [style.color]="envMeta(k).color">{{ envMeta(k).label }}</span>
          <span class="full">{{ envMeta(k).fullName }}</span>
        </div>
      </div>
    </header>
  `,
  styles: [`
    :host { display: block; }
    header { padding: 20px 24px 14px; border-bottom: 1px solid #E2E8F0; background: #fff; }
    .top { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 14px; }
    .left { display: flex; align-items: center; gap: 12px; }
    h1 { font: 800 18px/1.2 'JetBrains Mono', monospace; letter-spacing: -.02em; margin: 0; }
    .sub { margin-top: 2px; font-size: 11px; color: #64748B; font-weight: 500; }
    .stats { display: flex; gap: 8px; flex-wrap: wrap; }
    .stat { padding: 8px 12px; border-radius: 10px; border: 1px solid; text-align: center; background: #fff; }
    .num { display: block; font: 800 18px 'JetBrains Mono', monospace; }
    .label { display: block; font-size: 7px; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; color: #64748B; }
    .legend { display: flex; gap: 16px; margin-top: 10px; flex-wrap: wrap; }
    .env-item { display: flex; align-items: center; gap: 5px; }
    .dot { width: 8px; height: 8px; border-radius: 2px; }
    .tag { font: 700 10px 'JetBrains Mono', monospace; letter-spacing: .06em; }
    .full { font-size: 9px; color: #64748B; }
  `],
})
export class HeaderComponent {
  readonly envKeys = ENV_KEYS;
  readonly envMeta = getEnvMeta;

  constructor(readonly svc: DeployService) {}

  get stats() {
    return [
      { value: this.svc.totalDeployed,   label: 'Déployés',    color: PALETTE.green },
      { value: this.svc.totalInProgress, label: 'En cours',    color: PALETTE.orange },
      { value: this.svc.totalNotStarted, label: 'Non débutés', color: PALETTE.red },
      { value: this.svc.totalInPR,       label: 'En PR',       color: PALETTE.teal },
    ];
  }
}
