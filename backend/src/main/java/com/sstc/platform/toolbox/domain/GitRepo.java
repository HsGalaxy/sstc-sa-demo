package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - Development Tools Container: a Git repository known to the platform (US03).
 *
 * <p>Functionality mapping: {@code Toolbox.VersionControl}.</p>
 */
@Entity
@Table(name = "tb_git_repo")
public class GitRepo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 160)
    private String slug;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider = "GitHub (simulated)";

    @Column(name = "remote_url", length = 255)
    private String remoteUrl;

    @Column(name = "default_branch", nullable = false, length = 64)
    private String defaultBranch = "main";

    @Column(name = "language", length = 48)
    private String language;

    @Column(name = "visibility", nullable = false, length = 16)
    private String visibility = "PRIVATE";

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "stars", nullable = false)
    private int stars = 0;

    @Column(name = "open_pull_requests", nullable = false)
    private int openPullRequests = 0;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getOpenPullRequests() {
        return openPullRequests;
    }

    public void setOpenPullRequests(int openPullRequests) {
        this.openPullRequests = openPullRequests;
    }

    public LocalDateTime getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(LocalDateTime lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
}
