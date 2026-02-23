#!/usr/bin/env bash
# ══════════════════════════════════════════════════════════
# AJOUTS dans fetch-deployments.sh
#
# 1. Fonction resolve_tag_sha
# 2. headCommitDate / baseCommitDate dans github_compare
# 3. Appel resolve_tag_sha pour chaque déploiement
# ══════════════════════════════════════════════════════════

# ── AJOUTER cette fonction après github_compare() ──

resolve_tag_sha() {
  local product="$1" tag="$2"

  # 1. Chercher la ref du tag
  local ref
  ref=$(curl -sf --max-time 10 \
    -H "Authorization: Bearer ${GH_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "${GH_API}/repos/${GH_ORG}/${product}/git/refs/tags/${tag}" 2>/dev/null) || {
    echo ""
    return 0
  }

  local sha type
  sha=$(echo "$ref" | jq -r '.object.sha // empty')
  type=$(echo "$ref" | jq -r '.object.type // empty')

  # 2. Si annotated tag → résoudre vers le commit
  if [ "$type" = "tag" ]; then
    sha=$(curl -sf --max-time 10 \
      -H "Authorization: Bearer ${GH_TOKEN}" \
      -H "Accept: application/vnd.github+json" \
      "${GH_API}/repos/${GH_ORG}/${product}/git/tags/${sha}" 2>/dev/null \
      | jq -r '.object.sha // empty')
  fi

  echo "$sha"
}


# ── MODIFIER github_compare() — ajouter headCommitDate et baseCommitDate ──

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
    headCommitDate: (.commits | last | .commit.author.date),
    baseCommitDate: (.commits | first | .commit.author.date),
    commits: [.commits[] | {
      sha: .sha,
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


# ══════════════════════════════════════════════════════════
# DANS LA PHASE 3, pour chaque déploiement, AJOUTER après
# la construction de current_entry.json :
# ══════════════════════════════════════════════════════════

#     # Résoudre le SHA du tag
#     TAG_SHA=$(resolve_tag_sha "$PROD" "$CUR_V")
#     if [ -n "$TAG_SHA" ]; then
#       jq --arg sha "$TAG_SHA" '.tagCommitSha = $sha' \
#         "$WORK/current_entry.json" > "$WORK/current_entry_tmp.json"
#       mv "$WORK/current_entry_tmp.json" "$WORK/current_entry.json"
#     fi

# ══════════════════════════════════════════════════════════
# Placement exact dans la Phase 3 :
#
#   jq ".[$i] | {
#     produit: .component,
#     version: .version,
#     environnement: .environment,
#     timestamp: .timestamp,
#     namespace: (.namespace // null),
#     comparaison: null
#   }" "$WORK/prod_deploys.json" > "$WORK/current_entry.json"
#
#   CUR_V=$(jq -r ".[$i].version // \"\"" "$WORK/prod_deploys.json")
#
#   # ── AJOUTER ICI ──
#   TAG_SHA=$(resolve_tag_sha "$PROD" "$CUR_V")
#   if [ -n "$TAG_SHA" ]; then
#     jq --arg sha "$TAG_SHA" '.tagCommitSha = $sha' \
#       "$WORK/current_entry.json" > "$WORK/current_entry_tmp.json"
#     mv "$WORK/current_entry_tmp.json" "$WORK/current_entry.json"
#     echo "  ┃  ┃  SHA: ${TAG_SHA:0:7}"
#   fi
#
#   if [ "$i" -gt 0 ]; then
#     ... (comparaison logic)
# ══════════════════════════════════════════════════════════
