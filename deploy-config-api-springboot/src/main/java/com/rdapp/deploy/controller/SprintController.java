package com.rdapp.deploy.controller;

import com.rdapp.deploy.model.AffectVersionInfo;
import com.rdapp.deploy.model.SprintGlobalResponse;
import com.rdapp.deploy.service.SprintCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprint")
@CrossOrigin(origins = "*")
@Slf4j
public class SprintController {

    private final SprintCacheService cacheService;

    public SprintController(SprintCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * GET /api/sprint/global
     * Returns all sprint tickets grouped by status (not_started, in_progress, done)
     * with squad info and sprint metadata.
     */
    @GetMapping("/global")
    public ResponseEntity<SprintGlobalResponse> getGlobalSprint() {
        log.info("GET /api/sprint/global - Fetching global sprint data");
        SprintGlobalResponse response = cacheService.getCurrent();
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/sprint/refresh
     * Force refresh from Jira (admin use).
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> forceRefresh() {
        log.info("POST /api/sprint/refresh - Forcing cache refresh");
        cacheService.forceRefresh();
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/sprint/versions
     * Returns all active versions (non-archived).
     */
    @GetMapping("/versions")
    public ResponseEntity<List<AffectVersionInfo>> getVersions() {
        log.info("GET /api/sprint/versions - Fetching versions");
        List<AffectVersionInfo> versions = cacheService.getActiveVersions();
        return ResponseEntity.ok(versions);
    }
}
