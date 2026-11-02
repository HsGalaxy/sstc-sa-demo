package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - Development Tools Container: one commit of the version history (US03).
 */
@Entity
@Table(name = "tb_commit_record")
public class CommitRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "repo_id", nullable = false)
    private Long repoId;

    @Column(name = "sha", nullable = false, length = 40)
    private String sha;

    @Column(name = "branch", nullable = false, length = 64)
    private String branch = "main";

    @Column(name = "message", nullable = false, length = 512)
    private String message;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "author_name", length = 96)
    private String authorName;

    @Column(name = "committed_at", nullable = false)
    private LocalDateTime committedAt;

    @Column(name = "additions", nullable = false)
    private int additions = 0;

    @Column(name = "deletions", nullable = false)
    private int deletions = 0;

    @Column(name = "changed_files", nullable = false)
    private int changedFiles = 0;

    public Long getRepoId() {
        return repoId;
    }

    public void setRepoId(Long repoId) {
        this.repoId = repoId;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public LocalDateTime getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(LocalDateTime committedAt) {
        this.committedAt = committedAt;
    }

    public int getAdditions() {
        return additions;
    }

    public void setAdditions(int additions) {
        this.additions = additions;
    }

    public int getDeletions() {
        return deletions;
    }

    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }

    public int getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(int changedFiles) {
        this.changedFiles = changedFiles;
    }
}
