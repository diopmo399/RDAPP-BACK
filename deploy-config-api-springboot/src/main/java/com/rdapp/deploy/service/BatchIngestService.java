package com.rdapp.deploy.service;

import com.rdapp.deploy.dto.BatchIngestDto.*;
import com.rdapp.deploy.entity.*;
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
 * Service d'ingestion des données envoyées par GitHub Actions.
 *
 * Flux :
 *   GHA → GET /v1/squads                  (récupère la config)
 *   GHA → appels Jira DC                  (récupère sprints + issues)
 *   GHA → POST /v1/batch/ingest           (envoie les résultats ici)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchIngestService {

    private final SprintSyncRepository sprintRepo;
    private final SquadRepository squadRepo;
    private final AffectVersionRepository versionRepo;

    // ══════════════════════════════════════════
    // Ingest une escouade
    // ══════════════════════════════════════════

    @Transactional
    public IngestResponse ingestSquad(IngestPayload payload) {
        var squad = squadRepo.findById(payload.getSquadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Escouade non trouvée: " + payload.getSquadId()));

        int sprintsSaved = 0;
        int issuesSaved = 0;

        // Sprint actif
        if (payload.getActiveSprint() != null) {
            int issues = persistSprint(payload.getActiveSprint(), squad, payload.getBoardId(), payload.getProjectKey());
            sprintsSaved++;
            issuesSaved += issues;
        }

        // Sprints fermés
        if (payload.getClosedSprints() != null) {
            for (var closed : payload.getClosedSprints()) {
                issuesSaved += persistSprint(closed, squad, payload.getBoardId(), payload.getProjectKey());
                sprintsSaved++;
            }
        }

        // Sprints futurs
        if (payload.getFutureSprints() != null) {
            for (var future : payload.getFutureSprints()) {
                persistSprint(future, squad, payload.getBoardId(), payload.getProjectKey());
                sprintsSaved++;
            }
        }

        // Versions
        int versionsSaved = 0;
        if (payload.getVersions() != null) {
            versionsSaved = ingestVersions(payload.getVersions());
        }

        log.info("Ingest — squad={} sprints={} issues={} versions={} runId={}",
                squad.getName(), sprintsSaved, issuesSaved, versionsSaved, payload.getRunId());

        return IngestResponse.builder()
                .squadId(payload.getSquadId())
                .sprintsSaved(sprintsSaved)
                .issuesSaved(issuesSaved)
                .versionsSaved(versionsSaved)
                .runId(payload.getRunId())
                .ingestedAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════
    // Ingest bulk (toutes les escouades)
    // ══════════════════════════════════════════

    @Transactional
    public BulkIngestResponse ingestBulk(BulkIngestPayload payload) {
        int totalSprints = 0, totalIssues = 0, totalVersions = 0;
        var errors = new ArrayList<String>();

        for (var squadPayload : payload.getSquads()) {
            squadPayload.setRunId(payload.getRunId());
            squadPayload.setTriggeredBy(payload.getTriggeredBy());
            try {
                var result = ingestSquad(squadPayload);
                totalSprints += result.getSprintsSaved();
                totalIssues += result.getIssuesSaved();
                totalVersions += result.getVersionsSaved();
            } catch (Exception e) {
                errors.add(squadPayload.getSquadId() + ": " + e.getMessage());
                log.error("Ingest failed for squad {}: {}", squadPayload.getSquadId(), e.getMessage());
            }
        }

        return BulkIngestResponse.builder()
                .squadsProcessed(payload.getSquads().size())
                .totalSprintsSaved(totalSprints)
                .totalIssuesSaved(totalIssues)
                .totalVersionsSaved(totalVersions)
                .errors(errors)
                .runId(payload.getRunId())
                .ingestedAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════
    // Persist sprint + issues
    // ══════════════════════════════════════════

    private int persistSprint(SprintIngest data, Squad squad, Long boardId, String projectKey) {
        var existing = sprintRepo.findByJiraSprintId(data.getJiraSprintId());
        SprintSync entity;

        if (existing.isPresent()) {
            entity = existing.get();
            entity.clearIssues();
        } else {
            entity = new SprintSync();
            entity.setJiraSprintId(data.getJiraSprintId());
        }

        entity.setSquad(squad);
        entity.setName(data.getName());
        entity.setState(data.getState() != null ? data.getState().toLowerCase() : "unknown");
        entity.setGoal(data.getGoal());
        entity.setStartDate(parseDateTime(data.getStartDate()));
        entity.setEndDate(parseDateTime(data.getEndDate()));
        entity.setCompleteDate(parseDateTime(data.getCompleteDate()));
        entity.setBoardId(boardId);
        entity.setProjectKey(projectKey);

        int totalIssues = 0, doneIssues = 0;
        double totalSp = 0, doneSp = 0;

        if (data.getIssues() != null) {
            for (var issue : data.getIssues()) {
                boolean isDone = "done".equalsIgnoreCase(issue.getStatusCategory());
                double sp = issue.getStoryPoints() != null ? issue.getStoryPoints() : 0;
                totalSp += sp;
                totalIssues++;
                if (isDone) { doneIssues++; doneSp += sp; }

                entity.addIssue(SprintIssue.builder()
                        .issueKey(issue.getKey())
                        .summary(issue.getSummary())
                        .issueType(issue.getIssueType())
                        .statusName(issue.getStatusName())
                        .statusCategory(issue.getStatusCategory())
                        .priority(issue.getPriority())
                        .storyPoints(sp > 0 ? sp : null)
                        .assigneeName(issue.getAssigneeName())
                        .assigneeUsername(issue.getAssigneeUsername())
                        .fixVersion(issue.getFixVersion())
                        .createdAt(issue.getCreated())
                        .updatedAt(issue.getUpdated())
                        .resolutionDate(issue.getResolutionDate())
                        .build());
            }
        }

        entity.setTotalIssues(totalIssues);
        entity.setDoneIssues(doneIssues);
        entity.setTotalStoryPoints(totalSp);
        entity.setDoneStoryPoints(doneSp);

        sprintRepo.save(entity);
        return totalIssues;
    }

    // ══════════════════════════════════════════
    // Persist versions
    // ══════════════════════════════════════════

    private int ingestVersions(List<VersionIngest> versions) {
        int saved = 0;
        for (var v : versions) {
            var id = "jira-" + v.getJiraId();
            var status = v.isArchived() ? VersionStatus.ARCHIVED
                    : v.isReleased() ? VersionStatus.RELEASED
                    : VersionStatus.IN_PROGRESS;

            var existing = versionRepo.findById(id);
            if (existing.isPresent()) {
                var entity = existing.get();
                entity.setName(v.getName());
                entity.setDescription(v.getDescription());
                entity.setStatus(status);
                entity.setReleaseDate(parseDate(v.getReleaseDate()));
                versionRepo.save(entity);
            } else {
                versionRepo.save(AffectVersion.builder()
                        .id(id)
                        .name(v.getName())
                        .description(v.getDescription())
                        .status(status)
                        .releaseDate(parseDate(v.getReleaseDate()))
                        .build());
            }
            saved++;
        }
        return saved;
    }

    // ── Helpers ──

    private LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return OffsetDateTime.parse(iso).toLocalDateTime(); }
        catch (Exception e) { return null; }
    }

    private LocalDate parseDate(String d) {
        if (d == null || d.isBlank()) return null;
        try { return LocalDate.parse(d); }
        catch (Exception e) { return null; }
    }
}
