# Guide Scheduler - Déploiements automatiques Cloud Foundry

## Vue d'ensemble

Le workflow Cloud Foundry supporte maintenant les **déploiements automatiques planifiés** via GitHub Actions `schedule` (cron).

## Configuration actuelle

### Workflow : `deploy-cloudfoundry.yml`

```yaml
on:
  schedule:
    - cron: '0 6 * * 1-5'  # Du lundi au vendredi à 6h00 UTC
```

**Comportement par défaut** :
- Déploiement automatique vers **staging**
- Branche déployée : **main**
- Trigger : **scheduler**

## Syntaxe Cron

Format : `minute hour day month day-of-week` (tous en UTC)

| Champ | Valeurs | Description |
|-------|---------|-------------|
| minute | 0-59 | Minute de l'heure |
| hour | 0-23 | Heure du jour (UTC) |
| day | 1-31 | Jour du mois |
| month | 1-12 | Mois de l'année |
| day-of-week | 0-6 | Jour de la semaine (0 = dimanche, 1 = lundi, etc.) |

**Caractères spéciaux** :
- `*` : Toutes les valeurs
- `,` : Liste de valeurs (ex: `1,3,5`)
- `-` : Plage de valeurs (ex: `1-5`)
- `/` : Intervalle (ex: `*/15` = toutes les 15 minutes)

## Exemples de configuration

### Déploiement quotidien

```yaml
schedule:
  # Tous les jours à 2h00 UTC
  - cron: '0 2 * * *'
```

### Déploiement en semaine

```yaml
schedule:
  # Du lundi au vendredi à 8h00 UTC
  - cron: '0 8 * * 1-5'
```

### Déploiement hebdomadaire

```yaml
schedule:
  # Tous les lundis à 14h30 UTC
  - cron: '30 14 * * 1'
```

### Déploiement bi-quotidien

```yaml
schedule:
  # Tous les jours à 6h00 et 18h00 UTC
  - cron: '0 6,18 * * *'
```

### Déploiement toutes les 6 heures

```yaml
schedule:
  # Toutes les 6 heures (0h, 6h, 12h, 18h UTC)
  - cron: '0 */6 * * *'
```

### Déploiement mensuel

```yaml
schedule:
  # Premier jour du mois à 0h00 UTC
  - cron: '0 0 1 * *'
```

### Multiples horaires

```yaml
schedule:
  # Staging : tous les jours à 6h00 UTC
  - cron: '0 6 * * *'
  # Preprod : tous les lundis à 8h00 UTC (non supporté actuellement)
  # - cron: '0 8 * * 1'
```

**Note** : Actuellement, le scheduler déploie toujours vers **staging**. Pour déployer vers preprod via scheduler, vous devez modifier le code du workflow (voir section "Personnalisation avancée").

## Conversion de fuseaux horaires

GitHub Actions utilise **UTC**. Voici quelques conversions :

| Fuseau | Heure locale | UTC |
|--------|--------------|-----|
| Paris (CET/CEST) | 8h00 | 6h00 (hiver) / 7h00 (été) |
| New York (EST/EDT) | 8h00 | 13h00 (hiver) / 12h00 (été) |
| Tokyo (JST) | 8h00 | 23h00 (jour précédent) |
| Los Angeles (PST/PDT) | 8h00 | 16h00 (hiver) / 15h00 (été) |

**Exemple** : Pour déployer à 8h00 heure de Paris (hiver) :

```yaml
schedule:
  - cron: '0 6 * * *'  # 8h00 Paris = 6h00 UTC (hiver)
```

## Comportement du workflow

### Déclenchement manuel (workflow_dispatch)

```yaml
Environment: staging/preprod (choix utilisateur)
Ref: main (ou autre branche spécifiée)
Triggered by: manual ou repo-b
```

### Déclenchement automatique (schedule)

```yaml
Environment: staging (automatique)
Ref: main (automatique)
Triggered by: scheduler
```

### Logs du déploiement

Quand le workflow est déclenché par le scheduler, vous verrez :

```
::notice::Trigger: Scheduled (cron)
::notice::Environment: staging (automatic)
::notice::Ref: main
::notice::Scheduler deployment → Environment: staging
```

### Artifact deployment.json

```json
{
  "baseUrl": "https://my-app-staging.cfapps.io",
  "appName": "my-app-staging",
  "environment": "staging",
  "ref": "main",
  "deployedAt": "2025-12-30T06:00:00Z",
  "triggeredBy": "scheduler",
  "cfOrg": "my-org",
  "cfSpace": "development"
}
```

## Activation/Désactivation

### Activer le scheduler

Le scheduler est déjà activé dans le workflow. Pour modifier l'horaire :

```yaml
# Éditer .github/workflows/deploy-cloudfoundry.yml
schedule:
  - cron: '0 6 * * 1-5'  # Modifier cette ligne
```

### Désactiver temporairement

Commentez la section `schedule` :

```yaml
# schedule:
#   - cron: '0 6 * * 1-5'
```

### Désactiver définitivement

Supprimez complètement la section `schedule`.

## Personnalisation avancée

### Déployer vers preprod via scheduler

Actuellement, le scheduler déploie toujours vers staging. Pour changer ce comportement :

**Option 1 : Créer un workflow séparé**

Dupliquez le workflow :

```bash
cp .github/workflows/deploy-cloudfoundry.yml .github/workflows/deploy-cloudfoundry-preprod.yml
```

Modifiez le nouveau workflow :

```yaml
# deploy-cloudfoundry-preprod.yml
schedule:
  - cron: '0 8 * * 1'  # Tous les lundis à 8h00 UTC

# Dans le step "Determine application name"
if [ "${{ github.event_name }}" = "schedule" ]; then
  ENVIRONMENT="preprod"  # Au lieu de "staging"
  echo "::notice::Scheduler deployment → Environment: preprod"
else
  ENVIRONMENT="${{ github.event.inputs.environment }}"
fi
```

**Option 2 : Utiliser des inputs conditionnels**

Modifiez le workflow pour accepter un paramètre d'environnement via variables d'environnement :

```yaml
env:
  SCHEDULER_ENVIRONMENT: 'staging'  # Ou 'preprod'

# Dans le step "Determine application name"
if [ "${{ github.event_name }}" = "schedule" ]; then
  ENVIRONMENT="${{ env.SCHEDULER_ENVIRONMENT }}"
```

### Déployer une branche spécifique via scheduler

Modifiez le step "Checkout code" :

```yaml
- name: Checkout code
  uses: actions/checkout@v4
  with:
    ref: ${{ github.event_name == 'schedule' && 'develop' || github.event.inputs.ref || 'main' }}
```

Cela déploiera la branche `develop` si le workflow est déclenché par le scheduler, sinon `main` ou la branche spécifiée.

## Monitoring et debugging

### Voir les exécutions planifiées

```bash
# Lister tous les runs du workflow
gh run list --workflow=deploy-cloudfoundry.yml

# Filtrer les runs du scheduler
gh run list --workflow=deploy-cloudfoundry.yml --event=schedule

# Voir les logs d'un run
gh run view RUN_ID --log
```

### Vérifier le prochain déclenchement

GitHub Actions ne fournit pas d'API pour voir le prochain déclenchement.

Utilisez un calculateur cron en ligne :
- https://crontab.guru/
- https://crontab.cronhub.io/

**Exemple** : `0 6 * * 1-5`
- Prochain run : Tous les jours de semaine à 6h00 UTC

### Logs typiques

```
Trigger: Scheduled (cron)
Environment: staging (automatic)
Ref: main
Scheduler deployment → Environment: staging
Application name: my-app-staging
```

## Limitations GitHub Actions

### Délai d'exécution

GitHub Actions peut retarder l'exécution des workflows planifiés de **3 à 10 minutes** aux heures de pointe.

**Exemple** : Si votre cron est `0 6 * * *`, le workflow peut s'exécuter entre 6h00 et 6h10 UTC.

### Fréquence minimale

GitHub Actions ne garantit pas l'exécution si la fréquence est **inférieure à 5 minutes**.

❌ Ne fonctionne pas bien : `*/1 * * * *` (toutes les minutes)
✅ Fonctionne bien : `*/15 * * * *` (toutes les 15 minutes)

### Repos inactifs

Si le repo est inactif pendant **60 jours**, GitHub désactive automatiquement les workflows planifiés.

**Solution** : Faire un commit ou déclencher manuellement le workflow.

## Exemples de cas d'usage

### Déploiement nightly (nuit)

```yaml
schedule:
  # Tous les jours à 2h00 UTC (build nocturne)
  - cron: '0 2 * * *'
```

**Use case** : Build et déploiement nocturne pour tester les derniers commits du jour.

### Déploiement en heures creuses

```yaml
schedule:
  # Tous les jours à 22h00 UTC (heures creuses)
  - cron: '0 22 * * *'
```

**Use case** : Éviter les déploiements pendant les heures de travail.

### Déploiement avant l'ouverture

```yaml
schedule:
  # Du lundi au vendredi à 7h00 UTC (8h00 Paris hiver)
  - cron: '0 7 * * 1-5'
```

**Use case** : Déployer avant que l'équipe arrive au bureau.

### Refresh hebdomadaire

```yaml
schedule:
  # Tous les dimanches à 1h00 UTC
  - cron: '0 1 * * 0'
```

**Use case** : Redéploiement complet hebdomadaire pour éviter le drift.

## Combinaison avec Cypress

Le workflow Cypress (`cypress-e2e-cloudfoundry.yml`) ne déclenche **pas** automatiquement de tests lors d'un déploiement scheduler.

**Pour activer les tests automatiques après un déploiement scheduler** :

### Option 1 : Ajouter un step dans le workflow deploy

Ajoutez à la fin du job `deploy` :

```yaml
- name: Trigger Cypress tests
  if: github.event_name == 'schedule'
  env:
    GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: |
    gh workflow run cypress-e2e-cloudfoundry.yml \
      -f environment=staging \
      -f skip_deployment=true \
      -f base_url="${{ steps.get-route.outputs.base-url }}"
```

### Option 2 : Créer un workflow orchestrateur

Créez `.github/workflows/nightly-deploy-and-test.yml` :

```yaml
name: Nightly Deploy & Test

on:
  schedule:
    - cron: '0 2 * * *'

jobs:
  deploy-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Trigger deployment
        run: |
          gh workflow run deploy-cloudfoundry.yml

      - name: Wait and trigger Cypress
        run: |
          sleep 600  # Attendre 10 minutes
          gh workflow run cypress-e2e-cloudfoundry.yml \
            -f environment=staging \
            -f skip_deployment=true \
            -f base_url=https://my-app-staging.cfapps.io
```

## Checklist de configuration

- [ ] Définir l'horaire cron souhaité
- [ ] Convertir le fuseau horaire vers UTC
- [ ] Modifier `.github/workflows/deploy-cloudfoundry.yml`
- [ ] Commit et push
- [ ] Vérifier le prochain run sur GitHub Actions
- [ ] Attendre le premier déclenchement automatique
- [ ] Vérifier les logs et l'artifact `deployment.json`
- [ ] (Optionnel) Configurer les tests Cypress automatiques

## Support

Pour toute question :
- Consultez la doc GitHub : https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows#schedule
- Testez votre cron : https://crontab.guru/

---

**Configuration actuelle** : Déploiement automatique du lundi au vendredi à 6h00 UTC (staging, branche main)
