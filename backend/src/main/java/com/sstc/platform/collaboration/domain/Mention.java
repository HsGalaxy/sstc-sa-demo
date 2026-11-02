package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S03 - Messaging Service: one @mention detected inside a message (US03).
 *
 * <p>Functionality mapping: {@code TeamCollaboration.MentionDetector}.</p>
 */
@Entity
@Table(name = "tc_mention")
public class Mention extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Column(name = "alerted", nullable = false)
    private boolean alerted = false;

    public Mention() {
    }

    public Mention(Long messageId, Long channelId, Long targetUserId, String token) {
        this.messageId = messageId;
        this.channelId = channelId;
        this.targetUserId = targetUserId;
        this.token = token;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isAlerted() {
        return alerted;
    }

    public void setAlerted(boolean alerted) {
        this.alerted = alerted;
    }
}
