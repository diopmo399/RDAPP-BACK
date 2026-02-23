package com.rdapp.solutionrdapp.dashboardservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        private int cachedComparisons;
        private int newComparisons;
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
        private String tagCommitSha;                // ← AJOUT
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
        private String headCommitDate;              // ← AJOUT
        private String baseCommitDate;              // ← AJOUT
        private List<CommitIngest> commits;
        private List<FileIngest> files;
        private boolean fromCache;
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
    // Comparison Lookup (cache)
    // ══════════════════════════════════════════════════

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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ComparisonLookupResponse {
        private Map<String, ComparisonIngest> cached;
        private int totalRequested;
        private int totalCached;
        private int totalMissing;
    }
}
