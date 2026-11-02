import { useCallback, useEffect, useState, type ChangeEvent } from 'react'
import { api, type MeetingView, type SummaryView, type TranscriptView } from '../api/client'
import { useAuth } from '../app/AuthContext'
import { useToast } from '../app/ToastContext'
import {
  Badge,
  Bar,
  Button,
  Card,
  Empty,
  Field,
  Modal,
  Stat,
  formatDateTime,
  relativeTime
} from '../components/ui'

/**
 * S03 - Team Collaboration: meeting lifecycle plus the AI transcript and summary.
 *
 * Level-3 mapping:
 *  - US01 TeamCollaboration.MeetingOrchestrator - create a meeting, move its status.
 *  - US05 TeamCollaboration.AIMeetingAssistant - transcript and AI summary of an ended meeting.
 */

type BadgeTone =
  | 'default'
  | 'solid'
  | 'ok'
  | 'warn'
  | 'danger'
  | 'info'
  | 's01'
  | 's02'
  | 's03'
  | 's04'

type MeetingTab = 'transcript' | 'summary'

const MEETING_STATUSES: string[] = ['SCHEDULED', 'IN_PROGRESS', 'ENDED', 'CANCELLED']

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function statusTone(status: string): BadgeTone {
  switch (status) {
    case 'IN_PROGRESS':
      return 'warn'
    case 'ENDED':
      return 'ok'
    case 'CANCELLED':
      return 'danger'
    case 'SCHEDULED':
      return 'info'
    default:
      return 'default'
  }
}

/** mm:ss offset used by the transcript timeline. */
function formatOffset(seconds: number): string {
  const safe = Number.isFinite(seconds) && seconds > 0 ? Math.floor(seconds) : 0
  const minutes = Math.floor(safe / 60)
  const rest = safe % 60
  return `${String(minutes).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}

/** The summariser returns a 0..1 ratio; the panel shows a percentage. */
function confidencePercent(confidence: number): number {
  const ratio = confidence <= 1 ? confidence * 100 : confidence
  return Math.max(0, Math.min(100, Math.round(ratio)))
}

export function MeetingsPage() {
  const { can } = useAuth()
  const { push } = useToast()
  const canCollaborate = can('S03.COLLABORATION.USE')

  // US01 - meeting list state.
  const [meetings, setMeetings] = useState<MeetingView[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [busy, setBusy] = useState<string | null>(null)

  // US05 - transcript and summary of the selected meeting.
  const [tab, setTab] = useState<MeetingTab>('transcript')
  const [transcript, setTranscript] = useState<TranscriptView[]>([])
  const [transcriptLoading, setTranscriptLoading] = useState<boolean>(false)
  const [summary, setSummary] = useState<SummaryView | null>(null)
  const [summaryLoading, setSummaryLoading] = useState<boolean>(false)

  // US01 - "schedule a meeting" modal state.
  const [createOpen, setCreateOpen] = useState<boolean>(false)
  const [newTitle, setNewTitle] = useState<string>('')
  const [newAgenda, setNewAgenda] = useState<string>('')
  const [newStartAt, setNewStartAt] = useState<string>('')
  const [creating, setCreating] = useState<boolean>(false)

  const loadMeetings = useCallback(async () => {
    setLoading(true)
    try {
      const list = await api.meetings()
      setMeetings(list)
      setSelectedId((current) =>
        current !== null && list.some((meeting) => meeting.id === current)
          ? current
          : list.length > 0
            ? list[0].id
            : null
      )
    } catch (error) {
      push(errorText(error, 'Could not load the meetings'), 'error')
    } finally {
      setLoading(false)
    }
  }, [push])

  const loadTranscript = useCallback(
    async (meetingId: number) => {
      setTranscriptLoading(true)
      try {
        setTranscript(await api.transcript(meetingId))
      } catch (error) {
        push(errorText(error, 'Could not load the meeting transcript'), 'error')
      } finally {
        setTranscriptLoading(false)
      }
    },
    [push]
  )

  const loadSummary = useCallback(
    async (meetingId: number) => {
      setSummaryLoading(true)
      try {
        setSummary(await api.meetingSummary(meetingId))
      } catch (error) {
        push(errorText(error, 'Could not load the AI summary'), 'error')
      } finally {
        setSummaryLoading(false)
      }
    },
    [push]
  )

  useEffect(() => {
    void loadMeetings()
  }, [loadMeetings])

  // US05 - selecting a meeting loads both AI artefacts of that meeting.
  useEffect(() => {
    if (selectedId === null) {
      setTranscript([])
      setSummary(null)
      return
    }
    const meetingId = selectedId
    void loadTranscript(meetingId)
    void loadSummary(meetingId)
  }, [selectedId, loadTranscript, loadSummary])

  const selectedMeeting = meetings.find((meeting) => meeting.id === selectedId) ?? null

  /** US01 - move the meeting through its lifecycle. */
  const changeStatus = async (
    meeting: MeetingView,
    status: 'IN_PROGRESS' | 'ENDED' | 'CANCELLED'
  ) => {
    setBusy(`${status}-${meeting.id}`)
    try {
      const updated = await api.changeMeetingStatus(meeting.id, status)
      setMeetings((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      )
      if (status === 'ENDED') {
        // US05 - the MeetingOrchestrator triggers transcription + summarisation on ENDED.
        setTab('transcript')
        setSelectedId(updated.id)
        await loadTranscript(updated.id)
        await loadSummary(updated.id)
        push('Meeting ended - transcript and summary generated', 'ok')
      } else {
        push(`Meeting "${updated.title}" is now ${updated.status}`, 'ok')
      }
    } catch (error) {
      push(errorText(error, 'The meeting status could not be changed'), 'error')
    } finally {
      setBusy(null)
    }
  }

  /** US05 - regenerate the summary on demand through the AIMeetingAssistant. */
  const regenerateSummary = async (meetingId: number) => {
    setBusy(`SUMMARY-${meetingId}`)
    try {
      const generated = await api.summarizeMeeting(meetingId)
      setSummary(generated)
      setMeetings((current) =>
        current.map((meeting) =>
          meeting.id === meetingId ? { ...meeting, hasSummary: true } : meeting
        )
      )
      await loadTranscript(meetingId)
      push('AI summary regenerated by the simulated summariser', 'ok')
    } catch (error) {
      push(errorText(error, 'The AI summary could not be generated'), 'error')
    } finally {
      setBusy(null)
    }
  }

  /** US01 - schedule a meeting. */
  const createMeeting = async () => {
    const title = newTitle.trim()
    if (!title) {
      push('A meeting title is required', 'error')
      return
    }
    setCreating(true)
    try {
      const created = await api.createMeeting({
        title,
        agenda: newAgenda.trim() || undefined,
        // datetime-local already yields ISO local time, which is what the API expects.
        startAt: newStartAt ? `${newStartAt}:00` : undefined
      })
      await loadMeetings()
      setSelectedId(created.id)
      setCreateOpen(false)
      setNewTitle('')
      setNewAgenda('')
      setNewStartAt('')
      push(`Meeting "${created.title}" scheduled · join code ${created.joinCode}`, 'ok')
    } catch (error) {
      push(errorText(error, 'The meeting could not be created'), 'error')
    } finally {
      setCreating(false)
    }
  }

  const scheduled = meetings.filter((meeting) => meeting.status === 'SCHEDULED').length
  const inProgress = meetings.filter((meeting) => meeting.status === 'IN_PROGRESS').length
  const summaries = meetings.filter((meeting) => meeting.hasSummary).length
  const confidence = summary ? confidencePercent(summary.confidence) : 0

  return (
    <div className="grid">
      <div className="grid grid-4">
        <Stat label="Meetings" value={meetings.length} accent="s03" />
        <Stat label="Scheduled" value={scheduled} />
        <Stat label="In progress" value={inProgress} />
        <Stat label="Summaries generated" value={summaries} delta="AI Meeting Assistant" />
      </div>

      <Card
        title="Meeting lifecycle"
        hint="Create a meeting, start it, end it - ending triggers the AI transcript and summary."
        actions={
          <Button
            size="sm"
            variant="primary"
            disabled={!canCollaborate}
            title={canCollaborate ? 'Schedule a meeting' : 'S03.COLLABORATION.USE is required'}
            onClick={() => setCreateOpen(true)}
          >
            New meeting
          </Button>
        }
      >
        {loading ? (
          <div className="list">
            {[0, 1, 2].map((index) => (
              <div className="skeleton" key={index} style={{ height: 78 }} />
            ))}
          </div>
        ) : meetings.length === 0 ? (
          <Empty>No meetings yet - schedule the first stand-up.</Empty>
        ) : (
          <div className="list" style={{ maxHeight: 440, overflowY: 'auto' }}>
            {meetings.map((meeting) => (
              <div
                className="list-row"
                key={meeting.id}
                style={{
                  alignItems: 'flex-start',
                  borderColor: meeting.id === selectedId ? '#a4bcfd' : undefined,
                  background: meeting.id === selectedId ? '#f7f9ff' : undefined
                }}
              >
                <div className="grow">
                  <div className="title">{meeting.title}</div>
                  <div className="sub">{meeting.agenda ?? 'No agenda provided.'}</div>
                  <div className="chip-row" style={{ marginTop: 8 }}>
                    <Badge tone={statusTone(meeting.status)}>{meeting.status}</Badge>
                    <Badge>host {meeting.hostName ?? `#${meeting.hostId}`}</Badge>
                    <Badge>starts {formatDateTime(meeting.startAt)}</Badge>
                    <Badge>join code {meeting.joinCode}</Badge>
                    <Badge>{meeting.transcriptSegments} transcript segments</Badge>
                    <Badge tone={meeting.hasSummary ? 'ok' : 'default'}>
                      {meeting.hasSummary ? 'summary ready' : 'no summary'}
                    </Badge>
                    {meeting.participantIds.length > 0 ? (
                      <Badge>{meeting.participantIds.length} participants</Badge>
                    ) : null}
                  </div>
                </div>
                <div className="toolbar" style={{ justifyContent: 'flex-end' }}>
                  <Button size="sm" onClick={() => setSelectedId(meeting.id)}>
                    {meeting.id === selectedId ? 'Selected' : 'Open'}
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    disabled={!canCollaborate || busy !== null || meeting.status !== 'SCHEDULED'}
                    onClick={() => void changeStatus(meeting, 'IN_PROGRESS')}
                  >
                    {busy === `IN_PROGRESS-${meeting.id}` ? 'Starting…' : 'Start'}
                  </Button>
                  <Button
                    size="sm"
                    variant="danger"
                    disabled={
                      !canCollaborate ||
                      busy !== null ||
                      meeting.status === 'ENDED' ||
                      meeting.status === 'CANCELLED'
                    }
                    onClick={() => void changeStatus(meeting, 'ENDED')}
                  >
                    {busy === `ENDED-${meeting.id}` ? 'Ending…' : 'End'}
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={
                      !canCollaborate ||
                      busy !== null ||
                      meeting.status === 'ENDED' ||
                      meeting.status === 'CANCELLED'
                    }
                    onClick={() => void changeStatus(meeting, 'CANCELLED')}
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        <div className="soft" style={{ fontSize: 11, marginTop: 12 }}>
          US01 · TeamCollaboration.MeetingOrchestrator - meeting creation and the
          SCHEDULED / IN_PROGRESS / ENDED / CANCELLED lifecycle.
        </div>
      </Card>

      <Card
        title={selectedMeeting ? `Meeting detail · ${selectedMeeting.title}` : 'Meeting detail'}
        hint={
          selectedMeeting
            ? `${selectedMeeting.status} · join code ${selectedMeeting.joinCode} · host ${
                selectedMeeting.hostName ?? `#${selectedMeeting.hostId}`
              }`
            : 'Select a meeting to inspect its transcript and AI summary.'
        }
        actions={
          <div className="toolbar">
            <Button
              size="sm"
              variant={tab === 'transcript' ? 'primary' : 'default'}
              onClick={() => setTab('transcript')}
            >
              Transcript
            </Button>
            <Button
              size="sm"
              variant={tab === 'summary' ? 'primary' : 'default'}
              onClick={() => setTab('summary')}
            >
              AI summary
            </Button>
          </div>
        }
      >
        {selectedMeeting === null ? (
          <Empty>No meeting selected.</Empty>
        ) : tab === 'transcript' ? (
          <div>
            <div className="chip-row" style={{ marginBottom: 12 }}>
              <Badge tone="s04">AI Meeting Assistant · simulated transcriber</Badge>
              <Badge>{transcript.length} segments</Badge>
              <Badge>{selectedMeeting.status}</Badge>
            </div>
            {transcriptLoading ? (
              <div className="list">
                {[0, 1, 2].map((index) => (
                  <div className="skeleton" key={index} style={{ height: 44 }} />
                ))}
              </div>
            ) : transcript.length === 0 ? (
              <Empty>
                No transcript yet - the AIMeetingAssistant transcribes when the meeting ends.
              </Empty>
            ) : (
              <div className="timeline" style={{ maxHeight: 460, overflowY: 'auto' }}>
                {transcript.map((segment) => (
                  <div className="timeline-item" key={segment.id}>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'baseline' }}>
                      <span className="mono soft">{formatOffset(segment.offsetSeconds)}</span>
                      <span style={{ fontWeight: 700, fontSize: 13 }}>
                        {segment.speakerName ?? 'Participant'}
                      </span>
                    </div>
                    <div style={{ fontSize: 13, marginTop: 2 }}>{segment.text}</div>
                  </div>
                ))}
              </div>
            )}
            <div className="soft" style={{ fontSize: 11, marginTop: 12 }}>
              US05 · TeamCollaboration.AIMeetingAssistant - speaker, mm:ss offset and text of every
              transcribed segment.
            </div>
          </div>
        ) : (
          <div>
            <div className="chip-row" style={{ marginBottom: 10 }}>
              <Badge tone="s04">AI Meeting Assistant · simulated summariser</Badge>
              {summary ? <Badge>{summary.model}</Badge> : null}
              {summary ? <Badge>generated {relativeTime(summary.generatedAt)}</Badge> : null}
            </div>
            <div className="soft" style={{ fontSize: 12, marginBottom: 12 }}>
              Ending a meeting (US05) makes the MeetingOrchestrator request the transcript first and
              then the summary; the demo runs a deterministic summariser, so no external model call
              is made.
            </div>

            {summaryLoading ? (
              <div className="list">
                {[0, 1, 2].map((index) => (
                  <div className="skeleton" key={index} style={{ height: 60 }} />
                ))}
              </div>
            ) : summary === null ? (
              <Empty>
                No AI summary yet - end the meeting (or use “Generate summary”) to run the
                AIMeetingAssistant.
              </Empty>
            ) : (
              <div className="grid">
                <div>
                  <div style={{ fontWeight: 700, marginBottom: 4 }}>Overview</div>
                  <div className="sub" style={{ whiteSpace: 'pre-wrap' }}>
                    {summary.overview || 'The summariser did not produce an overview.'}
                  </div>
                </div>

                <div className="grid grid-2">
                  <div>
                    <div style={{ fontWeight: 700, marginBottom: 4 }}>Decisions</div>
                    {summary.decisions.length === 0 ? (
                      <div className="soft" style={{ fontSize: 12 }}>
                        No decision sentence was detected in this transcript.
                      </div>
                    ) : (
                      <ul style={{ margin: 0, paddingLeft: 18 }}>
                        {summary.decisions.map((decision, index) => (
                          <li key={index} style={{ fontSize: 13, marginBottom: 4 }}>
                            {decision}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                  <div>
                    <div style={{ fontWeight: 700, marginBottom: 4 }}>Action items</div>
                    {summary.actionItems.length === 0 ? (
                      <div className="soft" style={{ fontSize: 12 }}>
                        No action item was detected in this transcript.
                      </div>
                    ) : (
                      <ul style={{ margin: 0, paddingLeft: 18 }}>
                        {summary.actionItems.map((item, index) => (
                          <li key={index} style={{ fontSize: 13, marginBottom: 4 }}>
                            {item}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>

                <div className="progress-row">
                  <span className="label">Summariser confidence</span>
                  <div style={{ flex: 1 }}>
                    <Bar value={confidence} tone="linear-gradient(90deg, #9b51e0, #4f7cff)" />
                  </div>
                  <span className="value">{confidence}%</span>
                </div>
              </div>
            )}

            <div className="toolbar" style={{ marginTop: 12 }}>
              <Button
                size="sm"
                variant="primary"
                disabled={!canCollaborate || busy !== null}
                onClick={() => void regenerateSummary(selectedMeeting.id)}
              >
                {busy === `SUMMARY-${selectedMeeting.id}` ? 'Summarising…' : 'Generate summary'}
              </Button>
              <Button
                size="sm"
                onClick={() => {
                  void loadTranscript(selectedMeeting.id)
                  void loadSummary(selectedMeeting.id)
                }}
              >
                Refresh
              </Button>
            </div>

            <div className="soft" style={{ fontSize: 11, marginTop: 12 }}>
              US05 · TeamCollaboration.AIMeetingAssistant - overview, decisions, action items, model
              name and the confidence indicator of the generated summary.
            </div>
          </div>
        )}
      </Card>

      {createOpen ? (
        <Modal
          title="Schedule a meeting"
          onClose={() => setCreateOpen(false)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setCreateOpen(false)}>
                Cancel
              </Button>
              <Button variant="primary" disabled={creating} onClick={() => void createMeeting()}>
                {creating ? 'Scheduling…' : 'Schedule'}
              </Button>
            </>
          }
        >
          <Field label="Title">
            <input
              className="input"
              value={newTitle}
              placeholder="S03 weekly sync"
              onChange={(event: ChangeEvent<HTMLInputElement>) => setNewTitle(event.target.value)}
            />
          </Field>
          <Field label="Agenda">
            <textarea
              className="textarea"
              value={newAgenda}
              placeholder="Channels, mentions, digest ranking and the AI summary"
              onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setNewAgenda(event.target.value)}
            />
          </Field>
          <Field label="Start time">
            <input
              className="input"
              type="datetime-local"
              value={newStartAt}
              onChange={(event: ChangeEvent<HTMLInputElement>) => setNewStartAt(event.target.value)}
            />
          </Field>
          <div className="soft" style={{ fontSize: 12 }}>
            Leaving the start time empty schedules the meeting one hour from now. The join code is
            generated by the MeetingOrchestrator. Valid statuses:{' '}
            {MEETING_STATUSES.join(' · ')}.
          </div>
        </Modal>
      ) : null}
    </div>
  )
}
