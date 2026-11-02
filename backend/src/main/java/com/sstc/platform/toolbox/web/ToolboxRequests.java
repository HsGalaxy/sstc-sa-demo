package com.sstc.platform.toolbox.web;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * S04 - request payloads of the Toolbox sub-system.
 */
public final class ToolboxRequests {

    private ToolboxRequests() {
    }

    public static class DocumentRequest {

        @NotBlank
        public String title;

        public String content;

        public String template;
    }

    public static class DocumentEditRequest {

        /** APPEND / REPLACE / RENAME */
        @NotBlank
        public String operation;

        public String payload;
    }

    public static class PresentationRequest {

        @NotBlank
        public String title;

        public String theme;
    }

    public static class SlideRequest {

        public String title;

        /** Semicolon separated bullets. */
        public String bullets;

        public String notes;

        public String layout;
    }

    public static class RepoRequest {

        @NotBlank
        public String name;

        public String slug;

        public String language;

        public String description;

        public String visibility;
    }

    public static class PushRequest {

        @NotBlank
        public String message;

        public String branch;

        public Integer additions;

        public Integer deletions;

        public Integer changedFiles;
    }

    public static class EnvironmentRequest {

        @NotBlank
        public String serviceId;

        public String image = "sstc/dev-env:1.0";

        public Double cpuLimit;

        public Integer memoryMb;

        public String ports;
    }

    public static class UploadRequest {

        @NotBlank
        public String name;

        public String folder = "/";

        public String contentType;

        public Long sizeBytes;

        public String checksum;
    }

    public static class ShareRequest {

        public LocalDateTime expiresAt;

        public Integer expiresInDays;
    }
}
