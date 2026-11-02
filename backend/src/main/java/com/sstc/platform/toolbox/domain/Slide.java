package com.sstc.platform.toolbox.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S04 - Office Tools Container: one slide of a presentation (US02).
 */
@Entity
@Table(name = "tb_slide")
public class Slide extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "presentation_id", nullable = false)
    private Long presentationId;

    @Column(name = "slide_no", nullable = false)
    private int slideNo;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "bullets", length = 2000)
    private String bullets;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "layout", length = 48)
    private String layout = "title-content";

    public Long getPresentationId() {
        return presentationId;
    }

    public void setPresentationId(Long presentationId) {
        this.presentationId = presentationId;
    }

    public int getSlideNo() {
        return slideNo;
    }

    public void setSlideNo(int slideNo) {
        this.slideNo = slideNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBullets() {
        return bullets;
    }

    public void setBullets(String bullets) {
        this.bullets = bullets;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }
}
