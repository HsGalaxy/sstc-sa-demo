package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * S04 - Office Tools Container: an online slide deck (US02).
 *
 * <p>Functionality mapping: {@code Toolbox.PresentationBuilding}.</p>
 */
@Entity
@Table(name = "tb_presentation")
public class Presentation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "theme", length = 64)
    private String theme = "stc-light";

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "slide_count", nullable = false)
    private int slideCount = 0;

    @Column(name = "revision", nullable = false)
    private int revision = 1;

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

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public int getSlideCount() {
        return slideCount;
    }

    public void setSlideCount(int slideCount) {
        this.slideCount = slideCount;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
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
