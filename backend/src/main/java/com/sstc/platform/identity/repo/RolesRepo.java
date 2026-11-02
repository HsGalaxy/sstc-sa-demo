package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * S01 - Role &amp; Permission API persistence (US02).
 */
public interface RolesRepo extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    List<Role> findAllByOrderByRoleTypeAsc();
}
