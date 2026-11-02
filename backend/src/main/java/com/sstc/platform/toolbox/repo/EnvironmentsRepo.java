package com.sstc.platform.toolbox.repo;

import com.sstc.platform.toolbox.domain.DevEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * S04 - container environment accessor (US05).
 */
public interface EnvironmentsRepo extends JpaRepository<DevEnvironment, Long> {

    List<DevEnvironment> findAllByOrderByCreatedAtDesc();

    DevEnvironment findByServiceId(String serviceId);

    DevEnvironment findByContainerId(String containerId);

    long countByState(String state);
}
