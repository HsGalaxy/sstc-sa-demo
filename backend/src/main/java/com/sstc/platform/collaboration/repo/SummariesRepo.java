package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * S03 - AI meeting summary accessor (US05).
 */
public interface SummariesRepo extends JpaRepository<MeetingSummary, Long> {

    MeetingSummary findByMeetingId(Long meetingId);
}
