package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.AccountEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S01 - account lifecycle event persistence (US01).
 */
public interface AccountEventsRepo extends JpaRepository<AccountEvent, Long> {

    List<AccountEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AccountEvent> findByUserIdOrderByCreatedAtDesc(Long userId);
}
