package com.rdapp.deploy.jira.service;

import com.rdapp.deploy.entity.*;
import com.rdapp.deploy.jira.client.JiraClient;
import com.rdapp.deploy.jira.config.JiraProperties;
import com.rdapp.deploy.jira.dto.JiraDtos.*;
import com.rdapp.deploy.repository.AffectVersionRepository;
import com.rdapp.deploy.repository.SquadRepository;
import com.rdapp.deploy.repository.SprintSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'orchestration de la synchronisation Jira DC.
 *
 * Flux principal:
 *   1. boardId (ex: "BPMN-42") → chercher le board Jira
 *   2. board → récupérer les sprints (active, closed, future)
 *   3. sprint actif → récupérer les issues
 *   4. Persister localement dans sprint_sync + sprint_issue
 *   5. Optionnel : sync des Affect Versions depuis les fix versions Jira
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraSyncService {

    private final JiraClient jiraClient;
    private final JiraProperties jiraProps;
    private final SprintSyncRepository sprintRepo;
    private final SquadRepository squadRepo;
    private final AffectVersionRepository versionRepo;

    // ══════════════════════════════════════════
    // Sync sprint complet pour une escouade
    // ══════════════════════════════════════════

    /**
     * Synchronise le sprint actif + historique pour une escouade.
     * Utilise le boardId de l'escouade pour trouver le board Jira.
     */
    @Transactional
    public SprintSyncResult syncSquadSprint(String squadId) {
        assertConfigured();

        var squad = squadRepo.findByIdWithMembers(squadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escouade: " + squadId));

        if (squad.getBoardId() == null || squad.getBoardId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Board ID non configuré pour l'escouade: " + squad.getName());
        }

        // 1. Trouver le board Jira
        var board = jiraClient.findBoardByName(squad.getBoardId());
        log.info("Sync — board trouvé: {} (id={})", board.getName(), board.getId());

        // 2. Récupérer le sprint actif
        var activeSprint = jiraClient.getActiveSprint(board.getId());
        SprintSync syncedActive = null;

        if (activeSprint != null) {
            log.info("Sync — sprint actif: {} (id={})", activeSprint.getName(), activeSprint.getId());
            var issues = jiraClient.getSprintIssues(activeSprint.getId());
            syncedActive = persistSprint(activeSprint, issues, squad, board);
        } else {
            log.info("Sync — aucun sprint actif pour board {}", board.getName());
        }

        // 3. Récupérer les derniers sprints fermés (max 5)
        var closedSprints = jiraClient.getClosedSprints(board.getId());
        var syncedClosed = new ArrayList<SprintSync>();
        for (var closed : closedSprints.stream().limit(5).toList()) {
            var issues = jiraClient.getSprintIssues(closed.getId());
            syncedClosed.add(persistSprint(closed, issues, squad, board));
        }

        // 4. Récupérer les sprints futurs
        var futureSprints = jiraClient.getFutureSprints(board.getId());
        var syncedFuture = new ArrayList<SprintSync>();
        for (var future : futureSprints) {
            syncedFuture.add(persistSprint(future, List.of(), squad, board));
        }

        return SprintSyncResult.builder()
                .squadId(squadId)
                .squadName(squad.getName())
                .boardId(board.getId())
                .boardName(board.getName())
                .activeSprint(syncedActive)
                .closedSprints(syncedClosed)
                .futureSprints(syncedFuture)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════
    // Sync Affect Versions depuis Jira
    // ══════════════════════════════════════════

    /**
     * Synchronise les Affect Versions depuis un projet Jira.
     * Crée ou met à jour les versions locales.
     */
    @Transactional
    public VersionSyncResult syncProjectVersions(String projectKey) {
        assertConfigured();

        var jiraVersions = jiraClient.getProjectVersions(projectKey);
        log.info("Sync versions — {} versions trouvées pour {}", jiraVersions.size(), projectKey);

        int created = 0, updated = 0;

        for (var jv : jiraVersions) {
            var existing = versionRepo.findById("jira-" + jv.getId());
            if (existing.isPresent()) {
                var entity = existing.get();
                entity.setName(jv.getName());
                entity.setDescription(jv.getDescription());
                entity.setStatus(mapVersionStatus(jv));
                entity.setReleaseDate(parseDate(jv.getReleaseDate()));
                versionRepo.save(entity);
                updated++;
            } else {
                var entity = AffectVersion.builder()
                        .id("jira-" + jv.getId())
                        .name(jv.getName())
                        .description(jv.getDescription())
                        .status(mapVersionStatus(jv))
                        .releaseDate(parseDate(jv.getReleaseDate()))
                        .build();
                versionRepo.save(entity);
                created++;
            }
        }

        return VersionSyncResult.builder()
                .projectKey(projectKey)
                .totalFromJira(jiraVersions.size())
                .created(created)
                .updated(updated)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════
    // Sync toutes les escouades
    // ══════════════════════════════════════════

    /**
     * Synchronise les sprints de toutes les escouades qui ont un boardId.
     */
    @Transactional
    public List<SprintSyncResult> syncAllSquads() {
        assertConfigured();

        var squads = squadRepo.findAllWithMembers();
        var results = new ArrayList<SprintSyncResult>();

        for (var squad : squads) {
            if (squad.getBoardId() != null && !squad.getBoardId().isBlank()) {
                try {
                    results.add(syncSquadSprint(squad.getId()));
                } catch (Exception e) {
                    log.error("Sync failed for squad {}: {}", squad.getName(), e.getMessage());
                    results.add(SprintSyncResult.builder()
                            .squadId(squad.getId())
                            .squadName(squad.getName())
                            .error(e.getMessage())
                            .syncedAt(LocalDateTime.now())
                            .build());
                }
            }
        }
        return results;
    }

    // ══════════════════════════════════════════
    // Lecture des données synchronisées
    // ══════════════════════════════════════════

    @Transactional(readOnly = true)
    public SprintSync getActiveSprintForSquad(String squadId) {
        return sprintRepo.findActiveBySquadId(squadId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<SprintSync> getClosedSprintsForSquad(String squadId) {
        return sprintRepo.findClosedBySquadId(squadId);
    }

    @Transactional(readOnly = true)
    public List<SprintSync> getAllSprintsForSquad(String squadId) {
        return sprintRepo.findBySquadIdOrderBySyncedAtDesc(squadId);
    }

    // ══════════════════════════════════════════
    // Persist
    // ══════════════════════════════════════════

    private SprintSync persistSprint(JiraSprint jiraSprint, List<JiraIssue> issues,
                                      Squad squad, JiraBoard board) {
        var existing = sprintRepo.findByJiraSprintId(jiraSprint.getId());
        SprintSync entity;

        if (existing.isPresent()) {
            entity = existing.get();
            entity.clearIssues();
        } else {
            entity = new SprintSync();
            entity.setJiraSprintId(jiraSprint.getId());
        }

        entity.setSquad(squad);
        entity.setName(jiraSprint.getName());
        entity.setState(jiraSprint.getState() != null ? jiraSprint.getState().toLowerCase() : "unknown");
        entity.setGoal(jiraSprint.getGoal());
        entity.setStartDate(parseDateTime(jiraSprint.getStartDate()));
        entity.setEndDate(parseDateTime(jiraSprint.getEndDate()));
        entity.setCompleteDate(parseDateTime(jiraSprint.getCompleteDate()));
        entity.setBoardId(board.getId());
        entity.setProjectKey(board.getLocation() != null ? board.getLocation().getProjectKey() : null);

        // Stats
        int totalIssues = issues.size();
        int doneIssues = 0;
        double totalSp = 0;
        double doneSp = 0;

        for (var issue : issues) {
            var fields = issue.getFields();
            if (fields == null) continue;

            boolean isDone = fields.getStatus() != null
                    && fields.getStatus().getStatusCategory() != null
                    && "done".equalsIgnoreCase(fields.getStatus().getStatusCategory().getKey());

            double sp = fields.getStoryPoints() != null ? fields.getStoryPoints() : 0;
            totalSp += sp;
            if (isDone) {
                doneIssues++;
                doneSp += sp;
            }

            // Persist issue
            var issueEntity = SprintIssue.builder()
                    .issueKey(issue.getKey())
                    .summary(fields.getSummary())
                    .issueType(fields.getIssuetype() != null ? fields.getIssuetype().getName() : null)
                    .statusName(fields.getStatus() != null ? fields.getStatus().getName() : null)
                    .statusCategory(isDone ? "done"
                            : fields.getStatus() != null && fields.getStatus().getStatusCategory() != null
                            ? fields.getStatus().getStatusCategory().getKey() : null)
                    .priority(fields.getPriority() != null ? fields.getPriority().getName() : null)
                    .storyPoints(sp > 0 ? sp : null)
                    .assigneeName(fields.getAssignee() != null ? fields.getAssignee().getDisplayName() : null)
                    .assigneeUsername(fields.getAssignee() != null ? fields.getAssignee().getName() : null)
                    .fixVersion(fields.getFixVersions() != null && !fields.getFixVersions().isEmpty()
                            ? fields.getFixVersions().getFirst().getName() : null)
                    .affectVersion(mapVersionNames(fields.getVersions()))
                    .labels(mapLabels(fields.getLabels()))
                    .components(mapComponentNames(fields.getComponents()))
                    .createdAt(fields.getCreated())
                    .updatedAt(fields.getUpdated())
                    .resolutionDate(fields.getResolutiondate())
                    .build();
            entity.addIssue(issueEntity);
        }

        entity.setTotalIssues(totalIssues);
        entity.setDoneIssues(doneIssues);
        entity.setTotalStoryPoints(totalSp);
        entity.setDoneStoryPoints(doneSp);

        return sprintRepo.save(entity);
    }

    // ══════════════════════════════════════════
    // Mappers
    // ══════════════════════════════════════════

    private VersionStatus mapVersionStatus(JiraVersion jv) {
        if (jv.isArchived()) return VersionStatus.ARCHIVED;
        if (jv.isReleased()) return VersionStatus.RELEASED;
        // Heuristique : si date future → planned, sinon en cours
        if (jv.getReleaseDate() != null) {
            var date = parseDate(jv.getReleaseDate());
            if (date != null && date.isAfter(LocalDate.now())) return VersionStatus.PLANNED;
        }
        return VersionStatus.IN_PROGRESS;
    }

    /** Concatène les noms des "Affecte la/les version(s)" */
    private String mapVersionNames(List<JiraVersion> versions) {
        if (versions == null || versions.isEmpty()) return null;
        return versions.stream()
                .map(JiraVersion::getName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /** Concatène les étiquettes (labels) */
    private String mapLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) return null;
        return String.join(", ", labels);
    }

    /** Concatène les noms des composants */
    private String mapComponentNames(List<JiraComponent> components) {
        if (components == null || components.isEmpty()) return null;
        return components.stream()
                .map(JiraComponent::getName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception e) {
            log.debug("Failed to parse datetime: {}", iso);
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.debug("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    private void assertConfigured() {
        if (!jiraProps.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Jira non configuré. Définir JIRA_PAT_TOKEN ou JIRA_USERNAME/JIRA_PASSWORD.");
        }
    }

    // ══════════════════════════════════════════
    // Result DTOs internes
    // ══════════════════════════════════════════

    @lombok.Getter @lombok.Setter @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class SprintSyncResult {
        private String squadId;
        private String squadName;
        private Long boardId;
        private String boardName;
        private SprintSync activeSprint;
        private List<SprintSync> closedSprints;
        private List<SprintSync> futureSprints;
        private String error;
        private LocalDateTime syncedAt;
    }

    @lombok.Getter @lombok.Setter @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class VersionSyncResult {
        private String projectKey;
        private int totalFromJira;
        private int created;
        private int updated;
        private LocalDateTime syncedAt;
    }
}
