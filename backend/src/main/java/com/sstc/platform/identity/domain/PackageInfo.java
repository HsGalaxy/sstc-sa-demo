package com.sstc.platform.identity.domain;

/**
 * S01 - the S01 domain model is split into focused files:
 *
 * <ul>
 *   <li>{@link User} - account aggregate (Account Service, US01)</li>
 *   <li>{@link Role}, {@link Permission}, {@link RolePermission}, {@link UserRole} -
 *       Role &amp; Permission API (US02)</li>
 *   <li>{@link PermissionPolicy} - fine grained policy overrides (US03)</li>
 *   <li>{@link AccountEvent} - account lifecycle events (US01)</li>
 *   <li>{@link AccessAudit}, {@link SessionToken} - Identity API + Authorization Service (US04)</li>
 * </ul>
 *
 * <p>This file is intentionally kept as a package marker so the mapping between the
 * Level-3 components and the persistence model stays readable.</p>
 */
final class PackageInfo {

    private PackageInfo() {
    }
}
