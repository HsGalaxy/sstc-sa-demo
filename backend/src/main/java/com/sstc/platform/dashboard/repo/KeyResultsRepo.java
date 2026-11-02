package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.KeyResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S02 - key result accessor (US05 / US06).
 */
public interface KeyResultsRepo extends JpaRepository<KeyResult, Long> {

    List<KeyResult> findByOkrId(Long okrId);
}
