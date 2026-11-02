package com.sstc.platform.collaboration.web;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * S03 - request payloads for the Team Collaboration sub-system.
 */
public final class CollaborationRequests {

    private CollaborationRequests() {
    }

    public static class ChannelRequest {

        @NotBlank
        public String name;

        public String topic;

        public String purpose;

        public Boolean privateChannel;
    }

    public static class MessageRequest {

        @NotBlank
        public String body;

        /** TEXT / FILE / SYSTEM */
        public String kind = "TEXT";

        public String attachmentName;

        public Long replyToId;
    }

    public static class MeetingRequest {

        @NotBlank
        public String title;

        public String agenda;

        public Long channelId;

        public LocalDateTime startAt;

        public LocalDateTime endAt;

        public String participantIds;
    }
}
