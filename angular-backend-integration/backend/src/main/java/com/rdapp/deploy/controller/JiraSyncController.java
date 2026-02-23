package com.rdapp.deploy.controller;

import com.rdapp.deploy.dto.SprintSyncDto.*;
import com.rdapp.deploy.entity.SprintIssue;
import com.rdapp.deploy.entity.SprintSync;
import com.rdapp.deploy.jira.client.JiraClient;
import com.rdapp.deploy.jira.config.JiraProperties;
import com.rdapp.deploy.jira.service.JiraSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints Jira sync.
 *
 * POST /v1/jira/sync/squad/{squadId}         → Sync sprint pour une escouade
 * POST /v1/jira/sync/all                      → Sync toutes les escouades
 * POST /v1/jira/sync/versions/{projectKey}    → Sync Affect Versions
 * GET  /v1/jira/sprints/squad/{squadId}       → Sprints synchronisés
 * GET  /v1/jira/sprints/squad/{squadId}/active → Sprint actif
 * GET  /v1/jira/status                        → Status connexion Jira
 */
@RestController
@RequestMapping("/v1/jira")
@RequiredArgsConstructor
@Slf4j
public class JiraSyncController {

    private final JiraSyncService syncService;
    private final JiraProperties jiraProps;
    private final JiraClient jiraClient;

    // ══════════════════════════════════════════
    // Sync
    // ══════════════════════════════════════════

    @PostMapping("/sync/squad/{squadId}")
    public SyncResultResponse syncSquad(@PathVariable String squadId) {
        log.info("API → sync sprint for squad {}", squadId);
        var result = syncService.syncSquadSprint(squadId);
        return mapSyncResult(result);
    }

    @PostMapping("/sync/all")
    public List<SyncResultResponse> syncAll() {
        log.info("API → sync all squads");
        return syncService.syncAllSquads().stream()
                .map(this::mapSyncResult)
                .toList();
    }

    @PostMapping("/sync/versions/{projectKey}")
    public VersionSyncResponse syncVersions(@PathVariable String projectKey) {
        log.info("API → sync versions for project {}", projectKey);
        var result = syncService.syncProjectVersions(projectKey);
        return VersionSyncResponse.builder()
                .projectKey(result.getProjectKey())
                .totalFromJira(result.getTotalFromJira())
                .created(result.getCreated())
                .updated(result.getUpdated())
                .syncedAt(result.getSyncedAt())
                .build();
    }

    // ══════════════════════════════════════════
    // Read (données locales synchronisées)
    // ══════════════════════════════════════════

    @GetMapping("/sprints/squad/{squadId}")
    public List<SprintResponse> getSprintsForSquad(@PathVariable String squadId) {
        return syncService.getAllSprintsForSquad(squadId).stream()
                .map(this::mapSprint)
                .toList();
    }

    @GetMapping("/sprints/squad/{squadId}/active")
    public SprintResponse getActiveSprintForSquad(@PathVariable String squadId) {
        var active = syncService.getActiveSprintForSquad(squadId);
        if (active == null) return null;
        return mapSprint(active);
    }

    @GetMapping("/sprints/squad/{squadId}/closed")
    public List<SprintResponse> getClosedSprintsForSquad(@PathVariable String squadId) {
        return syncService.getClosedSprintsForSquad(squadId).stream()
                .map(this::mapSprint)
                .toList();
    }

    // ══════════════════════════════════════════
    // Jira status
    // ══════════════════════════════════════════

    @GetMapping("/status")
    public JiraStatusResponse getJiraStatus() {
        var response = JiraStatusResponse.builder()
                .configured(jiraProps.isConfigured())
                .baseUrl(jiraProps.getBaseUrl())
                .authType(jiraProps.getAuthType())
                .build();

        if (jiraProps.isConfigured()) {
            try {
                // Test de connexion — chercher un board quelconque
                jiraClient.findBoardByName("test-connection-" + System.currentTimeMillis());
                response.setConnected(true);
            } catch (Exception e) {
                // Si 404 = connecté mais board pas trouvé → OK
                if (e.getMessage() != null && e.getMessage().contains("non trouvé")) {
                    response.setConnected(true);
                } else {
                    response.setConnected(false);
                    response.setError(e.getMessage());
                }
            }
        }
        return response;
    }

    // ══════════════════════════════════════════
    // Mapping
    // ══════════════════════════════════════════

    private SyncResultResponse mapSyncResult(JiraSyncService.SprintSyncResult r) {
        return SyncResultResponse.builder()
                .squadId(r.getSquadId())
                .squadName(r.getSquadName())
                .boardId(r.getBoardId())
                .boardName(r.getBoardName())
                .activeSprint(r.getActiveSprint() != null ? mapSprint(r.getActiveSprint()) : null)
                .closedSprints(r.getClosedSprints() != null
                        ? r.getClosedSprints().stream().map(this::mapSprint).toList()
                        : List.of())
                .futureSprints(r.getFutureSprints() != null
                        ? r.getFutureSprints().stream().map(this::mapSprint).toList()
                        : List.of())
                .error(r.getError())
                .syncedAt(r.getSyncedAt())
                .build();
    }

    private SprintResponse mapSprint(SprintSync s) {
        return SprintResponse.builder()
                .id(s.getId())
                .jiraSprintId(s.getJiraSprintId())
                .squadId(s.getSquad() != null ? s.getSquad().getId() : null)
                .name(s.getName())
                .state(s.getState())
                .goal(s.getGoal())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .completeDate(s.getCompleteDate())
                .boardId(s.getBoardId())
                .projectKey(s.getProjectKey())
                .totalIssues(s.getTotalIssues())
                .doneIssues(s.getDoneIssues())
                .totalStoryPoints(s.getTotalStoryPoints())
                .doneStoryPoints(s.getDoneStoryPoints())
                .completionPercent(s.getCompletionPercent())
                .issues(s.getIssues() != null
                        ? s.getIssues().stream().map(this::mapIssue).toList()
                        : List.of())
                .syncedAt(s.getSyncedAt())
                .build();
    }

    private IssueResponse mapIssue(SprintIssue i) {
        return IssueResponse.builder()
                .issueKey(i.getIssueKey())
                .summary(i.getSummary())
                .issueType(i.getIssueType())
                .statusName(i.getStatusName())
                .statusCategory(i.getStatusCategory())
                .priority(i.getPriority())
                .storyPoints(i.getStoryPoints())
                .assigneeName(i.getAssigneeName())
                .assigneeUsername(i.getAssigneeUsername())
                .fixVersion(i.getFixVersion())
                .affectVersion(i.getAffectVersion())
                .labels(i.getLabels())
                .components(i.getComponents())
                .build();
    }
}
