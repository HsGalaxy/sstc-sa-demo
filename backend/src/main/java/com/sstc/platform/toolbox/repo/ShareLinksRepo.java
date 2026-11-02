package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - sharing link accessor (US04).
 */
public interface ShareLinksRepo extends JpaRepository<ShareLink, Long> {

    List<ShareLink> findByFileIdOrderByCreatedAtDesc(Long fileId);

    ShareLink findByToken(String token);
}
