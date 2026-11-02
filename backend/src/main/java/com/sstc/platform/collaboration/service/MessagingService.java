package com.sstc.platform.collaboration.service;

import com.sstc.platform.collaboration.domain.Channel;
import com.sstc.platform.collaboration.domain.ChannelMember;
import com.sstc.platform.collaboration.domain.Mention;
import com.sstc.platform.collaboration.domain.Message;
import com.sstc.platform.collaboration.repo.*;
import com.sstc.platform.collaboration.web.CollaborationRequests;
import com.sstc.platform.collaboration.web.CollaborationViews;
import com.sstc.platform.common.audit.AuditService;
import com.sstc.platform.common.security.CurrentUserHolder;
import com.sstc.platform.common.web.AppException;
import com.sstc.platform.identity.domain.User;
import com.sstc.platform.identity.service.UserDirectory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S03 - Messaging Service.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>{@code TeamCollaboration.ChannelMessageManager} - channel and message handling (US02)</li>
 *   <li>{@code TeamCollaboration.MentionDetector} - @mention parsing and targeted alerting (US03)</li>
 * </ul>
 */
@Service
public class MessagingService {

    /** Mention token grammar: @username, e.g. {@code @yunteng}. */
    private static final Pattern MENTION = Pattern.compile("@([a-zA-Z0-9._-]{2,32})");

    private final ChannelsRepo channels;

    private final ChannelMembersRepo members;

    private final MessagesRepo messages;

    private final MentionsRepo mentions;

    private final NotificationService notificationService;

    private final UserDirectory directory;

    private final AuditService auditService;

    public MessagingService(ChannelsRepo channels,
                            ChannelMembersRepo members,
                            MessagesRepo messages,
                            MentionsRepo mentions,
                            NotificationService notificationService,
                            UserDirectory directory,
                            AuditService auditService) {
        this.channels = channels;
        this.members = members;
        this.messages = messages;
        this.mentions = mentions;
        this.notificationService = notificationService;
        this.directory = directory;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CollaborationViews.ChannelView> listChannels() {
        List<CollaborationViews.ChannelView> result = new ArrayList<CollaborationViews.ChannelView>();
        for (Channel channel : channels.findByArchivedFalseOrderByNameAsc()) {
            CollaborationViews.ChannelView view = new CollaborationViews.ChannelView(channel);
            view.memberCount = members.findByChannelId(channel.getId()).size();
            view.messageCount = messages.countByChannelId(channel.getId());
            List<Message> latest = messages.findTop50ByChannelIdOrderByPostedAtDesc(channel.getId());
            view.lastActivity = latest.isEmpty() ? channel.getCreatedAt() : latest.get(0).getPostedAt();
            result.add(view);
        }
        return result;
    }

    @Transactional
    public CollaborationViews.ChannelView createChannel(CollaborationRequests.ChannelRequest request) {
        Channel channel = new Channel();
        channel.setName(request.name.trim());
        channel.setTopic(request.topic);
        channel.setPurpose(request.purpose);
        channel.setPrivateChannel(request.privateChannel != null && request.privateChannel);
        channel.setCreatedBy(CurrentUserHolder.id());
        channels.save(channel);
        members.save(new ChannelMember(channel.getId(), CurrentUserHolder.id()));
        auditService.success("S03", "CHANNEL_CREATE", "channel", channel.getName(),
                "channel created");
        CollaborationViews.ChannelView view = new CollaborationViews.ChannelView(channel);
        view.memberCount = 1;
        view.messageCount = 0;
        return view;
    }

    @Transactional
    public void joinChannel(Long channelId, Long userId) {
        Channel channel = channels.findById(channelId)
                .orElseThrow(() -> AppException.notFound("Channel " + channelId));
        if (!members.existsByChannelIdAndUserId(channelId, userId)) {
            members.save(new ChannelMember(channelId, userId));
            auditService.success("S03", "CHANNEL_JOIN", "channel", channel.getName(),
                    "member " + userId + " joined");
        }
    }

    @Transactional(readOnly = true)
    public List<CollaborationViews.MessageView> history(Long channelId, int limit) {
        Channel channel = channels.findById(channelId)
                .orElseThrow(() -> AppException.notFound("Channel " + channelId));
        Map<Long, User> users = directory.index();
        List<Message> list = messages.findByChannelIdOrderByPostedAtAsc(channelId);
        int from = Math.max(0, list.size() - limit);
        List<CollaborationViews.MessageView> result = new ArrayList<CollaborationViews.MessageView>();
        for (Message message : list.subList(from, list.size())) {
            result.add(decorate(message, channel, users));
        }
        return result;
    }

    /**
     * US02 + US03 - publish a group message, detect @mentions and raise the targeted alerts.
     */
    @Transactional
    public CollaborationViews.MessageView publish(Long channelId, CollaborationRequests.MessageRequest request) {
        Channel channel = channels.findById(channelId)
                .orElseThrow(() -> AppException.notFound("Channel " + channelId));
        Message message = new Message();
        message.setChannelId(channelId);
        message.setAuthorId(CurrentUserHolder.id());
        message.setBody(request.body);
        message.setKind(parseKind(request.kind));
        message.setAttachmentName(request.attachmentName);
        message.setReplyToId(request.replyToId);
        message.setPostedAt(LocalDateTime.now());
        messages.save(message);

        List<Mention> detected = detectMentions(message, channel);
        Map<Long, User> users = directory.index();
        CollaborationViews.MessageView view = decorate(message, channel, users);

        auditService.success("S03", "MESSAGE_PUBLISH", "message", String.valueOf(message.getId()),
                "channel=" + channel.getName() + " mentions=" + detected.size());
        return view;
    }

    /** {@code MentionDetector.ParseMentions(message)} - returns the mentioned accounts. */
    private List<Mention> detectMentions(Message message, Channel channel) {
        List<Mention> created = new ArrayList<Mention>();
        Matcher matcher = MENTION.matcher(message.getBody() == null ? "" : message.getBody());
        while (matcher.find()) {
            String username = matcher.group(1);
            User target = directory.byUsername(username);
            if (target == null || target.getId().equals(message.getAuthorId())) {
                continue;
            }
            Mention mention = new Mention(message.getId(), channel.getId(), target.getId(),
                    matcher.group(0));
            mention.setAlerted(true);
            mentions.save(mention);
            created.add(mention);
            notificationService.notifyMention(target.getId(), channel, message);
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<CollaborationViews.MentionView> mentionsOf(Long userId) {
        Map<Long, User> users = directory.index();
        List<CollaborationViews.MentionView> result = new ArrayList<CollaborationViews.MentionView>();
        for (Mention mention : mentions.findByTargetUserIdOrderByCreatedAtDesc(userId)) {
            CollaborationViews.MentionView view = new CollaborationViews.MentionView(mention);
            User target = users.get(mention.getTargetUserId());
            view.targetName = target == null ? null : target.getDisplayName();
            result.add(view);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CollaborationViews.MessageView> recentMessages(int limit) {
        Map<Long, User> users = directory.index();
        Map<Long, Channel> channelIndex = new LinkedHashMap<Long, Channel>();
        for (Channel channel : channels.findAll()) {
            channelIndex.put(channel.getId(), channel);
        }
        List<CollaborationViews.MessageView> result = new ArrayList<CollaborationViews.MessageView>();
        for (Message message : messages.findTop6ByOrderByPostedAtDesc()) {
            result.add(decorate(message, channelIndex.get(message.getChannelId()), users));
        }
        return result;
    }

    private CollaborationViews.MessageView decorate(Message message, Channel channel, Map<Long, User> users) {
        CollaborationViews.MessageView view = new CollaborationViews.MessageView(message);
        view.channelName = channel == null ? null : channel.getName();
        User author = message.getAuthorId() == null ? null : users.get(message.getAuthorId());
        if (author != null) {
            view.authorName = author.getDisplayName();
            view.authorInitials = UserDirectory.initials(author.getDisplayName());
        }
        List<CollaborationViews.MentionView> mentionViews = new ArrayList<CollaborationViews.MentionView>();
        for (Mention mention : mentions.findByMessageId(message.getId())) {
            CollaborationViews.MentionView mentionView = new CollaborationViews.MentionView(mention);
            User target = users.get(mention.getTargetUserId());
            mentionView.targetName = target == null ? null : target.getDisplayName();
            mentionViews.add(mentionView);
        }
        view.mentions = mentionViews;
        return view;
    }

    private Message.Kind parseKind(String raw) {
        if (raw == null) {
            return Message.Kind.TEXT;
        }
        try {
            return Message.Kind.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Message.Kind.TEXT;
        }
    }
}
