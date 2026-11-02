package com.sstc.platform.collaboration.repo;

import com.sstc.platform.collaboration.domain.MeetingTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S03 - meeting transcript accessor (US05).
 */
public interface TranscriptsRepo extends JpaRepository<MeetingTranscript, Long> {

    List<MeetingTranscript> findByMeetingIdOrderByOffsetSecondsAsc(Long meetingId);
}
