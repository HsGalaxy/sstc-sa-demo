package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.Slide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - slide accessor (US02).
 */
public interface SlidesRepo extends JpaRepository<Slide, Long> {

    List<Slide> findByPresentationIdOrderBySlideNoAsc(Long presentationId);
}
