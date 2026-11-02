package com.sstc.platform.dashboard.web;

import com.sstc.platform.dashboard.domain.FileRecord;
import com.sstc.platform.dashboard.domain.FileVersion;
import com.sstc.platform.dashboard.domain.KeyResult;
import com.sstc.platform.dashboard.domain.Okr;
import com.sstc.platform.dashboard.domain.OperationLog;
import com.sstc.platform.dashboard.domain.PerformanceSnapshot;
import com.sstc.platform.dashboard.domain.Task;
import com.sstc.platform.dashboard.domain.TaskCycle;
import com.sstc.platform.dashboard.domain.TaskPriority;
import com.sstc.platform.dashboard.domain.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * S02 - API projections for the Task Dashboard.
 */
public final class DashboardViews {

    private DashboardViews() {
    }

    /** Task card of the Kanban board (US03 create/update, US05 display). */
    public static class TaskView {

        public Long id;

        public String code;

        public String title;

        public String description;

        public TaskStatus status;

        public String statusLabel;

        public TaskPriority priority;

        public Long assigneeId;

        public String assigneeName;

        public String assigneeInitials;

        public Long reporterId;

        public String reporterName;

        public Long cycleId;

        public String cycleName;

        public Long okrId;

        public String okrObjective;

        public LocalDate dueDate;

        public boolean overdue;

        public double loggedHours;

        public double estimateHours;

        public boolean blocked;

        public List<String> tags;

        public int positionIndex;

        public int attachmentCount;

        public LocalDateTime createdAt;

        public LocalDateTime updatedAt;

        public TaskView(Task task) {
            this.id = task.getId();
            this.code = task.getCode();
            this.title = task.getTitle();
            this.description = task.getDescription();
            this.status = task.getStatus();
            this.statusLabel = task.getStatus() == null ? null : task.getStatus().getLabel();
            this.priority = task.getPriority();
            this.assigneeId = task.getAssigneeId();
            this.reporterId = task.getReporterId();
            this.cycleId = task.getCycleId();
            this.okrId = task.getOkrId();
            this.dueDate = task.getDueDate();
            this.overdue = task.getDueDate() != null
                    && task.getDueDate().isBefore(LocalDate.now())
                    && task.getStatus() != TaskStatus.DONE;
            this.loggedHours = task.getLoggedHours();
            this.estimateHours = task.getEstimateHours();
            this.blocked = task.isBlocked();
            this.tags = task.getTags() == null || task.getTags().isEmpty()
                    ? java.util.Collections.<String>emptyList()
                    : java.util.Arrays.asList(task.getTags().split("\\s*,\\s*"));
            this.positionIndex = task.getPositionIndex();
            this.createdAt = task.getCreatedAt();
            this.updatedAt = task.getUpdatedAt();
        }
    }

    /** Kanban column aggregation used by the board (US05). */
    public static class BoardColumn {

        public TaskStatus status;

        public String label;

        public List<TaskView> tasks;

        public int count;

        public BoardColumn(TaskStatus status, List<TaskView> tasks) {
            this.status = status;
            this.label = status.getLabel();
            this.tasks = tasks;
            this.count = tasks.size();
        }
    }

    public static class CycleView {

        public Long id;

        public String name;

        public String goal;

        public LocalDate startDate;

        public LocalDate endDate;

        public boolean active;

        public int totalTasks;

        public int doneTasks;

        public int progress;

        public CycleView(TaskCycle cycle) {
            this.id = cycle.getId();
            this.name = cycle.getName();
            this.goal = cycle.getGoal();
            this.startDate = cycle.getStartDate();
            this.endDate = cycle.getEndDate();
            this.active = cycle.isActive();
        }
    }

    public static class OkrView {

        public Long id;

        public String objective;

        public Long cycleId;

        public Long ownerId;

        public String ownerName;

        public int progress;

        public String status;

        public double weight;

        public List<KeyResultView> keyResults;

        public OkrView(Okr okr) {
            this.id = okr.getId();
            this.objective = okr.getObjective();
            this.cycleId = okr.getCycleId();
            this.ownerId = okr.getOwnerId();
            this.progress = okr.getProgress();
            this.status = okr.getStatus() == null ? null : okr.getStatus().name();
            this.weight = okr.getWeight();
        }
    }

    public static class KeyResultView {

        public Long id;

        public Long okrId;

        public String title;

        public double targetValue;

        public double currentValue;

        public String unit;

        public Long ownerId;

        public int ratio;

        public KeyResultView(KeyResult keyResult) {
            this.id = keyResult.getId();
            this.okrId = keyResult.getOkrId();
            this.title = keyResult.getTitle();
            this.targetValue = keyResult.getTargetValue();
            this.currentValue = keyResult.getCurrentValue();
            this.unit = keyResult.getUnit();
            this.ownerId = keyResult.getOwnerId();
            this.ratio = keyResult.getRatio();
        }
    }

    /** Task file index entry (US01 query, US02 upload). */
    public static class FileRecordView {

        public Long id;

        public String name;

        public FileRecord.Bucket bucket;

        public Long taskId;

        public String taskTitle;

        public Long ownerId;

        public String ownerName;

        public String contentType;

        public long sizeBytes;

        public String checksum;

        public int currentVersion;

        public boolean archived;

        public LocalDateTime uploadedAt;

        public int versionCount;

        public FileRecordView(FileRecord record) {
            this.id = record.getId();
            this.name = record.getName();
            this.bucket = record.getBucket();
            this.taskId = record.getTaskId();
            this.ownerId = record.getOwnerId();
            this.contentType = record.getContentType();
            this.sizeBytes = record.getSizeBytes();
            this.checksum = record.getChecksum();
            this.currentVersion = record.getCurrentVersion();
            this.archived = record.isArchived();
            this.uploadedAt = record.getUploadedAt();
        }
    }

    public static class FileVersionView {

        public Long id;

        public Long fileId;

        public int version;

        public long sizeBytes;

        public String checksum;

        public String changeNote;

        public Long authorId;

        public String authorName;

        public LocalDateTime createdOn;

        public boolean current;

        public FileVersionView(FileVersion version) {
            this.id = version.getId();
            this.fileId = version.getFileId();
            this.version = version.getVersion();
            this.sizeBytes = version.getSizeBytes();
            this.checksum = version.getChecksum();
            this.changeNote = version.getChangeNote();
            this.authorId = version.getAuthorId();
            this.createdOn = version.getCreatedOn();
            this.current = version.isCurrent();
        }
    }

    /** One operation log entry (US04). */
    public static class OperationLogView {

        public Long id;

        public Long actorId;

        public String actorName;

        public String action;

        public String targetType;

        public String targetId;

        public String detail;

        public String outcome;

        public String source;

        public LocalDateTime createdAt;

        public OperationLogView(OperationLog log) {
            this.id = log.getId();
            this.actorId = log.getActorId();
            this.actorName = log.getActorName();
            this.action = log.getAction();
            this.targetType = log.getTargetType();
            this.targetId = log.getTargetId();
            this.detail = log.getDetail();
            this.outcome = log.getOutcome();
            this.source = log.getSource();
            this.createdAt = log.getCreatedAt();
        }
    }

    public static class SnapshotView {

        public Long id;

        public LocalDate snapshotDate;

        public Long userId;

        public int tasksCompleted;

        public int tasksOpen;

        public int overdueTasks;

        public double onTimeRate;

        public double qualityScore;

        public double okrScore;

        public double throughput;

        public SnapshotView(PerformanceSnapshot snapshot) {
            this.id = snapshot.getId();
            this.snapshotDate = snapshot.getSnapshotDate();
            this.userId = snapshot.getUserId();
            this.tasksCompleted = snapshot.getTasksCompleted();
            this.tasksOpen = snapshot.getTasksOpen();
            this.overdueTasks = snapshot.getOverdueTasks();
            this.onTimeRate = snapshot.getOnTimeRate();
            this.qualityScore = snapshot.getQualityScore();
            this.okrScore = snapshot.getOkrScore();
            this.throughput = snapshot.getThroughput();
        }
    }

    /** Aggregated indicators of the Display Panel (US05) and Performance Calculator (US06). */
    public static class PerformanceView {

        public List<SnapshotView> trend;

        public List<MemberScore> members;

        public double teamOkrScore;

        public double teamOnTimeRate;

        public int openTasks;

        public int overdueTasks;

        public int completedThisCycle;

        public double averageThroughput;

        public List<Bottleneck> bottlenecks;
    }

    public static class MemberScore {

        public Long userId;

        public String name;

        public String initials;

        public String roleLabel;

        public int completed;

        public int open;

        public int overdue;

        public double onTimeRate;

        public double qualityScore;

        public double okrScore;

        public double totalScore;

        public String grade;

        public MemberScore() {
        }
    }

    public static class Bottleneck {

        public String type;

        public String label;

        public String detail;

        public String severity;

        public Long taskId;
    }
}
