package com.sstc.platform.toolbox.web;

import com.sstc.platform.toolbox.service.DevelopmentToolsService;
import com.sstc.platform.toolbox.service.FileManagementService;
import com.sstc.platform.toolbox.service.OfficeToolsService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * S04 - Toolbox REST surface: Office tools, development tools and file management.
 */
@RestController
@RequestMapping("/api/toolbox")
public class ToolboxController {

    private final OfficeToolsService officeToolsService;

    private final DevelopmentToolsService developmentToolsService;

    private final FileManagementService fileManagementService;

    public ToolboxController(OfficeToolsService officeToolsService,
                             DevelopmentToolsService developmentToolsService,
                             FileManagementService fileManagementService) {
        this.officeToolsService = officeToolsService;
        this.developmentToolsService = developmentToolsService;
        this.fileManagementService = fileManagementService;
    }

    // ------------------------------------------------------------------ US01

    @GetMapping("/documents")
    public List<ToolboxViews.DocumentView> documents() {
        return officeToolsService.listDocuments();
    }

    @GetMapping("/documents/{id}")
    public ToolboxViews.DocumentView document(@PathVariable Long id) {
        return officeToolsService.getDocument(id);
    }

    @PostMapping("/documents")
    public ToolboxViews.DocumentView createDocument(@Valid @RequestBody ToolboxRequests.DocumentRequest request) {
        return officeToolsService.createDocument(request);
    }

    /** {@code UpdateDocument(documentID, edit)} with the Office API quota check. */
    @PostMapping("/documents/{id}/edits")
    public ToolboxViews.DocumentView editDocument(@PathVariable Long id,
                                                  @Valid @RequestBody ToolboxRequests.DocumentEditRequest request) {
        return officeToolsService.updateDocument(id, request);
    }

    @GetMapping("/documents/{id}/edits")
    public List<ToolboxViews.DocEditView> documentEdits(@PathVariable Long id) {
        return officeToolsService.documentHistory(id);
    }

    // ------------------------------------------------------------------ US02

    @GetMapping("/presentations")
    public List<ToolboxViews.PresentationView> presentations() {
        return officeToolsService.listPresentations();
    }

    @GetMapping("/presentations/{id}")
    public ToolboxViews.PresentationView presentation(@PathVariable Long id) {
        return officeToolsService.getPresentation(id);
    }

    @PostMapping("/presentations")
    public ToolboxViews.PresentationView createPresentation(
            @Valid @RequestBody ToolboxRequests.PresentationRequest request) {
        return officeToolsService.createPresentation(request);
    }

    /** {@code UpdateSlide(presentationID, slideID, edit)}; a null slideId appends a new slide. */
    @PostMapping("/presentations/{id}/slides")
    public ToolboxViews.PresentationView updateSlide(@PathVariable Long id,
                                                     @RequestParam(required = false) Long slideId,
                                                     @RequestBody ToolboxRequests.SlideRequest request) {
        return officeToolsService.updateSlide(id, slideId, request);
    }

    // ------------------------------------------------------------------ US03

    @GetMapping("/repos")
    public List<ToolboxViews.RepoView> repos() {
        return developmentToolsService.listRepos();
    }

    @GetMapping("/repos/{id}")
    public ToolboxViews.RepoView repo(@PathVariable Long id) {
        return developmentToolsService.getRepo(id);
    }

    @PostMapping("/repos")
    public ToolboxViews.RepoView createRepo(@Valid @RequestBody ToolboxRequests.RepoRequest request) {
        return developmentToolsService.createRepo(request);
    }

    /** {@code GetHistory(repoID, branch)}. */
    @GetMapping("/repos/{id}/commits")
    public List<ToolboxViews.CommitView> commits(@PathVariable Long id) {
        return developmentToolsService.history(id);
    }

    /** {@code Commit(changes)} followed by {@code Push(repoID, changes)}. */
    @PostMapping("/repos/{id}/commits")
    public ToolboxViews.CommitView push(@PathVariable Long id,
                                        @Valid @RequestBody ToolboxRequests.PushRequest request) {
        return developmentToolsService.push(id, request);
    }

    @GetMapping("/commits/recent")
    public List<ToolboxViews.CommitView> recentCommits() {
        return developmentToolsService.recentCommits();
    }

    // ------------------------------------------------------------------ US05

    @GetMapping("/environments")
    public List<ToolboxViews.EnvironmentView> environments() {
        return developmentToolsService.listEnvironments();
    }

    /** {@code RouteRequest(request)} + {@code StartEnvironment(request)}. */
    @PostMapping("/environments")
    public ToolboxViews.EnvironmentView startEnvironment(
            @Valid @RequestBody ToolboxRequests.EnvironmentRequest request) {
        return developmentToolsService.startEnvironment(request);
    }

    @PatchMapping("/environments/{containerId}/stop")
    public ToolboxViews.EnvironmentView stopEnvironment(@PathVariable String containerId) {
        return developmentToolsService.stopEnvironment(containerId);
    }

    /** {@code GetServiceStatus(serviceID)} / {@code GetEnvironmentStatus(containerID)}. */
    @GetMapping("/environments/status")
    public Map<String, Object> environmentStatus(@RequestParam(required = false) String serviceId,
                                                 @RequestParam(required = false) String containerId) {
        Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        if (serviceId != null) {
            map.put("serviceId", serviceId);
            map.put("state", developmentToolsService.serviceStatus(serviceId));
        }
        if (containerId != null) {
            map.put("containerId", containerId);
            map.put("state", developmentToolsService.environmentStatus(containerId));
        }
        return map;
    }

    @PostMapping("/environments/heartbeat")
    public Map<String, Object> heartbeat() {
        int touched = developmentToolsService.heartbeat();
        Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        map.put("running", touched);
        return map;
    }

    // ------------------------------------------------------------------ US04

    @GetMapping("/files")
    public List<ToolboxViews.ToolboxFileView> files() {
        return fileManagementService.listFiles();
    }

    @GetMapping("/files/{id}")
    public ToolboxViews.ToolboxFileView file(@PathVariable Long id) {
        return fileManagementService.getFile(id);
    }

    /** {@code UploadFile(file, content)} - validation, storage and status confirmation. */
    @PostMapping("/files")
    public ToolboxViews.ToolboxFileView upload(@Valid @RequestBody ToolboxRequests.UploadRequest request) {
        return fileManagementService.upload(request);
    }

    /** {@code GetFileStatus(file)}. */
    @GetMapping("/files/{id}/status")
    public Map<String, Object> fileStatus(@PathVariable Long id) {
        Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        map.put("fileId", id);
        map.put("status", fileManagementService.fileStatus(id));
        return map;
    }

    /** {@code CreateShareLink(file, expiry)}. */
    @PostMapping("/files/{id}/links")
    public ToolboxViews.ShareLinkView createLink(@PathVariable Long id,
                                                 @RequestBody(required = false) ToolboxRequests.ShareRequest request) {
        return fileManagementService.createShareLink(id,
                request == null ? new ToolboxRequests.ShareRequest() : request);
    }

    @GetMapping("/files/{id}/links")
    public List<ToolboxViews.ShareLinkView> links(@PathVariable Long id) {
        return fileManagementService.linksOf(id);
    }

    @GetMapping("/links")
    public List<ToolboxViews.ShareLinkView> allLinks() {
        return fileManagementService.allLinks();
    }

    @DeleteMapping("/links/{id}")
    public ToolboxViews.ShareLinkView revokeLink(@PathVariable Long id) {
        return fileManagementService.revokeLink(id);
    }

    /** Public access path of a share link (no session required). */
    @GetMapping("/links/{token}/download")
    public ToolboxViews.ToolboxFileView download(@PathVariable String token) {
        return fileManagementService.accessLink(token);
    }

    @GetMapping("/storage")
    public Map<String, Object> storage() {
        return fileManagementService.storageStatistics();
    }

    /** Status panel of the simulated external integrations. */
    @GetMapping("/integrations")
    public ToolboxViews.IntegrationStatus integrations() {
        return fileManagementService.integrationStatus();
    }
}
