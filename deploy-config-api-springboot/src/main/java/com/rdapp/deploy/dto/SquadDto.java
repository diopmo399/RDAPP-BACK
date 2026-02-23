package com.rdapp.deploy.dto;

import com.rdapp.deploy.entity.MemberRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public final class SquadDto {

    private SquadDto() {}

    // ── Member nested ──
    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MemberResponse {
        private String id;
        private String name;
        private MemberRole role;
        private String employeeCode;
        private String username;
        private String github;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MemberCreate {
        @NotBlank @Size(max = 100)
        private String name;

        @NotNull
        private MemberRole role;

        @Size(max = 20)
        private String employeeCode;

        @Size(max = 50)
        private String username;

        @Size(max = 100)
        private String github;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MemberUpdate {
        @Size(max = 100)
        private String name;

        private MemberRole role;

        @Size(max = 20)
        private String employeeCode;

        @Size(max = 50)
        private String username;

        @Size(max = 100)
        private String github;
    }

    // ── Squad ──
    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String color;
        private String boardId;
        private List<MemberResponse> members;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Create {
        @NotBlank @Size(max = 100)
        private String name;

        @NotBlank @Size(max = 10)
        private String color;

        @Size(max = 50)
        private String boardId;

        @Valid
        private List<MemberCreate> members;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Update {
        @Size(max = 100)
        private String name;

        @Size(max = 10)
        private String color;

        @Size(max = 50)
        private String boardId;
    }

    // ── Board sync request / response ──
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class BoardSyncRequest {
        @NotBlank
        private String boardId;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BoardSyncResponse {
        private String squadId;
        private String boardId;
        private boolean success;
        private String message;
        private LocalDateTime syncedAt;
    }
}
