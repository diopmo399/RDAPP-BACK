package com.rdapp.solutionrdapp.dashboardservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs pour l'ingestion des déploiements depuis GitHub Actions.
 *
 * Flux :
 *   GHA → POST /api/v1/deploy/comparisons/lookup  (vérifie le cache)
 *   GHA → Portail Infonuagique                     (récupère déploiements)
 *   GHA → GitHub Compare API                       (seulement si pas en cache)
 *   GHA → POST /api/v1/deploy/ingest               (envoie les résultats)
 */
public final class DeployIngestDto {

    private DeployIngestDto() {}

    // ══════════════════════════════════════════════════
    // Ingest Payload (GHA → API)
    // ══════════════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class IngestPayload {
        private List<OrgEnvResult> results;
        private String runId;
        private String triggeredBy;
        private IngestMetadata metadata;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class IngestMetadata {
        private int totalDeployments;
        private int totalComparisons;
        private int cachedComparisons;  // comparaisons réutilisées depuis le cache
        private int newComparisons;     // nouvelles comparaisons calculées
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class OrgEnvResult {
        private String organisation;
        private String environnement;
        private List<DeploymentIngest> deployments;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class DeploymentIngest {
        private String produit;
        private String version;
        private String environnement;
        private String timestamp;
        private String namespace;
        private ComparisonIngest comparaison;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ComparisonIngest {
        private String baseVersion;
        private String headVersion;
        private String status;
        private int aheadBy;
        private int behindBy;
        private int totalCommits;
        private int filesChanged;
        private int additions;
        private int deletions;
        private List<CommitIngest> commits;
        private List<FileIngest> files;
        private boolean fromCache;     // true = réutilisé, false = nouveau
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CommitIngest {
        private String sha;
        private String message;
        private String author;
        private String date;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class FileIngest {
        private String filename;
        private String status;
        private int additions;
        private int deletions;
        private int changes;
    }

    // ══════════════════════════════════════════════════
    // Ingest Response
    // ══════════════════════════════════════════════════

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngestResponse {
        private int totalDeploymentsSaved;
        private int totalComparisonsSaved;
        private int environmentsProcessed;
        private List<String> errors;
        private String runId;
        private LocalDateTime ingestedAt;
    }

    // ══════════════════════════════════════════════════
    // Comparison Lookup (cache check)
    // ══════════════════════════════════════════════════

    /**
     * GHA envoie la liste des paires (produit, base, head) à vérifier.
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ComparisonLookupRequest {
        private List<ComparisonPair> pairs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ComparisonPair {
        private String produit;
        private String baseVersion;
        private String headVersion;
    }

    /**
     * L'API retourne les comparaisons existantes.
     *
     * Clé  : "produit|baseVersion|headVersion"
     * Valeur : ComparisonIngest si en cache, null sinon
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ComparisonLookupResponse {
        private Map<String, ComparisonIngest> cached;
        private int totalRequested;
        private int totalCached;
        private int totalMissing;
    }
}
