package com.rdapp.solutionrdapp.dashboardservice.domain.service;

import com.rdapp.solutionrdapp.dashboardservice.dto.DeployIngestDto.*;
import com.rdapp.solutionrdapp.dashboardservice.data.entity.comparaison.ComparaisonEntity;
import com.rdapp.solutionrdapp.dashboardservice.data.entity.comparaison.CommitEntity;
import com.rdapp.solutionrdapp.dashboardservice.data.entity.deploiement.DeploiementEntity;
import com.rdapp.solutionrdapp.dashboardservice.data.repository.ComparaisonRepository;
import com.rdapp.solutionrdapp.dashboardservice.data.repository.DeploiementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeployIngestService {

    private final DeploiementRepository deploiementRepository;
    private final ComparaisonRepository comparaisonRepository;

    // ══════════════════════════════════════════════════
    // 1. Lookup cache
    // ══════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ComparisonLookupResponse lookupComparisons(ComparisonLookupRequest request) {
        var cached = new LinkedHashMap<String, ComparisonIngest>();
        int totalCached = 0;
        int totalMissing = 0;

        if (request.getPairs() == null || request.getPairs().isEmpty()) {
            return new ComparisonLookupResponse(cached, 0, 0, 0);
        }

        for (var pair : request.getPairs()) {
            String key = buildCacheKey(pair.getProduit(), pair.getBaseVersion(), pair.getHeadVersion());

            var existing = comparaisonRepository
                    .findByProduitAndBaseVersionAndHeadVersion(
                            pair.getProduit(), pair.getBaseVersion(), pair.getHeadVersion());

            if (existing.isPresent()) {
                cached.put(key, entityToDto(existing.get()));
                totalCached++;
            } else {
                cached.put(key, null);
                totalMissing++;
            }
        }

        return new ComparisonLookupResponse(cached, request.getPairs().size(), totalCached, totalMissing);
    }

    // ══════════════════════════════════════════════════
    // 2. Ingest
    // ══════════════════════════════════════════════════

    @Transactional
    public IngestResponse ingest(IngestPayload payload) {
        int totalDeployments = 0;
        int totalComparisons = 0;
        int envsProcessed = 0;
        var errors = new ArrayList<String>();

        if (payload.getResults() == null) {
            return IngestResponse.builder()
                    .runId(payload.getRunId())
                    .ingestedAt(LocalDateTime.now())
                    .errors(List.of("No results in payload"))
                    .build();
        }

        for (var orgEnv : payload.getResults()) {
            try {
                var result = processOrgEnv(orgEnv);
                totalDeployments += result.deployments;
                totalComparisons += result.comparisons;
                envsProcessed++;
            } catch (Exception e) {
                String errorMsg = orgEnv.getOrganisation() + "/" + orgEnv.getEnvironnement()
                        + ": " + e.getMessage();
                errors.add(errorMsg);
                log.error("Ingest failed for {}/{}: {}",
                        orgEnv.getOrganisation(), orgEnv.getEnvironnement(), e.getMessage(), e);
            }
        }

        return IngestResponse.builder()
                .totalDeploymentsSaved(totalDeployments)
                .totalComparisonsSaved(totalComparisons)
                .environmentsProcessed(envsProcessed)
                .errors(errors)
                .runId(payload.getRunId())
                .ingestedAt(LocalDateTime.now())
                .build();
    }

    // ══════════════════════════════════════════════════
    // Process org/env
    // ══════════════════════════════════════════════════

    private ProcessResult processOrgEnv(OrgEnvResult orgEnv) {
        int deployments = 0;
        int comparisons = 0;

        if (orgEnv.getDeployments() == null) return new ProcessResult(0, 0);

        for (var deployment : orgEnv.getDeployments()) {
            var timestamp = parseTimestamp(deployment.getTimestamp());
            var existing = deploiementRepository.findByProduitAndVersionAndEnvironnementAndTimestamp(
                    deployment.getProduit(),
                    deployment.getVersion(),
                    deployment.getEnvironnement(),
                    timestamp);

            DeploiementEntity entity;
            if (existing.isPresent()) {
                entity = existing.get();
            } else {
                entity = new DeploiementEntity();
                entity.setProduit(deployment.getProduit());
                entity.setVersion(deployment.getVersion());
                entity.setEnvironnement(deployment.getEnvironnement());
                entity.setTimestamp(timestamp);
            }

            // ── tagCommitSha ──
            entity.setTagCommitSha(deployment.getTagCommitSha());

            // ── Comparaison ──
            if (deployment.getComparaison() != null) {
                var comp = deployment.getComparaison();

                if (!comp.isFromCache()) {
                    var compEntity = mapComparaison(deployment.getProduit(), comp);
                    compEntity = comparaisonRepository.save(compEntity);
                    entity.setComparaison(compEntity);
                    comparisons++;
                } else {
                    var cachedComp = comparaisonRepository.findByProduitAndBaseVersionAndHeadVersion(
                            deployment.getProduit(), comp.getBaseVersion(), comp.getHeadVersion());
                    cachedComp.ifPresent(entity::setComparaison);
                }
            }

            deploiementRepository.save(entity);
            deployments++;
        }

        return new ProcessResult(deployments, comparisons);
    }

    // ══════════════════════════════════════════════════
    // Entity → DTO (pour le lookup cache)
    // ══════════════════════════════════════════════════

    private ComparisonIngest entityToDto(ComparaisonEntity entity) {
        var dto = new ComparisonIngest();
        dto.setBaseVersion(entity.getBaseVersion());
        dto.setHeadVersion(entity.getHeadVersion());
        dto.setStatus(entity.getStatus());
        dto.setAheadBy(entity.getAheadBy());
        dto.setBehindBy(entity.getBehindBy());
        dto.setTotalCommits(entity.getTotalCommits());
        dto.setFilesChanged(entity.getFilesChanged());
        dto.setAdditions(entity.getAdditions());
        dto.setDeletions(entity.getDeletions());
        dto.setHeadCommitDate(entity.getHeadCommitDate());
        dto.setBaseCommitDate(entity.getBaseCommitDate());
        dto.setFromCache(true);
        dto.setFiles(null);

        if (entity.getCommits() != null) {
            dto.setCommits(entity.getCommits().stream()
                    .map(c -> new CommitIngest(c.getSha(), c.getMessageCourt(), c.getAuteur(),
                            c.getDate() != null ? c.getDate().toString() : null))
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    // ══════════════════════════════════════════════════
    // DTO → Entity (pour l'ingest)
    // ══════════════════════════════════════════════════

    private ComparaisonEntity mapComparaison(String produit, ComparisonIngest comp) {
        var existing = comparaisonRepository.findByProduitAndBaseVersionAndHeadVersion(
                produit, comp.getBaseVersion(), comp.getHeadVersion());
        if (existing.isPresent()) {
            return existing.get();
        }

        var entity = new ComparaisonEntity();
        entity.setProduit(produit);
        entity.setBaseVersion(comp.getBaseVersion());
        entity.setHeadVersion(comp.getHeadVersion());
        entity.setStatus(comp.getStatus());
        entity.setAheadBy(comp.getAheadBy());
        entity.setBehindBy(comp.getBehindBy());
        entity.setTotalCommits(comp.getTotalCommits());
        entity.setFilesChanged(comp.getFilesChanged());
        entity.setAdditions(comp.getAdditions());
        entity.setDeletions(comp.getDeletions());
        entity.setHeadCommitDate(comp.getHeadCommitDate());
        entity.setBaseCommitDate(comp.getBaseCommitDate());

        if (comp.getCommits() != null) {
            var commits = comp.getCommits().stream()
                    .map(c -> {
                        var commit = new CommitEntity();
                        commit.setSha(c.getSha());
                        commit.setMessageCourt(c.getMessage());
                        commit.setAuteur(c.getAuthor());
                        if (c.getDate() != null) {
                            try {
                                commit.setDate(java.time.OffsetDateTime.parse(c.getDate()));
                            } catch (Exception e) {
                                log.warn("Cannot parse commit date: {}", c.getDate());
                            }
                        }
                        commit.setComparaison(entity);
                        return commit;
                    })
                    .collect(Collectors.toList());
            entity.setCommits(commits);
        }

        return entity;
    }

    // ── Helpers ──

    private String buildCacheKey(String produit, String base, String head) {
        return produit + "|" + base + "|" + head;
    }

    private ZonedDateTime parseTimestamp(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return ZonedDateTime.parse(iso);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}", iso);
            return null;
        }
    }

    private record ProcessResult(int deployments, int comparisons) {}
}
