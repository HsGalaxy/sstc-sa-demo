package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.DocDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - online Word document accessor (US01).
 */
public interface DocumentsRepo extends JpaRepository<DocDocument, Long> {

    List<DocDocument> findAllByOrderByLastEditedAtDesc();
}
