package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * S01 - permission catalogue persistence (US02 / US03).
 */
public interface PermissionsRepo extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByOrderByModuleAscCodeAsc();

    List<Permission> findByModule(String module);
}
