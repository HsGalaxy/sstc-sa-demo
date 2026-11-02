package com.sstc.platform.identity.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S01 - Identity API + Authorization Service: one access decision record (US04).
 */
@Entity
@Table(name = "usr_access_audit")
public class AccessAudit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "permission_code", nullable = false, length = 96)
    private String permissionCode;

    @Column(name = "resource", length = 160)
    private String resource;

    @Column(name = "method", length = 12)
    private String method;

    @Column(name = "decision", nullable = false, length = 12)
    private String decision;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "duration_ms")
    private Long durationMs;

    public AccessAudit() {
    }

    public AccessAudit(Long userId, String username, String permissionCode, String resource,
                       String method, String decision, String reason, Long durationMs) {
        this.userId = userId;
        this.username = username;
        this.permissionCode = permissionCode;
        this.resource = resource;
        this.method = method;
        this.decision = decision;
        this.reason = reason;
        this.durationMs = durationMs;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
