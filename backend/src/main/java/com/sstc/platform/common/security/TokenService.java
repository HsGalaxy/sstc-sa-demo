package com.sstc.platform.common.security;

import com.sstc.platform.identity.domain.AccountStatus;
import com.sstc.platform.identity.domain.Permission;
import com.sstc.platform.identity.domain.PermissionPolicy;
import com.sstc.platform.identity.domain.RolePermission;
import com.sstc.platform.identity.domain.SessionToken;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.domain.UserRole;
import com.sstc.platform.identity.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * S01 - Identity API: credential verification, session issue and revocation, and the
 * permission lookup used by the Authorization Service (US04).
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final SessionTokensRepo tokens;

    private final UsersRepo users;

    private final UserRolesRepo userRoles;

    private final RolePermissionsRepo rolePermissions;

    private final PermissionsRepo permissions;

    private final PoliciesRepo policies;

    private final long ttlHours;

    public TokenService(SessionTokensRepo tokens,
                        UsersRepo users,
                        UserRolesRepo userRoles,
                        RolePermissionsRepo rolePermissions,
                        PermissionsRepo permissions,
                        PoliciesRepo policies,
                        @Value("${sstc.security.token-ttl-hours:12}") long ttlHours) {
        this.tokens = tokens;
        this.users = users;
        this.userRoles = userRoles;
        this.rolePermissions = rolePermissions;
        this.permissions = permissions;
        this.policies = policies;
        this.ttlHours = ttlHours;
    }

    @Transactional
    public String issue(User user) {
        LocalDateTime now = LocalDateTime.now();
        SessionToken token = new SessionToken(randomToken(), user.getId(), now, now.plusHours(ttlHours));
        tokens.save(token);
        return token.getToken();
    }

    @Transactional
    public void revoke(String token) {
        tokens.findByToken(token).ifPresent(t -> {
            t.setRevoked(true);
            tokens.save(t);
        });
    }

    /**
     * Resolve a bearer token to the current user. Returns an empty value when the token is
     * unknown, revoked, expired or the account is no longer active.
     */
    @Transactional(readOnly = true)
    public Optional<CurrentUser> resolve(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        SessionToken session = tokens.findByToken(token).orElse(null);
        if (session == null || session.isRevoked() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        User user = users.findById(session.getUserId()).orElse(null);
        if (user == null || user.getStatus() != AccountStatus.ACTIVE) {
            return Optional.empty();
        }
        return Optional.of(new CurrentUser(user, permissionsOf(user)));
    }

    /**
     * Effective permission set = permissions of the assigned role + GRANT policies - REVOKE
     * policies. This is the {@code Permission Lookup} component of the Identity API.
     */
    @Transactional(readOnly = true)
    public Set<String> permissionsOf(User user) {
        Set<String> effective = new HashSet<String>();
        List<UserRole> assignments = userRoles.findByUserId(user.getId());
        for (UserRole assignment : assignments) {
            for (RolePermission link : rolePermissions.findByRoleId(assignment.getRoleId())) {
                permissions.findById(link.getPermissionId())
                        .ifPresent(p -> effective.add(p.getCode()));
            }
        }
        if (assignments.isEmpty()) {
            // No explicit assignment: fall back to the built-in role matrix.
            effective.addAll(defaultPermissions(user));
        }
        for (PermissionPolicy policy : policies.findByUserId(user.getId())) {
            if (policy.getEffect() == PermissionPolicy.Effect.GRANT) {
                effective.add(policy.getPermissionCode());
            } else {
                effective.remove(policy.getPermissionCode());
            }
        }
        return effective;
    }

    /**
     * Built-in role matrix used when an account has no explicit {@code usr_user_role} row.
     * It mirrors the seeded role/permission bindings and keeps the demo resilient.
     */
    private Set<String> defaultPermissions(User user) {
        Set<String> result = new HashSet<String>();
        for (Permission permission : permissions.findAllByOrderByModuleAscCodeAsc()) {
            String module = permission.getModule() == null ? "" : permission.getModule();
            boolean allowed;
            switch (user.getRole()) {
                case OVERALL_LEAD:
                    allowed = true;
                    break;
                case SENIOR_MANAGER:
                    allowed = !"S01.ACCOUNT.MANAGE".equals(permission.getCode())
                            && !"S01.POLICY.MANAGE".equals(permission.getCode());
                    break;
                case TEAM_MEMBER:
                default:
                    allowed = module.startsWith("S02") || module.startsWith("S03")
                            || module.startsWith("S04");
                    break;
            }
            if (allowed) {
                result.add(permission.getCode());
            }
        }
        return result;
    }

    private String randomToken() {
        StringBuilder builder = new StringBuilder("stc_");
        for (int i = 0; i < 40; i++) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
