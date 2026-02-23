// ══════════════════════════════════════════════════════════
// MÉTHODES À REMPLACER dans deploy.service.ts
// ══════════════════════════════════════════════════════════

/**
 * Détermine dans quels environnements une version est présente.
 *
 * Priorité des règles :
 *   1. MÊME SHA         → même code exact (build↔rc↔release)
 *   2. RELEASE FINALE   → "1.5.4" contient tout "1.5.4-*"
 *   3. BASE SUPÉRIEURE  → "1.5.5" contient tout "1.5.4-*"
 *   4. DATE             → commit.date ≤ env.headCommitDate
 *   5. MATCH EXACT      → version === env.version
 */
getEnvsContainingVersion(app: AppConfig, version: string): EnvKey[] {
  if (!app.commits) return [];

  const versionInfo = app.commits[version];
  if (!versionInfo) return [];

  const versions = Object.keys(app.commits);
  const vIdx = versions.indexOf(version);
  if (vIdx === -1) return [];

  const baseVersion = version.replace(/-.*$/, '');
  const versionSha = versionInfo.tagCommitSha;
  const commitDate = this.resolveCommitDate(app, versions, vIdx);

  return ENV_KEYS.filter(ek => {
    const env = app.envs[ek];
    if (!env?.version) return false;

    const envVersion = env.version;
    const envBaseVersion = envVersion.replace(/-.*$/, '');

    // ── Règle 1 : MÊME SHA ──
    // build.T1639Z (SHA: a1b2c3d4) == rc.15 (SHA: a1b2c3d4)
    if (versionSha && env.tagCommitSha && versionSha === env.tagCommitSha) {
      return true;
    }

    // ── Règle 2 : Release finale contient tout ──
    // prod a "1.5.4" → tout "1.5.4-*" inclus
    if (envBaseVersion === baseVersion && !envVersion.includes('-')) {
      return true;
    }

    // ── Règle 3 : Base version supérieure ──
    // prod a "1.5.5" → tout "1.5.4-*" inclus
    if (this.isVersionGreater(envBaseVersion, baseVersion)) {
      return true;
    }

    // ── Règle 4 : Comparaison par date ──
    if (commitDate && env.headCommitDate) {
      return commitDate.getTime() <= new Date(env.headCommitDate).getTime();
    }

    // ── Règle 5 : Match exact ──
    return envVersion === version;
  });
}

/**
 * Récupère les commits d'une version.
 * Si rc/release sans commits → retrouve le build source via SHA.
 *
 * Exemple :
 *   "1.5.4-rc.15" (SHA a1b2c3d4, pas de commits propres)
 *   → build.T1639Z (SHA a1b2c3d4, a des commits)
 *   → retourne les commits du build
 */
getCommitsForVersion(app: AppConfig, version: string): Commit[] {
  const versionInfo = app.commits[version];
  if (!versionInfo) return [];

  // 1. Cette version a ses propres commits → les utiliser
  if (versionInfo.commits && versionInfo.commits.length > 0) {
    return versionInfo.commits;
  }

  // 2. sourceBuild renseigné par le backend → utiliser ses commits
  if (versionInfo.sourceBuild && app.commits[versionInfo.sourceBuild]) {
    const buildInfo = app.commits[versionInfo.sourceBuild];
    if (buildInfo.commits && buildInfo.commits.length > 0) {
      return buildInfo.commits;
    }
  }

  // 3. Fallback : chercher par SHA dans les autres versions
  const sha = versionInfo.tagCommitSha;
  if (!sha) return [];

  for (const [v, info] of Object.entries(app.commits)) {
    if (v === version) continue;
    if (info.tagCommitSha === sha && v.includes('-build')) {
      if (info.commits && info.commits.length > 0) {
        return info.commits;
      }
    }
  }

  return [];
}

/**
 * Résout la date du commit le plus récent pour une version.
 * Si la version n'a pas de commits, remonte les versions précédentes.
 */
private resolveCommitDate(
  app: AppConfig,
  versions: string[],
  vIdx: number
): Date | undefined {

  // 1. Cette version a des commits ?
  const date = this.getLatestRawDate(app, versions[vIdx]);
  if (date) return date;

  // 2. Remonter en arrière
  for (let i = vIdx - 1; i >= 0; i--) {
    const d = this.getLatestRawDate(app, versions[i]);
    if (d) return d;
  }

  // 3. Chercher en avant
  for (let i = vIdx + 1; i < versions.length; i++) {
    const commits = this.getCommitsForVersion(app, versions[i]);
    if (commits.length > 0) {
      const earliest = commits
        .map(c => c.rawDate)
        .filter((d): d is Date => !!d)
        .sort((a, b) => a.getTime() - b.getTime())
        .shift();
      if (earliest) return earliest;
    }
  }

  return undefined;
}

/**
 * Date du commit le plus récent d'une version
 * (en utilisant getCommitsForVersion pour résoudre via build source)
 */
private getLatestRawDate(app: AppConfig, version: string): Date | undefined {
  const commits = this.getCommitsForVersion(app, version);
  if (commits.length === 0) return undefined;

  return commits
    .map(c => c.rawDate)
    .filter((d): d is Date => !!d)
    .sort((a, b) => a.getTime() - b.getTime())
    .pop();
}

/**
 * Compare deux versions base (sans suffixe).
 * "1.5.5" > "1.5.4" → true
 */
private isVersionGreater(a: string, b: string): boolean {
  const pa = a.split('.').map(Number);
  const pb = b.split('.').map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const na = pa[i] || 0;
    const nb = pb[i] || 0;
    if (na > nb) return true;
    if (na < nb) return false;
  }
  return false; // equal → pas strictly greater
}

// ══════════════════════════════════════════════════════════
// ADAPTER LES APPELS EXISTANTS
// ══════════════════════════════════════════════════════════
//
// Partout où le code fait :
//   app.commits[version]          → retourne VersionInfo (pas Commit[])
//   app.commits[version].length   → app.commits[version].commits.length
//
// Exemples :
//
// AVANT :
//   const commits = app.commits[version];
//   commits.forEach(c => ...)
//
// APRÈS :
//   const commits = app.commits[version]?.commits || [];
//   commits.forEach(c => ...)
//
// OU utiliser la nouvelle méthode :
//   const commits = this.getCommitsForVersion(app, version);
//   commits.forEach(c => ...)
//
// ══════════════════════════════════════════════════════════
