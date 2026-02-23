package com.rdapp.deploy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "commit_record")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "version_tag", nullable = false, length = 50)
    private String versionTag;

    @Column(nullable = false, length = 12)
    private String sha;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 30)
    private String ticket;

    @Column(name = "ticket_title", length = 200)
    private String ticketTitle;

    @Column(name = "commit_type", length = 20)
    private String commitType;     // feat | fix | perf | test | docs | chore

    @Column(length = 80)
    private String author;

    @Column(name = "commit_date", length = 60)
    private String commitDate;

    @Column(name = "committed_at")
    private Instant committedAt;
}
