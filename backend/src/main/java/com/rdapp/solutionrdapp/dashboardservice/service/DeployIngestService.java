package com.rdapp.solutionrdapp.dashboardservice.service;

import com.rdapp.solutionrdapp.dashboardservice.dto.DeployIngestDto.*;
import com.rdapp.solutionrdapp.dashboardservice.domain.entity.ComparaisonEntity;
import com.rdapp.solutionrdapp.dashboardservice.domain.entity.CommitEntity;
import com.rdapp.solutionrdapp.dashboardservice.domain.entity.DeploiementEntity;
import com.rdapp.solutionrdapp.dashboardservice.domain.repository.ComparaisonRepository;
import com.rdapp.solutionrdapp.dashboardservice.domain.repository.DeploiementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'ingestion des déploiements + comparaisons GitHub.
 *
 * Supporte le cache des comparaisons :
 *   1. GHA appelle lookupComparisons() → reçoit les comparaisons déjà calculées
 *   2. GHA ne recalcule que les manquantes via GitHub Compare API
 *   3. GHA appelle ingest() → persiste tout (déploiements + nouvelles comparaisons)
 *
 * La clé de cache d'une comparaison est : produit + baseVersion + headVersion
 * car le diff entre deux tags est immuable (un tag = un commit fixe).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeployIngestService {

    private final DeploiementRepository deploiementRepository;
    private final ComparaisonRepository comparaisonRepository;

    // ══════════════════════════════════════════════════
    // 1. Lookup : retourner les comparaisons en cache
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

            // Chercher dans la table comparaison
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

        log.info("Comparison lookup — requested={} cached={} missing={}",
                request.getPairs().size(), totalCached, totalMissing);

        return new ComparisonLookupResponse(
                cached,
                request.getPairs().size(),
                totalCached,
                totalMissing
        );
    }

    // ══════════════════════════════════════════════════
    // 2. Ingest : persister déploiements + comparaisons
    // ══════════════════════════════════════════════════

    @Transactional
    public IngestResponse ingest(IngestPayload payload) {
        int totalDeployments = 0;
        int totalComparisons = 0;
        int envsProcessed = 0;
        var errors = new ArrayList<String>();

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
                        orgEnv.getOrganisation(), orgEnv.getEnvironnement(), e.getMessage());
            }
        }

        log.info("Deploy ingest — deployments={} comparisons={} envs={} errors={} runId={}",
                totalDeployments, totalComparisons, envsProcessed, errors.size(),
                payload.getRunId());

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
    // Process une paire org/env
    // ══════════════════════════════════════════════════

    private ProcessResult processOrgEnv(OrgEnvResult orgEnv) {
        int deployments = 0;
        int comparisons = 0;

        for (var deployment : orgEnv.getDeployments()) {
            // Upsert déploiement
            var existing = deploiementRepository.findByProduitAndVersionAndEnvironnementAndTimestamp(
                    deployment.getProduit(),
                    deployment.getVersion(),
                    deployment.getEnvironnement(),
                    parseTimestamp(deployment.getTimestamp()));

            DeploiementEntity entity;
            if (existing.isPresent()) {
                entity = existing.get();
            } else {
                entity = new DeploiementEntity();
                entity.setProduit(deployment.getProduit());
                entity.setVersion(deployment.getVersion());
                entity.setEnvironnement(deployment.getEnvironnement());
                entity.setTimestamp(parseTimestamp(deployment.getTimestamp()));
            }

            // Comparaison : persister seulement si nouvelle (pas fromCache)
            if (deployment.getComparaison() != null) {
                var comp = deployment.getComparaison();

                if (!comp.isFromCache()) {
                    // Nouvelle comparaison → sauvegarder dans la table comparaison
                    var compEntity = mapComparaison(deployment.getProduit(), comp);
                    compEntity = comparaisonRepository.save(compEntity);
                    entity.setComparaison(compEntity);
                    comparisons++;
                } else {
                    // fromCache=true → la comparaison existe déjà, juste lier
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
    // Mapping Entity → DTO (pour le lookup)
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
        dto.setFromCache(true);

        if (entity.getCommits() != null) {
            dto.setCommits(entity.getCommits().stream()
                    .map(c -> new CommitIngest(c.getSha(), c.getMessage(), c.getAuthor(), c.getDate()))
                    .collect(Collectors.toList()));
        }

        // Files ne sont pas stockés en BD (trop volumineux)
        // → GHA les recevra comme null, c'est OK
        dto.setFiles(null);

        return dto;
    }

    // ══════════════════════════════════════════════════
    // Mapping DTO → Entity (pour l'ingest)
    // ══════════════════════════════════════════════════

    private ComparaisonEntity mapComparaison(String produit, ComparisonIngest comp) {
        // Vérifier si cette comparaison existe déjà (idempotence)
        var existing = comparaisonRepository.findByProduitAndBaseVersionAndHeadVersion(
                produit, comp.getBaseVersion(), comp.getHeadVersion());
        if (existing.isPresent()) {
            return existing.get(); // Réutiliser l'existante
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

        if (comp.getCommits() != null) {
            var commits = comp.getCommits().stream()
                    .map(c -> {
                        var commit = new CommitEntity();
                        commit.setSha(c.getSha());
                        commit.setMessage(c.getMessage());
                        commit.setAuthor(c.getAuthor());
                        commit.setDate(c.getDate());
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
        try { return ZonedDateTime.parse(iso); }
        catch (Exception e) { return null; }
    }

    private record ProcessResult(int deployments, int comparisons) {}
}
