package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.PerformanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S02 - performance indicator snapshot accessor (US05 / US06).
 */
public interface SnapshotsRepo extends JpaRepository<PerformanceSnapshot, Long> {

    List<PerformanceSnapshot> findByUserIdIsNullOrderBySnapshotDateAsc();

    List<PerformanceSnapshot> findByUserIdOrderBySnapshotDateAsc(Long userId);
}
