package com.sstc.platform.dashboard.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S02 - Task Dashboard: a measurable key result of an OKR (US05, US06).
 */
@Entity
@Table(name = "td_key_result")
public class KeyResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "okr_id", nullable = false)
    private Long okrId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "target_value", nullable = false)
    private double targetValue = 100d;

    @Column(name = "current_value", nullable = false)
    private double currentValue = 0d;

    @Column(name = "unit", length = 24)
    private String unit = "%";

    @Column(name = "owner_id")
    private Long ownerId;

    /** Completion ratio in the range 0..100, recomputed on every update. */
    public int getRatio() {
        if (targetValue <= 0d) {
            return 0;
        }
        double ratio = currentValue / targetValue * 100d;
        if (ratio < 0d) {
            ratio = 0d;
        }
        if (ratio > 100d) {
            ratio = 100d;
        }
        return (int) Math.round(ratio);
    }

    public Long getOkrId() {
        return okrId;
    }

    public void setOkrId(Long okrId) {
        this.okrId = okrId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
}
