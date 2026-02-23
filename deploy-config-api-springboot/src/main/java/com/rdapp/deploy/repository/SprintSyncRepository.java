package com.rdapp.deploy.repository;

import com.rdapp.deploy.entity.SprintSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintSyncRepository extends JpaRepository<SprintSync, Long> {

    Optional<SprintSync> findByJiraSprintId(Long jiraSprintId);

    @Query("SELECT s FROM SprintSync s LEFT JOIN FETCH s.issues WHERE s.jiraSprintId = :jiraSprintId")
    Optional<SprintSync> findByJiraSprintIdWithIssues(Long jiraSprintId);

    List<SprintSync> findBySquadIdOrderBySyncedAtDesc(String squadId);

    @Query("SELECT s FROM SprintSync s LEFT JOIN FETCH s.issues WHERE s.squad.id = :squadId AND s.state = 'active'")
    Optional<SprintSync> findActiveBySquadId(String squadId);

    @Query("SELECT s FROM SprintSync s WHERE s.squad.id = :squadId AND s.state = 'closed' ORDER BY s.completeDate DESC")
    List<SprintSync> findClosedBySquadId(String squadId);

    List<SprintSync> findByBoardId(Long boardId);

    void deleteByJiraSprintId(Long jiraSprintId);
}
