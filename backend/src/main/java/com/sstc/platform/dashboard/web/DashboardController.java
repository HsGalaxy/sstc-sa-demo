package com.sstc.platform.dashboard.web;

import com.sstc.platform.dashboard.domain.TaskStatus;
import com.sstc.platform.dashboard.service.PerformanceService;
import com.sstc.platform.dashboard.service.TaskDashboardService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * S02 - Task Dashboard REST surface (board, tasks, cycles, OKR, performance, audit).
 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final TaskDashboardService dashboardService;

    private final PerformanceService performanceService;

    public DashboardController(TaskDashboardService dashboardService,
                               PerformanceService performanceService) {
        this.dashboardService = dashboardService;
        this.performanceService = performanceService;
    }

    // ------------------------------------------------------------------ US05

    @GetMapping("/tasks/board")
    public List<DashboardViews.BoardColumn> board() {
        return dashboardService.board();
    }

    @GetMapping("/tasks/summary")
    public Map<String, Object> summary() {
        return dashboardService.summary();
    }

    // ------------------------------------------------------------------ US03

    @GetMapping("/tasks")
    public List<DashboardViews.TaskView> tasks(@RequestParam(required = false) TaskStatus status,
                                               @RequestParam(required = false) Long assigneeId,
                                               @RequestParam(required = false) Long cycleId,
                                               @RequestParam(required = false) String keyword) {
        return dashboardService.listTasks(status, assigneeId, cycleId, keyword);
    }

    @GetMapping("/tasks/{id}")
    public DashboardViews.TaskView task(@PathVariable Long id) {
        return dashboardService.getTask(id);
    }

    @PostMapping("/tasks")
    public DashboardViews.TaskView createTask(@Valid @RequestBody DashboardRequests.TaskRequest request) {
        return dashboardService.createTask(request);
    }

    @PutMapping("/tasks/{id}")
    public DashboardViews.TaskView updateTask(@PathVariable Long id,
                                              @RequestBody DashboardRequests.TaskUpdateRequest request) {
        return dashboardService.updateTask(id, request);
    }

    @PatchMapping("/tasks/{id}/move")
    public DashboardViews.TaskView moveTask(@PathVariable Long id,
                                            @RequestParam TaskStatus status,
                                            @RequestParam(required = false) Integer position) {
        return dashboardService.moveTask(id, status, position);
    }

    @DeleteMapping("/tasks/{id}")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        dashboardService.deleteTask(id);
        return java.util.Collections.singletonMap("status", "task-deleted");
    }

    // ------------------------------------------------------------------ cycles

    @GetMapping("/cycles")
    public List<DashboardViews.CycleView> cycles() {
        return dashboardService.listCycles();
    }

    @PostMapping("/cycles")
    public DashboardViews.CycleView createCycle(@Valid @RequestBody DashboardRequests.CycleRequest request) {
        return dashboardService.createCycle(request);
    }

    // ------------------------------------------------------------------ OKR

    @GetMapping("/okrs")
    public List<DashboardViews.OkrView> okrs() {
        return dashboardService.listOkrs();
    }

    @PostMapping("/okrs")
    public DashboardViews.OkrView createOkr(@Valid @RequestBody DashboardRequests.OkrRequest request) {
        return dashboardService.createOkr(request);
    }

    @PostMapping("/okrs/{id}/key-results")
    public DashboardViews.KeyResultView addKeyResult(@PathVariable Long id,
                                                     @Valid @RequestBody DashboardRequests.KeyResultRequest request) {
        return dashboardService.addKeyResult(id, request);
    }

    @PatchMapping("/key-results/{id}")
    public DashboardViews.KeyResultView updateKeyResult(@PathVariable Long id,
                                                        @RequestParam double currentValue) {
        return dashboardService.updateKeyResult(id, currentValue);
    }

    // ------------------------------------------------------------------ US06 / US05

    @GetMapping("/performance")
    public DashboardViews.PerformanceView performance() {
        return performanceService.performance();
    }

    @PostMapping("/performance/snapshot")
    public Map<String, Object> captureSnapshot() {
        performanceService.captureSnapshot();
        return java.util.Collections.singletonMap("status", "snapshot-captured");
    }

    // ------------------------------------------------------------------ US04

    @GetMapping("/operations")
    public List<DashboardViews.OperationLogView> operations(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId) {
        return dashboardService.operations(limit, action, actorId);
    }

    @GetMapping("/tasks/{id}/history")
    public List<DashboardViews.OperationLogView> taskHistory(@PathVariable Long id) {
        return dashboardService.taskHistory(id);
    }
}
