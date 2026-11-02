package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.AccessAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S01 - access decision audit persistence (US04).
 */
public interface AccessAuditsRepo extends JpaRepository<AccessAudit, Long> {

    List<AccessAudit> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByDecision(String decision);
}
