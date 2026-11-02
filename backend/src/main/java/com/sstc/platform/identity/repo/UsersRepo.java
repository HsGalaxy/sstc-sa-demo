package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.AccountStatus;
import com.sstc.platform.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * S01 - Account Service persistence: the account aggregate of the platform.
 */
public interface UsersRepo extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtAsc();

    List<User> findByStatusOrderByDisplayNameAsc(AccountStatus status);

    long countByStatus(AccountStatus status);
}
