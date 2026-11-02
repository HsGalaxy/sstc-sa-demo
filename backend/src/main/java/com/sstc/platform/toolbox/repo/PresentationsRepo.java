package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - slide deck accessor (US02).
 */
public interface PresentationsRepo extends JpaRepository<Presentation, Long> {

    List<Presentation> findAllByOrderByLastEditedAtDesc();
}
