package com.rdapp.deploy.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

public class AppDto {

    // ── Responses ────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppResponse {
        private String id;
        private String name;
        private String icon;
        private String description;
        private String repo;
        private String tech;
        private String color;
        private Map<String, EnvResponse> envs;          // dev/qa/test/prod
        private Map<String, List<CommitResponse>> commits; // version → commits
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AppSummaryResponse {
        private String id;
        private String name;
        private String icon;
        private String description;
        private String repo;
        private String tech;
        private String color;
        private Map<String, EnvResponse> envs;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EnvResponse {
        private String version;
        private String status;
        private String lastDeploy;
        private String branch;
        private Integer instances;
        private String uptime;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CommitResponse {
        private String sha;
        private String message;
        private String ticket;
        private String ticketTitle;
        private String type;
        private String author;
        private String date;
    }

    // ── Requests (pour créer/modifier apps, déploiements, commits) ───

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateApp {
        private String id;
        private String name;
        private String icon;
        private String description;
        private String repo;
        private String tech;
        private String color;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateDeployment {
        private String version;
        private String status;
        private String branch;
        private Integer instances;
        private String uptime;
        private String lastDeploy;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CreateCommit {
        private String versionTag;
        private String sha;
        private String message;
        private String ticket;
        private String ticketTitle;
        private String commitType;
        private String author;
        private String commitDate;
    }
}
