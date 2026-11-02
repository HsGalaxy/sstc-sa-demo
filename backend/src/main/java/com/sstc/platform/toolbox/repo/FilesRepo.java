package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.ToolboxFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - stored project file accessor (US04).
 */
public interface FilesRepo extends JpaRepository<ToolboxFile, Long> {

    List<ToolboxFile> findAllByOrderByUploadedAtDesc();

    List<ToolboxFile> findBySharedTrueOrderByUploadedAtDesc();
}
