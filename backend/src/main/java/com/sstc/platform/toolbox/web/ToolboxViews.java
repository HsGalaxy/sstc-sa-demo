package com.sstc.platform.toolbox.web;

import com.sstc.platform.toolbox.domain.CommitRecord;
import com.sstc.platform.toolbox.domain.DevEnvironment;
import com.sstc.platform.toolbox.domain.DocDocument;
import com.sstc.platform.toolbox.domain.DocEdit;
import com.sstc.platform.toolbox.domain.GitRepo;
import com.sstc.platform.toolbox.domain.Presentation;
import com.sstc.platform.toolbox.domain.ShareLink;
import com.sstc.platform.toolbox.domain.Slide;
import com.sstc.platform.toolbox.domain.ToolboxFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * S04 - API projections for the Toolbox sub-system.
 */
public final class ToolboxViews {

    private ToolboxViews() {
    }

    /** Online Word document (US01). */
    public static class DocumentView {

        public Long id;

        public String title;

        public String content;

        public Long ownerId;

        public String ownerName;

        public String template;

        public int revision;

        public int wordCount;

        public LocalDateTime lastEditedAt;

        public String officeApi;

        public DocumentView(DocDocument document) {
            this.id = document.getId();
            this.title = document.getTitle();
            this.content = document.getContent();
            this.ownerId = document.getOwnerId();
            this.template = document.getTemplate();
            this.revision = document.getRevision();
            this.wordCount = document.getWordCount();
            this.lastEditedAt = document.getLastEditedAt();
            this.officeApi = document.getOfficeApi();
        }
    }

    /** Applied edit operation (US01). */
    public static class DocEditView {

        public Long id;

        public Long documentId;

        public Long editorId;

        public String editorName;

        public String operation;

        public String payload;

        public int resultCode;

        public int quotaRemaining;

        public LocalDateTime appliedAt;

        public DocEditView(DocEdit edit) {
            this.id = edit.getId();
            this.documentId = edit.getDocumentId();
            this.editorId = edit.getEditorId();
            this.operation = edit.getOperation();
            this.payload = edit.getPayload();
            this.resultCode = edit.getResultCode();
            this.quotaRemaining = edit.getQuotaRemaining();
            this.appliedAt = edit.getAppliedAt();
        }
    }

    /** Slide deck (US02). */
    public static class PresentationView {

        public Long id;

        public String title;

        public String theme;

        public Long ownerId;

        public String ownerName;

        public int slideCount;

        public int revision;

        public LocalDateTime lastEditedAt;

        public String officeApi;

        public List<SlideView> slides;

        public PresentationView(Presentation presentation) {
            this.id = presentation.getId();
            this.title = presentation.getTitle();
            this.theme = presentation.getTheme();
            this.ownerId = presentation.getOwnerId();
            this.slideCount = presentation.getSlideCount();
            this.revision = presentation.getRevision();
            this.lastEditedAt = presentation.getLastEditedAt();
            this.officeApi = presentation.getOfficeApi();
        }
    }

    public static class SlideView {

        public Long id;

        public Long presentationId;

        public int slideNo;

        public String title;

        public List<String> bullets;

        public String notes;

        public String layout;

        public SlideView(Slide slide) {
            this.id = slide.getId();
            this.presentationId = slide.getPresentationId();
            this.slideNo = slide.getSlideNo();
            this.title = slide.getTitle();
            this.bullets = slide.getBullets() == null || slide.getBullets().isEmpty()
                    ? Collections.<String>emptyList()
                    : Arrays.asList(slide.getBullets().split("\\s*;\\s*"));
            this.notes = slide.getNotes();
            this.layout = slide.getLayout();
        }
    }

    /** Git repository (US03). */
    public static class RepoView {

        public Long id;

        public String name;

        public String slug;

        public String provider;

        public String remoteUrl;

        public String defaultBranch;

        public String language;

        public String visibility;

        public String description;

        public int stars;

        public int openPullRequests;

        public LocalDateTime lastSyncAt;

        public int commitCount;

        public String lastCommitMessage;

        public LocalDateTime lastCommitAt;

        public RepoView(GitRepo repo) {
            this.id = repo.getId();
            this.name = repo.getName();
            this.slug = repo.getSlug();
            this.provider = repo.getProvider();
            this.remoteUrl = repo.getRemoteUrl();
            this.defaultBranch = repo.getDefaultBranch();
            this.language = repo.getLanguage();
            this.visibility = repo.getVisibility();
            this.description = repo.getDescription();
            this.stars = repo.getStars();
            this.openPullRequests = repo.getOpenPullRequests();
            this.lastSyncAt = repo.getLastSyncAt();
        }
    }

    public static class CommitView {

        public Long id;

        public Long repoId;

        public String sha;

        public String shortSha;

        public String branch;

        public String message;

        public Long authorId;

        public String authorName;

        public LocalDateTime committedAt;

        public int additions;

        public int deletions;

        public int changedFiles;

        public CommitView(CommitRecord commit) {
            this.id = commit.getId();
            this.repoId = commit.getRepoId();
            this.sha = commit.getSha();
            this.shortSha = commit.getSha() == null ? "" :
                    commit.getSha().substring(0, Math.min(7, commit.getSha().length()));
            this.branch = commit.getBranch();
            this.message = commit.getMessage();
            this.authorId = commit.getAuthorId();
            this.authorName = commit.getAuthorName();
            this.committedAt = commit.getCommittedAt();
            this.additions = commit.getAdditions();
            this.deletions = commit.getDeletions();
            this.changedFiles = commit.getChangedFiles();
        }
    }

    /** Containerised development environment (US05). */
    public static class EnvironmentView {

        public Long id;

        public String serviceId;

        public String containerId;

        public String image;

        public String state;

        public double cpuLimit;

        public int memoryMb;

        public String ports;

        public Long ownerId;

        public String ownerName;

        public LocalDateTime startedAt;

        public LocalDateTime lastHeartbeat;

        public long uptimeMinutes;

        public EnvironmentView(DevEnvironment environment) {
            this.id = environment.getId();
            this.serviceId = environment.getServiceId();
            this.containerId = environment.getContainerId();
            this.image = environment.getImage();
            this.state = environment.getState();
            this.cpuLimit = environment.getCpuLimit();
            this.memoryMb = environment.getMemoryMb();
            this.ports = environment.getPorts();
            this.ownerId = environment.getOwnerId();
            this.startedAt = environment.getStartedAt();
            this.lastHeartbeat = environment.getLastHeartbeat();
            this.uptimeMinutes = environment.getStartedAt() == null ? 0L
                    : java.time.Duration.between(environment.getStartedAt(), LocalDateTime.now()).toMinutes();
        }
    }

    /** Stored project file (US04). */
    public static class ToolboxFileView {

        public Long id;

        public String name;

        public String folder;

        public String contentType;

        public long sizeBytes;

        public String checksum;

        public Long ownerId;

        public String ownerName;

        public String status;

        public String storageBackend;

        public String validationError;

        public LocalDateTime uploadedAt;

        public boolean shared;

        public int shareLinkCount;

        public ToolboxFileView(ToolboxFile file) {
            this.id = file.getId();
            this.name = file.getName();
            this.folder = file.getFolder();
            this.contentType = file.getContentType();
            this.sizeBytes = file.getSizeBytes();
            this.checksum = file.getChecksum();
            this.ownerId = file.getOwnerId();
            this.status = file.getStatus();
            this.storageBackend = file.getStorageBackend();
            this.validationError = file.getValidationError();
            this.uploadedAt = file.getUploadedAt();
            this.shared = file.isShared();
        }
    }

    /** Sharing link (US04). */
    public static class ShareLinkView {

        public Long id;

        public Long fileId;

        public String fileName;

        public String token;

        public String url;

        public LocalDateTime expiresAt;

        public Long createdBy;

        public String createdByName;

        public boolean revoked;

        public int accessCount;

        public boolean expired;

        public ShareLinkView(ShareLink link) {
            this.id = link.getId();
            this.fileId = link.getFileId();
            this.token = link.getToken();
            this.url = link.getUrl();
            this.expiresAt = link.getExpiresAt();
            this.createdBy = link.getCreatedBy();
            this.revoked = link.isRevoked();
            this.accessCount = link.getAccessCount();
            this.expired = link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now());
        }
    }

    /** Quota / integration status of the external tools (Toolbox status panel). */
    public static class IntegrationStatus {

        public String officeProvider;

        public int officeQuotaRemaining;

        public int officeQuotaTotal;

        public String gitProvider;

        public int repositories;

        public String containerRuntime;

        public int environmentsRunning;

        public int environmentsTotal;

        public String storageProvider;

        public long storageUsedBytes;

        public int maxUploadMb;
    }
}
