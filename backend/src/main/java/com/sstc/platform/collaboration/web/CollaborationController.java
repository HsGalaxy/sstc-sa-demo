package com.sstc.platform.collaboration.web;

import com.sstc.platform.collaboration.service.MeetingService;
import com.sstc.platform.collaboration.service.MessagingService;
import com.sstc.platform.collaboration.service.NotificationService;
import com.sstc.platform.common.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S03 - Team Collaboration REST surface (channels, messages, mentions, notifications,
 * digest and meetings with the AI summary).
 */
@RestController
@RequestMapping("/api")
public class CollaborationController {

    private final MessagingService messagingService;

    private final NotificationService notificationService;

    private final MeetingService meetingService;

    public CollaborationController(MessagingService messagingService,
                                   NotificationService notificationService,
                                   MeetingService meetingService) {
        this.messagingService = messagingService;
        this.notificationService = notificationService;
        this.meetingService = meetingService;
    }

    // ------------------------------------------------------------------ US02 / US03

    @GetMapping("/channels")
    public List<CollaborationViews.ChannelView> channels() {
        return messagingService.listChannels();
    }

    @PostMapping("/channels")
    public CollaborationViews.ChannelView createChannel(
            @Valid @RequestBody CollaborationRequests.ChannelRequest request) {
        return messagingService.createChannel(request);
    }

    @PostMapping("/channels/{id}/join")
    public Map<String, Object> join(@PathVariable Long id, CurrentUser currentUser) {
        messagingService.joinChannel(id, currentUser.getId());
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("status", "joined");
        return map;
    }

    @GetMapping("/channels/{id}/messages")
    public List<CollaborationViews.MessageView> messages(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "50") int limit) {
        return messagingService.history(id, limit);
    }

    @PostMapping("/channels/{id}/messages")
    public CollaborationViews.MessageView publish(@PathVariable Long id,
                                                  @Valid @RequestBody CollaborationRequests.MessageRequest request) {
        return messagingService.publish(id, request);
    }

    @GetMapping("/messages/recent")
    public List<CollaborationViews.MessageView> recentMessages() {
        return messagingService.recentMessages(6);
    }

    @GetMapping("/mentions")
    public List<CollaborationViews.MentionView> mentions(CurrentUser currentUser) {
        return messagingService.mentionsOf(currentUser.getId());
    }

    // ------------------------------------------------------------------ US04 / US06

    @GetMapping("/notifications")
    public List<CollaborationViews.NotificationView> notifications(CurrentUser currentUser) {
        return notificationService.inbox(currentUser.getId());
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Object> unreadCount(CurrentUser currentUser) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("unread", notificationService.unreadCount(currentUser.getId()));
        return map;
    }

    @PatchMapping("/notifications/{id}/read")
    public CollaborationViews.NotificationView markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PostMapping("/notifications/read-all")
    public Map<String, Object> markAllRead(CurrentUser currentUser) {
        int updated = notificationService.markAllRead(currentUser.getId());
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("updated", updated);
        return map;
    }

    /** US06 - the ranked smart digest of unread items. */
    @GetMapping("/digest")
    public List<CollaborationViews.DigestItemView> digest(CurrentUser currentUser) {
        return notificationService.digest(currentUser.getId());
    }

    // ------------------------------------------------------------------ US01 / US05

    @GetMapping("/meetings")
    public List<CollaborationViews.MeetingView> meetings() {
        return meetingService.list();
    }

    @GetMapping("/meetings/{id}")
    public CollaborationViews.MeetingView meeting(@PathVariable Long id) {
        return meetingService.get(id);
    }

    @PostMapping("/meetings")
    public CollaborationViews.MeetingView createMeeting(
            @Valid @RequestBody CollaborationRequests.MeetingRequest request) {
        return meetingService.create(request);
    }

    @PatchMapping("/meetings/{id}/status")
    public CollaborationViews.MeetingView changeStatus(@PathVariable Long id, @RequestParam String status) {
        return meetingService.changeStatus(id, status);
    }

    @GetMapping("/meetings/{id}/transcript")
    public List<CollaborationViews.TranscriptView> transcript(@PathVariable Long id) {
        return meetingService.transcript(id);
    }

    /** US05 - AI meeting transcription and summarisation. */
    @PostMapping("/meetings/{id}/summary")
    public CollaborationViews.SummaryView summarize(@PathVariable Long id) {
        return meetingService.summarize(id);
    }

    @GetMapping("/meetings/{id}/summary")
    public CollaborationViews.SummaryView summary(@PathVariable Long id) {
        return meetingService.summaryOf(id);
    }
}
