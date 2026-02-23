#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════
# fetch-jira.sh
#
# Pour chaque escouade dans /tmp/squads.json :
#   1. Trouver le board Jira via boardId (nom du board)
#   2. Récupérer les sprints (active, closed, future)
#   3. Récupérer les issues de chaque sprint
#   4. Récupérer les versions du projet
#   5. Construire le payload JSON → /tmp/bulk-payload.json
#
# Variables d'env requises :
#   JIRA_BASE_URL, JIRA_PAT_TOKEN, MAX_CLOSED_SPRINTS
# ════════════════════════════════════════════════════════════

set -euo pipefail

AGILE_API="${JIRA_BASE_URL}/rest/agile/1.0"
REST_API="${JIRA_BASE_URL}/rest/api/2"
AUTH_HEADER="Authorization: Bearer ${JIRA_PAT_TOKEN}"
MAX_CLOSED="${MAX_CLOSED_SPRINTS:-5}"

# ── Fonctions utilitaires ──────────────────────────────────

jira_get() {
  local url="$1"
  local response
  local attempt=1
  local max_retries=3

  while [ $attempt -le $max_retries ]; do
    response=$(curl -sf \
      -H "$AUTH_HEADER" \
      -H "Accept: application/json" \
      "$url" 2>/dev/null) && break

    echo "  ⚠ Jira retry $attempt/$max_retries pour $url" >&2
    attempt=$((attempt + 1))
    sleep $((attempt * 2))
  done

  if [ -z "$response" ]; then
    echo "  ✗ Jira call failed: $url" >&2
    echo "{}"
    return 1
  fi

  echo "$response"
}

# Récupère toutes les pages d'issues d'un sprint
fetch_all_sprint_issues() {
  local sprint_id="$1"
  local start_at=0
  local max_results=50
  local all_issues="[]"

  while true; do
    local url="${AGILE_API}/sprint/${sprint_id}/issue?startAt=${start_at}&maxResults=${max_results}&fields=summary,status,issuetype,priority,assignee,creator,customfield_10016,fixVersions,components,created,updated,resolutiondate"
    local page
    page=$(jira_get "$url") || break

    local issues
    issues=$(echo "$page" | jq '.issues // []')
    local count
    count=$(echo "$issues" | jq 'length')

    if [ "$count" -eq 0 ]; then break; fi

    # Transformer les issues au format attendu par l'API
    local transformed
    transformed=$(echo "$issues" | jq '[.[] | {
      key: .key,
      summary: .fields.summary,
      issueType: .fields.issuetype.name,
      statusName: .fields.status.name,
      statusCategory: .fields.status.statusCategory.key,
      priority: .fields.priority.name,
      storyPoints: .fields.customfield_10016,
      assigneeName: .fields.assignee.displayName,
      assigneeUsername: .fields.assignee.name,
      fixVersion: ((.fields.fixVersions // []) | first | .name // null),
      created: .fields.created,
      updated: .fields.updated,
      resolutionDate: .fields.resolutiondate
    }]')

    all_issues=$(echo "$all_issues" "$transformed" | jq -s '.[0] + .[1]')

    local total
    total=$(echo "$page" | jq '.total // 0')
    start_at=$((start_at + count))
    if [ "$start_at" -ge "$total" ]; then break; fi
  done

  echo "$all_issues"
}

# Récupère les versions d'un projet
fetch_project_versions() {
  local project_key="$1"
  local versions
  versions=$(jira_get "${REST_API}/project/${project_key}/versions") || echo "[]"

  echo "$versions" | jq '[.[] | {
    jiraId: .id,
    name: .name,
    description: .description,
    released: .released,
    archived: .archived,
    releaseDate: .releaseDate
  }]'
}

# ── Traitement principal ───────────────────────────────────

echo "::group::Fetching Jira data"
echo "Jira base URL: ${JIRA_BASE_URL}"

SQUADS=$(cat /tmp/squads.json)
SQUAD_COUNT=$(echo "$SQUADS" | jq 'length')
SQUAD_PAYLOADS="[]"

for i in $(seq 0 $((SQUAD_COUNT - 1))); do
  SQUAD=$(echo "$SQUADS" | jq ".[$i]")
  SQUAD_ID=$(echo "$SQUAD" | jq -r '.id')
  SQUAD_NAME=$(echo "$SQUAD" | jq -r '.name')
  BOARD_NAME=$(echo "$SQUAD" | jq -r '.boardId')

  echo ""
  echo "━━━ [$((i+1))/$SQUAD_COUNT] $SQUAD_NAME (boardId=$BOARD_NAME) ━━━"

  # 1. Chercher le board
  echo "  → Recherche board '$BOARD_NAME'..."
  BOARD_RESULT=$(jira_get "${AGILE_API}/board?name=${BOARD_NAME}&maxResults=5") || continue
  BOARD_ID=$(echo "$BOARD_RESULT" | jq '.values[0].id // empty')
  BOARD_REAL_NAME=$(echo "$BOARD_RESULT" | jq -r '.values[0].name // "?"')
  PROJECT_KEY=$(echo "$BOARD_RESULT" | jq -r '.values[0].location.projectKey // ""')

  if [ -z "$BOARD_ID" ]; then
    echo "  ✗ Board non trouvé, skip"
    continue
  fi
  echo "  ✓ Board: $BOARD_REAL_NAME (id=$BOARD_ID, project=$PROJECT_KEY)"

  # 2. Récupérer les sprints
  echo "  → Récupération des sprints..."
  ALL_SPRINTS_RAW="[]"
  START_AT=0
  while true; do
    PAGE=$(jira_get "${AGILE_API}/board/${BOARD_ID}/sprint?startAt=${START_AT}&maxResults=50") || break
    VALUES=$(echo "$PAGE" | jq '.values // []')
    COUNT=$(echo "$VALUES" | jq 'length')
    [ "$COUNT" -eq 0 ] && break
    ALL_SPRINTS_RAW=$(echo "$ALL_SPRINTS_RAW" "$VALUES" | jq -s '.[0] + .[1]')
    IS_LAST=$(echo "$PAGE" | jq '.isLast // true')
    [ "$IS_LAST" = "true" ] && break
    START_AT=$((START_AT + COUNT))
  done

  ACTIVE_SPRINTS=$(echo "$ALL_SPRINTS_RAW" | jq '[.[] | select(.state == "active")]')
  CLOSED_SPRINTS=$(echo "$ALL_SPRINTS_RAW" | jq --argjson max "$MAX_CLOSED" \
    '[.[] | select(.state == "closed")] | sort_by(.completeDate) | reverse | .[:$max]')
  FUTURE_SPRINTS=$(echo "$ALL_SPRINTS_RAW" | jq '[.[] | select(.state == "future")]')

  ACTIVE_COUNT=$(echo "$ACTIVE_SPRINTS" | jq 'length')
  CLOSED_COUNT=$(echo "$CLOSED_SPRINTS" | jq 'length')
  FUTURE_COUNT=$(echo "$FUTURE_SPRINTS" | jq 'length')
  echo "  ✓ Sprints: $ACTIVE_COUNT active, $CLOSED_COUNT closed, $FUTURE_COUNT future"

  # 3. Récupérer les issues pour chaque sprint
  process_sprint() {
    local sprint_json="$1"
    local sprint_id=$(echo "$sprint_json" | jq '.id')
    local sprint_name=$(echo "$sprint_json" | jq -r '.name')
    local sprint_state=$(echo "$sprint_json" | jq -r '.state')

    echo "    → Sprint '$sprint_name' (id=$sprint_id, state=$sprint_state)..."
    local issues
    issues=$(fetch_all_sprint_issues "$sprint_id")
    local issue_count=$(echo "$issues" | jq 'length')
    echo "      ✓ $issue_count issues"

    echo "$sprint_json" | jq --argjson issues "$issues" '{
      jiraSprintId: .id,
      name: .name,
      state: .state,
      goal: .goal,
      startDate: .startDate,
      endDate: .endDate,
      completeDate: .completeDate,
      issues: $issues
    }'
  }

  # Sprint actif
  ACTIVE_PAYLOAD="null"
  if [ "$ACTIVE_COUNT" -gt 0 ]; then
    ACTIVE_PAYLOAD=$(process_sprint "$(echo "$ACTIVE_SPRINTS" | jq '.[0]')")
  fi

  # Sprints fermés
  CLOSED_PAYLOAD="[]"
  for j in $(seq 0 $((CLOSED_COUNT - 1))); do
    SPRINT_DATA=$(process_sprint "$(echo "$CLOSED_SPRINTS" | jq ".[$j]")")
    CLOSED_PAYLOAD=$(echo "$CLOSED_PAYLOAD" | jq --argjson s "$SPRINT_DATA" '. + [$s]')
  done

  # Sprints futurs (pas d'issues)
  FUTURE_PAYLOAD=$(echo "$FUTURE_SPRINTS" | jq '[.[] | {
    jiraSprintId: .id,
    name: .name,
    state: .state,
    goal: .goal,
    startDate: .startDate,
    endDate: .endDate,
    completeDate: .completeDate,
    issues: []
  }]')

  # 4. Versions du projet
  VERSIONS="[]"
  if [ -n "$PROJECT_KEY" ] && [ "$PROJECT_KEY" != "" ]; then
    echo "  → Récupération des versions ($PROJECT_KEY)..."
    VERSIONS=$(fetch_project_versions "$PROJECT_KEY")
    echo "  ✓ $(echo "$VERSIONS" | jq 'length') versions"
  fi

  # 5. Assembler le payload escouade
  SQUAD_PAYLOAD=$(jq -n \
    --arg sid "$SQUAD_ID" \
    --argjson bid "$BOARD_ID" \
    --arg bname "$BOARD_REAL_NAME" \
    --arg pkey "$PROJECT_KEY" \
    --argjson active "$ACTIVE_PAYLOAD" \
    --argjson closed "$CLOSED_PAYLOAD" \
    --argjson future "$FUTURE_PAYLOAD" \
    --argjson versions "$VERSIONS" \
    '{
      squadId: $sid,
      boardId: $bid,
      boardName: $bname,
      projectKey: $pkey,
      activeSprint: $active,
      closedSprints: $closed,
      futureSprints: $future,
      versions: $versions
    }')

  SQUAD_PAYLOADS=$(echo "$SQUAD_PAYLOADS" | jq --argjson sp "$SQUAD_PAYLOAD" '. + [$sp]')

  echo "  ✓ Payload assemblé"
done

# ── Écrire le payload bulk final ──

BULK_PAYLOAD=$(jq -n \
  --argjson squads "$SQUAD_PAYLOADS" \
  --arg runId "${GITHUB_RUN_ID}" \
  --arg triggeredBy "${GITHUB_TRIGGERING_ACTOR:-schedule}" \
  '{
    squads: $squads,
    runId: $runId,
    triggeredBy: $triggeredBy
  }')

echo "$BULK_PAYLOAD" > /tmp/bulk-payload.json

TOTAL_SIZE=$(wc -c < /tmp/bulk-payload.json)
echo ""
echo "═══ Payload prêt: $(echo "$SQUAD_PAYLOADS" | jq 'length') escouades, ${TOTAL_SIZE} bytes ═══"
echo "::endgroup::"
