package com.rdapp.deploy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "environment_deployment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "env_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnvironmentDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "env_key", nullable = false, length = 10)
    private String envKey;          // dev | qa | test | prod

    @Column(length = 50)
    private String version;

    @Column(length = 20)
    private String status;          // deployed | deploying | blocked

    @Column(name = "last_deploy", length = 60)
    private String lastDeploy;

    @Column(length = 120)
    private String branch;

    private Integer instances;

    @Column(length = 30)
    private String uptime;

    @Column(name = "deployed_at")
    private Instant deployedAt;
}
