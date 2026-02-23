package com.rdapp.deploy.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * DTOs Jira Data Center REST API.
 *
 * Agile API : /rest/agile/1.0
 * REST API  : /rest/api/2
 */
public final class JiraDtos {

    private JiraDtos() {}

    // ══════════════════════════════════════════
    // Wrapper paginé Jira (commun)
    // ══════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PagedResult<T> {
        private int maxResults;
        private int startAt;
        private int total;
        private boolean isLast;
        private List<T> values;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueSearchResult {
        private int maxResults;
        private int startAt;
        private int total;
        private List<JiraIssue> issues;
    }

    // ══════════════════════════════════════════
    // Board — /rest/agile/1.0/board/{boardId}
    // ══════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraBoard {
        private long id;
        private String name;
        private String type;   // scrum, kanban
        private JiraProject location;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraProject {
        private long projectId;
        private String projectKey;
        private String projectName;
        private String displayName;
    }

    // ══════════════════════════════════════════
    // Sprint — /rest/agile/1.0/board/{boardId}/sprint
    // ══════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraSprint {
        private long id;
        private String name;
        private String state;         // active, closed, future
        private String startDate;     // ISO-8601
        private String endDate;
        private String completeDate;
        private long originBoardId;
        private String goal;
    }

    // ══════════════════════════════════════════
    // Issue — /rest/agile/1.0/sprint/{sprintId}/issue
    // ══════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraIssue {
        private String id;
        private String key;           // e.g. ORC-312
        private String self;
        private JiraIssueFields fields;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraIssueFields {
        private String summary;
        private JiraIssueType issuetype;
        private JiraStatus status;
        private JiraPriority priority;
        private JiraUser assignee;
        private JiraUser creator;

        @JsonProperty("customfield_10016")  // Story Points (champ standard Jira Software)
        private Double storyPoints;

        @JsonProperty("customfield_10004")  // Sprint field (backup)
        private Object sprint;

        private List<JiraVersion> fixVersions;
        private List<JiraVersion> versions;  // Affect versions
        private List<JiraComponent> components;

        private String created;
        private String updated;
        private String resolutiondate;

        // Pour les sub-tasks
        private JiraIssue parent;
        private List<JiraIssue> subtasks;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraIssueType {
        private String id;
        private String name;           // Story, Bug, Task, Epic, Sub-task
        private boolean subtask;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraStatus {
        private String id;
        private String name;           // To Do, In Progress, In Review, Done
        private JiraStatusCategory statusCategory;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraStatusCategory {
        private int id;
        private String key;            // new, indeterminate, done
        private String name;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraPriority {
        private String id;
        private String name;           // Highest, High, Medium, Low, Lowest
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraUser {
        private String key;
        private String name;           // username
        private String displayName;
        private String emailAddress;
    }

    // ══════════════════════════════════════════
    // Version — /rest/api/2/project/{key}/versions
    // ══════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraVersion {
        private String id;
        private String name;
        private String description;
        private boolean released;
        private boolean archived;
        private String releaseDate;    // yyyy-MM-dd
        private String projectId;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JiraComponent {
        private String id;
        private String name;
    }

    // ══════════════════════════════════════════
    // Velocity — /rest/greenhopper/1.0/rapid/charts/velocity?rapidViewId={boardId}
    // ══════════════════════════════════════════

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VelocityReport {
        private VelocityStats velocityStatEntries;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VelocityStats {
        // Map<sprintId, SprintVelocity>
        // Structure complexe — on le parse manuellement
    }
}
