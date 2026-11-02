package com.sstc.platform.dashboard.repo;

import com.sstc.platform.dashboard.domain.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S02 - historical file version accessor (US01).
 */
public interface FileVersionsRepo extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findByFileIdOrderByVersionDesc(Long fileId);

    List<FileVersion> findByFileIdAndCurrentTrue(Long fileId);
}
