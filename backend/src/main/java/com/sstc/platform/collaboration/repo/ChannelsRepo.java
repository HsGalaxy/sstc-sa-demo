package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - channel accessor (US02).
 */
public interface ChannelsRepo extends JpaRepository<Channel, Long> {

    List<Channel> findByArchivedFalseOrderByNameAsc();
}
