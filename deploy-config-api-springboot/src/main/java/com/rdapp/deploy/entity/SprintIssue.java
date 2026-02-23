package com.rdapp.deploy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sprint_issue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SprintIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_sync_id", nullable = false)
    private SprintSync sprintSync;

    /** Clé Jira : ORC-312, PAY-2108, etc. */
    @Column(name = "issue_key", nullable = false, length = 30)
    private String issueKey;

    @Column(nullable = false, length = 500)
    private String summary;

    /** Story, Bug, Task, Epic, Sub-task */
    @Column(name = "issue_type", length = 30)
    private String issueType;

    /** To Do, In Progress, In Review, Done, etc. */
    @Column(name = "status_name", length = 50)
    private String statusName;

    /** new, indeterminate, done */
    @Column(name = "status_category", length = 20)
    private String statusCategory;

    /** Highest, High, Medium, Low, Lowest */
    @Column(length = 20)
    private String priority;

    @Column(name = "story_points")
    private Double storyPoints;

    @Column(name = "assignee_name", length = 100)
    private String assigneeName;

    @Column(name = "assignee_username", length = 50)
    private String assigneeUsername;

    @Column(name = "fix_version", length = 50)
    private String fixVersion;

    @Column(name = "affect_version", length = 50)
    private String affectVersion;

    @Column(name = "created_at", length = 30)
    private String createdAt;

    @Column(name = "updated_at", length = 30)
    private String updatedAt;

    @Column(name = "resolution_date", length = 30)
    private String resolutionDate;
}
