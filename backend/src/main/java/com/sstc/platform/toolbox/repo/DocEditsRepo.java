package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.DocEdit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - applied document edit accessor (US01).
 */
public interface DocEditsRepo extends JpaRepository<DocEdit, Long> {

    List<DocEdit> findByDocumentIdOrderByAppliedAtDesc(Long documentId);
}
