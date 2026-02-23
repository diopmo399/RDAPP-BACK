#!/usr/bin/env bash
# ══════════════════════════════════════════════════════════════
# push-deployments.sh
#
# Envoie le payload de /tmp/deploy-payload.json vers l'API.
# POST /v1/deploy/ingest
# ══════════════════════════════════════════════════════════════

set -euo pipefail

PAYLOAD_FILE="/tmp/deploy-payload.json"

if [ ! -f "$PAYLOAD_FILE" ]; then
  echo "::error::Payload file not found: $PAYLOAD_FILE"
  exit 1
fi

echo "::group::Pushing to API"
echo "  API URL: ${DASHBOARD_API_URL}/v1/deploy/ingest"

# Stats du payload
ORGENV_COUNT=$(jq '.results | length' "$PAYLOAD_FILE")
DEPLOY_COUNT=$(jq '.metadata.totalDeployments // 0' "$PAYLOAD_FILE")
COMP_COUNT=$(jq '.metadata.totalComparisons // 0' "$PAYLOAD_FILE")
PAYLOAD_SIZE=$(wc -c < "$PAYLOAD_FILE")

echo "  Org/Env pairs: $ORGENV_COUNT"
echo "  Déploiements: $DEPLOY_COUNT"
echo "  Comparaisons: $COMP_COUNT"
echo "  Payload: ${PAYLOAD_SIZE} bytes"

# Envoyer avec retry
MAX_RETRIES=3
ATTEMPT=1
SUCCESS=false

while [ $ATTEMPT -le $MAX_RETRIES ]; do
  echo ""
  echo "  → POST attempt $ATTEMPT/$MAX_RETRIES..."

  HTTP_CODE=$(curl -sf -w "%{http_code}" -o /tmp/ingest-response.json \
    -X POST \
    -H "Authorization: Bearer ${DASHBOARD_API_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "X-GHA-Run-Id: ${GITHUB_RUN_ID:-local}" \
    -H "X-GHA-Actor: ${GITHUB_TRIGGERING_ACTOR:-unknown}" \
    -d @"$PAYLOAD_FILE" \
    "${DASHBOARD_API_URL}/v1/deploy/ingest" 2>/dev/null) || HTTP_CODE="000"

  echo "  ← HTTP $HTTP_CODE"

  if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "207" ]; then
    SUCCESS=true
    echo "  ✓ Ingest successful"

    if [ -f /tmp/ingest-response.json ]; then
      echo ""
      echo "  Response:"
      jq '.' /tmp/ingest-response.json 2>/dev/null || cat /tmp/ingest-response.json
    fi

    if [ "$HTTP_CODE" = "207" ]; then
      echo ""
      echo "  ⚠ Partial success (207) — some errors occurred"
      jq -r '.errors[]? // empty' /tmp/ingest-response.json 2>/dev/null
    fi

    break
  fi

  echo "  ✗ Failed (HTTP $HTTP_CODE)"
  ATTEMPT=$((ATTEMPT + 1))

  if [ $ATTEMPT -le $MAX_RETRIES ]; then
    WAIT=$((ATTEMPT * 5))
    echo "  Retrying in ${WAIT}s..."
    sleep $WAIT
  fi
done

echo "::endgroup::"

if [ "$SUCCESS" = "false" ]; then
  echo "::error::Ingest failed after $MAX_RETRIES attempts"

  # Créer une réponse d'erreur pour le summary
  jq -n --arg runId "${GITHUB_RUN_ID:-local}" '{
    totalDeploymentsSaved: 0,
    totalComparisonsSaved: 0,
    environmentsProcessed: 0,
    errors: ["Ingest failed after max retries"],
    runId: $runId
  }' > /tmp/ingest-response.json

  exit 1
fi
