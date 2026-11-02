package com.sstc.platform.dashboard.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * S02 - Task Dashboard: one OKR objective of the team (US05, US06).
 */
@Entity
@Table(name = "td_okr")
public class Okr extends BaseEntity {

    private static final long serialVersionUID = 1L;

    public enum Status {
        ON_TRACK,
        AT_RISK,
        OFF_TRACK,
        ACHIEVED
    }

    @Column(name = "objective", nullable = false, length = 255)
    private String objective;

    @Column(name = "cycle_id")
    private Long cycleId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "progress", nullable = false)
    private int progress = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private Status status = Status.ON_TRACK;

    @Column(name = "weight", nullable = false)
    private double weight = 1.0d;

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public Long getCycleId() {
        return cycleId;
    }

    public void setCycleId(Long cycleId) {
        this.cycleId = cycleId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
