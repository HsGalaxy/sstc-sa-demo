package com.sstc.platform.dashboard.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S02 - Task Controller: full lifecycle operation event used for audit tracing (US04).
 *
 * <p>Functionality mapping: {@code TaskDashboard.OperationLogger}.</p>
 */
@Entity
@Table(name = "td_operation_log")
public class OperationLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", length = 96)
    private String actorName;

    @Column(name = "action", nullable = false, length = 48)
    private String action;

    @Column(name = "target_type", nullable = false, length = 48)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "outcome", nullable = false, length = 24)
    private String outcome = "SUCCESS";

    @Column(name = "source", length = 24)
    private String source = "API";

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
