package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.GitRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - Git repository accessor (US03).
 */
public interface GitReposRepo extends JpaRepository<GitRepo, Long> {

    List<GitRepo> findAllByOrderByLastSyncAtDesc();
}
