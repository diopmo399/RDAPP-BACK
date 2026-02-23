package com.rdapp.deploy.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs pour l'ingestion batch depuis GitHub Actions.
 * GHA appelle Jira DC, puis POST les résultats ici.
 */
public final class BatchIngestDto {

    private BatchIngestDto() {}

    // ── Payload complet envoyé par GHA ──

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class IngestPayload {
        private String squadId;
        private Long boardId;
        private String boardName;
        private String projectKey;
        private SprintIngest activeSprint;
        private List<SprintIngest> closedSprints;
        private List<SprintIngest> futureSprints;
        private List<VersionIngest> versions;
        private String runId;
        private String triggeredBy;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class SprintIngest {
        private Long jiraSprintId;
        private String name;
        private String state;
        private String goal;
        private String startDate;
        private String endDate;
        private String completeDate;
        private List<IssueIngest> issues;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class IssueIngest {
        private String key;
        private String summary;
        private String issueType;
        private String statusName;
        private String statusCategory;
        private String priority;
        private Double storyPoints;
        private String assigneeName;
        private String assigneeUsername;
        private String fixVersion;
        private String created;
        private String updated;
        private String resolutionDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class VersionIngest {
        private String jiraId;
        private String name;
        private String description;
        private boolean released;
        private boolean archived;
        private String releaseDate;
    }

    // ── Response ──

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IngestResponse {
        private String squadId;
        private int sprintsSaved;
        private int issuesSaved;
        private int versionsSaved;
        private String runId;
        private LocalDateTime ingestedAt;
    }

    // ── Bulk (toutes les escouades d'un coup) ──

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class BulkIngestPayload {
        private List<IngestPayload> squads;
        private String runId;
        private String triggeredBy;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkIngestResponse {
        private int squadsProcessed;
        private int totalSprintsSaved;
        private int totalIssuesSaved;
        private int totalVersionsSaved;
        private List<String> errors;
        private String runId;
        private LocalDateTime ingestedAt;
    }
}
