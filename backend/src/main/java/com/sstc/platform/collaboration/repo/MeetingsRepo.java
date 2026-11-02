package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - meeting accessor (US01).
 */
public interface MeetingsRepo extends JpaRepository<Meeting, Long> {

    List<Meeting> findAllByOrderByStartAtDesc();

    List<Meeting> findByStatusOrderByStartAtAsc(String status);
}
