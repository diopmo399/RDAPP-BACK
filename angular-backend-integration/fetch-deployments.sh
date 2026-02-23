#!/usr/bin/env bash
# ══════════════════════════════════════════════════════════════
# fetch-deployments.sh
#
# AVEC CACHE DES COMPARAISONS + FICHIERS TEMPORAIRES
#
# Corrections appliquées :
#   ✓ Fichiers /tmp/ au lieu de variables bash (Argument list too long)
#   ✓ jq : .component as $c | $products | index($c)
#   ✓ curl -G --data-urlencode pour les envs avec / (nonprod-est/qa)
#   ✓ .environnements (français) au lieu de .environments
#
# Flux :
#   Phase 1 : Portail Infonuagique → déploiements (fichiers /tmp/)
#   Phase 2 : POST /deploy/comparisons/lookup → cache
#   Phase 3 : GitHub Compare (manquantes) + assemblage payload
#
# Variables d'env requises :
#   PORTAIL_BASE_URL, GH_TOKEN, GH_ORG
#   DASHBOARD_API_URL, DASHBOARD_API_TOKEN
# ══════════════════════════════════════════════════════════════

set -euo pipefail

PORTAIL_API="${PORTAIL_BASE_URL}/deploiement-service/deploiements/search"
GH_API="https://api.github.com"

# Mois historique depuis le step config (ou fallback 6)
if [ -f /tmp/months.txt ]; then
  MONTHS=$(cat /tmp/months.txt)
else
  MONTHS="${MONTHS_HISTORY:-6}"
fi

# Date cutoff
if [[ "$(uname)" == "Darwin" ]]; then
  CUTOFF_DATE=$(date -v-${MONTHS}m -u +"%Y-%m-%dT%H:%M:%SZ")
else
  CUTOFF_DATE=$(date -u -d "${MONTHS} months ago" +"%Y-%m-%dT%H:%M:%SZ")
fi

# Répertoire temporaire de travail
WORK=/tmp/deploy-work
rm -rf "$WORK"
mkdir -p "$WORK"

echo "::group::Fetching Deployments + GitHub Compare (with cache)"
echo "  Portail URL: ${PORTAIL_BASE_URL}"
echo "  GitHub Org: ${GH_ORG}"
echo "  Mois historique: ${MONTHS}"
echo "  Cutoff: ${CUTOFF_DATE}"

STATS_CACHED=0
STATS_NEW=0
STATS_SKIPPED=0

# ── Fonctions utilitaires ──────────────────────────────────

github_compare() {
  local product="$1" base="$2" head="$3"
  local url="${GH_API}/repos/${GH_ORG}/${product}/compare/${base}...${head}"
  local response
  response=$(curl -sf --max-time 30 \
    -H "Authorization: Bearer ${GH_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "$url" 2>/dev/null) || {
    echo "null"
    return 0
  }
  echo "$response" | jq '{
    status: .status,
    aheadBy: .ahead_by,
    behindBy: .behind_by,
    totalCommits: .total_commits,
    commits: [.commits[] | {
      sha: .sha[0:7],
      message: (.commit.message | split("\n")[0]),
      author: .commit.author.name,
      date: .commit.author.date
    }],
    files: [.files[] | {
      filename: .filename,
      status: .status,
      additions: .additions,
      deletions: .deletions,
      changes: .changes
    }],
    filesChanged: (.files | length),
    additions: ([.files[].additions] | add // 0),
    deletions: ([.files[].deletions] | add // 0)
  }'
}

# ══════════════════════════════════════════════════════════════
# PHASE 1 : Collecter les déploiements
#            → fichiers /tmp/ pour éviter "Argument list too long"
# ══════════════════════════════════════════════════════════════

ORGS_FILE=/tmp/orgs.json
ENVS_FILE=/tmp/envs.json
ORG_COUNT=$(jq 'length' "$ORGS_FILE")
ENV_COUNT=$(jq 'length' "$ENVS_FILE")

# Compteurs
ORGENV_INDEX=0

echo ""
echo "━━━ Phase 1 : Collecte des déploiements ━━━"

# Collecter toutes les paires à comparer dans un fichier
echo "[]" > "$WORK/all_pairs.json"

for env_idx in $(seq 0 $((ENV_COUNT - 1))); do
  ENV=$(jq -r ".[$env_idx]" "$ENVS_FILE")

  for org_idx in $(seq 0 $((ORG_COUNT - 1))); do
    ORG_NAME=$(jq -r ".[$org_idx].nom" "$ORGS_FILE")
    PRODUCTS=$(jq ".[$org_idx].produits // []" "$ORGS_FILE")
    PRODUCT_COUNT=$(echo "$PRODUCTS" | jq 'length')

    [ "$PRODUCT_COUNT" -eq 0 ] && continue

    echo "  [$ENV] $ORG_NAME ($PRODUCT_COUNT produits)..."

    # Appeler Portail Infonuagique avec --data-urlencode (gère le / dans nonprod-est/qa)
    RAW_FILE="$WORK/raw_${env_idx}_${org_idx}.json"
    HTTP_CODE=$(curl -sf -G -w "%{http_code}" -o "$RAW_FILE" \
      "${PORTAIL_API}" \
      --data-urlencode "platform=paas" \
      --data-urlencode "squad=${ORG_NAME}" \
      --data-urlencode "environment=${ENV}" \
      2>/dev/null) || {
      echo "    ✗ Portail call failed (HTTP $HTTP_CODE)"
      continue
    }

    # Vérifier que le fichier est valide
    if [ ! -s "$RAW_FILE" ] || ! jq empty "$RAW_FILE" 2>/dev/null; then
      echo "    ✗ Réponse invalide"
      continue
    fi

    RAW_COUNT=$(jq 'length' "$RAW_FILE")
    [ "$RAW_COUNT" -eq 0 ] && { echo "    0 déploiements"; continue; }

    # Filtrer → fichier
    FILTERED_FILE="$WORK/filtered_${env_idx}_${org_idx}.json"
    jq --argjson products "$PRODUCTS" --arg cutoff "$CUTOFF_DATE" '
      [.[] |
        select(.component != null) |
        select(.component as $c | $products | index($c)) |
        select(.timestamp != null and .timestamp > $cutoff)
      ] | unique_by(.component + .version + .timestamp)
      | sort_by(.component, .timestamp)
    ' "$RAW_FILE" > "$FILTERED_FILE"

    FILTERED_COUNT=$(jq 'length' "$FILTERED_FILE")
    echo "    $FILTERED_COUNT déploiements (filtré de $RAW_COUNT)"

    [ "$FILTERED_COUNT" -eq 0 ] && continue

    # Sauvegarder l'entrée org/env
    ORGENV_FILE="$WORK/orgenv_${ORGENV_INDEX}.json"
    jq -n --arg org "$ORG_NAME" --arg env "$ENV" \
      --slurpfile deps "$FILTERED_FILE" \
      '{org: $org, env: $env, deployments: $deps[0]}' > "$ORGENV_FILE"
    ORGENV_INDEX=$((ORGENV_INDEX + 1))

    # Identifier les paires à comparer (versions consécutives différentes)
    UNIQUE_PRODS=$(jq -r '[.[].component] | unique | .[]' "$FILTERED_FILE")
    for PROD in $UNIQUE_PRODS; do
      # Extraire les versions triées par timestamp pour ce produit
      jq -r --arg p "$PROD" '
        [.[] | select(.component == $p)] | sort_by(.timestamp) | .[].version
      ' "$FILTERED_FILE" > "$WORK/versions_tmp.txt"

      PREV_V=""
      while IFS= read -r V; do
        if [ -n "$PREV_V" ] && [ "$V" != "$PREV_V" ] && [ -n "$V" ]; then
          # Vérifier si la paire existe déjà
          EXISTS=$(jq --arg p "$PROD" --arg b "$PREV_V" --arg h "$V" \
            '[.[] | select(.produit == $p and .baseVersion == $b and .headVersion == $h)] | length' \
            "$WORK/all_pairs.json")
          if [ "$EXISTS" -eq 0 ]; then
            # Ajouter la paire via fichier temporaire
            jq --arg p "$PROD" --arg b "$PREV_V" --arg h "$V" \
              '. + [{produit: $p, baseVersion: $b, headVersion: $h}]' \
              "$WORK/all_pairs.json" > "$WORK/all_pairs_tmp.json"
            mv "$WORK/all_pairs_tmp.json" "$WORK/all_pairs.json"
          fi
        fi
        PREV_V="$V"
      done < "$WORK/versions_tmp.txt"
    done
  done
done

TOTAL_ORGENVS=$ORGENV_INDEX
TOTAL_PAIRS=$(jq 'length' "$WORK/all_pairs.json")
echo ""
echo "═══ Total : $TOTAL_ORGENVS org/env, $TOTAL_PAIRS paires à comparer ═══"

# ══════════════════════════════════════════════════════════════
# PHASE 2 : Lookup cache — quels compares existent déjà ?
# ══════════════════════════════════════════════════════════════

echo ""
echo "━━━ Phase 2 : Lookup cache des comparaisons ━━━"

echo "{}" > "$WORK/cache_map.json"

if [ "$TOTAL_PAIRS" -gt 0 ]; then
  # Construire le payload de lookup
  jq -n --slurpfile pairs "$WORK/all_pairs.json" '{pairs: $pairs[0]}' > "$WORK/lookup_payload.json"

  echo "  → POST /deploy/comparisons/lookup ($TOTAL_PAIRS paires)..."

  LOOKUP_HTTP=$(curl -sf -w "%{http_code}" -o "$WORK/lookup_response.json" \
    -X POST \
    -H "Authorization: Bearer ${DASHBOARD_API_TOKEN}" \
    -H "Content-Type: application/json" \
    -d @"$WORK/lookup_payload.json" \
    "${DASHBOARD_API_URL}/v1/deploy/comparisons/lookup" 2>/dev/null) || LOOKUP_HTTP="000"

  if [ -s "$WORK/lookup_response.json" ] && jq empty "$WORK/lookup_response.json" 2>/dev/null; then
    STATS_CACHED=$(jq '.totalCached // 0' "$WORK/lookup_response.json")
    MISSING_COUNT=$(jq '.totalMissing // 0' "$WORK/lookup_response.json")
    jq '.cached // {}' "$WORK/lookup_response.json" > "$WORK/cache_map.json"
    echo "  ✓ HTTP $LOOKUP_HTTP — En cache: $STATS_CACHED | À calculer: $MISSING_COUNT"
  else
    echo "  ⚠ Lookup failed (HTTP $LOOKUP_HTTP) — on calcule tout"
  fi
else
  echo "  Aucune paire à vérifier"
fi

# ══════════════════════════════════════════════════════════════
# PHASE 3 : GitHub Compare (manquantes) + assemblage payload
# ══════════════════════════════════════════════════════════════

echo ""
echo "━━━ Phase 3 : GitHub Compare (manquantes) + assemblage ━━━"

# Initialiser le fichier des résultats
echo "[]" > "$WORK/all_results.json"
TOTAL_DEPLOYS=0

for oe_idx in $(seq 0 $((TOTAL_ORGENVS - 1))); do
  ORGENV_FILE="$WORK/orgenv_${oe_idx}.json"
  ORG_NAME=$(jq -r '.org' "$ORGENV_FILE")
  ENV=$(jq -r '.env' "$ORGENV_FILE")

  echo ""
  echo "  ┣━ $ORG_NAME / $ENV"

  # Extraire les produits uniques
  UNIQUE_PRODS=$(jq -r '[.deployments[].component] | unique | .[]' "$ORGENV_FILE")

  # Fichier pour les deploy entries de ce org/env
  echo "[]" > "$WORK/deploy_entries.json"

  for PROD in $UNIQUE_PRODS; do
    # Extraire et trier les déploiements de ce produit → fichier
    jq --arg p "$PROD" '[.deployments[] | select(.component == $p)] | sort_by(.timestamp)' \
      "$ORGENV_FILE" > "$WORK/prod_deploys.json"
    P_COUNT=$(jq 'length' "$WORK/prod_deploys.json")

    echo "  ┃  ┣━ $PROD ($P_COUNT)"

    for i in $(seq 0 $((P_COUNT - 1))); do
      # Construire l'entrée de déploiement
      jq ".[$i] | {
        produit: .component,
        version: .version,
        environnement: .environment,
        timestamp: .timestamp,
        namespace: (.namespace // null),
        comparaison: null
      }" "$WORK/prod_deploys.json" > "$WORK/current_entry.json"

      CUR_V=$(jq -r ".[$i].version // \"\"" "$WORK/prod_deploys.json")

      if [ "$i" -gt 0 ]; then
        PREV_V=$(jq -r ".[$(($i - 1))].version // \"\"" "$WORK/prod_deploys.json")

        if [ "$CUR_V" != "$PREV_V" ] && [ -n "$CUR_V" ] && [ -n "$PREV_V" ]; then
          CACHE_KEY="${PROD}|${PREV_V}|${CUR_V}"

          # Vérifier le cache
          jq --arg k "$CACHE_KEY" '.[$k] // null' "$WORK/cache_map.json" > "$WORK/cached_comp.json"
          CACHED_NULL=$(jq 'type == "null" or . == null' "$WORK/cached_comp.json")

          if [ "$CACHED_NULL" = "false" ]; then
            # ✓ EN CACHE — réutiliser
            CACHED_COMMITS=$(jq '.totalCommits // 0' "$WORK/cached_comp.json")
            echo "  ┃  ┃  [$i] ${PREV_V}→${CUR_V} ✓ CACHE ($CACHED_COMMITS commits)"

            # Marquer fromCache=true et ajouter à l'entrée
            jq --arg base "$PREV_V" --arg head "$CUR_V" \
              '. + {baseVersion: $base, headVersion: $head, fromCache: true}' \
              "$WORK/cached_comp.json" > "$WORK/comp_with_meta.json"

            jq --slurpfile comp "$WORK/comp_with_meta.json" \
              '.comparaison = $comp[0]' "$WORK/current_entry.json" > "$WORK/current_entry_tmp.json"
            mv "$WORK/current_entry_tmp.json" "$WORK/current_entry.json"

          else
            # ✗ PAS EN CACHE — appeler GitHub Compare
            echo -n "  ┃  ┃  [$i] ${PREV_V}→${CUR_V} → GitHub Compare..."

            COMPARE_RESULT=$(github_compare "$PROD" "$PREV_V" "$CUR_V")

            if [ "$COMPARE_RESULT" != "null" ] && [ -n "$COMPARE_RESULT" ]; then
              echo "$COMPARE_RESULT" > "$WORK/compare_result.json"
              NEW_COMMITS=$(jq '.totalCommits // 0' "$WORK/compare_result.json")
              echo " ✓ $NEW_COMMITS commits (NEW)"

              jq --arg base "$PREV_V" --arg head "$CUR_V" \
                '. + {baseVersion: $base, headVersion: $head, fromCache: false}' \
                "$WORK/compare_result.json" > "$WORK/comp_with_meta.json"

              jq --slurpfile comp "$WORK/comp_with_meta.json" \
                '.comparaison = $comp[0]' "$WORK/current_entry.json" > "$WORK/current_entry_tmp.json"
              mv "$WORK/current_entry_tmp.json" "$WORK/current_entry.json"

              STATS_NEW=$((STATS_NEW + 1))
            else
              echo " ✗ (404 ou erreur)"
              STATS_SKIPPED=$((STATS_SKIPPED + 1))
            fi
          fi
        fi
      fi

      # Ajouter l'entrée au tableau
      jq --slurpfile e "$WORK/current_entry.json" '. + $e' \
        "$WORK/deploy_entries.json" > "$WORK/deploy_entries_tmp.json"
      mv "$WORK/deploy_entries_tmp.json" "$WORK/deploy_entries.json"

      TOTAL_DEPLOYS=$((TOTAL_DEPLOYS + 1))
    done
  done

  # Ajouter le résultat org/env au tableau global
  jq -n --arg org "$ORG_NAME" --arg env "$ENV" \
    --slurpfile deps "$WORK/deploy_entries.json" \
    '{organisation: $org, environnement: $env, deployments: $deps[0]}' > "$WORK/result_entry.json"

  jq --slurpfile r "$WORK/result_entry.json" '. + $r' \
    "$WORK/all_results.json" > "$WORK/all_results_tmp.json"
  mv "$WORK/all_results_tmp.json" "$WORK/all_results.json"
done

# ── Payload final ──

TOTAL_COMP=$((STATS_CACHED + STATS_NEW))
jq -n \
  --slurpfile results "$WORK/all_results.json" \
  --arg runId "${GITHUB_RUN_ID:-local}" \
  --arg triggeredBy "${GITHUB_TRIGGERING_ACTOR:-schedule}" \
  --argjson totalDeploys "$TOTAL_DEPLOYS" \
  --argjson totalComp "$TOTAL_COMP" \
  --argjson cached "$STATS_CACHED" \
  --argjson newCompares "$STATS_NEW" \
  '{
    results: $results[0],
    runId: $runId,
    triggeredBy: $triggeredBy,
    metadata: {
      totalDeployments: $totalDeploys,
      totalComparisons: $totalComp,
      cachedComparisons: $cached,
      newComparisons: $newCompares
    }
  }' > /tmp/deploy-payload.json

TOTAL_SIZE=$(wc -c < /tmp/deploy-payload.json)

echo ""
echo "══════════════════════════════════════════════════"
echo "  Déploiements : $TOTAL_DEPLOYS"
echo "  Comparaisons : $TOTAL_COMP"
echo "    ├─ Cache    : $STATS_CACHED  (réutilisées)"
echo "    ├─ Nouvelles: $STATS_NEW     (GitHub API)"
echo "    └─ Échouées : $STATS_SKIPPED (404/erreur)"
echo "  Payload      : ${TOTAL_SIZE} bytes"
echo "══════════════════════════════════════════════════"
echo "::endgroup::"

# Cleanup
rm -rf "$WORK"
