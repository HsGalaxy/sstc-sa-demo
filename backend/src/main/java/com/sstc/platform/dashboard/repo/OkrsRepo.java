package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.Okr;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * S02 - OKR objective accessor (US05 / US06).
 */
public interface OkrsRepo extends JpaRepository<Okr, Long> {
}
