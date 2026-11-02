package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - File Management Adapter: a stored project file (US04).
 *
 * <p>This is the shared data contract ({@code FileRecord} in the Level-4 model) used by
 * {@code FileUploadHandler}, {@code StorageAdapter}, {@code ShareLinkService} and
 * {@code FileManagementController}.</p>
 */
@Entity
@Table(name = "tb_toolbox_file")
public class ToolboxFile extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "folder", nullable = false, length = 120)
    private String folder = "/";

    @Column(name = "content_type", length = 96)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes = 0L;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "owner_id")
    private Long ownerId;

    /** UPLOADED / VALIDATED / REJECTED / ARCHIVED */
    @Column(name = "status", nullable = false, length = 24)
    private String status = "UPLOADED";

    @Column(name = "storage_backend", length = 64)
    private String storageBackend = "Local object storage (simulated)";

    @Column(name = "validation_error", length = 255)
    private String validationError;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "shared", nullable = false)
    private boolean shared = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
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

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStorageBackend() {
        return storageBackend;
    }

    public void setStorageBackend(String storageBackend) {
        this.storageBackend = storageBackend;
    }

    public String getValidationError() {
        return validationError;
    }

    public void setValidationError(String validationError) {
        this.validationError = validationError;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}
