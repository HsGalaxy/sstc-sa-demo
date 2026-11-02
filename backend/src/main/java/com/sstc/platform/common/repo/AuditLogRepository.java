package com.sstc.platform.common.repo;

import com.sstc.platform.common.domain.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Cross-cutting audit accessor shared by all sub-systems.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select a from AuditLog a where (:module is null or a.module = :module) "
            + "and (:action is null or a.action = :action) "
            + "and (:actorId is null or a.actorId = :actorId) "
            + "order by a.createdAt desc")
    List<AuditLog> search(@Param("module") String module,
                          @Param("action") String action,
                          @Param("actorId") Long actorId,
                          Pageable pageable);

    List<AuditLog> findTop30ByModuleOrderByCreatedAtDesc(String module);
}
