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

  # Vérifier que la réponse est un JSON valide
  if ! echo "$response" | jq empty 2>/dev/null; then
    echo "  ✗ Invalid JSON response from: $url" >&2
    echo "  Response: ${response:0:200}" >&2
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
  local temp_issues="/tmp/issues_${sprint_id}.json"

  echo "[]" > "$temp_issues"

  while true; do
    local url="${AGILE_API}/sprint/${sprint_id}/issue?startAt=${start_at}&maxResults=${max_results}&fields=summary,status,issuetype,priority,assignee,creator,customfield_10016,fixVersions,versions,components,created,updated,resolutiondate"
    local page
    page=$(jira_get "$url") || break

    local issues
    issues=$(echo "$page" | jq '.issues // []')
    local count
    count=$(echo "$issues" | jq 'length')

    if [ "$count" -eq 0 ]; then break; fi

    # Transformer les issues au format attendu par l'API et ajouter au fichier
    echo "$issues" | jq '[.[] | {
      key: (.key // "UNKNOWN"),
      summary: (.fields.summary // "No summary"),
      issueType: (.fields.issuetype.name // "Unknown"),
      statusName: (.fields.status.name // "Unknown"),
      statusCategory: (.fields.status.statusCategory.key // "new"),
      priority: (.fields.priority.name // "None"),
      storyPoints: (.fields.customfield_10016 // null),
      assigneeName: (.fields.assignee.displayName // null),
      assigneeUsername: (.fields.assignee.name // null),
      fixVersion: ((.fields.fixVersions // []) | first | .name // null),
      affectVersion: ((.fields.versions // []) | first | .name // null),
      created: (.fields.created // null),
      updated: (.fields.updated // null),
      resolutionDate: (.fields.resolutiondate // null)
    }]' > "/tmp/issues_${sprint_id}_page.json"

    jq -s '.[0] + .[1]' "$temp_issues" "/tmp/issues_${sprint_id}_page.json" > "/tmp/issues_${sprint_id}_new.json"
    mv "/tmp/issues_${sprint_id}_new.json" "$temp_issues"
    rm -f "/tmp/issues_${sprint_id}_page.json"

    local total
    total=$(echo "$page" | jq '.total // 0')
    start_at=$((start_at + count))
    if [ "$start_at" -ge "$total" ]; then break; fi
  done

  cat "$temp_issues"
  rm -f "$temp_issues"
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
SQUAD_PAYLOADS_FILE="/tmp/squad_payloads.json"
echo "[]" > "$SQUAD_PAYLOADS_FILE"

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
  SPRINTS_TEMP="/tmp/sprints_${BOARD_ID}.json"
  echo "[]" > "$SPRINTS_TEMP"
  START_AT=0
  while true; do
    PAGE=$(jira_get "${AGILE_API}/board/${BOARD_ID}/sprint?startAt=${START_AT}&maxResults=50") || break
    VALUES=$(echo "$PAGE" | jq '.values // []')
    COUNT=$(echo "$VALUES" | jq 'length')
    [ "$COUNT" -eq 0 ] && break

    echo "$VALUES" > "/tmp/sprints_${BOARD_ID}_page.json"
    jq -s '.[0] + .[1]' "$SPRINTS_TEMP" "/tmp/sprints_${BOARD_ID}_page.json" > "/tmp/sprints_${BOARD_ID}_new.json"
    mv "/tmp/sprints_${BOARD_ID}_new.json" "$SPRINTS_TEMP"
    rm -f "/tmp/sprints_${BOARD_ID}_page.json"

    IS_LAST=$(echo "$PAGE" | jq '.isLast // true')
    [ "$IS_LAST" = "true" ] && break
    START_AT=$((START_AT + COUNT))
  done

  ALL_SPRINTS_RAW=$(cat "$SPRINTS_TEMP")
  rm -f "$SPRINTS_TEMP"

  # Filtrer les sprints avec validation
  if ! echo "$ALL_SPRINTS_RAW" | jq empty 2>/dev/null; then
    echo "  ✗ Invalid sprints JSON data" >&2
    ACTIVE_SPRINTS="[]"
    CLOSED_SPRINTS="[]"
    FUTURE_SPRINTS="[]"
  else
    echo "$ALL_SPRINTS_RAW" | jq '[.[] | select(.state == "active")]' > "/tmp/sprints_active_${BOARD_ID}.json" 2>/dev/null || echo "[]" > "/tmp/sprints_active_${BOARD_ID}.json"
    echo "$ALL_SPRINTS_RAW" | jq --arg max "$MAX_CLOSED" \
      '[.[] | select(.state == "closed")] | sort_by(.completeDate) | reverse | .[0:($max|tonumber)]' > "/tmp/sprints_closed_${BOARD_ID}.json" 2>/dev/null || echo "[]" > "/tmp/sprints_closed_${BOARD_ID}.json"
    echo "$ALL_SPRINTS_RAW" | jq '[.[] | select(.state == "future")]' > "/tmp/sprints_future_${BOARD_ID}.json" 2>/dev/null || echo "[]" > "/tmp/sprints_future_${BOARD_ID}.json"
  fi

  ACTIVE_SPRINTS=$(cat "/tmp/sprints_active_${BOARD_ID}.json")
  CLOSED_SPRINTS=$(cat "/tmp/sprints_closed_${BOARD_ID}.json")
  FUTURE_SPRINTS=$(cat "/tmp/sprints_future_${BOARD_ID}.json")

  ACTIVE_COUNT=$(echo "$ACTIVE_SPRINTS" | jq 'length' 2>/dev/null || echo "0")
  CLOSED_COUNT=$(echo "$CLOSED_SPRINTS" | jq 'length' 2>/dev/null || echo "0")
  FUTURE_COUNT=$(echo "$FUTURE_SPRINTS" | jq 'length' 2>/dev/null || echo "0")

  # Valider que les counts sont des nombres
  [[ "$ACTIVE_COUNT" =~ ^[0-9]+$ ]] || ACTIVE_COUNT=0
  [[ "$CLOSED_COUNT" =~ ^[0-9]+$ ]] || CLOSED_COUNT=0
  [[ "$FUTURE_COUNT" =~ ^[0-9]+$ ]] || FUTURE_COUNT=0

  echo "  ✓ Sprints: $ACTIVE_COUNT active, $CLOSED_COUNT closed, $FUTURE_COUNT future"

  # 3. Récupérer les issues pour chaque sprint
  process_sprint() {
    local sprint_json="$1"
    local sprint_id=$(echo "$sprint_json" | jq -r '.id // empty')
    local sprint_name=$(echo "$sprint_json" | jq -r '.name // "Unknown"')
    local sprint_state=$(echo "$sprint_json" | jq -r '.state // "unknown"')

    # Vérifier que l'ID est valide
    if [ -z "$sprint_id" ] || [ "$sprint_id" = "null" ]; then
      echo "    ⚠ Sprint sans ID valide, skip"
      echo '{"jiraSprintId": null, "name": "Invalid", "state": "unknown", "issues": []}'
      return
    fi

    echo "    → Sprint '$sprint_name' (id=$sprint_id, state=$sprint_state)..."
    local issues
    issues=$(fetch_all_sprint_issues "$sprint_id")

    # Valider que issues est un JSON valide
    if [ -z "$issues" ] || ! echo "$issues" | jq empty 2>/dev/null; then
      echo "    ⚠ Issues JSON invalide, utilisation d'un tableau vide"
      issues="[]"
    fi

    local issue_count=$(echo "$issues" | jq 'length' 2>/dev/null || echo "0")
    echo "      ✓ $issue_count issues"

    echo "$sprint_json" | jq --argjson issues "$issues" '{
      jiraSprintId: .id,
      name: .name,
      state: .state,
      goal: (.goal // ""),
      startDate: .startDate,
      endDate: .endDate,
      completeDate: .completeDate,
      issues: $issues
    }'
  }

  # Sprint actif
  ACTIVE_PAYLOAD="null"
  if [ "$ACTIVE_COUNT" -gt 0 ]; then
    ACTIVE_PAYLOAD=$(process_sprint "$(echo "$ACTIVE_SPRINTS" | jq '.[0]')") || ACTIVE_PAYLOAD="null"
  fi

  # Sprints fermés
  CLOSED_TEMP="/tmp/closed_sprints_payload_${BOARD_ID}.json"
  echo "[]" > "$CLOSED_TEMP"
  if [ "$CLOSED_COUNT" -gt 0 ]; then
    for j in $(seq 0 $((CLOSED_COUNT - 1))); do
      SPRINT_DATA=$(process_sprint "$(echo "$CLOSED_SPRINTS" | jq ".[$j]")") || continue
      if [ -n "$SPRINT_DATA" ]; then
        echo "$SPRINT_DATA" > "/tmp/closed_sprint_${BOARD_ID}_${j}.json"
        jq -s '.[0] + [.[1]]' "$CLOSED_TEMP" "/tmp/closed_sprint_${BOARD_ID}_${j}.json" > "/tmp/closed_new_${BOARD_ID}.json" 2>/dev/null || cp "$CLOSED_TEMP" "/tmp/closed_new_${BOARD_ID}.json"
        mv "/tmp/closed_new_${BOARD_ID}.json" "$CLOSED_TEMP"
        rm -f "/tmp/closed_sprint_${BOARD_ID}_${j}.json"
      fi
    done
  fi
  CLOSED_PAYLOAD=$(cat "$CLOSED_TEMP")
  rm -f "$CLOSED_TEMP"

  # Sprints futurs (pas d'issues)
  FUTURE_PAYLOAD=$(echo "$FUTURE_SPRINTS" | jq '[.[] | {
    jiraSprintId: .id,
    name: (.name // "Unknown"),
    state: (.state // "future"),
    goal: (.goal // ""),
    startDate: .startDate,
    endDate: .endDate,
    completeDate: .completeDate,
    issues: []
  }]' 2>/dev/null || echo "[]")

  # 4. Versions du projet
  VERSIONS="[]"
  if [ -n "$PROJECT_KEY" ] && [ "$PROJECT_KEY" != "" ]; then
    echo "  → Récupération des versions ($PROJECT_KEY)..."
    VERSIONS=$(fetch_project_versions "$PROJECT_KEY")
    if [ -z "$VERSIONS" ] || ! echo "$VERSIONS" | jq empty 2>/dev/null; then
      echo "  ⚠ Versions JSON invalide, utilisation d'un tableau vide"
      VERSIONS="[]"
    fi
    echo "  ✓ $(echo "$VERSIONS" | jq 'length' 2>/dev/null || echo "0") versions"
  fi

  # Valider tous les payloads avant assemblage
  [ -z "$ACTIVE_PAYLOAD" ] && ACTIVE_PAYLOAD="null"
  [ -z "$CLOSED_PAYLOAD" ] && CLOSED_PAYLOAD="[]"
  [ -z "$FUTURE_PAYLOAD" ] && FUTURE_PAYLOAD="[]"

  # Valider que les payloads sont du JSON valide
  if ! echo "$ACTIVE_PAYLOAD" | jq empty 2>/dev/null; then
    echo "  ⚠ ACTIVE_PAYLOAD invalide, utilisation de null"
    ACTIVE_PAYLOAD="null"
  fi
  if ! echo "$CLOSED_PAYLOAD" | jq empty 2>/dev/null; then
    echo "  ⚠ CLOSED_PAYLOAD invalide, utilisation de []"
    CLOSED_PAYLOAD="[]"
  fi
  if ! echo "$FUTURE_PAYLOAD" | jq empty 2>/dev/null; then
    echo "  ⚠ FUTURE_PAYLOAD invalide, utilisation de []"
    FUTURE_PAYLOAD="[]"
  fi

  # Valider BOARD_ID (doit être un nombre)
  if ! [[ "$BOARD_ID" =~ ^[0-9]+$ ]]; then
    echo "  ⚠ BOARD_ID invalide ($BOARD_ID), utilisation de null"
    BOARD_ID="null"
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
    }' 2>/dev/null)

  # Vérifier que l'assemblage a réussi
  if [ -z "$SQUAD_PAYLOAD" ] || ! echo "$SQUAD_PAYLOAD" | jq empty 2>/dev/null; then
    echo "  ✗ Échec de l'assemblage du payload, skip squad"
    continue
  fi

  echo "$SQUAD_PAYLOAD" > "/tmp/squad_${SQUAD_ID}.json"
  jq -s '.[0] + [.[1]]' "$SQUAD_PAYLOADS_FILE" "/tmp/squad_${SQUAD_ID}.json" > "/tmp/squad_payloads_new.json"
  mv "/tmp/squad_payloads_new.json" "$SQUAD_PAYLOADS_FILE"
  rm -f "/tmp/squad_${SQUAD_ID}.json"

  # Afficher la taille du payload pour monitoring
  CURRENT_SIZE=$(wc -c < "$SQUAD_PAYLOADS_FILE" | tr -d ' ')
  echo "  ✓ Payload assemblé (taille actuelle: $((CURRENT_SIZE / 1024)) KB)"

  # Nettoyer les fichiers temporaires de sprints
  rm -f "/tmp/sprints_active_${BOARD_ID}.json" "/tmp/sprints_closed_${BOARD_ID}.json" "/tmp/sprints_future_${BOARD_ID}.json"
done

# ── Écrire le payload bulk final ──

SQUAD_PAYLOADS=$(cat "$SQUAD_PAYLOADS_FILE")

# Valider que SQUAD_PAYLOADS est un JSON valide
if [ -z "$SQUAD_PAYLOADS" ] || ! echo "$SQUAD_PAYLOADS" | jq empty 2>/dev/null; then
  echo "⚠️  SQUAD_PAYLOADS invalide, utilisation d'un tableau vide"
  SQUAD_PAYLOADS="[]"
fi

jq -n \
  --argjson squads "$SQUAD_PAYLOADS" \
  --arg runId "${GITHUB_RUN_ID:-unknown}" \
  --arg triggeredBy "${GITHUB_TRIGGERING_ACTOR:-schedule}" \
  '{
    squads: $squads,
    runId: $runId,
    triggeredBy: $triggeredBy
  }' > /tmp/bulk-payload.json

TOTAL_SIZE=$(wc -c < /tmp/bulk-payload.json | tr -d ' ')
TOTAL_SIZE_KB=$((TOTAL_SIZE / 1024))
TOTAL_SIZE_MB=$((TOTAL_SIZE_KB / 1024))

echo ""
if [ "$TOTAL_SIZE_MB" -gt 0 ]; then
  echo "═══ Payload prêt: $(cat "$SQUAD_PAYLOADS_FILE" | jq 'length') escouades, ${TOTAL_SIZE_MB} MB (${TOTAL_SIZE} bytes) ═══"
else
  echo "═══ Payload prêt: $(cat "$SQUAD_PAYLOADS_FILE" | jq 'length') escouades, ${TOTAL_SIZE_KB} KB (${TOTAL_SIZE} bytes) ═══"
fi

# Avertissement si le payload est très volumineux (> 10 MB)
if [ "$TOTAL_SIZE_MB" -gt 10 ]; then
  echo "⚠️  ATTENTION: Le payload est très volumineux (${TOTAL_SIZE_MB} MB). Considérez:"
  echo "   - Réduire MAX_CLOSED_SPRINTS (actuellement: $MAX_CLOSED)"
  echo "   - Limiter les champs récupérés dans les issues"
  echo "   - Paginer les requêtes vers l'API"
fi

# Nettoyage final
rm -f "$SQUAD_PAYLOADS_FILE"

echo "::endgroup::"
