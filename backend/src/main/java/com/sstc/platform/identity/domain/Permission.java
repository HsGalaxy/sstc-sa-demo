package com.sstc.platform.identity.domain;

import com.sstc.platform.common.domain.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * S01 - Role &amp; Permission API: one fine grained permission that can be granted to a role
 * or to a single account (US02, US03).
 */
@Entity
@Table(name = "usr_permission")
public class Permission extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "code", nullable = false, unique = true, length = 96)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Logical module the permission belongs to: S01..S04. */
    @Column(name = "module", nullable = false, length = 16)
    private String module;

    @Column(name = "description", length = 255)
    private String description;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
