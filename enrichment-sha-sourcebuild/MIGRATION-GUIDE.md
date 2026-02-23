# Enrichissement SHA + SourceBuild — Guide de migration

## Résumé des changements

```
Tag Git → SHA commit
  build.T1639Z  → SHA a1b2c3d4
  rc.15         → SHA a1b2c3d4   ← même code
  1.5.4         → SHA a1b2c3d4   ← même code

→ Le SHA permet de :
  1. Savoir que rc.15 vient de build.T1639Z
  2. Réutiliser les commits du build pour le rc/release
  3. Déterminer si un colis est dans un env via le SHA
```

## Fichiers modifiés

### Backend (6 fichiers)

| Fichier | Action | Description |
|---------|--------|-------------|
| `DeploiementEntity.java` | MODIFIER | Ajouter `tagCommitSha` (VARCHAR 40) |
| `DeploiementRepository.java` | MODIFIER | Ajouter queries: `findByProduitAndTagCommitSha`, `findByProduit`, `findTopByProduitAndVersion` |
| `DeployIngestDto.java` | MODIFIER | Ajouter `tagCommitSha` dans `DeploymentIngest`, `headCommitDate`/`baseCommitDate` dans `ComparisonIngest` |
| `DeployIngestService.java` | MODIFIER | Persister `tagCommitSha`, `headCommitDate`, `baseCommitDate` |
| `AppConfigMapper.java` | NOUVEAU | Mapper entités → DTOs enrichis avec SHA + sourceBuild |
| `liquibase-and-backfill.sql` | NOUVEAU | Changeset + backfill SQL |

### GHA (1 fichier)

| Fichier | Action | Description |
|---------|--------|-------------|
| `fetch-deployments.sh` | MODIFIER | Ajouter `resolve_tag_sha()`, `headCommitDate`/`baseCommitDate` dans `github_compare()` |

### Angular (2 fichiers)

| Fichier | Action | Description |
|---------|--------|-------------|
| `models.ts` | MODIFIER | `EnvData` + `tagCommitSha`, nouveau `VersionInfo`, `AppConfig.commits` change de type |
| `deploy.service.ts` | MODIFIER | `getEnvsContainingVersion()` avec SHA, nouveau `getCommitsForVersion()` |

## Ordre d'exécution

```
1. Liquibase → ajouter colonne tag_commit_sha dans deploiement
2. Backend   → déployer les modifications Java
3. Backfill  → exécuter le SQL pour remplir head_commit_date/base_commit_date
4. GHA       → relancer le workflow pour peupler tag_commit_sha
5. Angular   → déployer les modifications frontend
```

## Breaking changes Angular

```typescript
// AVANT : app.commits[version] retourne Commit[]
const commits = app.commits[version];
const count = commits.length;

// APRÈS : app.commits[version] retourne VersionInfo
const commits = app.commits[version].commits;       // ou []
const count = commits.length;
const sha = app.commits[version].tagCommitSha;       // nouveau
const source = app.commits[version].sourceBuild;     // nouveau

// Méthode recommandée (gère le fallback SHA automatiquement) :
const commits = this.deployService.getCommitsForVersion(app, version);
```
