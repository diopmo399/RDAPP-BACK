package com.rdapp.deploy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sprint synchronisé depuis Jira DC.
 * Lié à une escouade via son board ID.
 */
@Entity
@Table(name = "sprint_sync")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SprintSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID du sprint dans Jira */
    @Column(name = "jira_sprint_id", nullable = false)
    private Long jiraSprintId;

    /** Escouade associée */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "squad_id")
    private Squad squad;

    @Column(nullable = false, length = 200)
    private String name;

    /** active, closed, future */
    @Column(nullable = false, length = 20)
    private String state;

    @Column(length = 500)
    private String goal;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "complete_date")
    private LocalDateTime completeDate;

    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "project_key", length = 20)
    private String projectKey;

    /** Stats calculées lors du sync */
    @Column(name = "total_issues")
    private Integer totalIssues;

    @Column(name = "done_issues")
    private Integer doneIssues;

    @Column(name = "total_story_points")
    private Double totalStoryPoints;

    @Column(name = "done_story_points")
    private Double doneStoryPoints;

    @OneToMany(mappedBy = "sprintSync", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SprintIssue> issues = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    // ── Helpers ──

    public void addIssue(SprintIssue issue) {
        issues.add(issue);
        issue.setSprintSync(this);
    }

    public void clearIssues() {
        issues.forEach(i -> i.setSprintSync(null));
        issues.clear();
    }

    public double getCompletionPercent() {
        if (totalIssues == null || totalIssues == 0) return 0;
        return (doneIssues != null ? doneIssues : 0) * 100.0 / totalIssues;
    }
}
