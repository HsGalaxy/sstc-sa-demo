package com.sstc.platform.dashboard.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S02 - File Tracker: an immutable historical version of a task file
 * (US01: access historical file versions and roll back).
 *
 * <p>Functionality mapping: {@code TaskDashboard.HistoricalVersionController}.</p>
 */
@Entity
@Table(name = "td_file_version")
public class FileVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes = 0L;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "change_note", length = 512)
    private String changeNote;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "current", nullable = false)
    private boolean current = false;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
