package com.rdapp.deploy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "application")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 10)
    private String icon;

    @Column(length = 255)
    private String description;

    @Column(length = 120)
    private String repo;

    @Column(length = 30, nullable = false)
    private String tech;          // camunda | spring | angular

    @Column(length = 20)
    private String color;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("envKey")
    @Builder.Default
    private List<EnvironmentDeployment> deployments = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("versionTag DESC, commitDate DESC")
    @Builder.Default
    private List<CommitRecord> commits = new ArrayList<>();
}
