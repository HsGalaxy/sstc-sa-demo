package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.DigestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - smart digest accessor (US06).
 */
public interface DigestItemsRepo extends JpaRepository<DigestItem, Long> {

    List<DigestItem> findByUserIdAndDismissedFalseOrderByRankScoreDesc(Long userId);
}
