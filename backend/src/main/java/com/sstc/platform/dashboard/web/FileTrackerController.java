package com.sstc.platform.dashboard.web;

import com.sstc.platform.dashboard.domain.FileRecord;
import com.sstc.platform.dashboard.service.FileTrackerService;
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
 * S02 - File Tracker REST surface (multi condition query, versions, rollback).
 */
@RestController
@RequestMapping("/api")
public class FileTrackerController {

    private final FileTrackerService fileTrackerService;

    public FileTrackerController(FileTrackerService fileTrackerService) {
        this.fileTrackerService = fileTrackerService;
    }

    /** US01 - multi condition file query with optional bucket/task/owner/keyword filters. */
    @GetMapping("/files")
    public List<DashboardViews.FileRecordView> files(@RequestParam(required = false) FileRecord.Bucket bucket,
                                                     @RequestParam(required = false) Long taskId,
                                                     @RequestParam(required = false) Long ownerId,
                                                     @RequestParam(required = false) Boolean archived,
                                                     @RequestParam(required = false) String keyword) {
        return fileTrackerService.query(bucket, taskId, ownerId, archived, keyword);
    }

    @GetMapping("/files/{id}")
    public DashboardViews.FileRecordView file(@PathVariable Long id) {
        return fileTrackerService.getFile(id);
    }

    /** US01 - full historical version list of one file. */
    @GetMapping("/files/{id}/versions")
    public List<DashboardViews.FileVersionView> versions(@PathVariable Long id) {
        return fileTrackerService.versions(id);
    }

    /** US02 - register an attachment for a task. */
    @PostMapping("/files")
    public DashboardViews.FileRecordView upload(@Valid @RequestBody DashboardRequests.UploadFileRequest request) {
        return fileTrackerService.upload(request);
    }

    @PostMapping("/files/{id}/versions")
    public DashboardViews.FileVersionView addVersion(@PathVariable Long id,
                                                     @Valid @RequestBody DashboardRequests.VersionRequest request) {
        return fileTrackerService.addVersion(id, request);
    }

    /** US01 - roll back to a previous version. */
    @PostMapping("/files/{id}/rollback")
    public DashboardViews.FileRecordView rollback(@PathVariable Long id, @RequestParam int version) {
        return fileTrackerService.rollback(id, version);
    }

    @PatchMapping("/files/{id}/archive")
    public DashboardViews.FileRecordView archive(@PathVariable Long id, @RequestParam boolean archived) {
        return fileTrackerService.archive(id, archived);
    }

    @GetMapping("/files/statistics")
    public Map<String, Object> statistics() {
        return fileTrackerService.bucketStatistics();
    }
}
