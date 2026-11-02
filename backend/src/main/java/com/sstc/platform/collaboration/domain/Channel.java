package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S03 - Messaging Service: a group channel / conversation (US02).
 */
@Entity
@Table(name = "tc_channel")
public class Channel extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "topic", length = 255)
    private String topic;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "private_channel", nullable = false)
    private boolean privateChannel = false;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @Column(name = "created_by")
    private Long createdBy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public boolean isPrivateChannel() {
        return privateChannel;
    }

    public void setPrivateChannel(boolean privateChannel) {
        this.privateChannel = privateChannel;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
