package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - Development Tools Container: a containerised development environment (US05).
 *
 * <p>Functionality mapping: {@code Toolbox.ContainerRuntime}.</p>
 */
@Entity
@Table(name = "tb_dev_environment")
public class DevEnvironment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "service_id", nullable = false, length = 64)
    private String serviceId;

    @Column(name = "container_id", nullable = false, unique = true, length = 64)
    private String containerId;

    @Column(name = "image", nullable = false, length = 120)
    private String image;

    /** RUNNING / STOPPED / FAILED / STARTING */
    @Column(name = "state", nullable = false, length = 24)
    private String state = "STOPPED";

    @Column(name = "cpu_limit", nullable = false)
    private double cpuLimit = 1.0d;

    @Column(name = "memory_mb", nullable = false)
    private int memoryMb = 1024;

    @Column(name = "ports", length = 120)
    private String ports;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public double getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(double cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public int getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(int memoryMb) {
        this.memoryMb = memoryMb;
    }

    public String getPorts() {
        return ports;
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
