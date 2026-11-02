package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S03 - Messaging Service: one published group message (US02).
 *
 * <p>Functionality mapping: {@code TeamCollaboration.ChannelMessageManager}.</p>
 */
@Entity
@Table(name = "tc_message")
public class Message extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public enum Kind {
        TEXT,
        FILE,
        SYSTEM,
        MEETING
    }

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private Kind kind = Kind.TEXT;

    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt;

    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "attachment_name", length = 200)
    private String attachmentName;

    @Column(name = "reply_to_id")
    private Long replyToId;

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }
}
