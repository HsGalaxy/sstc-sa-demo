package com.sstc.platform.dashboard.service;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.web.AppException;
import com.sstc.platform.dashboard.domain.FileRecord;
import com.sstc.platform.dashboard.domain.FileVersion;
import com.sstc.platform.dashboard.domain.Task;
import com.sstc.platform.dashboard.repo.*;
import com.sstc.platform.dashboard.web.DashboardRequests;
import com.sstc.platform.dashboard.web.DashboardViews;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.service.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * S02 - File Tracker.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>{@code TaskDashboard.FileQueryController} - multi condition file lookup (US01)</li>
 *   <li>{@code TaskDashboard.FileBucket} - classified storage of task attachments (US02)</li>
 *   <li>{@code TaskDashboard.HistoricalVersionController} - version history and rollback (US01)</li>
 * </ul>
 */
@Service
public class FileTrackerService {

    private final FileRecordsRepo fileRecords;

    private final FileVersionsRepo fileVersions;

    private final TasksRepo tasks;

    private final UserDirectory directory;

    private final AuditService auditService;

    private final TaskDashboardService dashboardService;

    public FileTrackerService(FileRecordsRepo fileRecords,
                              FileVersionsRepo fileVersions,
                              TasksRepo tasks,
                              UserDirectory directory,
                              AuditService auditService,
                              TaskDashboardService dashboardService) {
        this.fileRecords = fileRecords;
        this.fileVersions = fileVersions;
        this.tasks = tasks;
        this.directory = directory;
        this.auditService = auditService;
        this.dashboardService = dashboardService;
    }

    /** US01 - multi condition query across the file buckets. */
    @Transactional(readOnly = true)
    public List<DashboardViews.FileRecordView> query(FileRecord.Bucket bucket, Long taskId, Long ownerId,
                                                     Boolean archived, String keyword) {
        Map<Long, User> users = directory.index();
        Map<Long, String> taskTitles = new java.util.HashMap<Long, String>();
        for (Task task : tasks.findAll()) {
            taskTitles.put(task.getId(), task.getTitle());
        }
        List<DashboardViews.FileRecordView> result = new ArrayList<DashboardViews.FileRecordView>();
        for (FileRecord record : fileRecords.search(bucket, taskId, ownerId, archived, keyword)) {
            DashboardViews.FileRecordView view = new DashboardViews.FileRecordView(record);
            User owner = record.getOwnerId() == null ? null : users.get(record.getOwnerId());
            view.ownerName = owner == null ? null : owner.getDisplayName();
            view.taskTitle = record.getTaskId() == null ? null : taskTitles.get(record.getTaskId());
            view.versionCount = fileVersions.findByFileIdOrderByVersionDesc(record.getId()).size();
            result.add(view);
        }
        auditService.success("S02", "FILE_QUERY", "file", "query",
                "bucket=" + bucket + " task=" + taskId + " keyword=" + keyword);
        return result;
    }

    /** US01 - one index entry with its full version history. */
    @Transactional(readOnly = true)
    public DashboardViews.FileRecordView getFile(Long id) {
        FileRecord record = require(id);
        Map<Long, User> users = directory.index();
        DashboardViews.FileRecordView view = new DashboardViews.FileRecordView(record);
        User owner = record.getOwnerId() == null ? null : users.get(record.getOwnerId());
        view.ownerName = owner == null ? null : owner.getDisplayName();
        view.versionCount = fileVersions.findByFileIdOrderByVersionDesc(id).size();
        return view;
    }

    @Transactional(readOnly = true)
    public List<DashboardViews.FileVersionView> versions(Long fileId) {
        require(fileId);
        Map<Long, User> users = directory.index();
        List<DashboardViews.FileVersionView> result = new ArrayList<DashboardViews.FileVersionView>();
        for (FileVersion version : fileVersions.findByFileIdOrderByVersionDesc(fileId)) {
            DashboardViews.FileVersionView view = new DashboardViews.FileVersionView(version);
            User author = version.getAuthorId() == null ? null : users.get(version.getAuthorId());
            view.authorName = author == null ? null : author.getDisplayName();
            result.add(view);
        }
        return result;
    }

    /** US02 - register an uploaded attachment and create its first version. */
    @Transactional
    public DashboardViews.FileRecordView upload(DashboardRequests.UploadFileRequest request) {
        FileRecord record = new FileRecord();
        record.setName(request.name.trim());
        record.setBucket(request.bucket == null ? FileRecord.Bucket.OTHER : request.bucket);
        record.setTaskId(request.taskId);
        record.setOwnerId(CurrentUserHolder.id());
        record.setContentType(request.contentType);
        record.setSizeBytes(request.sizeBytes == null ? 0L : request.sizeBytes);
        record.setChecksum(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        record.setCurrentVersion(1);
        record.setUploadedAt(LocalDateTime.now());
        fileRecords.save(record);

        FileVersion version = new FileVersion();
        version.setFileId(record.getId());
        version.setVersion(1);
        version.setSizeBytes(record.getSizeBytes());
        version.setChecksum(record.getChecksum());
        version.setChangeNote(request.changeNote == null ? "Initial upload" : request.changeNote);
        version.setAuthorId(CurrentUserHolder.id());
        version.setCreatedOn(LocalDateTime.now());
        version.setCurrent(true);
        fileVersions.save(version);

        dashboardService.log("FILE_UPLOAD", "file", String.valueOf(record.getId()),
                record.getName() + " (" + record.getSizeBytes() + " bytes)", "SUCCESS");
        auditService.success("S02", "FILE_UPLOAD", "file", record.getName(), "v1 stored");
        return getFile(record.getId());
    }

    /** US01 - add a new version on top of the existing history. */
    @Transactional
    public DashboardViews.FileVersionView addVersion(Long fileId, DashboardRequests.VersionRequest request) {
        FileRecord record = require(fileId);
        List<FileVersion> history = fileVersions.findByFileIdOrderByVersionDesc(fileId);
        for (FileVersion existing : history) {
            existing.setCurrent(false);
            fileVersions.save(existing);
        }
        int next = history.isEmpty() ? 1 : history.get(0).getVersion() + 1;
        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersion(next);
        version.setSizeBytes(request.sizeBytes == null ? record.getSizeBytes() : request.sizeBytes);
        version.setChecksum(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        version.setChangeNote(request.changeNote);
        version.setAuthorId(CurrentUserHolder.id());
        version.setCreatedOn(LocalDateTime.now());
        version.setCurrent(true);
        fileVersions.save(version);

        record.setCurrentVersion(next);
        record.setSizeBytes(version.getSizeBytes());
        record.setChecksum(version.getChecksum());
        fileRecords.save(record);

        dashboardService.log("FILE_VERSION", "file", String.valueOf(fileId),
                "v" + next + " " + request.changeNote, "SUCCESS");
        auditService.success("S02", "FILE_VERSION", "file", record.getName(), "v" + next);
        return new DashboardViews.FileVersionView(version);
    }

    /** US01 - roll the index entry back to an older version. */
    @Transactional
    public DashboardViews.FileRecordView rollback(Long fileId, int targetVersion) {
        FileRecord record = require(fileId);
        FileVersion target = null;
        for (FileVersion version : fileVersions.findByFileIdOrderByVersionDesc(fileId)) {
            if (version.getVersion() == targetVersion) {
                target = version;
            }
        }
        if (target == null) {
            throw AppException.notFound("Version " + targetVersion + " of file " + fileId);
        }
        for (FileVersion version : fileVersions.findByFileIdOrderByVersionDesc(fileId)) {
            boolean isTarget = version.getVersion() == targetVersion;
            version.setCurrent(isTarget);
            fileVersions.save(version);
        }
        record.setCurrentVersion(targetVersion);
        record.setSizeBytes(target.getSizeBytes());
        record.setChecksum(target.getChecksum());
        fileRecords.save(record);

        dashboardService.log("FILE_ROLLBACK", "file", String.valueOf(fileId),
                "restored v" + targetVersion, "SUCCESS");
        auditService.success("S02", "FILE_ROLLBACK", "file", record.getName(), "restored v" + targetVersion);
        return getFile(fileId);
    }

    @Transactional
    public DashboardViews.FileRecordView archive(Long fileId, boolean archived) {
        FileRecord record = require(fileId);
        record.setArchived(archived);
        fileRecords.save(record);
        dashboardService.log("FILE_ARCHIVE", "file", String.valueOf(fileId),
                archived ? "archived" : "restored", "SUCCESS");
        auditService.success("S02", "FILE_ARCHIVE", "file", record.getName(), String.valueOf(archived));
        return getFile(fileId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> bucketStatistics() {
        Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        long total = 0L;
        for (FileRecord.Bucket bucket : FileRecord.Bucket.values()) {
            long count = 0L;
            long bytes = 0L;
            for (FileRecord record : fileRecords.findAll()) {
                if (record.getBucket() == bucket) {
                    count++;
                    bytes += record.getSizeBytes();
                }
            }
            Map<String, Object> entry = new java.util.LinkedHashMap<String, Object>();
            entry.put("count", count);
            entry.put("bytes", bytes);
            map.put(bucket.name(), entry);
            total += count;
        }
        map.put("totalFiles", total);
        return map;
    }

    private FileRecord require(Long id) {
        return fileRecords.findById(id).orElseThrow(() -> AppException.notFound("File " + id));
    }
}
