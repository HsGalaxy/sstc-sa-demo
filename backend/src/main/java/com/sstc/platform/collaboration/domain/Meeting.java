package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S03 - Meeting Service: one online meeting and its lifecycle (US01).
 *
 * <p>Functionality mapping: {@code TeamCollaboration.MeetingOrchestrator}.</p>
 */
@Entity
@Table(name = "tc_meeting")
public class Meeting extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "agenda", length = 1000)
    private String agenda;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "status", nullable = false, length = 24)
    private String status = "SCHEDULED";

    @Column(name = "join_code", nullable = false, unique = true, length = 16)
    private String joinCode;

    @Column(name = "participant_ids", length = 512)
    private String participantIds;

    @Column(name = "recording_url", length = 255)
    private String recordingUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAgenda() {
        return agenda;
    }

    public void setAgenda(String agenda) {
        this.agenda = agenda;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public String getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(String participantIds) {
        this.participantIds = participantIds;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }
}
