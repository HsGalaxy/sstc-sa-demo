package com.sstc.platform.dashboard.service;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.web.AppException;
import com.sstc.platform.dashboard.domain.KeyResult;
import com.sstc.platform.dashboard.domain.Okr;
import com.sstc.platform.dashboard.domain.OperationLog;
import com.sstc.platform.dashboard.domain.Task;
import com.sstc.platform.dashboard.domain.TaskCycle;
import com.sstc.platform.dashboard.domain.TaskStatus;
import com.sstc.platform.dashboard.repo.*;
import com.sstc.platform.dashboard.web.DashboardRequests;
import com.sstc.platform.dashboard.web.DashboardViews;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.service.UserDirectory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S02 - Task Dashboard.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>Task Controller - {@code TaskDashboard.ModificationScheduler} (US03) and
 *       {@code TaskDashboard.OperationLogger} (US04)</li>
 *   <li>Task Assessment - {@code TaskDashboard.DisplayPanel} (US05) board aggregation</li>
 * </ul>
 */
@Service
public class TaskDashboardService {

    private final TasksRepo tasks;

    private final CyclesRepo cycles;

    private final OkrsRepo okrs;

    private final KeyResultsRepo keyResults;

    private final FileRecordsRepo fileRecords;

    private final OperationLogsRepo operationLogs;

    private final UserDirectory directory;

    private final AuditService auditService;

    public TaskDashboardService(TasksRepo tasks,
                                CyclesRepo cycles,
                                OkrsRepo okrs,
                                KeyResultsRepo keyResults,
                                FileRecordsRepo fileRecords,
                                OperationLogsRepo operationLogs,
                                UserDirectory directory,
                                AuditService auditService) {
        this.tasks = tasks;
        this.cycles = cycles;
        this.okrs = okrs;
        this.keyResults = keyResults;
        this.fileRecords = fileRecords;
        this.operationLogs = operationLogs;
        this.directory = directory;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ board

    @Transactional(readOnly = true)
    public List<DashboardViews.BoardColumn> board() {
        Map<Long, User> users = directory.index();
        Map<Long, String> cycleNames = new HashMap<Long, String>();
        for (TaskCycle cycle : cycles.findAll()) {
            cycleNames.put(cycle.getId(), cycle.getName());
        }
        Map<Long, String> okrNames = new HashMap<Long, String>();
        for (Okr okr : okrs.findAll()) {
            okrNames.put(okr.getId(), okr.getObjective());
        }
        List<DashboardViews.BoardColumn> columns = new ArrayList<DashboardViews.BoardColumn>();
        for (TaskStatus status : TaskStatus.columns()) {
            List<DashboardViews.TaskView> columnTasks = new ArrayList<DashboardViews.TaskView>();
            for (Task task : tasks.findAllByOrderByStatusAscPositionIndexAsc()) {
                if (task.getStatus() != status) {
                    continue;
                }
                columnTasks.add(decorate(task, users, cycleNames, okrNames));
            }
            columns.add(new DashboardViews.BoardColumn(status, columnTasks));
        }
        return columns;
    }

    @Transactional(readOnly = true)
    public List<DashboardViews.TaskView> listTasks(TaskStatus status, Long assigneeId, Long cycleId,
                                                   String keyword) {
        Map<Long, User> users = directory.index();
        Map<Long, String> cycleNames = new HashMap<Long, String>();
        for (TaskCycle cycle : cycles.findAll()) {
            cycleNames.put(cycle.getId(), cycle.getName());
        }
        Map<Long, String> okrNames = new HashMap<Long, String>();
        for (Okr okr : okrs.findAll()) {
            okrNames.put(okr.getId(), okr.getObjective());
        }
        List<DashboardViews.TaskView> result = new ArrayList<DashboardViews.TaskView>();
        for (Task task : tasks.findAllByOrderByStatusAscPositionIndexAsc()) {
            if (status != null && task.getStatus() != status) {
                continue;
            }
            if (assigneeId != null && !assigneeId.equals(task.getAssigneeId())) {
                continue;
            }
            if (cycleId != null && !cycleId.equals(task.getCycleId())) {
                continue;
            }
            if (keyword != null && !keyword.trim().isEmpty()
                    && (task.getTitle() == null
                        || !task.getTitle().toLowerCase().contains(keyword.trim().toLowerCase()))) {
                continue;
            }
            result.add(decorate(task, users, cycleNames, okrNames));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public DashboardViews.TaskView getTask(Long id) {
        return decorate(require(id), directory.index(), cycleNames(), okrNames());
    }

    private DashboardViews.TaskView decorate(Task task, Map<Long, User> users,
                                             Map<Long, String> cycleNames,
                                             Map<Long, String> okrNames) {
        DashboardViews.TaskView view = new DashboardViews.TaskView(task);
        User assignee = task.getAssigneeId() == null ? null : users.get(task.getAssigneeId());
        if (assignee != null) {
            view.assigneeName = assignee.getDisplayName();
            view.assigneeInitials = UserDirectory.initials(assignee.getDisplayName());
        }
        User reporter = task.getReporterId() == null ? null : users.get(task.getReporterId());
        if (reporter != null) {
            view.reporterName = reporter.getDisplayName();
        }
        view.cycleName = task.getCycleId() == null ? null : cycleNames.get(task.getCycleId());
        view.okrObjective = task.getOkrId() == null ? null : okrNames.get(task.getOkrId());
        view.attachmentCount = fileRecords.findByTaskId(task.getId()).size();
        return view;
    }

    private Map<Long, String> cycleNames() {
        Map<Long, String> map = new HashMap<Long, String>();
        for (TaskCycle cycle : cycles.findAll()) {
            map.put(cycle.getId(), cycle.getName());
        }
        return map;
    }

    private Map<Long, String> okrNames() {
        Map<Long, String> map = new HashMap<Long, String>();
        for (Okr okr : okrs.findAll()) {
            map.put(okr.getId(), okr.getObjective());
        }
        return map;
    }

    // ------------------------------------------------------------------ US03

    @Transactional
    public DashboardViews.TaskView createTask(DashboardRequests.TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.title.trim());
        task.setDescription(request.description);
        task.setStatus(request.status == null ? TaskStatus.TODO : request.status);
        task.setPriority(request.priority);
        task.setAssigneeId(request.assigneeId);
        task.setCycleId(request.cycleId);
        task.setOkrId(request.okrId);
        task.setDueDate(request.dueDate);
        task.setEstimateHours(request.estimateHours == null ? 0d : request.estimateHours);
        task.setTags(request.tags);
        task.setBlocked(request.blocked != null && request.blocked);
        task.setReporterId(CurrentUserHolder.id());
        task.setCode(nextCode());
        task.setPositionIndex((int) tasks.count());
        tasks.save(task);
        log("TASK_CREATE", "task", String.valueOf(task.getId()),
                task.getCode() + " " + task.getTitle(), "SUCCESS");
        auditService.success("S02", "TASK_CREATE", "task", task.getCode(), task.getTitle());
        return getTask(task.getId());
    }

    @Transactional
    public DashboardViews.TaskView updateTask(Long id, DashboardRequests.TaskUpdateRequest request) {
        Task task = require(id);
        StringBuilder changes = new StringBuilder();
        if (request.title != null && !request.title.trim().isEmpty()) {
            task.setTitle(request.title.trim());
            changes.append("title ");
        }
        if (request.description != null) {
            task.setDescription(request.description);
            changes.append("description ");
        }
        if (request.status != null && request.status != task.getStatus()) {
            task.setStatus(request.status);
            changes.append("status=").append(request.status).append(' ');
        }
        if (request.priority != null) {
            task.setPriority(request.priority);
            changes.append("priority=").append(request.priority).append(' ');
        }
        if (request.assigneeId != null) {
            task.setAssigneeId(request.assigneeId);
            changes.append("assignee=").append(request.assigneeId).append(' ');
        }
        if (request.cycleId != null) {
            task.setCycleId(request.cycleId);
        }
        if (request.okrId != null) {
            task.setOkrId(request.okrId);
        }
        if (request.dueDate != null) {
            task.setDueDate(request.dueDate);
            changes.append("dueDate=").append(request.dueDate).append(' ');
        }
        if (request.estimateHours != null) {
            task.setEstimateHours(request.estimateHours);
        }
        if (request.loggedHours != null) {
            task.setLoggedHours(request.loggedHours);
            changes.append("loggedHours=").append(request.loggedHours).append(' ');
        }
        if (request.tags != null) {
            task.setTags(request.tags);
        }
        if (request.blocked != null) {
            task.setBlocked(request.blocked);
            changes.append("blocked=").append(request.blocked).append(' ');
        }
        if (request.positionIndex != null) {
            task.setPositionIndex(request.positionIndex);
        }
        tasks.save(task);
        String detail = changes.length() == 0 ? "no change" : changes.toString().trim();
        log("TASK_UPDATE", "task", String.valueOf(task.getId()), task.getCode() + " " + detail, "SUCCESS");
        auditService.success("S02", "TASK_UPDATE", "task", task.getCode(), detail);
        return getTask(task.getId());
    }

    @Transactional
    public DashboardViews.TaskView moveTask(Long id, TaskStatus status, Integer position) {
        Task task = require(id);
        TaskStatus from = task.getStatus();
        task.setStatus(status);
        if (position != null) {
            task.setPositionIndex(position);
        }
        tasks.save(task);
        log("TASK_MOVE", "task", String.valueOf(task.getId()),
                task.getCode() + " " + from + " -> " + status, "SUCCESS");
        auditService.success("S02", "TASK_MOVE", "task", task.getCode(), from + " -> " + status);
        return getTask(task.getId());
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = require(id);
        tasks.delete(task);
        log("TASK_DELETE", "task", String.valueOf(id), task.getCode(), "SUCCESS");
        auditService.success("S02", "TASK_DELETE", "task", task.getCode(), "removed");
    }

    // ------------------------------------------------------------------ cycles

    @Transactional(readOnly = true)
    public List<DashboardViews.CycleView> listCycles() {
        List<DashboardViews.CycleView> result = new ArrayList<DashboardViews.CycleView>();
        for (TaskCycle cycle : cycles.findAllByOrderByStartDateDesc()) {
            DashboardViews.CycleView view = new DashboardViews.CycleView(cycle);
            int total = 0;
            int done = 0;
            for (Task task : tasks.findByCycleId(cycle.getId())) {
                total++;
                if (task.getStatus() == TaskStatus.DONE) {
                    done++;
                }
            }
            view.totalTasks = total;
            view.doneTasks = done;
            view.progress = total == 0 ? 0 : (int) Math.round(done * 100d / total);
            result.add(view);
        }
        return result;
    }

    @Transactional
    public DashboardViews.CycleView createCycle(DashboardRequests.CycleRequest request) {
        TaskCycle cycle = new TaskCycle();
        cycle.setName(request.name.trim());
        cycle.setGoal(request.goal);
        cycle.setStartDate(request.startDate == null ? LocalDate.now() : request.startDate);
        cycle.setEndDate(request.endDate == null ? LocalDate.now().plusWeeks(2) : request.endDate);
        cycle.setActive(request.active == null || request.active);
        cycles.save(cycle);
        auditService.success("S02", "CYCLE_CREATE", "cycle", String.valueOf(cycle.getId()), cycle.getName());
        return new DashboardViews.CycleView(cycle);
    }

    // ------------------------------------------------------------------ OKR

    @Transactional(readOnly = true)
    public List<DashboardViews.OkrView> listOkrs() {
        Map<Long, User> users = directory.index();
        List<DashboardViews.OkrView> result = new ArrayList<DashboardViews.OkrView>();
        for (Okr okr : okrs.findAll()) {
            DashboardViews.OkrView view = new DashboardViews.OkrView(okr);
            User owner = okr.getOwnerId() == null ? null : users.get(okr.getOwnerId());
            view.ownerName = owner == null ? null : owner.getDisplayName();
            List<DashboardViews.KeyResultView> krs = new ArrayList<DashboardViews.KeyResultView>();
            for (KeyResult keyResult : keyResults.findByOkrId(okr.getId())) {
                krs.add(new DashboardViews.KeyResultView(keyResult));
            }
            view.keyResults = krs;
            result.add(view);
        }
        return result;
    }

    @Transactional
    public DashboardViews.OkrView createOkr(DashboardRequests.OkrRequest request) {
        Okr okr = new Okr();
        okr.setObjective(request.objective.trim());
        okr.setCycleId(request.cycleId);
        okr.setOwnerId(request.ownerId == null ? CurrentUserHolder.id() : request.ownerId);
        okr.setWeight(request.weight == null ? 1.0d : request.weight);
        if (request.status != null) {
            try {
                okr.setStatus(Okr.Status.valueOf(request.status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // keep default status
            }
        }
        okrs.save(okr);
        auditService.success("S02", "OKR_CREATE", "okr", String.valueOf(okr.getId()), okr.getObjective());
        return listOkrs().get(listOkrs().size() - 1);
    }

    @Transactional
    public DashboardViews.KeyResultView addKeyResult(Long okrId, DashboardRequests.KeyResultRequest request) {
        Okr okr = okrs.findById(okrId).orElseThrow(() -> AppException.notFound("OKR " + okrId));
        KeyResult keyResult = new KeyResult();
        keyResult.setOkrId(okr.getId());
        keyResult.setTitle(request.title.trim());
        keyResult.setTargetValue(request.targetValue);
        keyResult.setCurrentValue(request.currentValue);
        keyResult.setUnit(request.unit);
        keyResult.setOwnerId(request.ownerId == null ? okr.getOwnerId() : request.ownerId);
        keyResults.save(keyResult);
        recalculateOkr(okr);
        auditService.success("S02", "KEY_RESULT_CREATE", "okr", String.valueOf(okrId), keyResult.getTitle());
        return new DashboardViews.KeyResultView(keyResult);
    }

    @Transactional
    public DashboardViews.KeyResultView updateKeyResult(Long keyResultId, double currentValue) {
        KeyResult keyResult = keyResults.findById(keyResultId)
                .orElseThrow(() -> AppException.notFound("Key result " + keyResultId));
        keyResult.setCurrentValue(currentValue);
        keyResults.save(keyResult);
        okrs.findById(keyResult.getOkrId()).ifPresent(this::recalculateOkr);
        auditService.success("S02", "KEY_RESULT_UPDATE", "keyResult", String.valueOf(keyResultId),
                "currentValue=" + currentValue);
        return new DashboardViews.KeyResultView(keyResult);
    }

    private void recalculateOkr(Okr okr) {
        List<KeyResult> list = keyResults.findByOkrId(okr.getId());
        int sum = 0;
        for (KeyResult keyResult : list) {
            sum += keyResult.getRatio();
        }
        int progress = list.isEmpty() ? 0 : sum / list.size();
        okr.setProgress(progress);
        if (progress >= 100) {
            okr.setStatus(Okr.Status.ACHIEVED);
        } else if (progress < 40) {
            okr.setStatus(Okr.Status.OFF_TRACK);
        } else if (progress < 70) {
            okr.setStatus(Okr.Status.AT_RISK);
        } else {
            okr.setStatus(Okr.Status.ON_TRACK);
        }
        okrs.save(okr);
    }

    // ------------------------------------------------------------------ US04

    @Transactional(readOnly = true)
    public List<DashboardViews.OperationLogView> operations(int limit, String action, Long actorId) {
        List<DashboardViews.OperationLogView> result = new ArrayList<DashboardViews.OperationLogView>();
        List<OperationLog> logs = action == null && actorId == null
                ? operationLogs.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                : operationLogs.search(action, actorId, PageRequest.of(0, limit));
        for (OperationLog log : logs) {
            result.add(new DashboardViews.OperationLogView(log));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DashboardViews.OperationLogView> taskHistory(Long taskId) {
        List<DashboardViews.OperationLogView> result = new ArrayList<DashboardViews.OperationLogView>();
        for (OperationLog log : operationLogs
                .findByTargetTypeAndTargetIdOrderByCreatedAtDesc("task", String.valueOf(taskId))) {
            result.add(new DashboardViews.OperationLogView(log));
        }
        return result;
    }

    /** Writes the full lifecycle event required by US04. */
    @Transactional
    public void log(String action, String targetType, String targetId, String detail, String outcome) {
        OperationLog log = new OperationLog();
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setOutcome(outcome);
        log.setActorId(CurrentUserHolder.id());
        log.setActorName(CurrentUserHolder.name());
        log.setSource("API");
        operationLogs.save(log);
    }

    // ------------------------------------------------------------------ helpers

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        long total = tasks.count();
        long done = tasks.countByStatus(TaskStatus.DONE);
        long inProgress = tasks.countByStatus(TaskStatus.IN_PROGRESS);
        long todo = tasks.countByStatus(TaskStatus.TODO);
        long review = tasks.countByStatus(TaskStatus.REVIEW);
        long overdue = 0L;
        for (Task task : tasks.findAll()) {
            if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())
                    && task.getStatus() != TaskStatus.DONE) {
                overdue++;
            }
        }
        map.put("total", total);
        map.put("done", done);
        map.put("inProgress", inProgress);
        map.put("todo", todo);
        map.put("review", review);
        map.put("overdue", overdue);
        map.put("completionRate", total == 0 ? 0 : Math.round(done * 1000d / total) / 10d);
        map.put("loggedHours", round(tasks.findAll().stream().mapToDouble(Task::getLoggedHours).sum()));
        map.put("generatedAt", LocalDateTime.now());
        return map;
    }

    private double round(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private Task require(Long id) {
        return tasks.findById(id).orElseThrow(() -> AppException.notFound("Task " + id));
    }

    private String nextCode() {
        long count = tasks.count() + 1;
        while (true) {
            String candidate = String.format("STC-%03d", count++);
            boolean taken = false;
            for (Task existing : tasks.findAll()) {
                if (candidate.equals(existing.getCode())) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return candidate;
            }
        }
    }
}
