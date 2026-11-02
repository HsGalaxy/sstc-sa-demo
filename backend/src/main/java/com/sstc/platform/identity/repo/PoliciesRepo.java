package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.PermissionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S01 - per-account permission policy persistence (US03).
 */
public interface PoliciesRepo extends JpaRepository<PermissionPolicy, Long> {

    List<PermissionPolicy> findByUserId(Long userId);

    List<PermissionPolicy> findByUserIdAndPermissionCode(Long userId, String permissionCode);

    void deleteByUserId(Long userId);
}
