package com.rdapp.deploy.repository;

import com.rdapp.deploy.entity.Squad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SquadRepository extends JpaRepository<Squad, String> {

    @Query("SELECT s FROM Squad s LEFT JOIN FETCH s.members ORDER BY s.name")
    List<Squad> findAllWithMembers();

    @Query("SELECT s FROM Squad s LEFT JOIN FETCH s.members WHERE s.id = :id")
    Optional<Squad> findByIdWithMembers(String id);

    boolean existsByBoardId(String boardId);
}
