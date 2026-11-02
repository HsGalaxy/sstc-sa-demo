package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S01 - role to permission binding persistence (US02).
 */
public interface RolePermissionsRepo extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);
}
