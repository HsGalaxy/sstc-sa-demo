package com.sstc.platform.collaboration.service;

import com.sstc.platform.collaboration.domain.Channel;
import com.sstc.platform.collaboration.domain.DigestItem;
import com.sstc.platform.collaboration.domain.Message;
import com.sstc.platform.collaboration.domain.Notification;
import com.sstc.platform.collaboration.repo.*;
import com.sstc.platform.collaboration.web.CollaborationViews;
import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.web.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * S03 - Notification Service.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>{@code TeamCollaboration.NotificationDispatcher} - real time delivery (US04)</li>
 *   <li>{@code TeamCollaboration.SmartDigestEngine} - ranking of unread items (US06)</li>
 * </ul>
 *
 * <p>Digest ranking model: {@code score = base(kind) + 10 * recencyFactor}, where
 * {@code recencyFactor} decays from 1.0 (just delivered) to 0.0 after 24 hours. MENTION
 * notifications therefore always outrank SYSTEM ones, which is exactly the behaviour
 * described by US06.</p>
 */
@Service
public class NotificationService {

    private final NotificationsRepo notifications;

    private final DigestItemsRepo digestItems;

    private final AuditService auditService;

    public NotificationService(NotificationsRepo notifications,
                               DigestItemsRepo digestItems,
                               AuditService auditService) {
        this.notifications = notifications;
        this.digestItems = digestItems;
        this.auditService = auditService;
    }

    @Transactional
    public Notification dispatch(Long recipientId, Notification.Kind kind, Notification.Priority priority,
                                 String title, String body, String link) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setKind(kind);
        notification.setPriority(priority);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setDeliveredAt(LocalDateTime.now());
        notifications.save(notification);
        return notification;
    }

    /** Targeted alert raised by the Mention Detector (US03 -> US04). */
    @Transactional
    public Notification notifyMention(Long recipientId, Channel channel, Message message) {
        Notification notification = dispatch(recipientId, Notification.Kind.MENTION,
                Notification.Priority.HIGH,
                "You were mentioned in #" + (channel == null ? "channel" : channel.getName()),
                abbreviate(message.getBody()),
                "/collaboration?channel=" + (channel == null ? "" : channel.getId()));
        auditService.success("S03", "NOTIFICATION_MENTION", "notification",
                String.valueOf(notification.getId()), "recipient=" + recipientId);
        return notification;
    }

    @Transactional
    public Notification notifyTask(Long recipientId, String title, String body, String link) {
        return dispatch(recipientId, Notification.Kind.TASK, Notification.Priority.NORMAL, title, body, link);
    }

    @Transactional(readOnly = true)
    public List<CollaborationViews.NotificationView> inbox(Long userId) {
        List<CollaborationViews.NotificationView> result = new ArrayList<CollaborationViews.NotificationView>();
        for (Notification notification : notifications.findTop20ByRecipientIdOrderByDeliveredAtDesc(userId)) {
            result.add(new CollaborationViews.NotificationView(notification));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public CollaborationViews.NotificationView markRead(Long notificationId) {
        Notification notification = notifications.findById(notificationId)
                .orElseThrow(() -> AppException.notFound("Notification " + notificationId));
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notifications.save(notification);
        return new CollaborationViews.NotificationView(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        List<Notification> unread = notifications
                .findByRecipientIdAndReadFalseOrderByDeliveredAtDesc(userId);
        for (Notification notification : unread) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notifications.save(notification);
        }
        auditService.success("S03", "NOTIFICATION_READ_ALL", "notification", String.valueOf(userId),
                "marked " + unread.size() + " as read");
        return unread.size();
    }

    /**
     * US06 - rebuild and return the ranked digest of the unread items of one account.
     */
    @Transactional
    public List<CollaborationViews.DigestItemView> digest(Long userId) {
        List<Notification> unread = notifications
                .findByRecipientIdAndReadFalseOrderByDeliveredAtDesc(userId);
        for (DigestItem item : digestItems.findByUserIdAndDismissedFalseOrderByRankScoreDesc(userId)) {
            digestItems.delete(item);
        }
        List<CollaborationViews.DigestItemView> result = new ArrayList<CollaborationViews.DigestItemView>();
        for (Notification notification : unread) {
            double score = scoreOf(notification);
            String bucket = notification.getKind() == Notification.Kind.MENTION ? "MENTION"
                    : notification.getPriority() == Notification.Priority.HIGH ? "HIGH" : "NORMAL";
            String reason = notification.getKind() == Notification.Kind.MENTION
                    ? "Direct @mention, needs an answer"
                    : "Unread " + notification.getKind().name().toLowerCase() + " update";
            DigestItem item = new DigestItem(userId, notification.getId(), score, reason, bucket);
            digestItems.save(item);
            CollaborationViews.DigestItemView view = new CollaborationViews.DigestItemView(item);
            view.notification = new CollaborationViews.NotificationView(notification);
            result.add(view);
        }
        result.sort((a, b) -> Double.compare(b.rankScore, a.rankScore));
        auditService.success("S03", "DIGEST_BUILD", "digest", String.valueOf(userId),
                "ranked " + result.size() + " items");
        return result;
    }

    private double scoreOf(Notification notification) {
        double base;
        switch (notification.getKind()) {
            case MENTION:
                base = 90d;
                break;
            case TASK:
                base = 70d;
                break;
            case MEETING:
                base = 60d;
                break;
            case FILE:
                base = 50d;
                break;
            default:
                base = 30d;
                break;
        }
        long minutes = Duration.between(notification.getDeliveredAt(), LocalDateTime.now()).toMinutes();
        double recency = 1.0d - Math.min(minutes, 1440d) / 1440d;
        return Math.round((base + 10d * recency) * 10d) / 10d;
    }

    /** Used by the notification centre badge. */
    @Transactional(readOnly = true)
    public long unreadOfCurrentUser() {
        Long userId = CurrentUserHolder.id();
        return userId == null ? 0L : unreadCount(userId);
    }

    private String abbreviate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 160 ? text : text.substring(0, 157) + "...";
    }
}
