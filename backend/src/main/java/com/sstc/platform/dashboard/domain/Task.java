package com.sstc.platform.dashboard.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.LocalDate;

/**
 * S02 - Task Dashboard: one Kanban task item.
 *
 * <p>Functionality mapping: {@code TaskDashboard.ModificationScheduler} (US03) creates and
 * updates tasks, {@code TaskDashboard.DisplayPanel} (US05) reads them for the board.</p>
 */
@Entity
@Table(name = "td_task")
public class Task extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "code", nullable = false, unique = true, length = 24)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "cycle_id")
    private Long cycleId;

    @Column(name = "okr_id")
    private Long okrId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Logged effort in hours, tracked inside the task (US05 process indicator). */
    @Column(name = "logged_hours", nullable = false)
    private double loggedHours = 0d;

    /** Effort estimate in hours, used by the performance calculator (US06). */
    @Column(name = "estimate_hours", nullable = false)
    private double estimateHours = 0d;

    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "position_index", nullable = false)
    private int positionIndex = 0;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public Long getCycleId() {
        return cycleId;
    }

    public void setCycleId(Long cycleId) {
        this.cycleId = cycleId;
    }

    public Long getOkrId() {
        return okrId;
    }

    public void setOkrId(Long okrId) {
        this.okrId = okrId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public double getLoggedHours() {
        return loggedHours;
    }

    public void setLoggedHours(double loggedHours) {
        this.loggedHours = loggedHours;
    }

    public double getEstimateHours() {
        return estimateHours;
    }

    public void setEstimateHours(double estimateHours) {
        this.estimateHours = estimateHours;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public void setPositionIndex(int positionIndex) {
        this.positionIndex = positionIndex;
    }
}
