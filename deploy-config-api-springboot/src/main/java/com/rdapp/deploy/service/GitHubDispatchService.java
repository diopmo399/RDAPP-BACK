package com.rdapp.deploy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Déclenche un workflow GitHub Actions via repository_dispatch.
 *
 * Le bouton "↻ Mise à jour" dans Angular appelle :
 *   POST /api/v1/squads/{id}/sync
 *     → SquadService.syncBoard()
 *       → GitHubDispatchService.triggerSquadSync()
 *         → GitHub API POST /repos/{owner}/{repo}/dispatches
 *           → GHA workflow jira-sync-squad.yml s'exécute
 *           → GHA appelle Jira DC
 *           → GHA POST /api/v1/batch/ingest/bulk avec les résultats
 *
 * Variables d'env :
 *   GITHUB_TOKEN      PAT avec scope repo (ou fine-grained: actions:write)
 *   GITHUB_OWNER      ex: rdapp
 *   GITHUB_REPO       ex: deploy-config
 */
@Service
@Slf4j
public class GitHubDispatchService {

    private final RestClient ghClient;
    private final String owner;
    private final String repo;
    private final boolean configured;

    public GitHubDispatchService(
            @Value("${github.token:}") String token,
            @Value("${github.owner:}") String owner,
            @Value("${github.repo:}") String repo) {
        this.owner = owner;
        this.repo = repo;
        this.configured = token != null && !token.isBlank();

        if (configured) {
            this.ghClient = RestClient.builder()
                    .baseUrl("https://api.github.com")
                    .defaultHeader("Authorization", "Bearer " + token)
                    .defaultHeader("Accept", "application/vnd.github+json")
                    .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                    .build();
        } else {
            this.ghClient = null;
            log.warn("GitHub dispatch non configuré (GITHUB_TOKEN manquant)");
        }
    }

    /**
     * Déclenche le workflow jira-sync-squad.yml pour une escouade.
     */
    public void triggerSquadSync(String squadId, String boardId) {
        if (!configured) {
            log.info("GitHub dispatch skip (non configuré) — squad={} board={}", squadId, boardId);
            return;
        }

        var payload = Map.of(
                "event_type", "squad-sync",
                "client_payload", Map.of(
                        "squad_id", squadId,
                        "board_id", boardId
                )
        );

        try {
            ghClient.post()
                    .uri("/repos/{owner}/{repo}/dispatches", owner, repo)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("GitHub dispatch sent — squad={} board={}", squadId, boardId);
        } catch (Exception e) {
            log.error("GitHub dispatch failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "GitHub Actions dispatch échoué: " + e.getMessage());
        }
    }

    /**
     * Déclenche le workflow jira-sync.yml (toutes les escouades).
     */
    public void triggerFullSync() {
        if (!configured) {
            log.info("GitHub dispatch skip (non configuré) — full sync");
            return;
        }

        var payload = Map.of(
                "event_type", "jira-sync",
                "client_payload", Map.of("triggered_by", "api")
        );

        try {
            ghClient.post()
                    .uri("/repos/{owner}/{repo}/dispatches", owner, repo)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("GitHub dispatch sent — full sync");
        } catch (Exception e) {
            log.error("GitHub dispatch failed: {}", e.getMessage());
        }
    }

    public boolean isConfigured() {
        return configured;
    }
}
