package com.rdapp.deploy.controller;

import com.rdapp.deploy.dto.AppDto.*;
import com.rdapp.deploy.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pipeline & Application endpoints.
 *
 * GET  /v1/apps                        → Liste résumée (sans commits)
 * GET  /v1/apps/full                   → Liste complète (avec commits) — pour le dashboard
 * GET  /v1/apps/{id}                   → Détail complet d'une app
 * GET  /v1/apps/{id}/commits           → Commits groupés par version
 * PUT  /v1/apps/{id}/envs/{envKey}     → Mettre à jour un déploiement (webhook CI/CD)
 * POST /v1/apps/{id}/commits           → Ajouter un commit (webhook CI/CD)
 */
@RestController
@RequestMapping("/v1/apps")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService service;

    @GetMapping
    public List<AppSummaryResponse> listSummary() {
        return service.findAllSummary();
    }

    @GetMapping("/full")
    public List<AppResponse> listFull() {
        return service.findAllFull();
    }

    @GetMapping("/{id}")
    public AppResponse getById(@PathVariable String id) {
        return service.findByIdFull(id);
    }

    @GetMapping("/{id}/commits")
    public Map<String, List<CommitResponse>> getCommits(@PathVariable String id) {
        return service.getCommitsForApp(id);
    }

    @PutMapping("/{id}/envs/{envKey}")
    public EnvResponse updateDeployment(@PathVariable String id,
                                        @PathVariable String envKey,
                                        @RequestBody UpdateDeployment dto) {
        return service.updateDeployment(id, envKey, dto);
    }

    @PostMapping("/{id}/commits")
    @ResponseStatus(HttpStatus.CREATED)
    public CommitResponse addCommit(@PathVariable String id,
                                    @RequestBody CreateCommit dto) {
        return service.addCommit(id, dto);
    }
}
