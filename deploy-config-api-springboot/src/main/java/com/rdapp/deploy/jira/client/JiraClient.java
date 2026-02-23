package com.rdapp.deploy.jira.client;

import com.rdapp.deploy.jira.config.JiraProperties;
import com.rdapp.deploy.jira.dto.JiraDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Client REST pour Jira Data Center.
 *
 * Endpoints utilisés :
 *   Agile /rest/agile/1.0/board/{boardId}                       → Board info
 *   Agile /rest/agile/1.0/board/{boardId}/sprint                → Sprints du board
 *   Agile /rest/agile/1.0/sprint/{sprintId}/issue               → Issues du sprint
 *   REST  /rest/api/2/project/{projectKey}/versions              → Versions du projet
 *   REST  /rest/api/2/search?jql=...                             → JQL search
 */
@Component
@Slf4j
public class JiraClient {

    private final RestClient restClient;
    private final JiraProperties props;

    public JiraClient(RestClient jiraRestClient, JiraProperties props) {
        this.restClient = jiraRestClient;
        this.props = props;
    }

    // ══════════════════════════════════════════
    // Board
    // ══════════════════════════════════════════

    @Cacheable(value = "jira-boards", key = "#boardId")
    public JiraBoard getBoard(long boardId) {
        log.debug("Jira → GET board {}", boardId);
        return callWithRetry(() ->
                restClient.get()
                        .uri("/rest/agile/1.0/board/{id}", boardId)
                        .retrieve()
                        .body(JiraBoard.class)
        );
    }

    /**
     * Cherche un board par son nom (ex: "BPMN-42").
     * Retourne le premier résultat correspondant.
     */
    @Cacheable(value = "jira-boards", key = "'name:' + #boardName")
    public JiraBoard findBoardByName(String boardName) {
        log.debug("Jira → search board name={}", boardName);
        var result = callWithRetry(() ->
                restClient.get()
                        .uri("/rest/agile/1.0/board?name={name}&maxResults=5", boardName)
                        .retrieve()
                        .body(new ParameterizedTypeReference<PagedResult<JiraBoard>>() {})
        );
        if (result == null || result.getValues() == null || result.getValues().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Board Jira non trouvé: " + boardName);
        }
        return result.getValues().getFirst();
    }

    // ══════════════════════════════════════════
    // Sprints
    // ══════════════════════════════════════════

    /**
     * Liste tous les sprints d'un board (paginé automatiquement).
     */
    @Cacheable(value = "jira-sprints", key = "#boardId")
    public List<JiraSprint> getSprintsForBoard(long boardId) {
        log.debug("Jira → GET sprints for board {}", boardId);
        return fetchAllPaged(
                "/rest/agile/1.0/board/{id}/sprint",
                boardId,
                new ParameterizedTypeReference<PagedResult<JiraSprint>>() {}
        );
    }

    /**
     * Retourne le sprint actif d'un board (state=active).
     */
    public JiraSprint getActiveSprint(long boardId) {
        var sprints = getSprintsForBoard(boardId);
        return sprints.stream()
                .filter(s -> "active".equalsIgnoreCase(s.getState()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retourne les sprints fermés (state=closed), triés du plus récent au plus ancien.
     */
    public List<JiraSprint> getClosedSprints(long boardId) {
        var sprints = getSprintsForBoard(boardId);
        return sprints.stream()
                .filter(s -> "closed".equalsIgnoreCase(s.getState()))
                .sorted((a, b) -> {
                    if (a.getCompleteDate() == null) return 1;
                    if (b.getCompleteDate() == null) return -1;
                    return b.getCompleteDate().compareTo(a.getCompleteDate());
                })
                .toList();
    }

    /**
     * Retourne les sprints futurs (state=future).
     */
    public List<JiraSprint> getFutureSprints(long boardId) {
        var sprints = getSprintsForBoard(boardId);
        return sprints.stream()
                .filter(s -> "future".equalsIgnoreCase(s.getState()))
                .toList();
    }

    @Cacheable(value = "jira-sprint-detail", key = "#sprintId")
    public JiraSprint getSprint(long sprintId) {
        log.debug("Jira → GET sprint {}", sprintId);
        return callWithRetry(() ->
                restClient.get()
                        .uri("/rest/agile/1.0/sprint/{id}", sprintId)
                        .retrieve()
                        .body(JiraSprint.class)
        );
    }

    // ══════════════════════════════════════════
    // Issues
    // ══════════════════════════════════════════

    /**
     * Récupère toutes les issues d'un sprint (paginé).
     */
    @Cacheable(value = "jira-sprint-issues", key = "#sprintId")
    public List<JiraIssue> getSprintIssues(long sprintId) {
        log.debug("Jira → GET issues for sprint {}", sprintId);
        return fetchAllSprintIssues(sprintId);
    }

    /**
     * Recherche JQL libre.
     */
    public List<JiraIssue> searchIssues(String jql, int maxResults) {
        log.debug("Jira → JQL search: {}", jql);
        var result = callWithRetry(() ->
                restClient.get()
                        .uri("/rest/api/2/search?jql={jql}&maxResults={max}&fields=summary,status,issuetype,priority,assignee,creator,customfield_10016,fixVersions,components,created,updated,resolutiondate",
                                jql, maxResults)
                        .retrieve()
                        .body(IssueSearchResult.class)
        );
        return result != null && result.getIssues() != null ? result.getIssues() : List.of();
    }

    // ══════════════════════════════════════════
    // Versions (Affect Versions)
    // ══════════════════════════════════════════

    /**
     * Récupère les versions (Affect Versions) d'un projet Jira.
     */
    @Cacheable(value = "jira-versions", key = "#projectKey")
    public List<JiraVersion> getProjectVersions(String projectKey) {
        log.debug("Jira → GET versions for project {}", projectKey);
        return callWithRetry(() ->
                restClient.get()
                        .uri("/rest/api/2/project/{key}/versions", projectKey)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<JiraVersion>>() {})
        );
    }

    // ══════════════════════════════════════════
    // Pagination helper (Agile API)
    // ══════════════════════════════════════════

    private <T> List<T> fetchAllPaged(String uriTemplate, long id,
                                       ParameterizedTypeReference<PagedResult<T>> typeRef) {
        var allItems = new ArrayList<T>();
        int startAt = 0;
        boolean hasMore = true;

        while (hasMore) {
            final int offset = startAt;
            var page = callWithRetry(() ->
                    restClient.get()
                            .uri(uriTemplate + "?startAt={start}&maxResults=50", id, offset)
                            .retrieve()
                            .body(typeRef)
            );
            if (page == null || page.getValues() == null || page.getValues().isEmpty()) break;
            allItems.addAll(page.getValues());
            startAt += page.getValues().size();
            hasMore = !page.isLast() && startAt < page.getTotal();
        }
        return allItems;
    }

    private List<JiraIssue> fetchAllSprintIssues(long sprintId) {
        var allIssues = new ArrayList<JiraIssue>();
        int startAt = 0;
        boolean hasMore = true;

        while (hasMore) {
            final int offset = startAt;
            var result = callWithRetry(() ->
                    restClient.get()
                            .uri("/rest/agile/1.0/sprint/{id}/issue?startAt={start}&maxResults=50&fields=summary,status,issuetype,priority,assignee,creator,customfield_10016,fixVersions,components,created,updated,resolutiondate",
                                    sprintId, offset)
                            .retrieve()
                            .body(IssueSearchResult.class)
            );
            if (result == null || result.getIssues() == null || result.getIssues().isEmpty()) break;
            allIssues.addAll(result.getIssues());
            startAt += result.getIssues().size();
            hasMore = startAt < result.getTotal();
        }
        return allIssues;
    }

    // ══════════════════════════════════════════
    // Retry helper
    // ══════════════════════════════════════════

    private <T> T callWithRetry(java.util.function.Supplier<T> call) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= props.getMaxRetries(); attempt++) {
            try {
                return call.get();
            } catch (Exception ex) {
                lastException = ex;
                log.warn("Jira call failed (attempt {}/{}): {}", attempt, props.getMaxRetries(), ex.getMessage());
                if (attempt < props.getMaxRetries()) {
                    try {
                        Thread.sleep((long) props.getRetryDelayMs() * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Jira call failed after {} retries", props.getMaxRetries(), lastException);
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Erreur de communication avec Jira: " + (lastException != null ? lastException.getMessage() : "unknown"));
    }
}
