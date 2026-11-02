package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S03 - Meeting Service: AI generated summary of an ended meeting (US05).
 *
 * <p>Functionality mapping: {@code TeamCollaboration.AIMeetingAssistant}.</p>
 */
@Entity
@Table(name = "tc_meeting_summary")
public class MeetingSummary extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "meeting_id", nullable = false, unique = true)
    private Long meetingId;

    @Column(name = "overview", nullable = false, length = 2000)
    private String overview;

    /** Semicolon separated decisions. */
    @Column(name = "decisions", length = 2000)
    private String decisions;

    /** Semicolon separated action items. */
    @Column(name = "action_items", length = 2000)
    private String actionItems;

    @Column(name = "model", length = 64)
    private String model = "stc-summarizer-v1 (simulated)";

    @Column(name = "confidence", nullable = false)
    private double confidence = 0d;

    public Long getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(Long meetingId) {
        this.meetingId = meetingId;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getDecisions() {
        return decisions;
    }

    public void setDecisions(String decisions) {
        this.decisions = decisions;
    }

    public String getActionItems() {
        return actionItems;
    }

    public void setActionItems(String actionItems) {
        this.actionItems = actionItems;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
