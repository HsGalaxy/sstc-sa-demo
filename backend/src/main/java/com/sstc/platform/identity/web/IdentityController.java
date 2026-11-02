package com.sstc.platform.identity.web;

import com.sstc.platform.common.security.CurrentUser;
import com.sstc.platform.identity.service.IdentityService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S01 - REST surface of the Identity, Role &amp; Access Management sub-system.
 *
 * <p>Endpoint to user story mapping is documented in {@code docs/API.md}.</p>
 */
@RestController
@RequestMapping("/api")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    // ------------------------------------------------------------------ US04

    @PostMapping("/auth/login")
    public IdentityViews.SessionView login(@Valid @RequestBody IdentityRequests.LoginRequest request) {
        return identityService.login(request);
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization == null ? null
                : authorization.replaceFirst("(?i)^Bearer\\s+", "").trim();
        identityService.logout(token);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("status", "signed-out");
        return map;
    }

    @GetMapping("/identity/me")
    public IdentityViews.UserView me() {
        return identityService.me();
    }

    @GetMapping("/identity/permissions")
    public Map<String, Object> myPermissions(CurrentUser currentUser) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("username", currentUser == null ? null : currentUser.getUsername());
        map.put("role", currentUser == null ? null : currentUser.getRole());
        map.put("permissions", currentUser == null ? java.util.Collections.emptySet()
                : currentUser.getPermissions());
        return map;
    }

    @GetMapping("/identity/check")
    public Map<String, Object> check(@RequestParam String permission) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("permission", permission);
        map.put("allowed", identityService.isAllowed(permission));
        return map;
    }

    // ------------------------------------------------------------------ US01

    @GetMapping("/users")
    public List<IdentityViews.UserView> users() {
        return identityService.listUsers();
    }

    @GetMapping("/users/{id}")
    public IdentityViews.UserView user(@PathVariable Long id) {
        return identityService.getUser(id);
    }

    @PostMapping("/users")
    public IdentityViews.UserView createUser(@Valid @RequestBody IdentityRequests.CreateUserRequest request) {
        return identityService.createUser(request);
    }

    @PutMapping("/users/{id}")
    public IdentityViews.UserView updateUser(@PathVariable Long id,
                                             @RequestBody IdentityRequests.UpdateUserRequest request) {
        return identityService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public IdentityViews.UserView deactivateUser(@PathVariable Long id) {
        return identityService.deactivateUser(id);
    }

    @PostMapping("/users/{id}/password")
    public Map<String, Object> changePassword(@PathVariable Long id,
                                              @Valid @RequestBody IdentityRequests.ChangePasswordRequest request) {
        identityService.changePassword(id, request);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("status", "password-updated");
        return map;
    }

    // ------------------------------------------------------------------ US02

    @GetMapping("/roles")
    public List<IdentityViews.RoleView> roles() {
        return identityService.listRoles();
    }

    @PostMapping("/users/{id}/role")
    public IdentityViews.UserView assignRole(@PathVariable Long id,
                                             @Valid @RequestBody IdentityRequests.AssignRoleRequest request) {
        return identityService.assignRole(id, request);
    }

    @DeleteMapping("/users/{id}/role")
    public IdentityViews.UserView revokeRole(@PathVariable Long id) {
        return identityService.revokeRole(id);
    }

    @GetMapping("/permissions")
    public List<IdentityViews.PermissionView> permissions() {
        return identityService.listPermissions();
    }

    // ------------------------------------------------------------------ US03

    @GetMapping("/users/{id}/policies")
    public List<IdentityViews.PolicyView> policies(@PathVariable Long id) {
        return identityService.listPolicies(id);
    }

    @PostMapping("/users/{id}/policies")
    public IdentityViews.PolicyView setPolicy(@PathVariable Long id,
                                              @Valid @RequestBody IdentityRequests.PolicyRequest request) {
        return identityService.setPolicy(id, request);
    }

    @DeleteMapping("/policies/{policyId}")
    public Map<String, Object> revokePolicy(@PathVariable Long policyId) {
        identityService.revokePolicy(policyId);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("status", "policy-revoked");
        return map;
    }

    // ------------------------------------------------------------------ audit

    @GetMapping("/account-events")
    public List<IdentityViews.AccountEventView> accountEvents(
            @RequestParam(defaultValue = "30") int limit) {
        return identityService.accountEvents(limit);
    }

    @GetMapping("/access-audits")
    public List<IdentityViews.AccessAuditView> accessAudits(
            @RequestParam(defaultValue = "50") int limit) {
        return identityService.accessAudits(limit);
    }
}
