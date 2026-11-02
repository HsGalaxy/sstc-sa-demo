package com.sstc.platform.identity.repo;

import com.sstc.platform.identity.domain.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * S01 - session persistence of the Identity API (US04).
 */
public interface SessionTokensRepo extends JpaRepository<SessionToken, Long> {

    Optional<SessionToken> findByToken(String token);

    List<SessionToken> findByUserIdAndRevokedFalse(Long userId);
}
