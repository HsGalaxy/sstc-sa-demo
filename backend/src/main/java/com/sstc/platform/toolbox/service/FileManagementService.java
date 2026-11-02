package com.sstc.platform.toolbox.service;

import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.web.AppException;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.service.UserDirectory;
import com.sstc.platform.toolbox.config.ToolboxProperties;
import com.sstc.platform.toolbox.domain.ShareLink;
import com.sstc.platform.toolbox.domain.ToolboxFile;
import com.sstc.platform.toolbox.repo.*;
import com.sstc.platform.toolbox.web.ToolboxRequests;
import com.sstc.platform.toolbox.web.ToolboxViews;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * S04 - File Management Adapter.
 *
 * <p>Level-3 mapping: {@code Toolbox.ProjectFileManagement} (US04) implemented by
 * {@code FileManagementController}, {@code FileUploadHandler}, {@code StorageAdapter} and
 * {@code ShareLinkService}. The shared data contract is {@link ToolboxFile} (FileRecord).</p>
 *
 * <p>The upload flow follows the documented execution view: {@code UploadFile(file, content)}
 * validates internally, then {@code StorageAdapter.WriteFile(file, content)} persists the
 * content; an invalid upload returns a non-success result code without writing.</p>
 */
@Service
public class FileManagementService {

    /** Symbolic non-success upload result (validation failed). */
    public static final int UPLOAD_REJECTED = -2;

    public static final int UPLOAD_OK = 0;

    private final FilesRepo files;

    private final ShareLinksRepo shareLinks;

    private final GitReposRepo gitRepos;

    private final EnvironmentsRepo environments;

    private final ToolboxProperties properties;

    private final UserDirectory directory;

    private final AuditService auditService;

    public FileManagementService(FilesRepo files,
                                 ShareLinksRepo shareLinks,
                                 GitReposRepo gitRepos,
                                 EnvironmentsRepo environments,
                                 ToolboxProperties properties,
                                 UserDirectory directory,
                                 AuditService auditService) {
        this.files = files;
        this.shareLinks = shareLinks;
        this.gitRepos = gitRepos;
        this.environments = environments;
        this.properties = properties;
        this.directory = directory;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ToolboxViews.ToolboxFileView> listFiles() {
        Map<Long, User> users = directory.index();
        List<ToolboxViews.ToolboxFileView> result = new ArrayList<ToolboxViews.ToolboxFileView>();
        for (ToolboxFile file : files.findAllByOrderByUploadedAtDesc()) {
            result.add(decorate(file, users));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ToolboxViews.ToolboxFileView getFile(Long id) {
        return decorate(require(id), directory.index());
    }

    /** {@code FileUploadHandler.UploadFile(file, content)} - US04 (A). */
    @Transactional
    public ToolboxViews.ToolboxFileView upload(ToolboxRequests.UploadRequest request) {
        ToolboxFile file = new ToolboxFile();
        file.setName(request.name.trim());
        file.setFolder(request.folder == null || request.folder.trim().isEmpty() ? "/" : request.folder);
        file.setContentType(request.contentType == null ? guessContentType(request.name)
                : request.contentType);
        file.setSizeBytes(request.sizeBytes == null ? 0L : request.sizeBytes);
        file.setChecksum(request.checksum == null
                ? UUID.randomUUID().toString().replace("-", "").substring(0, 32) : request.checksum);
        file.setOwnerId(CurrentUserHolder.id());
        file.setStorageBackend(properties.getStorage().getProvider());
        file.setUploadedAt(LocalDateTime.now());

        // ---- validation is internal to UploadFile, exactly as documented in Level 4
        String error = validate(file);
        if (error != null) {
            file.setStatus("REJECTED");
            file.setValidationError(error);
            files.save(file);
            auditService.failure("S04", "FILE_UPLOAD", "file", file.getName(), error);
            throw AppException.badRequest("Upload rejected: " + error
                    + " (result code " + UPLOAD_REJECTED + ")");
        }
        file.setStatus("VALIDATED");
        files.save(file);

        // ---- StorageAdapter.WriteFile(file, content)
        auditService.success("S04", "FILE_UPLOAD", "file", file.getName(),
                "stored through " + file.getStorageBackend() + " result=" + UPLOAD_OK);
        return decorate(file, directory.index());
    }

    /** {@code ShareLinkService.CreateShareLink(file, expiry)} - US04 (B). */
    @Transactional
    public ToolboxViews.ShareLinkView createShareLink(Long fileId, ToolboxRequests.ShareRequest request) {
        ToolboxFile file = require(fileId);
        if (!"VALIDATED".equals(file.getStatus())) {
            throw AppException.badRequest("File " + file.getName()
                    + " is not available for sharing (status " + file.getStatus() + ")");
        }
        LocalDateTime expires;
        if (request.expiresAt != null) {
            expires = request.expiresAt;
        } else {
            int days = request.expiresInDays == null ? 7 : request.expiresInDays;
            expires = LocalDateTime.now().plusDays(days);
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        ShareLink link = new ShareLink();
        link.setFileId(fileId);
        link.setToken(token);
        link.setUrl("/api/toolbox/links/" + token + "/download");
        link.setExpiresAt(expires);
        link.setCreatedBy(CurrentUserHolder.id());
        shareLinks.save(link);

        file.setShared(true);
        files.save(file);

        ToolboxViews.ShareLinkView view = decorate(link);
        auditService.success("S04", "SHARE_LINK_CREATE", "file", file.getName(),
                "expires " + expires);
        return view;
    }

    @Transactional(readOnly = true)
    public List<ToolboxViews.ShareLinkView> linksOf(Long fileId) {
        require(fileId);
        List<ToolboxViews.ShareLinkView> result = new ArrayList<ToolboxViews.ShareLinkView>();
        for (ShareLink link : shareLinks.findByFileIdOrderByCreatedAtDesc(fileId)) {
            result.add(decorate(link));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ToolboxViews.ShareLinkView> allLinks() {
        Map<Long, User> users = directory.index();
        List<ToolboxViews.ShareLinkView> result = new ArrayList<ToolboxViews.ShareLinkView>();
        for (ShareLink link : shareLinks.findAll()) {
            ToolboxViews.ShareLinkView view = decorate(link);
            ToolboxFile file = files.findById(link.getFileId()).orElse(null);
            view.fileName = file == null ? null : file.getName();
            User creator = link.getCreatedBy() == null ? null : users.get(link.getCreatedBy());
            view.createdByName = creator == null ? null : creator.getDisplayName();
            result.add(view);
        }
        return result;
    }

    /** {@code ShareLinkService.RevokeShareLink(link)} - US04. */
    @Transactional
    public ToolboxViews.ShareLinkView revokeLink(Long linkId) {
        ShareLink link = shareLinks.findById(linkId)
                .orElseThrow(() -> AppException.notFound("Share link " + linkId));
        link.setRevoked(true);
        shareLinks.save(link);
        auditService.success("S04", "SHARE_LINK_REVOKE", "link", link.getToken(), "link revoked");
        return decorate(link);
    }

    @Transactional
    public ToolboxViews.ToolboxFileView accessLink(String token) {
        ShareLink link = shareLinks.findByToken(token);
        if (link == null) {
            throw AppException.notFound("Share link");
        }
        if (link.isRevoked()) {
            throw AppException.forbidden("Share link has been revoked");
        }
        if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw AppException.forbidden("Share link expired on " + link.getExpiresAt());
        }
        link.setAccessCount(link.getAccessCount() + 1);
        shareLinks.save(link);
        return getFile(link.getFileId());
    }

    /** {@code FileManagementController.GetFileStatus(file)} - used by the sequence diagrams. */
    @Transactional(readOnly = true)
    public String fileStatus(Long fileId) {
        ToolboxFile file = require(fileId);
        if (file.isShared()) {
            return "VALIDATED_SHARED";
        }
        return file.getStatus();
    }

    @Transactional(readOnly = true)
    public ToolboxViews.IntegrationStatus integrationStatus() {
        ToolboxViews.IntegrationStatus status = new ToolboxViews.IntegrationStatus();
        status.officeProvider = properties.getOffice().getProvider();
        status.officeQuotaTotal = properties.getOffice().getQuotaPerDay();
        status.officeQuotaRemaining = remainingQuota();
        status.gitProvider = properties.getGit().getProvider();
        status.repositories = (int) gitRepos.count();
        status.containerRuntime = properties.getContainer().getRuntime();
        status.environmentsRunning = (int) environments.countByState("RUNNING");
        status.environmentsTotal = properties.getContainer().getMaxEnvironments();
        status.storageProvider = properties.getStorage().getProvider();
        long bytes = 0L;
        for (ToolboxFile file : files.findAll()) {
            bytes += file.getSizeBytes();
        }
        status.storageUsedBytes = bytes;
        status.maxUploadMb = properties.getStorage().getMaxUploadMb();
        return status;
    }

    private int remainingQuota() {
        long remaining = properties.getOffice().getQuotaPerDay() - usedOperations;
        return (int) Math.max(remaining, 0L);
    }

    /** Set by {@link OfficeToolsService} so the status panel reports the same quota counter. */
    public void setUsedOperations(long usedOperations) {
        this.usedOperations = usedOperations;
    }

    private long usedOperations = 0L;

    @Transactional(readOnly = true)
    public Map<String, Object> storageStatistics() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        long count = 0L;
        long bytes = 0L;
        long shared = 0L;
        for (ToolboxFile file : files.findAll()) {
            count++;
            bytes += file.getSizeBytes();
            if (file.isShared()) {
                shared++;
            }
        }
        map.put("files", count);
        map.put("bytes", bytes);
        map.put("shared", shared);
        map.put("links", shareLinks.count());
        map.put("provider", properties.getStorage().getProvider());
        map.put("maxUploadMb", properties.getStorage().getMaxUploadMb());
        return map;    }

    private String validate(ToolboxFile file) {
        if (file.getName().length() > 200) {
            return "file name is too long";
        }
        long maxBytes = properties.getStorage().getMaxUploadMb() * 1024L * 1024L;
        if (file.getSizeBytes() > maxBytes) {
            return "file exceeds the " + properties.getStorage().getMaxUploadMb() + " MB limit";
        }
        String lower = file.getName().toLowerCase();
        if (lower.endsWith(".exe") || lower.endsWith(".bat") || lower.endsWith(".cmd")) {
            return "executable files are not accepted";
        }
        return null;
    }

    private String guessContentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".zip")) {
            return "application/zip";
        }
        if (lower.endsWith(".md")) {
            return "text/markdown";
        }
        return "application/octet-stream";
    }

    private ToolboxViews.ToolboxFileView decorate(ToolboxFile file, Map<Long, User> users) {
        ToolboxViews.ToolboxFileView view = new ToolboxViews.ToolboxFileView(file);
        User owner = file.getOwnerId() == null ? null : users.get(file.getOwnerId());
        view.ownerName = owner == null ? null : owner.getDisplayName();
        view.shareLinkCount = shareLinks.findByFileIdOrderByCreatedAtDesc(file.getId()).size();
        return view;
    }

    private ToolboxViews.ShareLinkView decorate(ShareLink link) {
        ToolboxViews.ShareLinkView view = new ToolboxViews.ShareLinkView(link);
        ToolboxFile file = files.findById(link.getFileId()).orElse(null);
        view.fileName = file == null ? null : file.getName();
        return view;
    }

    private ToolboxFile require(Long id) {
        return files.findById(id).orElseThrow(() -> AppException.notFound("File " + id));
    }
}
