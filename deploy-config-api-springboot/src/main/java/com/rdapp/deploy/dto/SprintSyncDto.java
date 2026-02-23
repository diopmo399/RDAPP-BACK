package com.rdapp.deploy.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public final class SprintSyncDto {

    private SprintSyncDto() {}

    // ── Sprint response ──

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SprintResponse {
        private Long id;
        private Long jiraSprintId;
        private String squadId;
        private String name;
        private String state;
        private String goal;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime completeDate;
        private Long boardId;
        private String projectKey;
        private Integer totalIssues;
        private Integer doneIssues;
        private Double totalStoryPoints;
        private Double doneStoryPoints;
        private Double completionPercent;
        private List<IssueResponse> issues;
        private LocalDateTime syncedAt;
    }

    // ── Issue response ──

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IssueResponse {
        private String issueKey;
        private String summary;
        private String issueType;
        private String statusName;
        private String statusCategory;
        private String priority;
        private Double storyPoints;
        private String assigneeName;
        private String assigneeUsername;
        private String fixVersion;
    }

    // ── Sync result response ──

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SyncResultResponse {
        private String squadId;
        private String squadName;
        private Long boardId;
        private String boardName;
        private SprintResponse activeSprint;
        private List<SprintResponse> closedSprints;
        private List<SprintResponse> futureSprints;
        private String error;
        private LocalDateTime syncedAt;
    }

    // ── Version sync result ──

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VersionSyncResponse {
        private String projectKey;
        private int totalFromJira;
        private int created;
        private int updated;
        private LocalDateTime syncedAt;
    }

    // ── Jira connection status ──

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class JiraStatusResponse {
        private boolean configured;
        private boolean connected;
        private String baseUrl;
        private String authType;
        private String error;
    }
}
