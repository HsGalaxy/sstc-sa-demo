package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - File Management Adapter: a sharing link generated for a project file (US04).
 *
 * <p>Functionality mapping: {@code ShareLinkService.CreateShareLink(file, expiry)}.</p>
 */
@Entity
@Table(name = "tb_share_link")
public class ShareLink extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "access_count", nullable = false)
    private int accessCount = 0;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }
}
