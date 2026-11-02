package com.sstc.platform.dashboard.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S02 - File Tracker: a task attachment index entry.
 *
 * <p>Functionality mapping: {@code TaskDashboard.FileQueryController} (US01) queries these
 * records, {@code TaskDashboard.FileBucket} (US02) stores them.</p>
 */
@Entity
@Table(name = "td_file_record")
public class FileRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** Classification buckets of the File Category Bucket component. */
    public enum Bucket {
        DESIGN,
        DEVELOPMENT,
        DOCUMENT,
        REPORT,
        OTHER
    }

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "bucket", nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    private Bucket bucket = Bucket.OTHER;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "content_type", length = 96)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes = 0L;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "current_version", nullable = false)
    private int currentVersion = 1;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(int currentVersion) {
        this.currentVersion = currentVersion;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
