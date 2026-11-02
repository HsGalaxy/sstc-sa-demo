package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - published message accessor (US02).
 */
public interface MessagesRepo extends JpaRepository<Message, Long> {

    List<Message> findByChannelIdOrderByPostedAtAsc(Long channelId);

    List<Message> findTop50ByChannelIdOrderByPostedAtDesc(Long channelId);

    long countByChannelId(Long channelId);

    List<Message> findTop6ByOrderByPostedAtDesc();
}
