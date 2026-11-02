package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.TaskCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S02 - delivery cycle accessor.
 */
public interface CyclesRepo extends JpaRepository<TaskCycle, Long> {

    List<TaskCycle> findAllByOrderByStartDateDesc();

    List<TaskCycle> findByActiveTrue();
}
