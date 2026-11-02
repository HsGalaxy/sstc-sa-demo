package com.sstc.platform.common.security;

import com.sstc.platform.identity.domain.RoleType;
import com.sstc.platform.identity.domain.User;

import java.util.Collections;
import java.util.Set;

/**
 * The identity attached to the current request. Injected into controllers through
 * {@link CurrentUserResolver} so the sub-systems never touch the session table directly.
 */
public class CurrentUser {

    private final Long id;

    private final String username;

    private final String displayName;

    private final RoleType role;

    private final Set<String> permissions;

    public CurrentUser(User user, Set<String> permissions) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.displayName = user.getDisplayName();
        this.role = user.getRole();
        this.permissions = permissions == null ? Collections.<String>emptySet() : permissions;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RoleType getRole() {
        return role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean has(String permission) {
        return permissions.contains(permission);
    }

    public boolean isLead() {
        return role == RoleType.OVERALL_LEAD;
    }
}
