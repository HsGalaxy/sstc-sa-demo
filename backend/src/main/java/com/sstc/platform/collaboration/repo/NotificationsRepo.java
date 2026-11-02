package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - notification accessor (US04).
 */
public interface NotificationsRepo extends JpaRepository<Notification, Long> {

    List<Notification> findTop20ByRecipientIdOrderByDeliveredAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndReadFalseOrderByDeliveredAtDesc(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);
}
