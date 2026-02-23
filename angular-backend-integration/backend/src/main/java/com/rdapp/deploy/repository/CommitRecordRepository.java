package com.rdapp.deploy.repository;

import com.rdapp.deploy.entity.CommitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitRecordRepository extends JpaRepository<CommitRecord, String> {

    List<CommitRecord> findByApplicationIdOrderByVersionTagDescCommitDateDesc(String applicationId);

    List<CommitRecord> findByApplicationIdAndVersionTagOrderByCommitDateDesc(String applicationId, String versionTag);
}
