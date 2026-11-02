package com.sstc.platform.dashboard.web;

import com.sstc.platform.dashboard.domain.FileRecord;
import com.sstc.platform.dashboard.domain.TaskPriority;
import com.sstc.platform.dashboard.domain.TaskStatus;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * S02 - request payloads for the Task Dashboard.
 */
public final class DashboardRequests {

    private DashboardRequests() {
    }

    public static class TaskRequest {

        @NotBlank
        public String title;

        public String description;

        public TaskStatus status = TaskStatus.TODO;

        public TaskPriority priority = TaskPriority.MEDIUM;

        public Long assigneeId;

        public Long cycleId;

        public Long okrId;

        public LocalDate dueDate;

        public Double estimateHours;

        public String tags;

        public Boolean blocked;
    }

    public static class TaskUpdateRequest {

        public String title;

        public String description;

        public TaskStatus status;

        public TaskPriority priority;

        public Long assigneeId;

        public Long cycleId;

        public Long okrId;

        public LocalDate dueDate;

        public Double estimateHours;

        public Double loggedHours;

        public String tags;

        public Boolean blocked;

        public Integer positionIndex;
    }

    public static class CycleRequest {

        @NotBlank
        public String name;

        public String goal;

        public LocalDate startDate;

        public LocalDate endDate;

        public Boolean active;
    }

    public static class OkrRequest {

        @NotBlank
        public String objective;

        public Long cycleId;

        public Long ownerId;

        public Double weight;

        public String status;
    }

    public static class KeyResultRequest {

        @NotBlank
        public String title;

        public double targetValue = 100d;

        public double currentValue = 0d;

        public String unit = "%";

        public Long ownerId;
    }

    public static class FileQueryRequest {

        public FileRecord.Bucket bucket;

        public Long taskId;

        public Long ownerId;

        public Boolean archived;

        public String keyword;
    }

    public static class UploadFileRequest {

        @NotBlank
        public String name;

        public Long taskId;

        public FileRecord.Bucket bucket = FileRecord.Bucket.OTHER;

        public String contentType;

        public Long sizeBytes;

        public String changeNote;
    }

    public static class VersionRequest {

        @NotBlank
        public String changeNote;

        public Long sizeBytes;
    }
}
