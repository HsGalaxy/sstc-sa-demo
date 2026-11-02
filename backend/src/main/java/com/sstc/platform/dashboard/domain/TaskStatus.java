package com.sstc.platform.dashboard.domain;

import java.util.Arrays;
import java.util.List;

/**
 * S02 - Kanban column of a task (US03).
 */
public enum TaskStatus {

    TODO("To Do", 0),
    IN_PROGRESS("In Progress", 1),
    REVIEW("Review", 2),
    DONE("Done", 3);

    private final String label;
    private final int order;

    TaskStatus(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }

    public static List<TaskStatus> columns() {
        return Arrays.asList(TODO, IN_PROGRESS, REVIEW, DONE);
    }
}
