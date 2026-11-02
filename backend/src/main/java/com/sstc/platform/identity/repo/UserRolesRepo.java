package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S01 - account to role assignment persistence (US02).
 */
public interface UserRolesRepo extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
