package com.rdapp.deploy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "squad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Squad {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String color;

    @Column(name = "board_id", length = 50)
    private String boardId;

    @OneToMany(mappedBy = "squad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SquadMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Helper ──
    public void addMember(SquadMember member) {
        members.add(member);
        member.setSquad(this);
    }

    public void removeMember(SquadMember member) {
        members.remove(member);
        member.setSquad(null);
    }
}
