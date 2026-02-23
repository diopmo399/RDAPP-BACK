package com.rdapp.deploy.dto;

import com.rdapp.deploy.entity.VersionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AffectVersionDto {

    private AffectVersionDto() {}

    // ── Response ──
    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private VersionStatus status;
        private LocalDate releaseDate;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Create ──
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Create {
        @NotBlank @Size(max = 20)
        private String name;

        @NotNull
        private VersionStatus status;

        private LocalDate releaseDate;

        @Size(max = 500)
        private String description;
    }

    // ── Update (PATCH) ──
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Update {
        @Size(max = 20)
        private String name;

        private VersionStatus status;

        private LocalDate releaseDate;

        @Size(max = 500)
        private String description;
    }
}
