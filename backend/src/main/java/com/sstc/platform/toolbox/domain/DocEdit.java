package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - Office Tools Container: one applied edit operation of a document (US01).
 *
 * <p>The record is the audit trail behind the {@code UpdateDocument} contract of
 * {@code WordDocumentBuilder}.</p>
 */
@Entity
@Table(name = "tb_doc_edit")
public class DocEdit extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "editor_id", nullable = false)
    private Long editorId;

    /** APPEND / REPLACE / RENAME / CHECK_QUOTA */
    @Column(name = "operation", nullable = false, length = 32)
    private String operation;

    @Column(name = "payload", length = 2000)
    private String payload;

    @Column(name = "result_code", nullable = false)
    private int resultCode = 0;

    @Column(name = "quota_remaining", nullable = false)
    private int quotaRemaining = 0;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getEditorId() {
        return editorId;
    }

    public void setEditorId(Long editorId) {
        this.editorId = editorId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public int getQuotaRemaining() {
        return quotaRemaining;
    }

    public void setQuotaRemaining(int quotaRemaining) {
        this.quotaRemaining = quotaRemaining;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
