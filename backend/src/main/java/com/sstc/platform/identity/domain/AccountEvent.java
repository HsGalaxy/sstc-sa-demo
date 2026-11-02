package com.sstc.platform.identity.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S01 - Account Service: lifecycle event emitted for audit and notification (US01).
 */
@Entity
@Table(name = "usr_account_event")
public class AccountEvent extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 48)
    private String eventType;

    @Column(name = "detail", length = 512)
    private String detail;

    @Column(name = "actor_id")
    private Long actorId;

    public AccountEvent() {
    }

    public AccountEvent(Long userId, String eventType, String detail, Long actorId) {
        this.userId = userId;
        this.eventType = eventType;
        this.detail = detail;
        this.actorId = actorId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }
}
