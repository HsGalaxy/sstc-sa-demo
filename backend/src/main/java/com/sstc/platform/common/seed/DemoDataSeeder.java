package com.sstc.platform.common.seed;

import com.sstc.platform.collaboration.domain.Channel;
import com.sstc.platform.collaboration.domain.ChannelMember;
import com.sstc.platform.collaboration.domain.Meeting;
import com.sstc.platform.collaboration.domain.MeetingSummary;
import com.sstc.platform.collaboration.domain.MeetingTranscript;
import com.sstc.platform.collaboration.domain.Mention;
import com.sstc.platform.collaboration.domain.Message;
import com.sstc.platform.collaboration.domain.Notification;
import com.sstc.platform.collaboration.repo.*;
import com.sstc.platform.dashboard.domain.FileRecord;
import com.sstc.platform.dashboard.domain.FileVersion;
import com.sstc.platform.dashboard.domain.KeyResult;
import com.sstc.platform.dashboard.domain.Okr;
import com.sstc.platform.dashboard.domain.OperationLog;
import com.sstc.platform.dashboard.domain.Task;
import com.sstc.platform.dashboard.domain.TaskCycle;
import com.sstc.platform.dashboard.domain.TaskPriority;
import com.sstc.platform.dashboard.domain.TaskStatus;
import com.sstc.platform.dashboard.repo.*;
import com.sstc.platform.identity.domain.AccountEvent;
import com.sstc.platform.identity.domain.Permission;
import com.sstc.platform.identity.domain.PermissionPolicy;
import com.sstc.platform.identity.domain.Role;
import com.sstc.platform.identity.domain.RolePermission;
import com.sstc.platform.identity.domain.RoleType;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.domain.UserRole;
import com.sstc.platform.identity.repo.*;
import com.sstc.platform.toolbox.domain.CommitRecord;
import com.sstc.platform.toolbox.domain.DevEnvironment;
import com.sstc.platform.toolbox.domain.DocDocument;
import com.sstc.platform.toolbox.domain.GitRepo;
import com.sstc.platform.toolbox.domain.Presentation;
import com.sstc.platform.toolbox.domain.Slide;
import com.sstc.platform.toolbox.domain.ToolboxFile;
import com.sstc.platform.toolbox.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo data of the STC platform.
 *
 * <p>The seeder is idempotent: it only runs when the account table is empty, or when
 * {@code sstc.seed.reset=true}. It creates a realistic small team (three actors of the Level-1
 * taxonomy), a delivery cycle with Kanban tasks, OKRs, task files with version history,
 * collaboration channels with mentions and notifications, a transcribed meeting with an AI
 * summary, and the Toolbox assets (documents, deck, repositories, container environments and
 * project files).</p>
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UsersRepo users;

    private final RolesRepo roles;

    private final PermissionsRepo permissions;

    private final RolePermissionsRepo rolePermissions;

    private final UserRolesRepo userRoles;

    private final PoliciesRepo policies;

    private final AccountEventsRepo accountEvents;

    private final TasksRepo tasks;

    private final CyclesRepo cycles;

    private final OkrsRepo okrs;

    private final KeyResultsRepo keyResults;

    private final FileRecordsRepo fileRecords;

    private final FileVersionsRepo fileVersions;

    private final OperationLogsRepo operationLogs;

    private final ChannelsRepo channels;

    private final ChannelMembersRepo channelMembers;

    private final MessagesRepo messages;

    private final MentionsRepo mentions;

    private final NotificationsRepo notifications;

    private final MeetingsRepo meetings;

    private final TranscriptsRepo transcripts;

    private final SummariesRepo summaries;

    private final GitReposRepo repos;

    private final CommitsRepo commits;

    private final EnvironmentsRepo environments;

    private final DocumentsRepo documents;

    private final PresentationsRepo presentations;

    private final SlidesRepo slides;

    private final FilesRepo toolboxFiles;

    private final PasswordEncoder passwordEncoder;

    private final boolean enabled;

    private final boolean reset;

    public DemoDataSeeder(UsersRepo users,
                          RolesRepo roles,
                          PermissionsRepo permissions,
                          RolePermissionsRepo rolePermissions,
                          UserRolesRepo userRoles,
                          PoliciesRepo policies,
                          AccountEventsRepo accountEvents,
                          TasksRepo tasks,
                          CyclesRepo cycles,
                          OkrsRepo okrs,
                          KeyResultsRepo keyResults,
                          FileRecordsRepo fileRecords,
                          FileVersionsRepo fileVersions,
                          OperationLogsRepo operationLogs,
                          ChannelsRepo channels,
                          ChannelMembersRepo channelMembers,
                          MessagesRepo messages,
                          MentionsRepo mentions,
                          NotificationsRepo notifications,
                          MeetingsRepo meetings,
                          TranscriptsRepo transcripts,
                          SummariesRepo summaries,
                          GitReposRepo repos,
                          CommitsRepo commits,
                          EnvironmentsRepo environments,
                          DocumentsRepo documents,
                          PresentationsRepo presentations,
                          SlidesRepo slides,
                          FilesRepo toolboxFiles,
                          PasswordEncoder passwordEncoder,
                          @org.springframework.beans.factory.annotation.Value("${sstc.seed.enabled:true}") boolean enabled,
                          @org.springframework.beans.factory.annotation.Value("${sstc.seed.reset:false}") boolean reset) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.rolePermissions = rolePermissions;
        this.userRoles = userRoles;
        this.policies = policies;
        this.accountEvents = accountEvents;
        this.tasks = tasks;
        this.cycles = cycles;
        this.okrs = okrs;
        this.keyResults = keyResults;
        this.fileRecords = fileRecords;
        this.fileVersions = fileVersions;
        this.operationLogs = operationLogs;
        this.channels = channels;
        this.channelMembers = channelMembers;
        this.messages = messages;
        this.mentions = mentions;
        this.notifications = notifications;
        this.meetings = meetings;
        this.transcripts = transcripts;
        this.summaries = summaries;
        this.repos = repos;
        this.commits = commits;
        this.environments = environments;
        this.documents = documents;
        this.presentations = presentations;
        this.slides = slides;
        this.toolboxFiles = toolboxFiles;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.reset = reset;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            LOG.info("Demo seeding disabled (sstc.seed.enabled=false)");
            return;
        }
        if (users.count() > 0 && !reset) {
            LOG.info("Demo data already present: {} accounts, {} tasks, {} messages",
                    users.count(), tasks.count(), messages.count());
            return;
        }
        if (reset) {
            LOG.warn("sstc.seed.reset=true - clearing existing demo data");
        }
        seedPermissions();
        Map<String, Role> roleIndex = seedRoles();
        Map<String, User> people = seedUsers(roleIndex);
        TaskCycle cycle = seedCycle();
        Map<String, Okr> okrIndex = seedOkrs(cycle, people);
        List<Task> created = seedTasks(cycle, okrIndex, people);
        seedTaskFiles(created, people);
        seedCollaboration(people);
        seedToolbox(people);
        seedOperationLog(people, created);
        LOG.info("Demo data ready: {} accounts, {} tasks, {} messages, {} files",
                users.count(), tasks.count(), messages.count(), fileRecords.count());
    }

    // ------------------------------------------------------------------ S01

    private void seedPermissions() {
        Object[][] matrix = {
                {"S01.ACCOUNT.VIEW", "View accounts", "S01"},
                {"S01.ACCOUNT.MANAGE", "Create, update and deactivate accounts", "S01"},
                {"S01.ROLE.MANAGE", "Assign and revoke roles", "S01"},
                {"S01.POLICY.MANAGE", "Edit permission policies", "S01"},
                {"S01.IDENTITY.ACCESS", "Use protected platform functions", "S01"},
                {"S01.AUDIT.VIEW", "Read the access audit trail", "S01"},
                {"S02.TASK.VIEW", "View tasks and the Kanban board", "S02"},
                {"S02.TASK.MANAGE", "Create and update tasks", "S02"},
                {"S02.FILE.VIEW", "Query task files and version history", "S02"},
                {"S02.FILE.MANAGE", "Upload files and manage versions", "S02"},
                {"S02.OKR.VIEW", "View OKR and key results", "S02"},
                {"S02.PERFORMANCE.VIEW", "View OKR performance scores", "S02"},
                {"S02.AUDIT.VIEW", "Read the operation log", "S02"},
                {"S03.COLLABORATION.USE", "Use channels, messages and meetings", "S03"},
                {"S03.NOTIFICATION.USE", "Receive notifications and the smart digest", "S03"},
                {"S04.OFFICE.USE", "Create and edit documents and presentations", "S04"},
                {"S04.DEVTOOLS.USE", "Use Git and container environments", "S04"},
                {"S04.FILE.USE", "Upload, store and share project files", "S04"}
        };
        for (Object[] row : matrix) {
            if (!permissions.findByCode((String) row[0]).isPresent()) {
                Permission permission = new Permission();
                permission.setCode((String) row[0]);
                permission.setName((String) row[1]);
                permission.setModule((String) row[2]);
                permission.setDescription((String) row[1]);
                permissions.save(permission);
            }
        }
    }

    private Map<String, Role> seedRoles() {
        Map<String, Role> index = new LinkedHashMap<String, Role>();
        index.put("ROLE_OVERALL_LEAD", ensureRole("ROLE_OVERALL_LEAD", "Overall Lead",
                "Owns identities, roles and permission policies", RoleType.OVERALL_LEAD, "S01"));
        index.put("ROLE_SENIOR_MANAGER", ensureRole("ROLE_SENIOR_MANAGER", "Senior Manager",
                "Reviews tasks, OKR and project files", RoleType.SENIOR_MANAGER, "S02"));
        index.put("ROLE_TEAM_MEMBER", ensureRole("ROLE_TEAM_MEMBER", "Team Member",
                "Works on tasks, chat and the toolbox", RoleType.TEAM_MEMBER, "S03"));
        return index;
    }

    private Role ensureRole(String code, String name, String description, RoleType type, String ownerModule) {
        Role role = roles.findByCode(code).orElse(null);
        if (role == null) {
            role = new Role();
            role.setCode(code);
            role.setName(name);
            role.setDescription(description);
            role.setRoleType(type);
            role.setBuiltIn(true);
            roles.save(role);
        }
        List<RolePermission> existing = rolePermissions.findByRoleId(role.getId());
        if (existing.isEmpty()) {
            for (Permission permission : permissions.findAllByOrderByModuleAscCodeAsc()) {
                boolean bind = "ROLE_OVERALL_LEAD".equals(code)
                        || ("ROLE_SENIOR_MANAGER".equals(code)
                            && !"S01.ACCOUNT.MANAGE".equals(permission.getCode())
                            && !"S01.POLICY.MANAGE".equals(permission.getCode()))
                        || ("ROLE_TEAM_MEMBER".equals(code)
                            && !permission.getModule().startsWith("S01"));
                if (bind) {
                    rolePermissions.save(new RolePermission(role.getId(), permission.getId()));
                }
            }
        }
        return role;
    }

    private Map<String, User> seedUsers(Map<String, Role> roleIndex) {
        Map<String, User> people = new LinkedHashMap<String, User>();
        people.put("lead", ensureUser("lead", "Alex Lead", "lead@stc.dev", "Lead@123",
                "Overall Lead", RoleType.OVERALL_LEAD, "#4f7cff"));
        people.put("manager", ensureUser("manager", "Nina Manager", "manager@stc.dev", "Manager@123",
                "Senior Manager", RoleType.SENIOR_MANAGER, "#22a06b"));
        people.put("yunteng", ensureUser("yunteng", "Yunteng Hou", "yunteng@stc.dev", "Member@123",
                "Developer, Toolbox owner", RoleType.TEAM_MEMBER, "#f2994a"));
        people.put("jinghui", ensureUser("jinghui", "Jinghui Huang", "jinghui@stc.dev", "Member@123",
                "Developer, Task Dashboard owner", RoleType.TEAM_MEMBER, "#9b51e0"));
        people.put("changyang", ensureUser("changyang", "Changyang Qian", "changyang@stc.dev", "Member@123",
                "Developer, Collaboration owner", RoleType.TEAM_MEMBER, "#2d9cdb"));
        people.put("hao", ensureUser("hao", "Hao Tan", "hao@stc.dev", "Member@123",
                "Developer, Identity owner", RoleType.TEAM_MEMBER, "#eb5757"));

        for (User user : people.values()) {
            if (userRoles.findByUserId(user.getId()).isEmpty()) {
                Role role = roleIndex.get(roleCode(user.getRole()));
                if (role != null) {
                    userRoles.save(new UserRole(user.getId(), role.getId()));
                }
            }
            accountEvents.save(new AccountEvent(user.getId(), "ACCOUNT_CREATED",
                    "Seeded demo account (" + user.getRole().getLabel() + ")", user.getId()));
        }

        // US03 demonstration: a fine grained policy override for one member.
        User changyang = people.get("changyang");
        if (policies.findByUserId(changyang.getId()).isEmpty()) {
            policies.save(new PermissionPolicy(changyang.getId(), "S02.PERFORMANCE.VIEW",
                    PermissionPolicy.Effect.GRANT, "Needs OKR indicators for the weekly report"));
            policies.save(new PermissionPolicy(changyang.getId(), "S01.AUDIT.VIEW",
                    PermissionPolicy.Effect.REVOKE, "Access audit is restricted to the lead"));
        }
        return people;
    }

    private User ensureUser(String username, String displayName, String email, String rawPassword,
                            String jobTitle, RoleType roleType, String color) {
        User user = users.findByUsername(username).orElse(null);
        if (user != null) {
            return user;
        }
        user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setJobTitle(jobTitle);
        user.setRole(roleType);
        user.setAvatarColor(color);
        user.setStatus(com.sstc.platform.identity.domain.AccountStatus.ACTIVE);
        user.setLastLoginAt(LocalDateTime.now().minusHours(3));
        return users.save(user);
    }

    private String roleCode(RoleType type) {
        switch (type) {
            case OVERALL_LEAD:
                return "ROLE_OVERALL_LEAD";
            case SENIOR_MANAGER:
                return "ROLE_SENIOR_MANAGER";
            default:
                return "ROLE_TEAM_MEMBER";
        }
    }

    // ------------------------------------------------------------------ S02

    private TaskCycle seedCycle() {
        if (cycles.count() > 0) {
            return cycles.findAllByOrderByStartDateDesc().get(0);
        }
        TaskCycle cycle = new TaskCycle();
        cycle.setName("Cycle 4 - Architecture execution");
        cycle.setGoal("Freeze the API contract, finish the sequence diagrams and demo the platform");
        cycle.setStartDate(LocalDate.now().minusDays(9));
        cycle.setEndDate(LocalDate.now().plusDays(5));
        cycle.setActive(true);
        cycles.save(cycle);

        TaskCycle previous = new TaskCycle();
        previous.setName("Cycle 3 - Level 4 design");
        previous.setGoal("Complete the UML class diagrams of all four sub-systems");
        previous.setStartDate(LocalDate.now().minusDays(24));
        previous.setEndDate(LocalDate.now().minusDays(10));
        previous.setActive(false);
        cycles.save(previous);
        return cycle;
    }

    private Map<String, Okr> seedOkrs(TaskCycle cycle, Map<String, User> people) {
        Map<String, Okr> index = new LinkedHashMap<String, Okr>();
        if (okrs.count() > 0) {
            for (Okr okr : okrs.findAll()) {
                index.put(okr.getObjective(), okr);
            }
            return index;
        }
        index.put("toolbox", createOkr("Ship the Toolbox sub-system with full US01-US05 coverage",
                cycle, people.get("yunteng"), 70, 1.2d,
                new String[]{"12 L3 components implemented;60", "Word and PPT editing covered;75",
                        "Container runtime demo ready;60"},
                new String[]{"Toolbox components implemented;12;12", "Office editing coverage;100;75",
                        "Container demo;100;60"}));
        index.put("dashboard", createOkr("Make the Task Dashboard the single source of task truth",
                cycle, people.get("jinghui"), 62, 1.0d,
                new String[]{"Kanban board adopted by the team;80", "Version history usable;50"},
                new String[]{"Kanban adoption;100;80", "Version history coverage;100;50"}));
        index.put("collaboration", createOkr("Reduce status meetings through collaboration features",
                cycle, people.get("changyang"), 55, 1.0d,
                new String[]{"AI meeting summary in use;60", "Smart digest keeps mentions on top;50"},
                new String[]{"Meeting summary adoption;100;60", "Digest precision;100;50"}));
        return index;
    }

    private Okr createOkr(String objective, TaskCycle cycle, User owner, int progress, double weight,
                          String[] unusedA, String[] keyResultSpecs) {
        Okr okr = new Okr();
        okr.setObjective(objective);
        okr.setCycleId(cycle.getId());
        okr.setOwnerId(owner.getId());
        okr.setProgress(progress);
        okr.setWeight(weight);
        okr.setStatus(progress >= 70 ? Okr.Status.ON_TRACK
                : progress >= 40 ? Okr.Status.AT_RISK : Okr.Status.OFF_TRACK);
        okrs.save(okr);
        for (String raw : keyResultSpecs) {
            String[] parts = raw.split(";");
            KeyResult keyResult = new KeyResult();
            keyResult.setOkrId(okr.getId());
            keyResult.setTitle(parts[0]);
            keyResult.setTargetValue(Double.parseDouble(parts[1]));
            keyResult.setCurrentValue(Double.parseDouble(parts[2]));
            keyResult.setUnit("%");
            keyResult.setOwnerId(owner.getId());
            keyResults.save(keyResult);
        }
        return okr;
    }

    private List<Task> seedTasks(TaskCycle cycle, Map<String, Okr> okrIndex, Map<String, User> people) {
        List<Task> created = new ArrayList<Task>();
        if (tasks.count() > 0) {
            return tasks.findAll();
        }
        Object[][] rows = {
                {"Build the Toolbox API for documents and slides", TaskStatus.DONE, TaskPriority.HIGH,
                        "yunteng", 16d, 14d, 6, "toolbox", "S04,api", false,
                        "Word and PPT builders are exposed through REST and verified with the quota check."},
                {"Implement the container runtime adapter", TaskStatus.IN_PROGRESS, TaskPriority.URGENT,
                        "yunteng", 12d, 8d, 2, "toolbox", "S04,docker", false,
                        "StartEnvironment returns the container id; heartbeat keeps the state fresh."},
                {"File sharing link with expiry and revocation", TaskStatus.REVIEW, TaskPriority.MEDIUM,
                        "yunteng", 6d, 5d, 1, "toolbox", "S04,files", false,
                        "CreateShareLink and RevokeShareLink implemented; waiting for the review pass."},
                {"Kanban board with drag and drop columns", TaskStatus.DONE, TaskPriority.HIGH,
                        "jinghui", 14d, 13d, 4, "dashboard", "S02,ui", false,
                        "Four columns with moveTask endpoint and optimistic UI updates."},
                {"File version history and rollback", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                        "jinghui", 10d, 7d, 3, "dashboard", "S02,files", false,
                        "HistoricalVersionController exposed; rollback verified on three files."},
                {"OKR performance calculator with grading", TaskStatus.TODO, TaskPriority.MEDIUM,
                        "jinghui", 8d, 0d, 6, "dashboard", "S02,okr", false,
                        "Weighted score of on-time rate, quality and OKR progress."},
                {"Group chat with @mention alerts", TaskStatus.DONE, TaskPriority.MEDIUM,
                        "changyang", 8d, 8d, 3, "collaboration", "S03,chat", false,
                        "Mention detector parses @username and dispatches targeted notifications."},
                {"AI meeting summary pipeline", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                        "changyang", 12d, 9d, 1, "collaboration", "S03,ai", false,
                        "Transcription and summary generated when the meeting status becomes ENDED."},
                {"Smart digest ranking of unread items", TaskStatus.TODO, TaskPriority.LOW,
                        "changyang", 6d, 0d, 9, "collaboration", "S03,digest", true,
                        "Ranking model documented; blocked until the notification volume is realistic."},
                {"Role and permission policy API", TaskStatus.DONE, TaskPriority.HIGH,
                        "hao", 12d, 12d, 7, "dashboard", "S01,security", false,
                        "Role assignment, policy overrides and the effective permission lookup."},
                {"Access audit trail of protected calls", TaskStatus.REVIEW, TaskPriority.MEDIUM,
                        "hao", 6d, 5d, 0, "dashboard", "S01,audit", false,
                        "Every /api call records the checked permission and the decision."},
                {"Level 4 class diagrams for all sub-systems", TaskStatus.DONE, TaskPriority.URGENT,
                        "manager", 20d, 22d, 10, "dashboard", "SA,L4", false,
                        "Interface plus _impl for every L3 component, dependencies backed by signatures."},
                {"Architecture execution: six sequence diagrams", TaskStatus.IN_PROGRESS, TaskPriority.URGENT,
                        "yunteng", 10d, 6d, 2, "toolbox", "SA,sequence", false,
                        "Scenarios follow the L4 contracts with alt/opt frames and symbolic error codes."},
                {"Demo script and walkthrough for the presentation", TaskStatus.TODO, TaskPriority.HIGH,
                        "manager", 5d, 0d, 4, "collaboration", "SA,demo", false,
                        "Run through the four sub-systems in ten minutes."}
        };
        int position = 0;
        for (Object[] row : rows) {
            Task task = new Task();
            task.setCode(String.format("STC-%03d", position + 1));
            task.setTitle((String) row[0]);
            task.setStatus((TaskStatus) row[1]);
            task.setPriority((TaskPriority) row[2]);
            task.setAssigneeId(people.get((String) row[3]).getId());
            task.setReporterId(people.get("manager").getId());
            task.setCycleId(cycle.getId());
            Okr okr = okrIndex.get((String) row[7]);
            task.setOkrId(okr == null ? null : okr.getId());
            task.setEstimateHours((Double) row[4]);
            task.setLoggedHours((Double) row[5]);
            task.setDueDate(LocalDate.now().plusDays((Integer) row[6]));
            task.setTags((String) row[8]);
            task.setBlocked((Boolean) row[9]);
            task.setDescription((String) row[10]);
            task.setPositionIndex(position++);
            tasks.save(task);
            created.add(task);
        }
        return created;
    }

    private void seedTaskFiles(List<Task> created, Map<String, User> people) {
        if (fileRecords.count() > 0 || created.isEmpty()) {
            return;
        }
        Object[][] rows = {
                {"Toolbox API contract v3.pdf", FileRecord.Bucket.DOCUMENT, 0, "manager", 428_000L, 3},
                {"Sequence diagrams - Toolbox.png", FileRecord.Bucket.DESIGN, 0, "yunteng", 962_000L, 2},
                {"toolbox-service.zip", FileRecord.Bucket.DEVELOPMENT, 0, "yunteng", 1_240_000L, 1},
                {"Kanban board screenshot.png", FileRecord.Bucket.DESIGN, 3, "jinghui", 512_000L, 1},
                {"File tracker design notes.md", FileRecord.Bucket.DOCUMENT, 4, "jinghui", 24_000L, 2},
                {"Chat mention test log.txt", FileRecord.Bucket.REPORT, 6, "changyang", 12_000L, 1},
                {"Meeting summary - cycle 4.docx", FileRecord.Bucket.REPORT, 7, "changyang", 88_000L, 1},
                {"Access audit export.csv", FileRecord.Bucket.REPORT, 10, "hao", 156_000L, 1}
        };
        for (Object[] row : rows) {
            FileRecord record = new FileRecord();
            record.setName((String) row[0]);
            record.setBucket((FileRecord.Bucket) row[1]);
            record.setTaskId(created.get(Math.min((Integer) row[2], created.size() - 1)).getId());
            record.setOwnerId(people.get((String) row[3]).getId());
            record.setContentType(contentTypeOf((String) row[0]));
            record.setSizeBytes((Long) row[4]);
            record.setChecksum(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
            int versions = (Integer) row[5];
            record.setCurrentVersion(versions);
            record.setUploadedAt(LocalDateTime.now().minusDays(6).plusHours(versions));
            fileRecords.save(record);

            for (int v = 1; v <= versions; v++) {
                FileVersion version = new FileVersion();
                version.setFileId(record.getId());
                version.setVersion(v);
                version.setSizeBytes(record.getSizeBytes() - (versions - v) * 4096L);
                version.setChecksum(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
                version.setChangeNote(v == 1 ? "Initial upload" : "Revision " + v
                        + " after review comments");
                version.setAuthorId(record.getOwnerId());
                version.setCreatedOn(record.getUploadedAt().plusDays(v - 1));
                version.setCurrent(v == versions);
                fileVersions.save(version);
            }
        }
    }

    private String contentTypeOf(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".zip")) {
            return "application/zip";
        }
        if (lower.endsWith(".md")) {
            return "text/markdown";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".csv")) {
            return "text/csv";
        }
        return "text/plain";
    }

    // ------------------------------------------------------------------ S03

    private void seedCollaboration(Map<String, User> people) {
        if (channels.count() > 0) {
            return;
        }
        Channel general = createChannel("general", "Team announcements",
                "Everything the whole team should see", people.get("lead"));
        Channel toolbox = createChannel("toolbox-s04", "Toolbox sub-system",
                "Word, PPT, Git, containers and project files", people.get("yunteng"));
        Channel dashboard = createChannel("task-dashboard-s02", "Task dashboard",
                "Kanban, files, OKR and performance", people.get("jinghui"));
        Channel identity = createChannel("identity-s01", "Identity and access",
                "Accounts, roles and permission policies", people.get("hao"));

        for (Channel channel : Arrays.asList(general, toolbox, dashboard, identity)) {
            for (User user : people.values()) {
                channelMembers.save(new ChannelMember(channel.getId(), user.getId()));
            }
        }

        post(general, people.get("lead"),
                "Welcome to the STC platform demo. The architecture is implemented end to end: "
                        + "S01 identity, S02 task dashboard, S03 collaboration and S04 toolbox.",
                Message.Kind.SYSTEM, null, people);
        post(toolbox, people.get("yunteng"),
                "The Toolbox API is live: documents, presentations, repositories and container "
                        + "environments. @manager the quota panel is ready for review.",
                Message.Kind.TEXT, null, people);
        post(toolbox, people.get("manager"),
                "Reviewed the contract. Please keep the symbolic error codes as documented in the "
                        + "sequence diagrams. @yunteng add the sharing link scenario to the demo.",
                Message.Kind.TEXT, null, people);
        post(dashboard, people.get("jinghui"),
                "Kanban board and file tracker are wired to the API. Version rollback works on "
                        + "the design documents. @manager the OKR indicators are on the display panel.",
                Message.Kind.TEXT, null, people);
        post(dashboard, people.get("manager"),
                "Good. I will review the performance scores before the Friday demo.",
                Message.Kind.TEXT, null, people);
        post(identity, people.get("hao"),
                "Role matrix and permission policies are seeded: three roles, eighteen permissions "
                        + "and two per-account overrides. @lead the access audit page shows every "
                        + "decision made by the authorization service.",
                Message.Kind.TEXT, null, people);
        post(general, people.get("changyang"),
                "Meeting notes uploaded after the cycle review.",
                Message.Kind.FILE, "Meeting summary - cycle 4.docx", people);
        post(general, people.get("lead"),
                "Cycle 4 ends on Friday. Focus on the toolbox demo and the sequence diagrams.",
                Message.Kind.TEXT, null, people);

        Meeting meeting = new Meeting();
        meeting.setTitle("Cycle 4 review - architecture execution");
        meeting.setAgenda("Board review, blockers, AI summary walkthrough and demo preparation");
        meeting.setChannelId(general.getId());
        meeting.setHostId(people.get("lead").getId());
        meeting.setStartAt(LocalDateTime.now().minusHours(4));
        meeting.setEndAt(LocalDateTime.now().minusHours(3));
        meeting.setStatus("ENDED");
        meeting.setJoinCode("STC-482913");
        meeting.setParticipantIds(joinIds(people.values()));
        meetings.save(meeting);

        Meeting planned = new Meeting();
        planned.setTitle("Demo rehearsal - full platform walkthrough");
        planned.setAgenda("Run the four sub-systems in ten minutes, then collect feedback");
        planned.setChannelId(general.getId());
        planned.setHostId(people.get("manager").getId());
        planned.setStartAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        planned.setStatus("SCHEDULED");
        planned.setJoinCode("STC-730145");
        planned.setParticipantIds(joinIds(people.values()));
        meetings.save(planned);

        seedTranscriptAndSummary(meeting, people);
        seedNotifications(people, general);
    }

    private Channel createChannel(String name, String topic, String purpose, User creator) {
        Channel channel = new Channel();
        channel.setName(name);
        channel.setTopic(topic);
        channel.setPurpose(purpose);
        channel.setCreatedBy(creator.getId());
        channels.save(channel);
        return channel;
    }

    private void post(Channel channel, User author, String body, Message.Kind kind,
                      String attachment, Map<String, User> people) {
        Message message = new Message();
        message.setChannelId(channel.getId());
        message.setAuthorId(author.getId());
        message.setBody(body);
        message.setKind(kind);
        message.setAttachmentName(attachment);
        message.setPostedAt(LocalDateTime.now().minusMinutes(90L - messages.count() * 7));
        messages.save(message);

        int index = body.indexOf('@');
        while (index >= 0) {
            int end = index + 1;
            while (end < body.length() && (Character.isLetterOrDigit(body.charAt(end))
                    || body.charAt(end) == '.' || body.charAt(end) == '_' || body.charAt(end) == '-')) {
                end++;
            }
            String username = body.substring(index + 1, end);
            User target = people.get(username);
            if (target != null && !target.getId().equals(author.getId())) {
                Mention mention = new Mention(message.getId(), channel.getId(), target.getId(), "@" + username);
                mention.setAlerted(true);
                mentions.save(mention);
                Notification notification = new Notification();
                notification.setRecipientId(target.getId());
                notification.setKind(Notification.Kind.MENTION);
                notification.setPriority(Notification.Priority.HIGH);
                notification.setTitle("You were mentioned in #" + channel.getName());
                notification.setBody(body.length() <= 160 ? body : body.substring(0, 157) + "...");
                notification.setLink("/collaboration?channel=" + channel.getId());
                notification.setDeliveredAt(message.getPostedAt().plusMinutes(1));
                notifications.save(notification);
            }
            index = body.indexOf('@', end);
        }
    }

    private void seedNotifications(Map<String, User> people, Channel general) {
        Object[][] rows = {
                {"jinghui", Notification.Kind.TASK, Notification.Priority.HIGH,
                        "Task STC-005 needs your review", "File version history is waiting for the review pass",
                        "/dashboard"},
                {"yunteng", Notification.Kind.FILE, Notification.Priority.NORMAL,
                        "Toolbox API contract v3.pdf updated", "Version 3 was uploaded by Nina Manager",
                        "/dashboard/files"},
                {"manager", Notification.Kind.MEETING, Notification.Priority.NORMAL,
                        "Demo rehearsal scheduled", "Tomorrow 10:00 - full platform walkthrough",
                        "/collaboration"},
                {"hao", Notification.Kind.SYSTEM, Notification.Priority.LOW,
                        "Permission policy updated", "S01.AUDIT.VIEW revoked for changyang",
                        "/identity"},
                {"changyang", Notification.Kind.TASK, Notification.Priority.NORMAL,
                        "Smart digest task is blocked", "STC-009 is flagged as blocked in the Kanban board",
                        "/dashboard"},
                {"yunteng", Notification.Kind.TASK, Notification.Priority.HIGH,
                        "Sequence diagrams due in 2 days", "STC-013 architecture execution",
                        "/dashboard"}
        };
        int offset = 0;
        for (Object[] row : rows) {
            Notification notification = new Notification();
            notification.setRecipientId(people.get((String) row[0]).getId());
            notification.setKind((Notification.Kind) row[1]);
            notification.setPriority((Notification.Priority) row[2]);
            notification.setTitle((String) row[3]);
            notification.setBody((String) row[4]);
            notification.setLink((String) row[5]);
            notification.setDeliveredAt(LocalDateTime.now().minusMinutes(30L + offset * 17));
            notifications.save(notification);
            offset++;
        }
    }

    private void seedTranscriptAndSummary(Meeting meeting, Map<String, User> people) {
        String[] lines = {
                "Let us review the current task board and the blockers of this cycle.",
                "We decided to move the toolbox integration tests to Friday so QA has a full pass.",
                "I will update the OKR progress for the toolbox sub-system today.",
                "The file sharing link flow still returns an expired token for large uploads.",
                "Agreed: the digest engine keeps @mentions at the top of the unread list.",
                "Action: prepare the demo script and the architecture walkthrough for the presentation.",
                "Next step is to freeze the API contract and tag the release candidate.",
                "Follow up with the lead about the permission policy for the senior manager role."
        };
        List<User> speakers = new ArrayList<User>(people.values());
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            User speaker = speakers.get(i % speakers.size());
            MeetingTranscript segment = new MeetingTranscript();
            segment.setMeetingId(meeting.getId());
            segment.setSpeakerId(speaker.getId());
            segment.setSpeakerName(speaker.getDisplayName());
            segment.setOffsetSeconds(offset);
            segment.setText(lines[i]);
            transcripts.save(segment);
            offset += 95;
        }
        List<String> decisions = new ArrayList<String>();
        List<String> actions = new ArrayList<String>();
        StringBuilder overview = new StringBuilder();
        for (MeetingTranscript segment
                : transcripts.findByMeetingIdOrderByOffsetSecondsAsc(meeting.getId())) {
            String lower = segment.getText().toLowerCase();
            if (lower.contains("decided") || lower.contains("agreed")) {
                decisions.add(segment.getSpeakerName() + ": " + segment.getText());
            }
            if (lower.contains("will ") || lower.contains("action") || lower.contains("next step")
                    || lower.contains("follow up")) {
                actions.add(segment.getSpeakerName() + ": " + segment.getText());
            }
            overview.append(segment.getSpeakerName()).append(": ").append(segment.getText()).append(' ');
        }
        MeetingSummary summary = new MeetingSummary();
        summary.setMeetingId(meeting.getId());
        summary.setOverview(overview.toString().trim());
        summary.setDecisions(join(decisions));
        summary.setActionItems(join(actions));
        summary.setModel("stc-summarizer-v1 (simulated)");
        summary.setConfidence(0.91d);
        // saved through the repository of the meeting summary
        summaries.save(summary);
    }

    private String joinIds(java.util.Collection<User> people) {
        StringBuilder builder = new StringBuilder();
        for (User user : people) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(user.getId());
        }
        return builder.toString();
    }

    private String join(List<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(item);
        }
        return builder.toString();
    }

    // ------------------------------------------------------------------ S04

    private void seedToolbox(Map<String, User> people) {
        if (documents.count() == 0) {
            DocDocument document = new DocDocument();
            document.setTitle("STC Platform - architecture overview");
            document.setContent("Smart Working Management App for small teams.\n"
                    + "Sub-systems: S01 identity and access, S02 task dashboard, "
                    + "S03 team collaboration, S04 toolbox.\n"
                    + "This document is edited through the Toolbox Office API.");
            document.setOwnerId(people.get("manager").getId());
            document.setTemplate("report");
            document.setRevision(3);
            document.setWordCount(38);
            document.setLastEditedAt(LocalDateTime.now().minusHours(5));
            documents.save(document);

            DocDocument script = new DocDocument();
            script.setTitle("Demo script - ten minute walkthrough");
            script.setContent("1. Sign in as the overall lead.\n"
                    + "2. Show the Kanban board and move a task.\n"
                    + "3. Open the collaboration channel and mention a teammate.\n"
                    + "4. Show the meeting summary generated by the AI assistant.\n"
                    + "5. Upload a project file and create a share link.\n"
                    + "6. Show the container environment and the access audit trail.");
            script.setOwnerId(people.get("lead").getId());
            script.setTemplate("blank");
            script.setRevision(1);
            script.setWordCount(58);
            script.setLastEditedAt(LocalDateTime.now().minusHours(2));
            documents.save(script);
        }

        if (presentations.count() == 0) {
            Presentation deck = new Presentation();
            deck.setTitle("STC Platform - architecture and execution");
            deck.setTheme("stc-dark");
            deck.setOwnerId(people.get("yunteng").getId());
            deck.setSlideCount(4);
            deck.setRevision(2);
            deck.setLastEditedAt(LocalDateTime.now().minusHours(4));
            presentations.save(deck);
            createSlide(deck.getId(), 1, "STC Platform", "Smart Working Management App for small teams;"
                    + "C4 model levels 1-4;Sequence diagrams as architecture execution");
            createSlide(deck.getId(), 2, "Sub-system decomposition",
                    "S01 identity, role and access;S02 task dashboard;S03 team collaboration;"
                            + "S04 toolbox");
            createSlide(deck.getId(), 3, "Runtime view",
                    "Frontend: React single page application;Backend: Spring Boot REST services;"
                            + "Database: H2 with JPA entities;External tools simulated in S04");
            createSlide(deck.getId(), 4, "Demo checklist",
                    "Kanban move;Mention notification;AI meeting summary;File share link;"
                            + "Container environment;Access audit trail");
        }

        if (repos.count() == 0) {
            GitRepo platform = createRepo("stc-platform", "sstc-platform", "Java",
                    "Full stack reference implementation of the STC architecture", people.get("yunteng"));
            GitRepo frontend = createRepo("stc-web", "sstc-web", "TypeScript",
                    "React dashboard of the STC platform", people.get("jinghui"));
            GitRepo docs = createRepo("stc-architecture", "sstc-architecture", "Markdown",
                    "C4 diagrams, sequence diagrams and design documents", people.get("manager"));
            createCommits(platform, people.get("yunteng"), new String[]{
                    "feat(toolbox): expose document editing with quota check",
                    "feat(toolbox): container runtime adapter with heartbeat",
                    "fix(files): reject uploads above the size limit",
                    "test(toolbox): cover the sharing link expiry path"});
            createCommits(frontend, people.get("jinghui"), new String[]{
                    "feat(board): drag and drop between Kanban columns",
                    "feat(files): version history drawer with rollback",
                    "style(ui): unify the sub-system colour palette"});
            createCommits(docs, people.get("manager"), new String[]{
                    "docs(sequence): six scenarios aligned with the L4 contracts",
                    "docs(c4): level 3 component descriptions",
                    "docs(api): document every REST endpoint"});
        }

        if (environments.count() == 0) {
            createEnvironment("toolbox-api", "RUNNING", people.get("yunteng"), "8080:8080");
            createEnvironment("dashboard-ui", "RUNNING", people.get("jinghui"), "5173:5173");
            createEnvironment("postgres-lab", "STOPPED", people.get("hao"), "5432:5432");
        }

        if (toolboxFiles.count() == 0) {
            Object[][] rows = {
                    {"architecture-overview.pdf", "/docs", "manager", 1_280_000L, true},
                    {"sequence-diagrams-toolbox.png", "/design", "yunteng", 962_000L, false},
                    {"api-contract-v3.md", "/docs", "yunteng", 32_000L, true},
                    {"demo-checklist.docx", "/demo", "manager", 88_000L, false}
            };
            for (Object[] row : rows) {
                ToolboxFile file = new ToolboxFile();
                file.setName((String) row[0]);
                file.setFolder((String) row[1]);
                file.setOwnerId(people.get((String) row[2]).getId());
                file.setSizeBytes((Long) row[3]);
                file.setChecksum(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
                file.setStatus("VALIDATED");
                file.setShared((Boolean) row[4]);
                file.setContentType(contentTypeOf((String) row[0]));
                file.setUploadedAt(LocalDateTime.now().minusDays(3));
                toolboxFiles.save(file);
            }
        }
    }

    private GitRepo createRepo(String name, String slug, String language, String description, User owner) {
        GitRepo repo = new GitRepo();
        repo.setName(name);
        repo.setSlug(slug);
        repo.setLanguage(language);
        repo.setDescription(description);
        repo.setProvider("GitHub (simulated)");
        repo.setDefaultBranch("main");
        repo.setRemoteUrl("https://git.example.com/stc/" + slug + ".git");
        repo.setVisibility("PRIVATE");
        repo.setStars(3);
        repo.setOpenPullRequests(1);
        repo.setLastSyncAt(LocalDateTime.now().minusHours(6));
        repos.save(repo);
        return repo;
    }

    private void createCommits(GitRepo repo, User author, String[] messages) {
        int index = 0;
        for (String message : messages) {
            CommitRecord commit = new CommitRecord();
            commit.setRepoId(repo.getId());
            commit.setSha(UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            commit.setBranch(repo.getDefaultBranch());
            commit.setMessage(message);
            commit.setAuthorId(author.getId());
            commit.setAuthorName(author.getDisplayName());
            commit.setCommittedAt(LocalDateTime.now().minusHours(6L + index * 5L));
            commit.setAdditions(40 + index * 17);
            commit.setDeletions(8 + index * 3);
            commit.setChangedFiles(1 + index);
            commits.save(commit);
            index++;
        }
    }

    private void createEnvironment(String serviceId, String state, User owner, String ports) {
        DevEnvironment environment = new DevEnvironment();
        environment.setServiceId(serviceId);
        environment.setContainerId("ctr-" + UUID.randomUUID().toString().substring(0, 12));
        environment.setImage("sstc/" + serviceId + ":1.0");
        environment.setState(state);
        environment.setCpuLimit(1.5d);
        environment.setMemoryMb(2048);
        environment.setPorts(ports);
        environment.setOwnerId(owner.getId());
        environment.setStartedAt("RUNNING".equals(state) ? LocalDateTime.now().minusHours(2) : null);
        environment.setLastHeartbeat(LocalDateTime.now().minusMinutes(2));
        environments.save(environment);
    }

    private void createSlide(Long presentationId, int number, String title, String bullets) {
        Slide slide = new Slide();
        slide.setPresentationId(presentationId);
        slide.setSlideNo(number);
        slide.setTitle(title);
        slide.setBullets(bullets);
        slide.setNotes("Auto generated by the seeded demo data.");
        slide.setLayout("title-content");
        slides.save(slide);
    }

    // ------------------------------------------------------------------ S02 audit

    private void seedOperationLog(Map<String, User> people, List<Task> created) {
        if (operationLogs.count() > 0 || created.isEmpty()) {
            return;
        }
        Object[][] rows = {
                {"TASK_CREATE", "STC-013", "yunteng", "Architecture execution: six sequence diagrams"},
                {"TASK_MOVE", "STC-002", "yunteng", "TODO -> IN_PROGRESS"},
                {"FILE_UPLOAD", "Toolbox API contract v3.pdf", "manager", "v3 stored in the document bucket"},
                {"TASK_UPDATE", "STC-005", "jinghui", "loggedHours=7.0"},
                {"FILE_VERSION", "Sequence diagrams - Toolbox.png", "yunteng", "v2 after review comments"},
                {"TASK_CREATE", "STC-014", "manager", "Demo script and walkthrough for the presentation"}
        };
        int index = 0;
        for (Object[] row : rows) {
            OperationLog log = new OperationLog();
            String action = (String) row[0];
            log.setAction(action);
            log.setTargetId((String) row[1]);
            log.setTargetType(action.startsWith("TASK") ? "task" : "file");
            log.setActorId(people.get((String) row[2]).getId());
            log.setActorName(people.get((String) row[2]).getDisplayName());
            log.setDetail((String) row[3]);
            log.setOutcome("SUCCESS");
            log.setSource(index % 3 == 0 ? "API" : "UI");
            operationLogs.save(log);
            index++;
        }
    }
}
