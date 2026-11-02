package com.sstc.platform.dashboard.service;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.dashboard.domain.Okr;
import com.sstc.platform.dashboard.domain.PerformanceSnapshot;
import com.sstc.platform.dashboard.domain.Task;
import com.sstc.platform.dashboard.domain.TaskPriority;
import com.sstc.platform.dashboard.domain.TaskStatus;
import com.sstc.platform.dashboard.repo.*;
import com.sstc.platform.dashboard.web.DashboardViews;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.service.UserDirectory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * S02 - Task Assessment.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>{@code TaskDashboard.DisplayPanel} - real time process and outcome indicators (US5)</li>
 *   <li>{@code TaskDashboard.PerformanceCalculator} - OKR based performance score (US6)</li>
 * </ul>
 *
 * <p>Scoring model (documented so the sequence diagram and the UI agree):</p>
 * <pre>
 *   onTimeRate   = completed tasks delivered on or before due date / completed tasks
 *   qualityScore = weighted completion of HIGH/URGENT work (priority weight)
 *   okrScore     = average progress of the OKRs owned by the member
 *   totalScore   = 0.4*onTime + 0.3*quality + 0.3*okr     (0..100)
 *   grade        = A (&gt;=85) / B (&gt;=70) / C (&gt;=55) / D otherwise
 * </pre>
 */
@Service
public class PerformanceService {

    private final TasksRepo tasks;

    private final OkrsRepo okrs;

    private final SnapshotsRepo snapshots;

    private final UserDirectory directory;

    private final AuditService auditService;

    public PerformanceService(TasksRepo tasks,
                              OkrsRepo okrs,
                              SnapshotsRepo snapshots,
                              UserDirectory directory,
                              AuditService auditService) {
        this.tasks = tasks;
        this.okrs = okrs;
        this.snapshots = snapshots;
        this.directory = directory;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public DashboardViews.PerformanceView performance() {
        DashboardViews.PerformanceView view = new DashboardViews.PerformanceView();
        List<Task> allTasks = tasks.findAll();
        List<User> members = directory.activeMembers();

        List<DashboardViews.MemberScore> scores = new ArrayList<DashboardViews.MemberScore>();
        double okrSum = 0d;
        int okrCount = 0;
        for (User member : members) {
            DashboardViews.MemberScore score = scoreOf(member, allTasks);
            if (score.completed > 0 || score.open > 0) {
                scores.add(score);
            }
            for (Okr okr : okrs.findAll()) {
                if (member.getId().equals(okr.getOwnerId())) {
                    okrSum += okr.getProgress();
                    okrCount++;
                }
            }
        }
        view.members = scores;
        view.teamOkrScore = okrCount == 0 ? 0d : round(okrSum / okrCount);

        int completed = 0;
        int open = 0;
        int overdue = 0;
        double onTimeHits = 0d;
        double onTimeTotal = 0d;
        for (Task task : allTasks) {
            if (task.getStatus() == TaskStatus.DONE) {
                completed++;
                if (task.getDueDate() != null) {
                    onTimeTotal++;
                    if (task.getUpdatedAt() != null
                            && !task.getUpdatedAt().toLocalDate().isAfter(task.getDueDate())) {
                        onTimeHits++;
                    }
                }
            } else {
                open++;
                if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
                    overdue++;
                }
            }
        }
        view.openTasks = open;
        view.overdueTasks = overdue;
        view.completedThisCycle = completed;
        view.teamOnTimeRate = onTimeTotal == 0d ? 0d : round(onTimeHits * 100d / onTimeTotal);

        List<DashboardViews.SnapshotView> trend = new ArrayList<DashboardViews.SnapshotView>();
        for (PerformanceSnapshot snapshot : snapshots.findByUserIdIsNullOrderBySnapshotDateAsc()) {
            trend.add(new DashboardViews.SnapshotView(snapshot));
        }
        double throughput = 0d;
        for (DashboardViews.SnapshotView item : trend) {
            throughput += item.throughput;
        }
        view.averageThroughput = trend.isEmpty() ? 0d : round(throughput / trend.size());
        view.trend = trend;
        view.bottlenecks = bottlenecks(allTasks);
        return view;
    }

    private DashboardViews.MemberScore scoreOf(User member, List<Task> allTasks) {
        DashboardViews.MemberScore score = new DashboardViews.MemberScore();
        score.userId = member.getId();
        score.name = member.getDisplayName();
        score.initials = UserDirectory.initials(member.getDisplayName());
        score.roleLabel = member.getRole() == null ? null : member.getRole().getLabel();

        double onTimeHits = 0d;
        double onTimeTotal = 0d;
        double weighted = 0d;
        double weightSum = 0d;
        double okrSum = 0d;
        int okrCount = 0;
        for (Task task : allTasks) {
            if (!member.getId().equals(task.getAssigneeId())) {
                continue;
            }
            double weight = weightOf(task.getPriority());
            weightSum += weight;
            if (task.getStatus() == TaskStatus.DONE) {
                score.completed++;
                weighted += weight;
                if (task.getDueDate() != null && task.getUpdatedAt() != null) {
                    onTimeTotal++;
                    if (!task.getUpdatedAt().toLocalDate().isAfter(task.getDueDate())) {
                        onTimeHits++;
                    }
                }
            } else {
                score.open++;
                if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
                    score.overdue++;
                }
            }
        }
        for (Okr okr : okrs.findAll()) {
            if (member.getId().equals(okr.getOwnerId())) {
                okrSum += okr.getProgress();
                okrCount++;
            }
        }
        score.onTimeRate = onTimeTotal == 0d ? 100d : round(onTimeHits * 100d / onTimeTotal);
        score.qualityScore = weightSum == 0d ? 0d : round(weighted * 100d / weightSum);
        score.okrScore = okrCount == 0 ? 0d : round(okrSum / okrCount);
        score.totalScore = round(0.4d * score.onTimeRate + 0.3d * score.qualityScore
                + 0.3d * score.okrScore);
        score.grade = grade(score.totalScore);
        return score;
    }

    private String grade(double total) {
        if (total >= 85d) {
            return "A";
        }
        if (total >= 70d) {
            return "B";
        }
        if (total >= 55d) {
            return "C";
        }
        return "D";
    }

    private double weightOf(TaskPriority priority) {
        if (priority == null) {
            return 1d;
        }
        switch (priority) {
            case URGENT:
                return 2.0d;
            case HIGH:
                return 1.5d;
            case MEDIUM:
                return 1.2d;
            default:
                return 1.0d;
        }
    }

    /** Bottleneck flagging required by the case study ("flag potential bottlenecks"). */
    private List<DashboardViews.Bottleneck> bottlenecks(List<Task> allTasks) {
        List<DashboardViews.Bottleneck> result = new ArrayList<DashboardViews.Bottleneck>();
        for (Task task : allTasks) {
            if (task.getStatus() == TaskStatus.DONE) {
                continue;
            }
            if (task.isBlocked()) {
                DashboardViews.Bottleneck item = new DashboardViews.Bottleneck();
                item.type = "BLOCKED";
                item.label = task.getCode() + " " + task.getTitle();
                item.detail = "Task is flagged as blocked";
                item.severity = "HIGH";
                item.taskId = task.getId();
                result.add(item);
            } else if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
                DashboardViews.Bottleneck item = new DashboardViews.Bottleneck();
                item.type = "OVERDUE";
                item.label = task.getCode() + " " + task.getTitle();
                item.detail = "Due " + task.getDueDate() + " and still " + task.getStatus();
                item.severity = "MEDIUM";
                item.taskId = task.getId();
                result.add(item);
            } else if (task.getEstimateHours() > 0 && task.getLoggedHours() > task.getEstimateHours() * 1.5d) {
                DashboardViews.Bottleneck item = new DashboardViews.Bottleneck();
                item.type = "OVERRUN";
                item.label = task.getCode() + " " + task.getTitle();
                item.detail = "Logged " + task.getLoggedHours() + "h against an estimate of "
                        + task.getEstimateHours() + "h";
                item.severity = "MEDIUM";
                item.taskId = task.getId();
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Generates the daily indicator snapshot rendered by the Display Panel. Runs on start-up
     * and every day at 02:00 so the dashboard always has a trend to show.
     */
    @Scheduled(initialDelay = 4000L, fixedDelay = 86_400_000L)
    @Transactional
    public void captureSnapshot() {
        LocalDate today = LocalDate.now();
        List<Task> allTasks = tasks.findAll();
        if (snapshots.findByUserIdIsNullOrderBySnapshotDateAsc().stream()
                .anyMatch(s -> today.equals(s.getSnapshotDate()))) {
            return;
        }
        int completed = 0;
        int open = 0;
        int overdue = 0;
        double logged = 0d;
        for (Task task : allTasks) {
            if (task.getStatus() == TaskStatus.DONE) {
                completed++;
            } else {
                open++;
                if (task.getDueDate() != null && task.getDueDate().isBefore(today)) {
                    overdue++;
                }
            }
            logged += task.getLoggedHours();
        }
        double okrSum = 0d;
        int okrCount = 0;
        for (Okr okr : okrs.findAll()) {
            okrSum += okr.getProgress();
            okrCount++;
        }
        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        snapshot.setSnapshotDate(today);
        snapshot.setTasksCompleted(completed);
        snapshot.setTasksOpen(open);
        snapshot.setOverdueTasks(overdue);
        snapshot.setOnTimeRate(completed == 0 ? 0d
                : round((completed - overdue) * 100d / Math.max(completed, 1)));
        snapshot.setQualityScore(round(okrCount == 0 ? 0d : okrSum / okrCount));
        snapshot.setOkrScore(round(okrCount == 0 ? 0d : okrSum / okrCount));
        snapshot.setThroughput(round(logged));
        snapshots.save(snapshot);
        auditService.success("S02", "SNAPSHOT_CAPTURE", "snapshot", today.toString(),
                "completed=" + completed + " open=" + open + " overdue=" + overdue);
    }

    private double round(double value) {
        return Math.round(value * 10d) / 10d;
    }
}
