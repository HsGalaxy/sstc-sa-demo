package com.sstc.platform.common.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Cross-cutting audit trail of the platform. Every sub-system writes here so the
 * {@code OperationLogger} (S02 US04), the {@code AccessAuditRecorder} (S01 US04) and the
 * Toolbox integrations share a single, queryable history.
 */
@Entity
@Table(name = "cm_audit_log")
public class AuditLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "module", nullable = false, length = 16)
    private String module;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_name", length = 96)
    private String actorName;

    @Column(name = "target_type", length = 48)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "outcome", nullable = false, length = 24)
    private String outcome = "SUCCESS";

    @Column(name = "duration_ms")
    private Long durationMs;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
