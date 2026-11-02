package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.Mention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - detected @mention accessor (US03).
 */
public interface MentionsRepo extends JpaRepository<Mention, Long> {

    List<Mention> findByMessageId(Long messageId);

    List<Mention> findByTargetUserIdOrderByCreatedAtDesc(Long userId);

    long countByTargetUserIdAndAlertedFalse(Long userId);
}
