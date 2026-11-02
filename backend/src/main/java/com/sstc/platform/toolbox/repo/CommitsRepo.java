package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.CommitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - commit history accessor (US03).
 */
public interface CommitsRepo extends JpaRepository<CommitRecord, Long> {

    List<CommitRecord> findByRepoIdOrderByCommittedAtDesc(Long repoId);

    List<CommitRecord> findTop20ByOrderByCommittedAtDesc();
}
