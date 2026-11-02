package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S03 - Meeting Service: one transcribed segment of a meeting (US05).
 */
@Entity
@Table(name = "tc_meeting_transcript")
public class MeetingTranscript extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "speaker_id")
    private Long speakerId;

    @Column(name = "speaker_name", length = 96)
    private String speakerName;

    @Column(name = "offset_seconds", nullable = false)
    private int offsetSeconds = 0;

    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public Long getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(Long speakerId) {
        this.speakerId = speakerId;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public int getOffsetSeconds() {
        return offsetSeconds;
    }

    public void setOffsetSeconds(int offsetSeconds) {
        this.offsetSeconds = offsetSeconds;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
