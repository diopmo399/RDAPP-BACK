package com.rdapp.deploy.service;

import com.rdapp.deploy.entity.AffectVersion;
import com.rdapp.deploy.entity.SprintIssue;
import com.rdapp.deploy.entity.SprintSync;
import com.rdapp.deploy.entity.Squad;
import com.rdapp.deploy.entity.VersionStatus;
import com.rdapp.deploy.mapper.SprintMapper;
import com.rdapp.deploy.model.AffectVersionInfo;
import com.rdapp.deploy.model.SprintGlobalResponse;
import com.rdapp.deploy.model.SprintTicket;
import com.rdapp.deploy.repository.AffectVersionRepository;
import com.rdapp.deploy.repository.SprintSyncRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SprintCacheService {

    private final SprintSyncRepository sprintSyncRepository;
    private final AffectVersionRepository affectVersionRepository;
    private final SprintMapper sprintMapper;

    public SprintCacheService(
            SprintSyncRepository sprintSyncRepository,
            AffectVersionRepository affectVersionRepository,
            SprintMapper sprintMapper) {
        this.sprintSyncRepository = sprintSyncRepository;
        this.affectVersionRepository = affectVersionRepository;
        this.sprintMapper = sprintMapper;
    }

    /**
     * Récupère les sprints actifs avec cache.
     * Le cache est invalidé par forceRefresh() ou automatiquement après le sync Jira.
     */
    @Cacheable(value = "globalSprint", key = "'current'")
    @Transactional(readOnly = true)
    public SprintGlobalResponse getCurrent() {
        log.info("Fetching active sprints from database (cache miss)");

        List<SprintSync> activeSprints = sprintSyncRepository.findAllActiveSprintsWithIssues();

        if (activeSprints.isEmpty()) {
            log.warn("No active sprints found");
            return createEmptyResponse();
        }

        // Utiliser le premier sprint actif comme principal
        SprintSync mainSprint = activeSprints.get(0);

        // Collecter toutes les issues de tous les sprints actifs
        List<SprintIssue> allIssues = activeSprints.stream()
                .flatMap(sprint -> sprint.getIssues().stream())
                .collect(Collectors.toList());

        return buildGlobalResponse(mainSprint, allIssues, activeSprints);
    }

    /**
     * Force le rafraîchissement du cache.
     */
    @CacheEvict(value = "globalSprint", key = "'current'")
    public void forceRefresh() {
        log.info("Cache cleared - next call will fetch fresh data");
    }

    /**
     * Récupère toutes les versions actives (non archivées).
     */
    @Transactional(readOnly = true)
    public List<AffectVersionInfo> getActiveVersions() {
        log.info("Fetching active versions");
        List<AffectVersion> versions = affectVersionRepository.findByStatusInOrderByReleaseDateDesc(
            Arrays.asList(VersionStatus.PLANNED, VersionStatus.IN_PROGRESS, VersionStatus.RELEASED)
        );
        return versions.stream()
                .map(sprintMapper::toAffectVersionInfo)
                .collect(Collectors.toList());
    }

    private SprintGlobalResponse buildGlobalResponse(SprintSync mainSprint, List<SprintIssue> allIssues, List<SprintSync> allSprints) {
        SprintGlobalResponse response = new SprintGlobalResponse();

        // Sprint info - utiliser le mapper
        response.setSprint(sprintMapper.toSprintInfo(mainSprint));

        // Squads map
        Map<String, SprintGlobalResponse.SquadInfo> squadsMap = new HashMap<>();
        for (SprintSync sprint : allSprints) {
            if (sprint.getSquad() != null) {
                Squad squad = sprint.getSquad();
                squadsMap.put(squad.getId(),
                    new SprintGlobalResponse.SquadInfo(squad.getId(), squad.getName(), squad.getColor()));
            }
        }
        response.setSquads(squadsMap);

        // Affect Versions - récupérer les versions actives (non archivées)
        List<AffectVersion> versions = affectVersionRepository.findByStatusInOrderByReleaseDateDesc(
            Arrays.asList(VersionStatus.PLANNED, VersionStatus.IN_PROGRESS, VersionStatus.RELEASED)
        );
        List<AffectVersionInfo> versionInfos = versions.stream()
                .map(sprintMapper::toAffectVersionInfo)
                .collect(Collectors.toList());
        response.setVersions(versionInfos);

        // Grouper les issues par statut
        List<SprintTicket> notStarted = new ArrayList<>();
        List<SprintTicket> inProgress = new ArrayList<>();
        List<SprintTicket> done = new ArrayList<>();

        double totalPoints = 0.0;
        double donePoints = 0.0;

        for (SprintIssue issue : allIssues) {
            // Utiliser le mapper pour convertir
            SprintTicket ticket = sprintMapper.toSprintTicket(issue);

            if (ticket.getStoryPoints() != null) {
                totalPoints += ticket.getStoryPoints();
            }

            // Grouper par status
            switch (ticket.getStatus()) {
                case DONE:
                    if (ticket.getStoryPoints() != null) {
                        donePoints += ticket.getStoryPoints();
                    }
                    done.add(ticket);
                    break;
                case IN_PROGRESS:
                    inProgress.add(ticket);
                    break;
                case NOT_STARTED:
                default:
                    notStarted.add(ticket);
                    break;
            }
        }

        response.setNotStarted(notStarted);
        response.setInProgress(inProgress);
        response.setDone(done);
        response.setTotalPoints(totalPoints);
        response.setDonePoints(donePoints);
        response.setLastSync(mainSprint.getSyncedAt() != null ?
            mainSprint.getSyncedAt().atZone(ZoneId.systemDefault()).toInstant() : null);

        return response;
    }

    private SprintGlobalResponse createEmptyResponse() {
        SprintGlobalResponse response = new SprintGlobalResponse();
        response.setNotStarted(Collections.emptyList());
        response.setInProgress(Collections.emptyList());
        response.setDone(Collections.emptyList());
        response.setSquads(Collections.emptyMap());
        response.setVersions(Collections.emptyList());
        response.setTotalPoints(0.0);
        response.setDonePoints(0.0);
        return response;
    }
}
