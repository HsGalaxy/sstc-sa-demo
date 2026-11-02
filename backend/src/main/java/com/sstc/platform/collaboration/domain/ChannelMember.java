package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S03 - Messaging Service: membership of a channel (US02).
 */
@Entity
@Table(name = "tc_channel_member")
public class ChannelMember extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "muted", nullable = false)
    private boolean muted = false;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    public ChannelMember() {
    }

    public ChannelMember(Long channelId, Long userId) {
        this.channelId = channelId;
        this.userId = userId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public Long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
