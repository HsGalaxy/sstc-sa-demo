package com.sstc.platform.collaboration.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S03 - Notification Service: one delivered notification (US04).
 *
 * <p>Functionality mapping: {@code TeamCollaboration.NotificationDispatcher}.</p>
 */
@Entity
@Table(name = "tc_notification")
public class Notification extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public enum Kind {
        MENTION,
        TASK,
        MEETING,
        FILE,
        SYSTEM
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH
    }

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 24)
    private Kind kind = Kind.SYSTEM;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private Priority priority = Priority.NORMAL;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", length = 512)
    private String body;

    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "read_flag", nullable = false)
    private boolean read = false;

    @Column(name = "delivered_at", nullable = false)
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
