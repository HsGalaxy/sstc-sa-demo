package com.sstc.platform.toolbox.service;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.web.AppException;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.service.UserDirectory;
import com.sstc.platform.toolbox.config.ToolboxProperties;
import com.sstc.platform.toolbox.domain.CommitRecord;
import com.sstc.platform.toolbox.domain.DevEnvironment;
import com.sstc.platform.toolbox.domain.GitRepo;
import com.sstc.platform.toolbox.repo.*;
import com.sstc.platform.toolbox.web.ToolboxRequests;
import com.sstc.platform.toolbox.web.ToolboxViews;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * S04 - Development Tools Container.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>{@code Toolbox.VersionControl} - repositories, commits and publishing (US03)</li>
 *   <li>{@code Toolbox.ContainerRuntime} - containerised development environments (US05)</li>
 * </ul>
 */
@Service
public class DevelopmentToolsService {

    private final GitReposRepo repos;

    private final CommitsRepo commits;

    private final EnvironmentsRepo environments;

    private final ToolboxProperties properties;

    private final UserDirectory directory;

    private final AuditService auditService;

    public DevelopmentToolsService(GitReposRepo repos,
                                   CommitsRepo commits,
                                   EnvironmentsRepo environments,
                                   ToolboxProperties properties,
                                   UserDirectory directory,
                                   AuditService auditService) {
        this.repos = repos;
        this.commits = commits;
        this.environments = environments;
        this.properties = properties;
        this.directory = directory;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ US03

    @Transactional(readOnly = true)
    public List<ToolboxViews.RepoView> listRepos() {
        List<ToolboxViews.RepoView> result = new ArrayList<ToolboxViews.RepoView>();
        for (GitRepo repo : repos.findAllByOrderByLastSyncAtDesc()) {
            result.add(decorate(repo));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ToolboxViews.RepoView getRepo(Long id) {
        return decorate(require(id));
    }

    @Transactional
    public ToolboxViews.RepoView createRepo(ToolboxRequests.RepoRequest request) {
        GitRepo repo = new GitRepo();
        repo.setName(request.name.trim());
        repo.setSlug(request.slug == null || request.slug.trim().isEmpty()
                ? slugify(request.name) : request.slug.trim());
        repo.setLanguage(request.language == null ? "Java" : request.language);
        repo.setDescription(request.description);
        repo.setVisibility(request.visibility == null ? "PRIVATE" : request.visibility.toUpperCase());
        repo.setProvider(properties.getGit().getProvider());
        repo.setDefaultBranch(properties.getGit().getDefaultBranch());
        repo.setRemoteUrl("https://git.example.com/stc/" + repo.getSlug() + ".git");
        repo.setLastSyncAt(LocalDateTime.now());
        repos.save(repo);
        auditService.success("S04", "REPO_CREATE", "repo", repo.getSlug(), repo.getName());
        return decorate(repo);
    }

    /** {@code VersionControlService.GetHistory(repoID, branch)} - US03. */
    @Transactional(readOnly = true)
    public List<ToolboxViews.CommitView> history(Long repoId) {
        require(repoId);
        Map<Long, User> users = directory.index();
        List<ToolboxViews.CommitView> result = new ArrayList<ToolboxViews.CommitView>();
        for (CommitRecord commit : commits.findByRepoIdOrderByCommittedAtDesc(repoId)) {
            ToolboxViews.CommitView view = new ToolboxViews.CommitView(commit);
            User author = commit.getAuthorId() == null ? null : users.get(commit.getAuthorId());
            if (author != null) {
                view.authorName = author.getDisplayName();
            }
            result.add(view);
        }
        return result;
    }

    /**
     * {@code VersionControlService.Commit(changes)} + {@code GitRepositoryAdapter.Push(...)}.
     * Returns an integer result code, {@code 0} meaning success, exactly like the contract used
     * in the sequence diagrams.
     */
    @Transactional
    public ToolboxViews.CommitView push(Long repoId, ToolboxRequests.PushRequest request) {
        GitRepo repo = require(repoId);
        if (request.message == null || request.message.trim().isEmpty()) {
            throw AppException.badRequest("Commit message is required");
        }
        CommitRecord commit = new CommitRecord();
        commit.setRepoId(repoId);
        commit.setSha(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8));
        commit.setBranch(request.branch == null ? repo.getDefaultBranch() : request.branch);
        commit.setMessage(request.message.trim());
        commit.setAuthorId(CurrentUserHolder.id());
        commit.setAuthorName(CurrentUserHolder.name());
        commit.setCommittedAt(LocalDateTime.now());
        Random random = new Random();
        commit.setAdditions(request.additions == null ? 10 + random.nextInt(120) : request.additions);
        commit.setDeletions(request.deletions == null ? random.nextInt(40) : request.deletions);
        commit.setChangedFiles(request.changedFiles == null ? 1 + random.nextInt(6) : request.changedFiles);
        commits.save(commit);

        repo.setLastSyncAt(LocalDateTime.now());
        repos.save(repo);

        ToolboxViews.CommitView view = new ToolboxViews.CommitView(commit);
        view.authorName = commit.getAuthorName();
        auditService.success("S04", "REPO_PUSH", "repo", repo.getSlug(),
                view.shortSha + " " + commit.getMessage());
        return view;
    }

    @Transactional(readOnly = true)
    public List<ToolboxViews.CommitView> recentCommits() {
        List<ToolboxViews.CommitView> result = new ArrayList<ToolboxViews.CommitView>();
        for (CommitRecord commit : commits.findTop20ByOrderByCommittedAtDesc()) {
            result.add(new ToolboxViews.CommitView(commit));
        }
        return result;
    }

    // ------------------------------------------------------------------ US05

    @Transactional(readOnly = true)
    public List<ToolboxViews.EnvironmentView> listEnvironments() {
        Map<Long, User> users = directory.index();
        List<ToolboxViews.EnvironmentView> result = new ArrayList<ToolboxViews.EnvironmentView>();
        for (DevEnvironment environment : environments.findAllByOrderByCreatedAtDesc()) {
            result.add(decorate(environment, users));
        }
        return result;
    }

    /** {@code DevelopmentToolsController.RouteRequest(request)} + {@code StartEnvironment}. */
    @Transactional
    public ToolboxViews.EnvironmentView startEnvironment(ToolboxRequests.EnvironmentRequest request) {
        long running = environments.countByState("RUNNING");
        if (running >= properties.getContainer().getMaxEnvironments()) {
            throw AppException.conflict("Container runtime limit reached ("
                    + properties.getContainer().getMaxEnvironments() + " environments)");
        }
        DevEnvironment environment = environments.findByServiceId(request.serviceId);
        if (environment == null) {
            environment = new DevEnvironment();
            environment.setServiceId(request.serviceId);
            environment.setContainerId("ctr-" + UUID.randomUUID().toString().substring(0, 12));
            environment.setOwnerId(CurrentUserHolder.id());
        }
        environment.setImage(request.image == null ? "sstc/dev-env:1.0" : request.image);
        environment.setCpuLimit(request.cpuLimit == null ? 1.0d : request.cpuLimit);
        environment.setMemoryMb(request.memoryMb == null ? 1024 : request.memoryMb);
        environment.setPorts(request.ports == null ? "8080:8080" : request.ports);
        environment.setState("RUNNING");
        environment.setStartedAt(LocalDateTime.now());
        environment.setLastHeartbeat(LocalDateTime.now());
        environments.save(environment);
        auditService.success("S04", "ENVIRONMENT_START", "environment", environment.getContainerId(),
                "service=" + environment.getServiceId());
        return decorate(environment, directory.index());
    }

    @Transactional
    public ToolboxViews.EnvironmentView stopEnvironment(String containerId) {
        DevEnvironment environment = environments.findByContainerId(containerId);
        if (environment == null) {
            throw AppException.notFound("Environment " + containerId);
        }
        environment.setState("STOPPED");
        environment.setLastHeartbeat(LocalDateTime.now());
        environments.save(environment);
        auditService.success("S04", "ENVIRONMENT_STOP", "environment", containerId, "stopped");
        return decorate(environment, directory.index());
    }

    @Transactional(readOnly = true)
    public String serviceStatus(String serviceId) {
        DevEnvironment environment = environments.findByServiceId(serviceId);
        if (environment == null) {
            return "NOT_CREATED";
        }
        return environment.getState();
    }

    @Transactional(readOnly = true)
    public String environmentStatus(String containerId) {
        DevEnvironment environment = environments.findByContainerId(containerId);
        return environment == null ? "UNKNOWN" : environment.getState();
    }

    @Transactional
    public int heartbeat() {
        int touched = 0;
        for (DevEnvironment environment : environments.findAll()) {
            if ("RUNNING".equals(environment.getState())) {
                environment.setLastHeartbeat(LocalDateTime.now());
                environments.save(environment);
                touched++;
            }
        }
        return touched;
    }

    private ToolboxViews.RepoView decorate(GitRepo repo) {
        ToolboxViews.RepoView view = new ToolboxViews.RepoView(repo);
        List<CommitRecord> history = commits.findByRepoIdOrderByCommittedAtDesc(repo.getId());
        view.commitCount = history.size();
        if (!history.isEmpty()) {
            view.lastCommitMessage = history.get(0).getMessage();
            view.lastCommitAt = history.get(0).getCommittedAt();
        }
        return view;
    }

    private ToolboxViews.EnvironmentView decorate(DevEnvironment environment, Map<Long, User> users) {
        ToolboxViews.EnvironmentView view = new ToolboxViews.EnvironmentView(environment);
        User owner = environment.getOwnerId() == null ? null : users.get(environment.getOwnerId());
        view.ownerName = owner == null ? null : owner.getDisplayName();
        return view;
    }

    private GitRepo require(Long id) {
        return repos.findById(id).orElseThrow(() -> AppException.notFound("Repository " + id));
    }

    private String slugify(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
