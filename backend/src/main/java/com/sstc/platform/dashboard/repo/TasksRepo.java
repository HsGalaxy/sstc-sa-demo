package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.FileRecord;
import com.sstc.platform.dashboard.domain.FileVersion;
import com.sstc.platform.dashboard.domain.KeyResult;
import com.sstc.platform.dashboard.domain.Okr;
import com.sstc.platform.dashboard.domain.OperationLog;
import com.sstc.platform.dashboard.domain.PerformanceSnapshot;
import com.sstc.platform.dashboard.domain.Task;
import com.sstc.platform.dashboard.domain.TaskCycle;
import com.sstc.platform.dashboard.domain.TaskPriority;
import com.sstc.platform.dashboard.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * S02 - Task Dashboard persistence accessors. In the Level-3 model these are the supporting
 * "Accessor" components that only provide persistence, never user facing functionality.
 */
public interface TasksRepo extends JpaRepository<Task, Long> {

    List<Task> findAllByOrderByStatusAscPositionIndexAsc();

    List<Task> findByAssigneeIdOrderByDueDateAsc(Long assigneeId);

    List<Task> findByCycleId(Long cycleId);

    long countByStatus(TaskStatus status);

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    long countByAssigneeIdAndDueDateBeforeAndStatusNot(Long assigneeId, LocalDate date, TaskStatus status);

    List<Task> findTop8ByOrderByUpdatedAtDesc();

    @Query("select t from Task t where (:status is null or t.status = :status) "
            + "and (:assigneeId is null or t.assigneeId = :assigneeId) "
            + "and (:cycleId is null or t.cycleId = :cycleId) "
            + "and (:priority is null or t.priority = :priority) "
            + "and (:keyword is null or lower(t.title) like lower(concat('%', :keyword, '%')) "
            + "or lower(t.code) like lower(concat('%', :keyword, '%'))) "
            + "order by t.status asc, t.positionIndex asc")
    Page<Task> search(@Param("status") TaskStatus status,
                      @Param("assigneeId") Long assigneeId,
                      @Param("cycleId") Long cycleId,
                      @Param("priority") TaskPriority priority,
                      @Param("keyword") String keyword,
                      Pageable pageable);
}
