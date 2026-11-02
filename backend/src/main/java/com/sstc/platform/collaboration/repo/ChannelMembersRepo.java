package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.ChannelMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - channel membership accessor.
 */
public interface ChannelMembersRepo extends JpaRepository<ChannelMember, Long> {

    List<ChannelMember> findByUserId(Long userId);

    List<ChannelMember> findByChannelId(Long channelId);

    boolean existsByChannelIdAndUserId(Long channelId, Long userId);
}
