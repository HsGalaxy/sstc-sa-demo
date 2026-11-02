package com.sstc.platform.collaboration.web;

import com.sstc.platform.collaboration.domain.Channel;
import com.sstc.platform.collaboration.domain.DigestItem;
import com.sstc.platform.collaboration.domain.Meeting;
import com.sstc.platform.collaboration.domain.MeetingSummary;
import com.sstc.platform.collaboration.domain.MeetingTranscript;
import com.sstc.platform.collaboration.domain.Mention;
import com.sstc.platform.collaboration.domain.Message;
import com.sstc.platform.collaboration.domain.Notification;

import java.time.LocalDateTime;
import java.util.List;

/**
 * S03 - API projections for the Team Collaboration sub-system.
 */
public final class CollaborationViews {

    private CollaborationViews() {
    }

    public static class ChannelView {

        public Long id;

        public String name;

        public String topic;

        public String purpose;

        public boolean privateChannel;

        public int memberCount;

        public long messageCount;

        public LocalDateTime lastActivity;

        public ChannelView(Channel channel) {
            this.id = channel.getId();
            this.name = channel.getName();
            this.topic = channel.getTopic();
            this.purpose = channel.getPurpose();
            this.privateChannel = channel.isPrivateChannel();
        }
    }

    /** Published message (US02). */
    public static class MessageView {

        public Long id;

        public Long channelId;

        public String channelName;

        public Long authorId;

        public String authorName;

        public String authorInitials;

        public String body;

        public String kind;

        public LocalDateTime postedAt;

        public boolean edited;

        public String attachmentName;

        public Long replyToId;

        public List<MentionView> mentions;

        public MessageView(Message message) {
            this.id = message.getId();
            this.channelId = message.getChannelId();
            this.authorId = message.getAuthorId();
            this.body = message.getBody();
            this.kind = message.getKind() == null ? "TEXT" : message.getKind().name();
            this.postedAt = message.getPostedAt();
            this.edited = message.isEdited();
            this.attachmentName = message.getAttachmentName();
            this.replyToId = message.getReplyToId();
        }
    }

    /** Detected @mention (US03). */
    public static class MentionView {

        public Long id;

        public Long messageId;

        public Long channelId;

        public Long targetUserId;

        public String targetName;

        public String token;

        public boolean alerted;

        public LocalDateTime createdAt;

        public MentionView(Mention mention) {
            this.id = mention.getId();
            this.messageId = mention.getMessageId();
            this.channelId = mention.getChannelId();
            this.targetUserId = mention.getTargetUserId();
            this.token = mention.getToken();
            this.alerted = mention.isAlerted();
            this.createdAt = mention.getCreatedAt();
        }
    }

    /** Delivered notification (US04). */
    public static class NotificationView {

        public Long id;

        public Long recipientId;

        public String kind;

        public String priority;

        public String title;

        public String body;

        public String link;

        public boolean read;

        public LocalDateTime deliveredAt;

        public LocalDateTime readAt;

        public NotificationView(Notification notification) {
            this.id = notification.getId();
            this.recipientId = notification.getRecipientId();
            this.kind = notification.getKind() == null ? null : notification.getKind().name();
            this.priority = notification.getPriority() == null ? null : notification.getPriority().name();
            this.title = notification.getTitle();
            this.body = notification.getBody();
            this.link = notification.getLink();
            this.read = notification.isRead();
            this.deliveredAt = notification.getDeliveredAt();
            this.readAt = notification.getReadAt();
        }
    }

    /** Ranked smart digest entry (US06). */
    public static class DigestItemView {

        public Long id;

        public Long userId;

        public Long notificationId;

        public double rankScore;

        public String reason;

        public String bucket;

        public boolean dismissed;

        public NotificationView notification;

        public DigestItemView(DigestItem item) {
            this.id = item.getId();
            this.userId = item.getUserId();
            this.notificationId = item.getNotificationId();
            this.rankScore = item.getRankScore();
            this.reason = item.getReason();
            this.bucket = item.getBucket();
            this.dismissed = item.isDismissed();
        }
    }

    public static class MeetingView {

        public Long id;

        public String title;

        public String agenda;

        public Long channelId;

        public Long hostId;

        public String hostName;

        public LocalDateTime startAt;

        public LocalDateTime endAt;

        public String status;

        public String joinCode;

        public List<Long> participantIds;

        public boolean hasSummary;

        public int transcriptSegments;

        public MeetingView(Meeting meeting) {
            this.id = meeting.getId();
            this.title = meeting.getTitle();
            this.agenda = meeting.getAgenda();
            this.channelId = meeting.getChannelId();
            this.hostId = meeting.getHostId();
            this.startAt = meeting.getStartAt();
            this.endAt = meeting.getEndAt();
            this.status = meeting.getStatus();
            this.joinCode = meeting.getJoinCode();
            this.participantIds = parseIds(meeting.getParticipantIds());
        }

        private static List<Long> parseIds(String raw) {
            List<Long> ids = new java.util.ArrayList<Long>();
            if (raw == null || raw.trim().isEmpty()) {
                return ids;
            }
            for (String part : raw.split(",")) {
                try {
                    ids.add(Long.valueOf(part.trim()));
                } catch (NumberFormatException ignored) {
                    // ignore malformed entry
                }
            }
            return ids;
        }
    }

    public static class TranscriptView {

        public Long id;

        public Long meetingId;

        public Long speakerId;

        public String speakerName;

        public int offsetSeconds;

        public String text;

        public TranscriptView(MeetingTranscript transcript) {
            this.id = transcript.getId();
            this.meetingId = transcript.getMeetingId();
            this.speakerId = transcript.getSpeakerId();
            this.speakerName = transcript.getSpeakerName();
            this.offsetSeconds = transcript.getOffsetSeconds();
            this.text = transcript.getText();
        }
    }

    /** AI generated meeting summary (US05). */
    public static class SummaryView {

        public Long id;

        public Long meetingId;

        public String overview;

        public List<String> decisions;

        public List<String> actionItems;

        public String model;

        public double confidence;

        public LocalDateTime generatedAt;

        public SummaryView(MeetingSummary summary) {
            this.id = summary.getId();
            this.meetingId = summary.getMeetingId();
            this.overview = summary.getOverview();
            this.decisions = split(summary.getDecisions());
            this.actionItems = split(summary.getActionItems());
            this.model = summary.getModel();
            this.confidence = summary.getConfidence();
            this.generatedAt = summary.getCreatedAt();
        }

        private static List<String> split(String raw) {
            List<String> items = new java.util.ArrayList<String>();
            if (raw == null || raw.trim().isEmpty()) {
                return items;
            }
            for (String part : raw.split(";")) {
                if (!part.trim().isEmpty()) {
                    items.add(part.trim());
                }
            }
            return items;
        }
    }
}
