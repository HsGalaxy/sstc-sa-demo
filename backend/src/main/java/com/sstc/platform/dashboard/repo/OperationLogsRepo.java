package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.OperationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * S02 - operation log accessor (US04).
 */
public interface OperationLogsRepo extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<OperationLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);

    @Query("select l from OperationLog l where (:action is null or l.action = :action) "
            + "and (:actorId is null or l.actorId = :actorId) "
            + "order by l.createdAt desc")
    List<OperationLog> search(@Param("action") String action,
                              @Param("actorId") Long actorId,
                              Pageable pageable);
}
