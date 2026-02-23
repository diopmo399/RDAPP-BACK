import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../header/header.component';
import { AppCardComponent } from '../app-card/app-card.component';
import { DeployService, DataSource } from '../../services/deploy.service';
import { DeployApiService } from '../../services/deploy-api.service';
import { AppConfig } from '../../models/deploy.models';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-deploy-dashboard',
  standalone: true,
  imports: [CommonModule, HeaderComponent, AppCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-header></app-header>

    <!-- Barre de source de données -->
    <div class="source-bar" [class.api]="dataSource === 'api'" [class.mock]="dataSource === 'mock'" [class.loading]="dataSource === 'loading'">
      <span class="source-dot"></span>
      <span *ngIf="dataSource === 'api'">✓ Connecté au backend API</span>
      <span *ngIf="dataSource === 'mock'">⚠ Données locales (backend indisponible)</span>
      <span *ngIf="dataSource === 'loading'">◉ Chargement...</span>
      <button *ngIf="dataSource !== 'loading'" class="refresh-btn" (click)="refresh()">↻ Rafraîchir</button>
    </div>

    <!-- Loader -->
    <div class="loader-wrap" *ngIf="dataSource === 'loading'">
      <div class="loader">
        <div class="loader-bar"></div>
      </div>
      <p class="loader-text">Chargement des données...</p>
    </div>

    <!-- Dashboard grid -->
    <div class="grid-wrap" *ngIf="dataSource !== 'loading'">
      <div class="grid">
        <app-card *ngFor="let app of svc.apps; trackBy: trackByAppId" [app]="app"></app-card>
      </div>
    </div>

    <footer>
      <span>💡 Cliquez sur une app → Pipeline ou Sprint</span>
      <div class="brand">
        <svg width="12" height="12" viewBox="0 0 100 100">
          <polygon points="50,3 97,25 97,75 50,97 3,75 3,25" fill="#00874E"/>
        </svg>
        <span>RDAPP · DEPLOY + SPRINT</span>
      </div>
    </footer>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; }

    .source-bar {
      display: flex; align-items: center; gap: 8px;
      padding: 6px 24px; font-size: 11px; font-weight: 500;
      font-family: 'JetBrains Mono', monospace;
      transition: all .3s ease;
    }
    .source-bar.api    { background: #F0FDF4; color: #16A34A; }
    .source-bar.mock   { background: #FEF3C7; color: #D97706; }
    .source-bar.loading { background: #F0F7FF; color: #3B82F6; }
    .source-dot {
      width: 8px; height: 8px; border-radius: 50%;
      display: inline-block;
    }
    .source-bar.api .source-dot    { background: #16A34A; }
    .source-bar.mock .source-dot   { background: #D97706; }
    .source-bar.loading .source-dot {
      background: #3B82F6;
      animation: pulse-dot 1s ease-in-out infinite;
    }
    @keyframes pulse-dot {
      0%, 100% { opacity: 1; transform: scale(1); }
      50%      { opacity: .5; transform: scale(.7); }
    }
    .refresh-btn {
      margin-left: auto;
      background: transparent; border: 1px solid currentColor;
      color: inherit; cursor: pointer;
      padding: 2px 10px; border-radius: 4px;
      font: 500 10px 'JetBrains Mono', monospace;
      transition: all .2s;
    }
    .refresh-btn:hover { background: rgba(0,0,0,.05); }

    .loader-wrap {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; padding: 80px 24px;
    }
    .loader { width: 200px; height: 3px; background: #E2E8F0; border-radius: 2px; overflow: hidden; }
    .loader-bar {
      width: 40%; height: 100%; background: #00874E; border-radius: 2px;
      animation: slide 1.2s ease-in-out infinite;
    }
    @keyframes slide {
      0%   { transform: translateX(-100%); }
      100% { transform: translateX(350%); }
    }
    .loader-text { color: #94A3B8; font-size: 12px; margin-top: 12px; }

    .grid-wrap { padding: 16px 24px 40px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 12px; }

    footer {
      padding: 10px 24px; border-top: 1px solid #E2E8F0;
      display: flex; justify-content: space-between; align-items: center;
      font-size: 9px; color: #94A3B8; flex-wrap: wrap; gap: 8px; background: #fff;
    }
    .brand {
      display: flex; align-items: center; gap: 6px;
      font: 700 9px 'JetBrains Mono', monospace;
      letter-spacing: .08em; color: #64748B;
    }
  `],
})
export class DeployDashboardComponent implements OnInit, OnDestroy {

  dataSource: DataSource = 'loading';
  private sub?: Subscription;

  constructor(
    readonly svc: DeployService,
    private api: DeployApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.svc.init();
    this.sub = this.svc.dataSource$.subscribe(ds => {
      this.dataSource = ds;
      this.cdr.markForCheck();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  refresh(): void {
    this.svc.refresh();
  }

  trackByAppId(_: number, app: AppConfig): string { return app.id; }
}
