package com.sstc.platform.identity.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S01 - Identity API: an issued session token
 * (US04: verify identity, keep a secure session, revoke on sign-out).
 */
@Entity
@Table(name = "usr_session_token")
public class SessionToken extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "token", nullable = false, unique = true, length = 96)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    public SessionToken() {
    }

    public SessionToken(String token, Long userId, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
