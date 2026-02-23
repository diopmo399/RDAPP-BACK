# Rdapp Deploy + Sprint Dashboard — Angular

Tableau de bord Angular standalone pour le suivi des déploiements et du sprint.

## Architecture

```
src/
├── styles.scss                              ← Global styles + Google Fonts
├── app/
│   ├── models/
│   │   └── deploy.models.ts                 ← Interfaces, types, constantes (palette, envs…)
│   ├── data/
│   │   └── apps.data.ts                     ← Mock data: 6 apps, escouades, tickets
│   ├── services/
│   │   └── deploy.service.ts                ← State management + business logic
│   ├── pipes/
│   │   └── truncate.pipe.ts                 ← Pipe utilitaire
│   ├── styles/
│   │   └── tokens.scss                      ← SCSS design tokens partagés
│   ├── components/
│   │   ├── header/                          ← Stats globaux + légende environnements
│   │   │   └── header.component.ts
│   │   ├── sprint-panel/                    ← Panneau réutilisable (NS/IP/PR)
│   │   │   ├── sprint-panel.component.ts
│   │   │   ├── sprint-panel.component.html
│   │   │   └── sprint-panel.component.scss
│   │   ├── pipeline/                        ← Pipeline DEV→QA→TEST→PROD + commits
│   │   │   ├── pipeline.component.ts
│   │   │   ├── pipeline.component.html
│   │   │   └── pipeline.component.scss
│   │   ├── app-card/                        ← Carte application (collapsed/expanded)
│   │   │   ├── app-card.component.ts
│   │   │   ├── app-card.component.html
│   │   │   └── app-card.component.scss
│   │   └── deploy-dashboard/               ← Composant racine
│   │       └── deploy-dashboard.component.ts
│   └── index.ts                             ← Barrel exports
└── README.md
```

## Applications (6)

| # | App | Tech | Badge |
|---|-----|------|-------|
| 1 | ⚙️ Orchestrateur Paiements | Camunda | 🟠 |
| 2 | 🔄 Workflow Réclamations | Camunda | 🟠 |
| 3 | 🏦 API Comptes | Spring Boot | 🟢 |
| 4 | 💳 API Paiements | Spring Boot | 🟢 |
| 5 | 🔔 Service Notifications | Spring Boot | 🟢 |
| 6 | 🌐 AccèsD Web | Angular | 🔴 |

## Composants

### `<app-deploy-dashboard>` — Racine
Orchestre le header, la grille de cartes et le footer.

### `<app-header>` — En-tête
4 stats globaux (déployés, en cours, non débutés, en PR) + légende des environnements.

### `<app-card>` — Carte application
- **Mode collapsed** : barre mini avec statut des 4 envs
- **Mode expanded** : 2 onglets (Pipeline / Sprint)

### `<app-pipeline>` — Onglet Pipeline
- 4 boîtes environnement DEV → QA → TEST → PROD
- Connecteurs animés entre versions identiques
- Clic sur un env → liste de commits par version
- Liens Jira + GitHub

### `<app-sprint-panel>` — Panneau Sprint (×3)
Composant réutilisé pour chaque colonne :
- **🚫 Non débutés** : tickets groupés par escouade
- **🔨 En cours** : + barre de progression + branche Git
- **⎇ En PR** : + checks CI (●●●●) + commentaires + date

### `DeployService` — Service central
- State UI : app sélectionnée, tab actif, env, version
- Méthodes de calcul : `versionsInEnv()`, `envsForVersion()`, `groupBySquad()`
- Stats : `totalDeployed`, `totalNotStarted`, `totalInProgress`, `totalInPR`

## Intégration

### Prérequis
- Angular 15+ (standalone components)

### Dans un composant standalone
```typescript
import { Component } from '@angular/core';
import { DeployDashboardComponent } from './components/deploy-dashboard/deploy-dashboard.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [DeployDashboardComponent],
  template: '<app-deploy-dashboard />'
})
export class AppComponent {}
```

### Dans un module NgModule
```typescript
@NgModule({
  imports: [DeployDashboardComponent],
})
export class AppModule {}
```

### Fonts (dans index.html)
```html
<link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;600;700;800&display=swap" rel="stylesheet">
```

## Design

- **Thème** : fond blanc, cartes blanches avec ombres subtiles
- **Fonts** : IBM Plex Sans (body) + JetBrains Mono (code/labels)
- **Couleurs** : palette Rdapp officielle
- **Responsive** : grille 3 colonnes → 1 colonne sur mobile

## Dépendances

Zéro dépendance externe :
- `@angular/core`
- `@angular/common`

## Évolution recommandée

1. Remplacer `apps.data.ts` par un **HttpClient** vers votre API
2. Ajouter **NgRx** ou **Angular Signals** pour le state reactif
3. Connecter un **WebSocket** pour les mises à jour en temps réel
4. Ajouter des **tests unitaires** avec Jasmine/Karma
