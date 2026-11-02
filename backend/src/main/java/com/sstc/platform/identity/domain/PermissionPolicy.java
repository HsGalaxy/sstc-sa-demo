package com.sstc.platform.identity.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * S01 - Role &amp; Permission API: a per-account permission override
 * (US03: update policy, grant / revoke permission).
 */
@Entity
@Table(name = "usr_permission_policy")
public class PermissionPolicy extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public enum Effect {
        GRANT,
        REVOKE
    }

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "permission_code", nullable = false, length = 96)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect", nullable = false, length = 16)
    private Effect effect = Effect.GRANT;

    @Column(name = "reason", length = 255)
    private String reason;

    public PermissionPolicy() {
    }

    public PermissionPolicy(Long userId, String permissionCode, Effect effect, String reason) {
        this.userId = userId;
        this.permissionCode = permissionCode;
        this.effect = effect;
        this.reason = reason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public Effect getEffect() {
        return effect;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
