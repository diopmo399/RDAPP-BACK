#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════
# push-to-api.sh
#
# Envoie le payload Jira agrégé vers l'API Deploy Config.
#
# Input  : /tmp/bulk-payload.json (généré par fetch-jira.sh)
# Output : /tmp/ingest-response.json
#
# Variables d'env requises :
#   DEPLOY_API_URL, DEPLOY_API_TOKEN
# ════════════════════════════════════════════════════════════

set -euo pipefail

PAYLOAD_FILE="/tmp/bulk-payload.json"
RESPONSE_FILE="/tmp/ingest-response.json"

if [ ! -f "$PAYLOAD_FILE" ]; then
  echo "::error::Payload non trouvé: $PAYLOAD_FILE"
  exit 1
fi

SQUAD_COUNT=$(jq '.squads | length' "$PAYLOAD_FILE")
PAYLOAD_SIZE=$(wc -c < "$PAYLOAD_FILE")

echo "::group::Pushing to API"
echo "API URL: ${DEPLOY_API_URL}/batch/ingest/bulk"
echo "Escouades: $SQUAD_COUNT"
echo "Payload: ${PAYLOAD_SIZE} bytes"

# ── Retry loop ──

MAX_RETRIES=3
ATTEMPT=1
SUCCESS=false

while [ $ATTEMPT -le $MAX_RETRIES ]; do
  echo ""
  echo "→ Tentative $ATTEMPT/$MAX_RETRIES..."

  HTTP_CODE=$(curl -s -o "$RESPONSE_FILE" -w "%{http_code}" \
    -X POST \
    -H "Authorization: Bearer ${DEPLOY_API_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "X-GHA-Run-Id: ${GITHUB_RUN_ID}" \
    -H "X-GHA-Actor: ${GITHUB_TRIGGERING_ACTOR:-schedule}" \
    -d @"$PAYLOAD_FILE" \
    "${DEPLOY_API_URL}/batch/ingest/bulk" 2>/dev/null) || HTTP_CODE=0

  echo "  HTTP $HTTP_CODE"

  if [ "$HTTP_CODE" -eq 201 ] || [ "$HTTP_CODE" -eq 200 ]; then
    SUCCESS=true
    break
  fi

  # Afficher le body d'erreur si disponible
  if [ -f "$RESPONSE_FILE" ]; then
    echo "  Response:"
    cat "$RESPONSE_FILE" | jq '.' 2>/dev/null || cat "$RESPONSE_FILE"
  fi

  if [ "$HTTP_CODE" -ge 400 ] && [ "$HTTP_CODE" -lt 500 ]; then
    echo "::error::Erreur client ($HTTP_CODE) — pas de retry"
    break
  fi

  ATTEMPT=$((ATTEMPT + 1))
  if [ $ATTEMPT -le $MAX_RETRIES ]; then
    SLEEP=$((ATTEMPT * 5))
    echo "  → Retry dans ${SLEEP}s..."
    sleep $SLEEP
  fi
done

echo ""

if [ "$SUCCESS" = true ]; then
  echo "✓ Ingest réussi!"
  echo ""
  echo "Résultat:"
  jq '.' "$RESPONSE_FILE"

  # Vérifier les erreurs partielles
  ERRORS=$(jq '.errors | length // 0' "$RESPONSE_FILE")
  if [ "$ERRORS" -gt 0 ]; then
    echo ""
    echo "::warning::$ERRORS erreur(s) partielle(s) lors de l'ingest"
    jq -r '.errors[]' "$RESPONSE_FILE"
  fi
else
  echo "::error::Ingest échoué après $MAX_RETRIES tentatives (HTTP $HTTP_CODE)"

  # Créer une réponse d'erreur pour le summary
  jq -n \
    --arg err "Ingest failed: HTTP $HTTP_CODE" \
    --arg runId "${GITHUB_RUN_ID}" \
    '{squadsProcessed: 0, totalSprintsSaved: 0, totalIssuesSaved: 0, totalVersionsSaved: 0, errors: [$err], runId: $runId}' \
    > "$RESPONSE_FILE"

  exit 1
fi

echo "::endgroup::"
