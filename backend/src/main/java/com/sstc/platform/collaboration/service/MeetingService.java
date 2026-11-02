package com.sstc.platform.collaboration.service;

import com.sstc.platform.collaboration.domain.Meeting;
import com.sstc.platform.collaboration.domain.MeetingSummary;
import com.sstc.platform.collaboration.domain.MeetingTranscript;
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
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * S03 - Meeting Service.
 *
 * <p>Level-3 mapping:</p>
 * <ul>
 *   <li>{@code TeamCollaboration.MeetingOrchestrator} - meeting lifecycle (US01)</li>
 *   <li>{@code TeamCollaboration.AIMeetingAssistant} - transcription and summarisation (US05)</li>
 * </ul>
 */
@Service
public class MeetingService {

    private final MeetingsRepo meetings;

    private final TranscriptsRepo transcripts;

    private final SummariesRepo summaries;

    private final UserDirectory directory;

    private final AuditService auditService;

    private final NotificationService notificationService;

    public MeetingService(MeetingsRepo meetings,
                          TranscriptsRepo transcripts,
                          SummariesRepo summaries,
                          UserDirectory directory,
                          AuditService auditService,
                          NotificationService notificationService) {
        this.meetings = meetings;
        this.transcripts = transcripts;
        this.summaries = summaries;
        this.directory = directory;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CollaborationViews.MeetingView> list() {
        Map<Long, User> users = directory.index();
        List<CollaborationViews.MeetingView> result = new ArrayList<CollaborationViews.MeetingView>();
        for (Meeting meeting : meetings.findAllByOrderByStartAtDesc()) {
            result.add(decorate(meeting, users));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CollaborationViews.MeetingView get(Long id) {
        return decorate(require(id), directory.index());
    }

    @Transactional
    public CollaborationViews.MeetingView create(CollaborationRequests.MeetingRequest request) {
        Meeting meeting = new Meeting();
        meeting.setTitle(request.title.trim());
        meeting.setAgenda(request.agenda);
        meeting.setChannelId(request.channelId);
        meeting.setHostId(CurrentUserHolder.id());
        meeting.setStartAt(request.startAt == null ? LocalDateTime.now().plusHours(1) : request.startAt);
        meeting.setEndAt(request.endAt);
        meeting.setStatus("SCHEDULED");
        meeting.setJoinCode(randomCode());
        meeting.setParticipantIds(request.participantIds == null
                ? String.valueOf(CurrentUserHolder.id()) : request.participantIds);
        meetings.save(meeting);
        auditService.success("S03", "MEETING_CREATE", "meeting", meeting.getJoinCode(), meeting.getTitle());
        return get(meeting.getId());
    }

    /** US01 - move the meeting lifecycle forward. */
    @Transactional
    public CollaborationViews.MeetingView changeStatus(Long id, String status) {
        Meeting meeting = require(id);
        String target = status == null ? "" : status.toUpperCase();
        if (!"SCHEDULED".equals(target) && !"IN_PROGRESS".equals(target)
                && !"ENDED".equals(target) && !"CANCELLED".equals(target)) {
            throw AppException.badRequest("status must be SCHEDULED, IN_PROGRESS, ENDED or CANCELLED");
        }
        meeting.setStatus(target);
        if ("IN_PROGRESS".equals(target)) {
            meeting.setStartAt(LocalDateTime.now());
        }
        if ("ENDED".equals(target)) {
            meeting.setEndAt(LocalDateTime.now());
        }
        meetings.save(meeting);
        auditService.success("S03", "MEETING_STATUS", "meeting", meeting.getJoinCode(),
                "status=" + target);

        if ("ENDED".equals(target)) {
            // MeetingOrchestrator triggers the AI Meeting Assistant when the meeting ends.
            generateTranscript(meeting);
            summarize(meeting.getId());
        }
        return get(id);
    }

    /** US05 - the transcription produced by the AI Meeting Assistant. */
    @Transactional
    public List<CollaborationViews.TranscriptView> transcript(Long meetingId) {
        require(meetingId);
        Map<Long, User> users = directory.index();
        List<CollaborationViews.TranscriptView> result = new ArrayList<CollaborationViews.TranscriptView>();
        for (MeetingTranscript transcript : transcripts.findByMeetingIdOrderByOffsetSecondsAsc(meetingId)) {
            CollaborationViews.TranscriptView view = new CollaborationViews.TranscriptView(transcript);
            if (transcript.getSpeakerId() != null) {
                User speaker = users.get(transcript.getSpeakerId());
                if (speaker != null) {
                    view.speakerName = speaker.getDisplayName();
                }
            }
            result.add(view);
        }
        return result;
    }

    /** US05 - generate (or regenerate) the AI summary of an ended meeting. */
    @Transactional
    public CollaborationViews.SummaryView summarize(Long meetingId) {
        Meeting meeting = require(meetingId);
        if (transcripts.findByMeetingIdOrderByOffsetSecondsAsc(meetingId).isEmpty()) {
            generateTranscript(meeting);
        }
        List<MeetingTranscript> segments = transcripts.findByMeetingIdOrderByOffsetSecondsAsc(meetingId);
        List<String> decisions = new ArrayList<String>();
        List<String> actions = new ArrayList<String>();
        StringBuilder overview = new StringBuilder();
        for (MeetingTranscript segment : segments) {
            String text = segment.getText() == null ? "" : segment.getText();
            String lower = text.toLowerCase();
            if (lower.contains("decide") || lower.contains("agreed") || lower.contains("decision")) {
                decisions.add(segment.getSpeakerName() + ": " + text);
            }
            if (lower.contains("will ") || lower.contains("action") || lower.contains("next step")
                    || lower.contains("follow up")) {
                actions.add(segment.getSpeakerName() + ": " + text);
            }
            if (overview.length() < 600) {
                overview.append(segment.getSpeakerName()).append(": ").append(text).append(' ');
            }
        }
        MeetingSummary summary = summaries.findByMeetingId(meetingId);
        if (summary == null) {
            summary = new MeetingSummary();
            summary.setMeetingId(meetingId);
        }
        summary.setOverview(overview.length() <= 900 ? overview.toString().trim()
                : overview.substring(0, 900).trim() + "...");
        summary.setDecisions(join(decisions));
        summary.setActionItems(join(actions));
        summary.setConfidence(segments.isEmpty() ? 0d : Math.min(0.98d, 0.72d + segments.size() * 0.02d));
        summaries.save(summary);

        auditService.success("S03", "MEETING_SUMMARY", "meeting", meeting.getJoinCode(),
                "segments=" + segments.size() + " decisions=" + decisions.size()
                        + " actions=" + actions.size());
        return new CollaborationViews.SummaryView(summary);
    }

    @Transactional(readOnly = true)
    public CollaborationViews.SummaryView summaryOf(Long meetingId) {
        MeetingSummary summary = summaries.findByMeetingId(meetingId);
        return summary == null ? null : new CollaborationViews.SummaryView(summary);
    }

    /**
     * Simulated transcription. A real deployment would call a speech-to-text provider; the
     * deterministic template keeps the demo reproducible and makes the AI summary verifiable.
     */
    private void generateTranscript(Meeting meeting) {
        if (!transcripts.findByMeetingIdOrderByOffsetSecondsAsc(meeting.getId()).isEmpty()) {
            return;
        }
        List<User> participants = participantsOf(meeting);
        String[] lines = {
                "Let us review the current task board and the blockers of this cycle.",
                "We decided to move the toolbox integration tests to Friday so QA has a full pass.",
                "I will update the OKR progress for the toolbox sub-system today.",
                "The file sharing link flow still returns an expired token for large uploads.",
                "Agreed: the digest engine keeps @mentions at the top of the unread list.",
                "Action: prepare the demo script and the architecture walkthrough for the presentation.",
                "Next step is to freeze the API contract and tag the release candidate.",
                "Follow up with the lead about the permission policy for the senior manager role."
        };
        Random random = new Random(meeting.getId() == null ? 7L : meeting.getId());
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            User speaker = participants.isEmpty() ? null
                    : participants.get(random.nextInt(participants.size()));
            MeetingTranscript segment = new MeetingTranscript();
            segment.setMeetingId(meeting.getId());
            segment.setSpeakerId(speaker == null ? null : speaker.getId());
            segment.setSpeakerName(speaker == null ? "Participant" : speaker.getDisplayName());
            segment.setOffsetSeconds(offset);
            segment.setText(lines[i]);
            transcripts.save(segment);
            offset += 60 + random.nextInt(90);
        }
        auditService.success("S03", "MEETING_TRANSCRIBE", "meeting", meeting.getJoinCode(),
                lines.length + " segments transcribed");
    }

    private List<User> participantsOf(Meeting meeting) {
        List<User> result = new ArrayList<User>();
        if (meeting.getParticipantIds() == null) {
            return directory.activeMembers();
        }
        for (String part : meeting.getParticipantIds().split(",")) {
            try {
                User user = directory.get(Long.valueOf(part.trim()));
                if (user != null) {
                    result.add(user);
                }
            } catch (NumberFormatException ignored) {
                // skip malformed entry
            }
        }
        return result.isEmpty() ? directory.activeMembers() : result;
    }

    private CollaborationViews.MeetingView decorate(Meeting meeting, Map<Long, User> users) {
        CollaborationViews.MeetingView view = new CollaborationViews.MeetingView(meeting);
        User host = meeting.getHostId() == null ? null : users.get(meeting.getHostId());
        view.hostName = host == null ? null : host.getDisplayName();
        view.hasSummary = summaries.findByMeetingId(meeting.getId()) != null;
        view.transcriptSegments = transcripts.findByMeetingIdOrderByOffsetSecondsAsc(meeting.getId()).size();
        return view;
    }

    private Meeting require(Long id) {
        return meetings.findById(id).orElseThrow(() -> AppException.notFound("Meeting " + id));
    }

    private String randomCode() {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = "STC-" + (100000 + new Random().nextInt(899999));
            boolean taken = false;
            for (Meeting existing : meetings.findAll()) {
                if (code.equals(existing.getJoinCode())) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return code;
            }
        }
        return "STC-" + System.currentTimeMillis() % 1000000;
    }

    private String join(List<String> items) {
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(item);
        }
        return builder.toString();
    }

    /** Convenience used by the seeder and by the notification of a scheduled meeting. */
    @Transactional
    public void notifyParticipants(Meeting meeting, String title, String body) {
        for (User participant : participantsOf(meeting)) {
            notificationService.notifyTask(participant.getId(), title, body, "/collaboration");
        }
    }
}
