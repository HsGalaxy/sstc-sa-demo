package com.sstc.platform.identity.web;

import com.sstc.platform.identity.domain.AccountStatus;
import com.sstc.platform.identity.domain.RoleType;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * S01 - request payloads. Plain public fields keep the JSON contract obvious; Bean
 * Validation guards the invariants of the Account Service.
 */
public final class IdentityRequests {

    private IdentityRequests() {
    }

    public static class LoginRequest {

        @NotBlank
        public String username;

        @NotBlank
        public String password;
    }

    public static class CreateUserRequest {

        @NotBlank
        @Size(min = 3, max = 32)
        public String username;

        @NotBlank
        public String displayName;

        @NotBlank
        @Email
        public String email;

        @NotBlank
        @Size(min = 6, max = 64)
        public String password;

        public String jobTitle;

        public RoleType role = RoleType.TEAM_MEMBER;

        public String avatarColor;
    }

    public static class UpdateUserRequest {

        public String displayName;

        @Email
        public String email;

        public String jobTitle;

        public RoleType role;

        public AccountStatus status;

        public String avatarColor;
    }

    public static class ChangePasswordRequest {

        @NotBlank
        public String currentPassword;

        @NotBlank
        @Size(min = 6, max = 64)
        public String newPassword;
    }

    public static class AssignRoleRequest {

        @NotBlank
        public String roleCode;

        public boolean adminPrivilege;
    }

    public static class PolicyRequest {

        @NotBlank
        public String permissionCode;

        /** GRANT or REVOKE */
        @NotBlank
        public String effect;

        public String reason;
    }
}
