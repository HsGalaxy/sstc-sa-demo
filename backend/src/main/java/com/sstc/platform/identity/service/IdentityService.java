package com.sstc.platform.identity.service;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUser;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.security.TokenService;
import com.sstc.platform.common.web.AppException;
import com.sstc.platform.identity.domain.AccessAudit;
import com.sstc.platform.identity.domain.AccountEvent;
import com.sstc.platform.identity.domain.AccountStatus;
import com.sstc.platform.identity.domain.Permission;
import com.sstc.platform.identity.domain.PermissionPolicy;
import com.sstc.platform.identity.domain.Role;
import com.sstc.platform.identity.domain.RolePermission;
import com.sstc.platform.identity.domain.RoleType;
import com.sstc.platform.identity.domain.SessionToken;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.domain.UserRole;
import com.sstc.platform.identity.repo.*;
import com.sstc.platform.identity.web.IdentityRequests;
import com.sstc.platform.identity.web.IdentityViews;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * S01 - Identity, Role &amp; Access Management.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>Account Service - {@code UserRoleManager.AccountController} (US01)</li>
 *   <li>Role &amp; Permission API - {@code RoleAssignmentController} (US02),
 *       {@code PermissionPolicyController} (US03)</li>
 *   <li>Identity API + Authorization Service - {@code AuthorizationAuditController} (US04)</li>
 * </ul>
 */
@Service
public class IdentityService {

    private final UsersRepo users;

    private final RolesRepo roles;

    private final PermissionsRepo permissions;

    private final RolePermissionsRepo rolePermissions;

    private final UserRolesRepo userRoles;

    private final PoliciesRepo policies;

    private final AccountEventsRepo accountEvents;

    private final AccessAuditsRepo accessAudits;

    private final SessionTokensRepo sessionTokens;

    private final TokenService tokenService;

    private final UserDirectory directory;

    private final PasswordEncoder passwordEncoder;

    private final AuditService auditService;

    public IdentityService(UsersRepo users,
                           RolesRepo roles,
                           PermissionsRepo permissions,
                           RolePermissionsRepo rolePermissions,
                           UserRolesRepo userRoles,
                           PoliciesRepo policies,
                           AccountEventsRepo accountEvents,
                           AccessAuditsRepo accessAudits,
                           SessionTokensRepo sessionTokens,
                           TokenService tokenService,
                           UserDirectory directory,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
        this.userRoles = userRoles;
        this.policies = policies;
        this.accountEvents = accountEvents;
        this.accessAudits = accessAudits;
        this.sessionTokens = sessionTokens;
        this.tokenService = tokenService;
        this.directory = directory;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ US04

    @Transactional
    public IdentityViews.SessionView login(IdentityRequests.LoginRequest request) {
        User user = users.findByUsername(request.username.trim()).orElse(null);
        if (user == null) {
            user = users.findByEmail(request.username.trim()).orElse(null);
        }
        if (user == null || !passwordEncoder.matches(request.password, user.getPasswordHash())) {
            auditService.failure("S01", "LOGIN", "account", request.username, "invalid credentials");
            throw AppException.unauthorized("Invalid username or password");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            auditService.failure("S01", "LOGIN", "account", user.getUsername(),
                    "account status " + user.getStatus());
            throw AppException.forbidden("Account is " + user.getStatus().name().toLowerCase());
        }
        String token = tokenService.issue(user);
        user.setLastLoginAt(LocalDateTime.now());
        users.save(user);
        SessionToken session = sessionTokens.findByToken(token).orElse(null);
        Set<String> effective = tokenService.permissionsOf(user);
        accountEvents.save(new AccountEvent(user.getId(), "LOGIN", "Successful sign-in", user.getId()));
        auditService.success("S01", "LOGIN", "account", user.getUsername(), "session opened");
        return new IdentityViews.SessionView(token,
                session == null ? null : session.getExpiresAt(),
                new IdentityViews.UserView(user, new ArrayList<String>(effective)));
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            tokenService.revoke(token);
        }
        auditService.success("S01", "LOGOUT", "session", token, "session revoked");
    }

    @Transactional(readOnly = true)
    public IdentityViews.UserView me() {
        User user = directory.require(CurrentUserHolder.id());
        Set<String> effective = tokenService.permissionsOf(user);
        return new IdentityViews.UserView(user, new ArrayList<String>(effective));
    }

    // ------------------------------------------------------------------ US01

    @Transactional(readOnly = true)
    public List<IdentityViews.UserView> listUsers() {
        List<IdentityViews.UserView> result = new ArrayList<IdentityViews.UserView>();
        for (User user : directory.all()) {
            result.add(new IdentityViews.UserView(user,
                    new ArrayList<String>(tokenService.permissionsOf(user))));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public IdentityViews.UserView getUser(Long id) {
        User user = directory.require(id);
        return new IdentityViews.UserView(user,
                new ArrayList<String>(tokenService.permissionsOf(user)));
    }

    @Transactional
    public IdentityViews.UserView createUser(IdentityRequests.CreateUserRequest request) {
        if (users.existsByUsername(request.username)) {
            throw AppException.conflict("Username already exists: " + request.username);
        }
        if (users.existsByEmail(request.email)) {
            throw AppException.conflict("Email already exists: " + request.email);
        }
        User user = new User();
        user.setUsername(request.username.trim());
        user.setDisplayName(request.displayName.trim());
        user.setEmail(request.email.trim());
        user.setPasswordHash(passwordEncoder.encode(request.password));
        user.setJobTitle(request.jobTitle);
        user.setRole(request.role == null ? RoleType.TEAM_MEMBER : request.role);
        user.setAvatarColor(request.avatarColor == null ? randomColor() : request.avatarColor);
        user.setStatus(AccountStatus.ACTIVE);
        users.save(user);

        // bind the role of the requested role type so the permission matrix applies
        roles.findByCode(roleCodeOf(user.getRole())).ifPresent(role ->
                userRoles.save(new UserRole(user.getId(), role.getId())));
        accountEvents.save(new AccountEvent(user.getId(), "ACCOUNT_CREATED",
                "Account created for " + user.getDisplayName(), CurrentUserHolder.id()));
        auditService.success("S01", "ACCOUNT_CREATE", "account", user.getUsername(),
                "created with role " + user.getRole());
        return getUser(user.getId());
    }

    @Transactional
    public IdentityViews.UserView updateUser(Long id, IdentityRequests.UpdateUserRequest request) {
        User user = directory.require(id);
        StringBuilder changes = new StringBuilder();
        if (request.displayName != null && !request.displayName.trim().isEmpty()) {
            user.setDisplayName(request.displayName.trim());
            changes.append("displayName ");
        }
        if (request.email != null && !request.email.trim().isEmpty()
                && !request.email.equals(user.getEmail())) {
            if (users.existsByEmail(request.email)) {
                throw AppException.conflict("Email already exists: " + request.email);
            }
            user.setEmail(request.email.trim());
            changes.append("email ");
        }
        if (request.jobTitle != null) {
            user.setJobTitle(request.jobTitle);
            changes.append("jobTitle ");
        }
        if (request.avatarColor != null) {
            user.setAvatarColor(request.avatarColor);
        }
        if (request.role != null && request.role != user.getRole()) {
            user.setRole(request.role);
            userRoles.deleteByUserId(user.getId());
            roles.findByCode(roleCodeOf(request.role)).ifPresent(role ->
                    userRoles.save(new UserRole(user.getId(), role.getId())));
            changes.append("role=").append(request.role).append(' ');
        }
        if (request.status != null && request.status != user.getStatus()) {
            user.setStatus(request.status);
            changes.append("status=").append(request.status).append(' ');
        }
        users.save(user);
        accountEvents.save(new AccountEvent(user.getId(), "ACCOUNT_UPDATED",
                changes.length() == 0 ? "No field changed" : changes.toString().trim(),
                CurrentUserHolder.id()));
        auditService.success("S01", "ACCOUNT_UPDATE", "account", user.getUsername(),
                changes.length() == 0 ? "no change" : changes.toString().trim());
        return getUser(user.getId());
    }

    @Transactional
    public IdentityViews.UserView deactivateUser(Long id) {
        User user = directory.require(id);
        user.setStatus(AccountStatus.DEACTIVATED);
        users.save(user);
        for (SessionToken token : sessionTokens.findByUserIdAndRevokedFalse(id)) {
            token.setRevoked(true);
            sessionTokens.save(token);
        }
        accountEvents.save(new AccountEvent(user.getId(), "ACCOUNT_DEACTIVATED",
                "Account deactivated, all sessions revoked", CurrentUserHolder.id()));
        auditService.success("S01", "ACCOUNT_DEACTIVATE", "account", user.getUsername(),
                "sessions revoked");
        return getUser(user.getId());
    }

    @Transactional
    public void changePassword(Long id, IdentityRequests.ChangePasswordRequest request) {
        User user = directory.require(id);
        if (!passwordEncoder.matches(request.currentPassword, user.getPasswordHash())) {
            throw AppException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword));
        users.save(user);
        auditService.success("S01", "PASSWORD_CHANGE", "account", user.getUsername(), "password rotated");
    }

    // ------------------------------------------------------------------ US02

    @Transactional(readOnly = true)
    public List<IdentityViews.RoleView> listRoles() {
        List<IdentityViews.RoleView> result = new ArrayList<IdentityViews.RoleView>();
        for (Role role : roles.findAllByOrderByRoleTypeAsc()) {
            List<String> codes = new ArrayList<String>();
            for (RolePermission link : rolePermissions.findByRoleId(role.getId())) {
                permissions.findById(link.getPermissionId()).ifPresent(p -> codes.add(p.getCode()));
            }
            long members = 0L;
            for (UserRole assignment : userRoles.findAll()) {
                if (assignment.getRoleId().equals(role.getId())) {
                    members++;
                }
            }
            result.add(new IdentityViews.RoleView(role, codes, members));
        }
        return result;
    }

    @Transactional
    public IdentityViews.UserView assignRole(Long userId, IdentityRequests.AssignRoleRequest request) {
        User user = directory.require(userId);
        Role role = roles.findByCode(request.roleCode)
                .orElseThrow(() -> AppException.notFound("Role " + request.roleCode));
        userRoles.deleteByUserId(userId);
        UserRole assignment = new UserRole(userId, role.getId());
        assignment.setAdminPrivilege(request.adminPrivilege);
        userRoles.save(assignment);
        if (role.getRoleType() != null) {
            user.setRole(role.getRoleType());
            users.save(user);
        }
        accountEvents.save(new AccountEvent(userId, "ROLE_ASSIGNED",
                "Role " + role.getCode() + " assigned", CurrentUserHolder.id()));
        auditService.success("S01", "ROLE_ASSIGN", "account", user.getUsername(),
                "role=" + role.getCode() + " admin=" + request.adminPrivilege);
        return getUser(userId);
    }

    @Transactional
    public IdentityViews.UserView revokeRole(Long userId) {
        User user = directory.require(userId);
        userRoles.deleteByUserId(userId);
        accountEvents.save(new AccountEvent(userId, "ROLE_REVOKED",
                "All role assignments removed", CurrentUserHolder.id()));
        auditService.success("S01", "ROLE_REVOKE", "account", user.getUsername(), "assignments removed");
        return getUser(userId);
    }

    @Transactional(readOnly = true)
    public List<IdentityViews.PermissionView> listPermissions() {
        List<IdentityViews.PermissionView> result = new ArrayList<IdentityViews.PermissionView>();
        for (Permission permission : permissions.findAllByOrderByModuleAscCodeAsc()) {
            result.add(new IdentityViews.PermissionView(permission));
        }
        return result;
    }

    // ------------------------------------------------------------------ US03

    @Transactional
    public IdentityViews.PolicyView setPolicy(Long userId, IdentityRequests.PolicyRequest request) {
        User user = directory.require(userId);
        PermissionPolicy.Effect effect;
        try {
            effect = PermissionPolicy.Effect.valueOf(request.effect.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw AppException.badRequest("effect must be GRANT or REVOKE");
        }
        PermissionPolicy policy = new PermissionPolicy(userId, request.permissionCode, effect, request.reason);
        policies.save(policy);
        accountEvents.save(new AccountEvent(userId, "POLICY_UPDATED",
                effect + " " + request.permissionCode, CurrentUserHolder.id()));
        auditService.success("S01", "POLICY_UPDATE", "account", user.getUsername(),
                effect + " " + request.permissionCode);
        return new IdentityViews.PolicyView(policy);
    }

    @Transactional(readOnly = true)
    public List<IdentityViews.PolicyView> listPolicies(Long userId) {
        List<IdentityViews.PolicyView> result = new ArrayList<IdentityViews.PolicyView>();
        for (PermissionPolicy policy : policies.findByUserId(userId)) {
            result.add(new IdentityViews.PolicyView(policy));
        }
        return result;
    }

    @Transactional
    public void revokePolicy(Long policyId) {
        PermissionPolicy policy = policies.findById(policyId)
                .orElseThrow(() -> AppException.notFound("Policy " + policyId));
        policies.delete(policy);
        auditService.success("S01", "POLICY_REVOKE", "policy", String.valueOf(policyId),
                "policy removed");
    }

    // ------------------------------------------------------------------ audit

    @Transactional(readOnly = true)
    public List<IdentityViews.AccountEventView> accountEvents(int limit) {
        List<IdentityViews.AccountEventView> result = new ArrayList<IdentityViews.AccountEventView>();
        for (AccountEvent event : accountEvents.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))) {
            result.add(new IdentityViews.AccountEventView(event));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<IdentityViews.AccessAuditView> accessAudits(int limit) {
        List<IdentityViews.AccessAuditView> result = new ArrayList<IdentityViews.AccessAuditView>();
        for (AccessAudit audit : accessAudits.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))) {
            result.add(new IdentityViews.AccessAuditView(audit));
        }
        return result;
    }

    /** Authorization decision used by the API layer (US04). */
    @Transactional(readOnly = true)
    public boolean isAllowed(String permissionCode) {
        CurrentUser current = CurrentUserHolder.get();
        if (current == null) {
            return false;
        }
        if (current.isLead()) {
            return true;
        }
        return current.has(permissionCode);
    }

    public static String roleCodeOf(RoleType roleType) {
        if (roleType == null) {
            return "ROLE_TEAM_MEMBER";
        }
        switch (roleType) {
            case OVERALL_LEAD:
                return "ROLE_OVERALL_LEAD";
            case SENIOR_MANAGER:
                return "ROLE_SENIOR_MANAGER";
            case TEAM_MEMBER:
            default:
                return "ROLE_TEAM_MEMBER";
        }
    }

    private String randomColor() {
        List<String> palette = Arrays.asList("#4f7cff", "#22a06b", "#f2994a", "#9b51e0",
                "#eb5757", "#2d9cdb", "#f2c94c");
        return palette.get((int) (Math.random() * palette.size()));
    }
}
