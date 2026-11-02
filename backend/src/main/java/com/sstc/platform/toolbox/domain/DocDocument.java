package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - Office Tools Container: an online Word-style document (US01).
 *
 * <p>Functionality mapping: {@code Toolbox.DocumentEditing}.</p>
 */
@Entity
@Table(name = "tb_doc_document")
public class DocDocument extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", length = 8000)
    private String content;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "template", length = 64)
    private String template = "blank";

    @Column(name = "revision", nullable = false)
    private int revision = 1;

    @Column(name = "word_count", nullable = false)
    private int wordCount = 0;

    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;

    @Column(name = "office_api", length = 64)
    private String officeApi = "Microsoft 365 (simulated)";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public LocalDateTime getLastEditedAt() {
        return lastEditedAt;
    }

    public void setLastEditedAt(LocalDateTime lastEditedAt) {
        this.lastEditedAt = lastEditedAt;
    }

    public String getOfficeApi() {
        return officeApi;
    }

    public void setOfficeApi(String officeApi) {
        this.officeApi = officeApi;
    }
}
