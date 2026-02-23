package com.rdapp.deploy.repository;

import com.rdapp.deploy.entity.SquadMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SquadMemberRepository extends JpaRepository<SquadMember, String> {

    List<SquadMember> findBySquadId(String squadId);

    void deleteBySquadId(String squadId);
}
