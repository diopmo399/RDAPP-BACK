package com.rdapp.deploy.repository;

import com.rdapp.deploy.entity.AffectVersion;
import com.rdapp.deploy.entity.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AffectVersionRepository extends JpaRepository<AffectVersion, String> {

    List<AffectVersion> findByStatusInOrderByReleaseDateDesc(List<VersionStatus> statuses);

    List<AffectVersion> findAllByOrderByReleaseDateDesc();

    boolean existsByName(String name);
}
