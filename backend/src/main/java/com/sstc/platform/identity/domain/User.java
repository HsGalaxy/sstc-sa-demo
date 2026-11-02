package com.sstc.platform.identity.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S01 - Account Service: a platform account.
 *
 * <p>Functionality mapping: {@code UserRoleManager.AccountController} (US01).</p>
 */
@Entity
@Table(name = "usr_account")
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "display_name", nullable = false, length = 96)
    private String displayName;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    /** BCrypt hash - never returned by the API. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "job_title", length = 96)
    private String jobTitle;

    @Column(name = "avatar_color", length = 16)
    private String avatarColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private RoleType role = RoleType.TEAM_MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
