package com.sstc.platform.identity.web;

import com.sstc.platform.identity.domain.AccessAudit;
import com.sstc.platform.identity.domain.AccountEvent;
import com.sstc.platform.identity.domain.AccountStatus;
import com.sstc.platform.identity.domain.Permission;
import com.sstc.platform.identity.domain.PermissionPolicy;
import com.sstc.platform.identity.domain.Role;
import com.sstc.platform.identity.domain.RoleType;
import com.sstc.platform.identity.domain.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * S01 - API projections. Entities are never serialised directly: the views hide the
 * password hash and flatten the relations the UI needs.
 */
public final class IdentityViews {

    private IdentityViews() {
    }

    /** Account projection returned by the Account Service (US01). */
    public static class UserView {

        public Long id;

        public String username;

        public String displayName;

        public String email;

        public String jobTitle;

        public String avatarColor;

        public RoleType role;

        public String roleLabel;

        public AccountStatus status;

        public LocalDateTime lastLoginAt;

        public LocalDateTime createdAt;

        public List<String> permissions;

        public String initials;

        public UserView(User user, List<String> permissions) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.displayName = user.getDisplayName();
            this.email = user.getEmail();
            this.jobTitle = user.getJobTitle();
            this.avatarColor = user.getAvatarColor();
            this.role = user.getRole();
            this.roleLabel = user.getRole() == null ? null : user.getRole().getLabel();
            this.status = user.getStatus();
            this.lastLoginAt = user.getLastLoginAt();
            this.createdAt = user.getCreatedAt();
            this.permissions = permissions;
            this.initials = initialsOf(user.getDisplayName());
        }

        public static String initialsOf(String name) {
            if (name == null || name.isEmpty()) {
                return "?";
            }
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, 1).toUpperCase();
            }
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
    }

    public static class RoleView {

        public Long id;

        public String code;

        public String name;

        public String description;

        public boolean builtIn;

        public RoleType roleType;

        public List<String> permissionCodes;

        public long memberCount;

        public RoleView(Role role, List<String> permissionCodes, long memberCount) {
            this.id = role.getId();
            this.code = role.getCode();
            this.name = role.getName();
            this.description = role.getDescription();
            this.builtIn = role.isBuiltIn();
            this.roleType = role.getRoleType();
            this.permissionCodes = permissionCodes;
            this.memberCount = memberCount;
        }
    }

    public static class PermissionView {

        public Long id;

        public String code;

        public String name;

        public String module;

        public String description;

        public PermissionView(Permission permission) {
            this.id = permission.getId();
            this.code = permission.getCode();
            this.name = permission.getName();
            this.module = permission.getModule();
            this.description = permission.getDescription();
        }
    }

    public static class PolicyView {

        public Long id;

        public Long userId;

        public String permissionCode;

        public String effect;

        public String reason;

        public LocalDateTime updatedAt;

        public PolicyView(PermissionPolicy policy) {
            this.id = policy.getId();
            this.userId = policy.getUserId();
            this.permissionCode = policy.getPermissionCode();
            this.effect = policy.getEffect() == null ? null : policy.getEffect().name();
            this.reason = policy.getReason();
            this.updatedAt = policy.getUpdatedAt();
        }
    }

    public static class AccountEventView {

        public Long id;

        public Long userId;

        public String eventType;

        public String detail;

        public Long actorId;

        public LocalDateTime createdAt;

        public AccountEventView(AccountEvent event) {
            this.id = event.getId();
            this.userId = event.getUserId();
            this.eventType = event.getEventType();
            this.detail = event.getDetail();
            this.actorId = event.getActorId();
            this.createdAt = event.getCreatedAt();
        }
    }

    public static class AccessAuditView {

        public Long id;

        public Long userId;

        public String username;

        public String permissionCode;

        public String resource;

        public String method;

        public String decision;

        public String reason;

        public Long durationMs;

        public LocalDateTime createdAt;

        public AccessAuditView(AccessAudit audit) {
            this.id = audit.getId();
            this.userId = audit.getUserId();
            this.username = audit.getUsername();
            this.permissionCode = audit.getPermissionCode();
            this.resource = audit.getResource();
            this.method = audit.getMethod();
            this.decision = audit.getDecision();
            this.reason = audit.getReason();
            this.durationMs = audit.getDurationMs();
            this.createdAt = audit.getCreatedAt();
        }
    }

    /** Sign-in response (US04: verify identity and open a session). */
    public static class SessionView {

        public String token;

        public LocalDateTime expiresAt;

        public UserView user;

        public SessionView(String token, LocalDateTime expiresAt, UserView user) {
            this.token = token;
            this.expiresAt = expiresAt;
            this.user = user;
        }
    }
}
