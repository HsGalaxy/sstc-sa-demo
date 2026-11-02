package com.sstc.platform.dashboard.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

/**
 * S02 - Task Dashboard: a daily snapshot of process and outcome indicators rendered by the
 * Display Panel (US05) and consumed by the Performance Calculator (US06).
 */
@Entity
@Table(name = "td_performance_snapshot")
public class PerformanceSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tasks_completed", nullable = false)
    private int tasksCompleted = 0;

    @Column(name = "tasks_open", nullable = false)
    private int tasksOpen = 0;

    @Column(name = "overdue_tasks", nullable = false)
    private int overdueTasks = 0;

    @Column(name = "on_time_rate", nullable = false)
    private double onTimeRate = 0d;

    @Column(name = "quality_score", nullable = false)
    private double qualityScore = 0d;

    @Column(name = "okr_score", nullable = false)
    private double okrScore = 0d;

    @Column(name = "throughput", nullable = false)
    private double throughput = 0d;

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(LocalDate snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(int tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public int getTasksOpen() {
        return tasksOpen;
    }

    public void setTasksOpen(int tasksOpen) {
        this.tasksOpen = tasksOpen;
    }

    public int getOverdueTasks() {
        return overdueTasks;
    }

    public void setOverdueTasks(int overdueTasks) {
        this.overdueTasks = overdueTasks;
    }

    public double getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(double onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public double getOkrScore() {
        return okrScore;
    }

    public void setOkrScore(double okrScore) {
        this.okrScore = okrScore;
    }

    public double getThroughput() {
        return throughput;
    }

    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }
}
