package com.rdapp.solutionrdapp.dashboardservice.controller;

import com.rdapp.solutionrdapp.dashboardservice.dto.DeployIngestDto.*;
import com.rdapp.solutionrdapp.dashboardservice.service.DeployIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints d'ingestion des déploiements depuis GitHub Actions.
 *
 * POST /api/v1/deploy/comparisons/lookup
 *   → GHA vérifie quelles comparaisons existent déjà (évite de recalculer)
 *
 * POST /api/v1/deploy/ingest
 *   ← GHA envoie les déploiements + comparaisons (nouvelles + cached)
 */
@RestController
@RequestMapping("/api/v1/deploy")
@RequiredArgsConstructor
@Slf4j
public class DeployIngestController {

    private final DeployIngestService deployIngestService;

    // ══════════════════════════════════════════════════
    // 1. Lookup : GHA demande "quels compares existent déjà ?"
    // ══════════════════════════════════════════════════

    /**
     * POST /api/v1/deploy/comparisons/lookup
     *
     * GHA envoie :
     * {
     *   "pairs": [
     *     {"produit": "oc-interaction-rdapp-spa", "baseVersion": "2026.S1.3", "headVersion": "2026.S2.1"},
     *     {"produit": "oc-interaction-rdapp-spa", "baseVersion": "2026.S2.1", "headVersion": "2026.S2.2"}
     *   ]
     * }
     *
     * L'API retourne :
     * {
     *   "cached": {
     *     "oc-interaction-rdapp-spa|2026.S1.3|2026.S2.1": { ...comparaisonData... },
     *   },
     *   "totalRequested": 2,
     *   "totalCached": 1,
     *   "totalMissing": 1
     * }
     *
     * GHA n'appelle GitHub Compare que pour les paires manquantes.
     */
    @PostMapping("/comparisons/lookup")
    public ResponseEntity<ComparisonLookupResponse> lookupComparisons(
            @RequestBody ComparisonLookupRequest request) {

        log.info("Comparison lookup — {} pairs",
                request.getPairs() != null ? request.getPairs().size() : 0);

        var response = deployIngestService.lookupComparisons(request);

        log.info("Comparison lookup result — cached={} missing={}",
                response.getTotalCached(), response.getTotalMissing());

        return ResponseEntity.ok(response);
    }

    // ══════════════════════════════════════════════════
    // 2. Ingest : GHA envoie les résultats finaux
    // ══════════════════════════════════════════════════

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest(
            @RequestBody IngestPayload payload,
            @RequestHeader(value = "X-GHA-Run-Id", required = false) String ghaRunId) {

        log.info("Deploy ingest — results={} runId={} ghaRunId={}",
                payload.getResults() != null ? payload.getResults().size() : 0,
                payload.getRunId(), ghaRunId);

        var response = deployIngestService.ingest(payload);

        var status = (response.getErrors() == null || response.getErrors().isEmpty())
                ? HttpStatus.CREATED
                : HttpStatus.MULTI_STATUS;

        return ResponseEntity.status(status).body(response);
    }
}
