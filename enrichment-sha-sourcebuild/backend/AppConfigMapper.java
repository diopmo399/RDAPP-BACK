package com.rdapp.solutionrdapp.dashboardservice.mapper;

import com.rdapp.solutionrdapp.dashboardservice.data.entity.comparaison.ComparaisonEntity;
import com.rdapp.solutionrdapp.dashboardservice.data.entity.comparaison.CommitEntity;
import com.rdapp.solutionrdapp.dashboardservice.data.entity.deploiement.DeploiementEntity;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper : DeploiementEntity + ComparaisonEntity + CommitEntity
 *        → AppConfigDto (JSON retourné au Angular)
 *
 * Enrichissements :
 *   - tagCommitSha  : SHA du commit derrière le tag Git
 *   - sourceBuild   : version du build source (pour rc/release)
 *   - headCommitDate: date du dernier commit du tag
 *
 * Logique SHA :
 *   build.T1639Z  → SHA a1b2c3d4
 *   rc.15         → SHA a1b2c3d4  ← même commit = même code
 *   1.5.4         → SHA a1b2c3d4  ← release finale = même code
 *   → On retrouve les commits du build pour le rc/release
 */
public class AppConfigMapper {

    private AppConfigMapper() {}

    // ══════════════════════════════════════════════════════════
    // DTOs de sortie (match les interfaces Angular)
    // ══════════════════════════════════════════════════════════

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CommitDto {
        private String sha;
        private String message;
        private String ticket;
        private String ticketTitle;
        private String type;
        private String author;
        private String date;           // "16 fév 14:57"
        private String statusName;
        private String statusCategory;
        private String rawDate;        // "2026-02-16T14:57:00Z"
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EnvDataDto {
        private String version;
        private String status;
        private String lastDeploy;
        private String branch;
        private int instances;
        private String uptime;
        private String headCommitDate;
        private String dateDeploiement;
        private String tagCommitSha;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VersionInfoDto {
        private String tagCommitSha;
        private String sourceBuild;
        private List<CommitDto> commits;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppConfigDto {
        private String id;
        private String name;
        private String icon;
        private String description;
        private String repo;
        private String tech;
        private String color;
        private Map<String, EnvDataDto> envs;
        private Map<String, VersionInfoDto> commits;
    }

    // ══════════════════════════════════════════════════════════
    // Mapping principal
    // ══════════════════════════════════════════════════════════

    public static AppConfigDto map(
            String produit,
            String name,
            String description,
            String repo,
            String tech,
            List<DeploiementEntity> deploiements,
            Map<String, String> envMapping) {

        var envs = mapEnvs(deploiements, envMapping);
        var shaToBuilds = buildShaIndex(deploiements);
        var commits = mapVersions(deploiements, shaToBuilds);

        return AppConfigDto.builder()
                .id(produit)
                .name(name)
                .description(description)
                .repo(repo)
                .tech(tech)
                .envs(envs)
                .commits(commits)
                .build();
    }

    // ══════════════════════════════════════════════════════════
    // ENVS — dernier déploiement par environnement
    // ══════════════════════════════════════════════════════════

    private static Map<String, EnvDataDto> mapEnvs(
            List<DeploiementEntity> deploiements,
            Map<String, String> envMapping) {

        var envs = new LinkedHashMap<String, EnvDataDto>();

        var latestByEnv = new LinkedHashMap<String, DeploiementEntity>();
        for (var dep : deploiements) {
            if (dep.getEnvironnement() == null) continue;
            var existing = latestByEnv.get(dep.getEnvironnement());
            if (existing == null || isAfter(dep.getTimestamp(), existing.getTimestamp())) {
                latestByEnv.put(dep.getEnvironnement(), dep);
            }
        }

        for (var entry : latestByEnv.entrySet()) {
            String envKey = envMapping.getOrDefault(entry.getKey(), extractEnvKey(entry.getKey()));
            var dep = entry.getValue();

            String headCommitDate = null;
            if (dep.getComparaison() != null) {
                headCommitDate = dep.getComparaison().getHeadCommitDate();

                if (headCommitDate == null && dep.getComparaison().getCommits() != null) {
                    headCommitDate = dep.getComparaison().getCommits().stream()
                            .map(CommitEntity::getDate)
                            .filter(Objects::nonNull)
                            .max(OffsetDateTime::compareTo)
                            .map(OffsetDateTime::toString)
                            .orElse(null);
                }
            }

            envs.put(envKey, EnvDataDto.builder()
                    .version(dep.getVersion())
                    .status("deployed")
                    .dateDeploiement(dep.getTimestamp() != null ? dep.getTimestamp().toString() : null)
                    .headCommitDate(headCommitDate)
                    .tagCommitSha(dep.getTagCommitSha())
                    .build());
        }

        return envs;
    }

    // ══════════════════════════════════════════════════════════
    // INDEX SHA → builds
    // ══════════════════════════════════════════════════════════

    private static Map<String, List<DeploiementEntity>> buildShaIndex(
            List<DeploiementEntity> deploiements) {

        return deploiements.stream()
                .filter(d -> d.getTagCommitSha() != null)
                .filter(d -> d.getVersion() != null && d.getVersion().contains("-build"))
                .filter(d -> d.getComparaison() != null)
                .collect(Collectors.groupingBy(DeploiementEntity::getTagCommitSha));
    }

    // ══════════════════════════════════════════════════════════
    // VERSIONS — commits enrichis avec SHA + sourceBuild
    // ══════════════════════════════════════════════════════════

    private static Map<String, VersionInfoDto> mapVersions(
            List<DeploiementEntity> deploiements,
            Map<String, List<DeploiementEntity>> shaToBuilds) {

        var versions = new LinkedHashMap<String, VersionInfoDto>();

        // version → meilleur déploiement (celui qui a une comparaison)
        var versionToDep = new LinkedHashMap<String, DeploiementEntity>();
        for (var dep : deploiements) {
            if (dep.getVersion() == null) continue;
            var existing = versionToDep.get(dep.getVersion());
            if (existing == null) {
                versionToDep.put(dep.getVersion(), dep);
            } else if (dep.getComparaison() != null && existing.getComparaison() == null) {
                versionToDep.put(dep.getVersion(), dep);
            }
        }

        var sortedVersions = new ArrayList<>(versionToDep.keySet());
        sortedVersions.sort(Comparator.naturalOrder());

        for (var version : sortedVersions) {
            var dep = versionToDep.get(version);
            String sha = dep.getTagCommitSha();
            String sourceBuild = null;
            List<CommitDto> commits;

            // Extraire les commits de la comparaison
            if (dep.getComparaison() != null
                    && dep.getComparaison().getCommits() != null
                    && !dep.getComparaison().getCommits().isEmpty()) {
                commits = mapCommits(dep.getComparaison().getCommits());
            } else {
                commits = List.of();
            }

            // Pas de commits + SHA connu → chercher le build source
            if (commits.isEmpty() && sha != null) {
                var builds = shaToBuilds.getOrDefault(sha, List.of());
                var buildWithCommits = builds.stream()
                        .filter(b -> !b.getVersion().equals(version))
                        .filter(b -> b.getComparaison().getCommits() != null)
                        .filter(b -> !b.getComparaison().getCommits().isEmpty())
                        .findFirst();

                if (buildWithCommits.isPresent()) {
                    var build = buildWithCommits.get();
                    sourceBuild = build.getVersion();
                    commits = mapCommits(build.getComparaison().getCommits());
                }
            }

            // rc/release → indiquer le build source
            if (sha != null && !version.contains("-build")) {
                var builds = shaToBuilds.getOrDefault(sha, List.of());
                sourceBuild = builds.stream()
                        .map(DeploiementEntity::getVersion)
                        .filter(v -> !v.equals(version))
                        .findFirst()
                        .orElse(sourceBuild);
            }

            versions.put(version, VersionInfoDto.builder()
                    .tagCommitSha(sha)
                    .sourceBuild(sourceBuild)
                    .commits(commits)
                    .build());
        }

        return versions;
    }

    // ══════════════════════════════════════════════════════════
    // CommitEntity → CommitDto
    // ══════════════════════════════════════════════════════════

    private static List<CommitDto> mapCommits(List<CommitEntity> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .map(c -> {
                    var builder = CommitDto.builder()
                            .sha(c.getSha())
                            .message(c.getMessageCourt())
                            .ticket(c.getNumeroTicket())
                            .author(c.getAuteur());

                    if (c.getDate() != null) {
                        builder.rawDate(c.getDate().toString());
                        builder.date(formatDate(c.getDate()));
                    }

                    if (c.getSprintIssue() != null) {
                        builder.statusName(c.getSprintIssue().getStatusName());
                        builder.statusCategory(c.getSprintIssue().getStatusCategory());
                        builder.ticketTitle(c.getSprintIssue().getSummary());
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════

    private static String extractEnvKey(String fullEnv) {
        if (fullEnv == null) return "";
        int slash = fullEnv.lastIndexOf('/');
        return slash >= 0 ? fullEnv.substring(slash + 1) : fullEnv;
    }

    private static boolean isAfter(ZonedDateTime a, ZonedDateTime b) {
        if (a == null) return false;
        if (b == null) return true;
        return a.isAfter(b);
    }

    private static String formatDate(OffsetDateTime date) {
        if (date == null) return null;
        var mois = new String[]{"", "janv", "fév", "mars", "avr", "mai", "juin",
                "juil", "août", "sept", "oct", "nov", "déc"};
        return String.format("%d %s %02d:%02d",
                date.getDayOfMonth(),
                mois[date.getMonthValue()],
                date.getHour(),
                date.getMinute());
    }
}
