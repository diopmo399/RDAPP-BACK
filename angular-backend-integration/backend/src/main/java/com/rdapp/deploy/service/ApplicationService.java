package com.rdapp.deploy.service;

import com.rdapp.deploy.dto.AppDto.*;
import com.rdapp.deploy.entity.Application;
import com.rdapp.deploy.entity.CommitRecord;
import com.rdapp.deploy.entity.EnvironmentDeployment;
import com.rdapp.deploy.repository.ApplicationRepository;
import com.rdapp.deploy.repository.CommitRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository appRepo;
    private final CommitRecordRepository commitRepo;

    // ══════════════════════════════════════════════
    // Liste (sans commits — léger)
    // ══════════════════════════════════════════════

    public List<AppSummaryResponse> findAllSummary() {
        return appRepo.findAllWithDeployments().stream()
                .map(this::toSummary)
                .toList();
    }

    // ══════════════════════════════════════════════
    // Détail complet (avec commits)
    // ══════════════════════════════════════════════

    public AppResponse findByIdFull(String id) {
        var app = appRepo.findByIdWithAll(id);
        if (app == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application non trouvée: " + id);
        return toFull(app);
    }

    // ══════════════════════════════════════════════
    // Liste complète (avec commits) — pour le dashboard
    // ══════════════════════════════════════════════

    public List<AppResponse> findAllFull() {
        var apps = appRepo.findAllWithDeployments();
        // Charger les commits pour chaque app
        return apps.stream().map(app -> {
            var commits = commitRepo.findByApplicationIdOrderByVersionTagDescCommitDateDesc(app.getId());
            app.setCommits(commits);
            return toFull(app);
        }).toList();
    }

    // ══════════════════════════════════════════════
    // Commits par app
    // ══════════════════════════════════════════════

    public Map<String, List<CommitResponse>> getCommitsForApp(String appId) {
        var commits = commitRepo.findByApplicationIdOrderByVersionTagDescCommitDateDesc(appId);
        return groupCommits(commits);
    }

    // ══════════════════════════════════════════════
    // Update deployment (webhook CI/CD)
    // ══════════════════════════════════════════════

    @Transactional
    public EnvResponse updateDeployment(String appId, String envKey, UpdateDeployment dto) {
        var app = appRepo.findById(appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App non trouvée: " + appId));

        var dep = app.getDeployments().stream()
                .filter(d -> d.getEnvKey().equals(envKey))
                .findFirst()
                .orElseGet(() -> {
                    var d = new EnvironmentDeployment();
                    d.setApplication(app);
                    d.setEnvKey(envKey);
                    app.getDeployments().add(d);
                    return d;
                });

        if (dto.getVersion() != null)    dep.setVersion(dto.getVersion());
        if (dto.getStatus() != null)     dep.setStatus(dto.getStatus());
        if (dto.getBranch() != null)      dep.setBranch(dto.getBranch());
        if (dto.getInstances() != null)   dep.setInstances(dto.getInstances());
        if (dto.getUptime() != null)      dep.setUptime(dto.getUptime());
        if (dto.getLastDeploy() != null)  dep.setLastDeploy(dto.getLastDeploy());

        appRepo.save(app);
        return toEnv(dep);
    }

    // ══════════════════════════════════════════════
    // Add commit (webhook CI/CD)
    // ══════════════════════════════════════════════

    @Transactional
    public CommitResponse addCommit(String appId, CreateCommit dto) {
        var app = appRepo.findById(appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App non trouvée: " + appId));

        var c = CommitRecord.builder()
                .application(app)
                .versionTag(dto.getVersionTag())
                .sha(dto.getSha())
                .message(dto.getMessage())
                .ticket(dto.getTicket())
                .ticketTitle(dto.getTicketTitle())
                .commitType(dto.getCommitType())
                .author(dto.getAuthor())
                .commitDate(dto.getCommitDate())
                .build();

        commitRepo.save(c);
        return toCommit(c);
    }

    // ══════════════════════════════════════════════
    // Mapping
    // ══════════════════════════════════════════════

    private AppSummaryResponse toSummary(Application app) {
        return AppSummaryResponse.builder()
                .id(app.getId())
                .name(app.getName())
                .icon(app.getIcon())
                .description(app.getDescription())
                .repo(app.getRepo())
                .tech(app.getTech())
                .color(app.getColor())
                .envs(mapEnvs(app.getDeployments()))
                .build();
    }

    private AppResponse toFull(Application app) {
        return AppResponse.builder()
                .id(app.getId())
                .name(app.getName())
                .icon(app.getIcon())
                .description(app.getDescription())
                .repo(app.getRepo())
                .tech(app.getTech())
                .color(app.getColor())
                .envs(mapEnvs(app.getDeployments()))
                .commits(groupCommits(app.getCommits()))
                .build();
    }

    private Map<String, EnvResponse> mapEnvs(List<EnvironmentDeployment> deps) {
        var map = new LinkedHashMap<String, EnvResponse>();
        for (var ek : List.of("dev", "qa", "test", "prod")) {
            deps.stream()
                .filter(d -> d.getEnvKey().equals(ek))
                .findFirst()
                .ifPresent(d -> map.put(ek, toEnv(d)));
        }
        return map;
    }

    private EnvResponse toEnv(EnvironmentDeployment d) {
        return EnvResponse.builder()
                .version(d.getVersion())
                .status(d.getStatus())
                .lastDeploy(d.getLastDeploy())
                .branch(d.getBranch())
                .instances(d.getInstances())
                .uptime(d.getUptime())
                .build();
    }

    private Map<String, List<CommitResponse>> groupCommits(List<CommitRecord> commits) {
        if (commits == null || commits.isEmpty()) return Map.of();
        return commits.stream()
                .collect(Collectors.groupingBy(
                        CommitRecord::getVersionTag,
                        LinkedHashMap::new,
                        Collectors.mapping(this::toCommit, Collectors.toList())
                ));
    }

    private CommitResponse toCommit(CommitRecord c) {
        return CommitResponse.builder()
                .sha(c.getSha())
                .message(c.getMessage())
                .ticket(c.getTicket())
                .ticketTitle(c.getTicketTitle())
                .type(c.getCommitType())
                .author(c.getAuthor())
                .date(c.getCommitDate())
                .build();
    }
}
