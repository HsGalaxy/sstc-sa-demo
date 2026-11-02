import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  api,
  type ChannelView,
  type IntegrationStatus,
  type MeetingView,
  type MessageView,
  type PerformanceView,
  type TaskView
} from '../api/client'
import { useAuth } from '../app/AuthContext'
import {
  Avatar,
  Badge,
  Bar,
  Button,
  Card,
  Empty,
  ProgressRow,
  Sparkline,
  Stat,
  formatBytes,
  relativeTime,
  renderMentions
} from '../components/ui'

/**
 * Landing page of the dashboard. It aggregates one headline figure per sub-system so the
 * four modules of the architecture are visible at a glance:
 * S01 identity, S02 task dashboard, S03 collaboration, S04 toolbox.
 */
export function OverviewPage() {
  const { user, meta } = useAuth()
  const navigate = useNavigate()
  const [tasks, setTasks] = useState<TaskView[]>([])
  const [performance, setPerformance] = useState<PerformanceView | null>(null)
  const [channels, setChannels] = useState<ChannelView[]>([])
  const [messages, setMessages] = useState<MessageView[]>([])
  const [meetings, setMeetings] = useState<MeetingView[]>([])
  const [integrations, setIntegrations] = useState<IntegrationStatus | null>(null)
  const [files, setFiles] = useState<{ count: number } | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    Promise.all([
      api.tasks(),
      api.performance(),
      api.channels(),
      api.recentMessages(),
      api.meetings(),
      api.integrations(),
      api.fileStatistics()
    ])
      .then(([taskList, performanceView, channelList, messageList, meetingList, status, stats]) => {
        if (!alive) return
        setTasks(taskList)
        setPerformance(performanceView)
        setChannels(channelList)
        setMessages(messageList)
        setMeetings(meetingList)
        setIntegrations(status)
        const total = (stats as { totalFiles?: number }).totalFiles
        setFiles({ count: total ?? 0 })
      })
      .catch(() => undefined)
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  const open = tasks.filter((task) => task.status !== 'DONE')
  const overdue = tasks.filter((task) => task.overdue)
  const trend = performance?.trend ?? []

  if (loading) {
    return (
      <div className="grid grid-4">
        {[0, 1, 2, 3, 4, 5, 6, 7].map((index) => (
          <div className="skeleton" key={index} style={{ height: 108 }} />
        ))}
      </div>
    )
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <Avatar name={user?.displayName} color={user?.avatarColor} size="lg" />
          <div style={{ flex: 1, minWidth: 260 }}>
            <div style={{ fontSize: 18, fontWeight: 700 }}>
              Welcome back, {user?.displayName?.split(' ')[0]}
            </div>
            <div className="muted" style={{ fontSize: 13 }}>
              {user?.roleLabel ?? user?.role} · {open.length} open tasks · {overdue.length} overdue ·{' '}
              {channels.length} channels
            </div>
          </div>
          <div className="chip-row">
            {(meta?.modules ?? []).map((module) => (
              <Badge key={module.id} tone={module.id.toLowerCase() as 's01'}>
                {module.id} {module.name}
              </Badge>
            ))}
          </div>
        </div>
      </Card>

      <div className="grid grid-4">
        <Stat
          label="S02 · Open tasks"
          value={open.length}
          accent="s02"
          delta={`${tasks.filter((task) => task.status === 'DONE').length} completed`}
        />
        <Stat
          label="S02 · Overdue"
          value={overdue.length}
          accent="s02"
          delta={`${performance?.bottlenecks.length ?? 0} bottlenecks flagged`}
        />
        <Stat
          label="S03 · Channels"
          value={channels.length}
          accent="s03"
          delta={`${channels.reduce((sum, channel) => sum + channel.messageCount, 0)} messages`}
        />
        <Stat
          label="S04 · Toolbox"
          value={integrations?.repositories ?? 0}
          accent="s04"
          delta={`${integrations?.environmentsRunning ?? 0} environments running`}
        />
      </div>

      <div className="grid grid-2-1">
        <Card
          title="Delivery indicators"
          hint="S02 · DisplayPanel - process and outcome indicators of the current cycle"
          actions={
            <Button size="sm" variant="ghost" onClick={() => navigate('/okr')}>
              Open OKR & performance
            </Button>
          }
        >
          <div className="grid grid-3" style={{ marginBottom: 16 }}>
            <div>
              <div className="soft" style={{ fontSize: 11, letterSpacing: 0.6 }}>
                TEAM OKR SCORE
              </div>
              <div style={{ fontSize: 24, fontWeight: 700 }}>{performance?.teamOkrScore ?? 0}%</div>
              <Bar value={performance?.teamOkrScore ?? 0} />
            </div>
            <div>
              <div className="soft" style={{ fontSize: 11, letterSpacing: 0.6 }}>
                ON-TIME RATE
              </div>
              <div style={{ fontSize: 24, fontWeight: 700 }}>{performance?.teamOnTimeRate ?? 0}%</div>
              <Bar value={performance?.teamOnTimeRate ?? 0} />
            </div>
            <div>
              <div className="soft" style={{ fontSize: 11, letterSpacing: 0.6 }}>
                AVG THROUGHPUT
              </div>
              <div style={{ fontSize: 24, fontWeight: 700 }}>{performance?.averageThroughput ?? 0} h</div>
              <div className="soft" style={{ fontSize: 12 }}>
                logged effort per snapshot
              </div>
            </div>
          </div>

          {trend.length > 0 && (
            <>
              <Sparkline values={trend.map((item) => item.onTimeRate)} color="#22a06b" label="On-time rate" />
              <div className="soft" style={{ fontSize: 12 }}>
                On-time rate over {trend.length} captured snapshots
              </div>
            </>
          )}

          {(performance?.members ?? []).slice(0, 4).map((member) => (
            <div className="progress-row" key={member.userId} style={{ marginTop: 10 }}>
              <span className="label">
                {member.name} · grade {member.grade}
              </span>
              <div style={{ flex: 1 }}>
                <Bar value={member.totalScore} />
              </div>
              <span className="value">{member.totalScore}</span>
            </div>
          ))}
        </Card>

        <Card
          title="External integrations"
          hint="S04 · Toolbox adapters (Office, Git, container runtime, storage)"
        >
          <div className="list">
            <div className="list-row">
              <Badge tone="s04">Office</Badge>
              <div className="grow">
                <div className="title">{integrations?.officeProvider}</div>
                <div className="sub">
                  quota {integrations?.officeQuotaRemaining}/{integrations?.officeQuotaTotal} operations left
                </div>
              </div>
            </div>
            <div className="list-row">
              <Badge tone="s04">Git</Badge>
              <div className="grow">
                <div className="title">{integrations?.gitProvider}</div>
                <div className="sub">{integrations?.repositories ?? 0} repositories</div>
              </div>
            </div>
            <div className="list-row">
              <Badge tone="s04">Runtime</Badge>
              <div className="grow">
                <div className="title">{integrations?.containerRuntime}</div>
                <div className="sub">
                  {integrations?.environmentsRunning ?? 0}/{integrations?.environmentsTotal ?? 0} environments running
                </div>
              </div>
            </div>
            <div className="list-row">
              <Badge tone="s04">Storage</Badge>
              <div className="grow">
                <div className="title">{integrations?.storageProvider}</div>
                <div className="sub">
                  {formatBytes(integrations?.storageUsedBytes ?? 0)} used · max upload{' '}
                  {integrations?.maxUploadMb ?? 0} MB
                </div>
              </div>
            </div>
          </div>
          <div style={{ marginTop: 12 }}>
            <Button size="sm" variant="ghost" onClick={() => navigate('/toolbox/files')}>
              Open project files
            </Button>
          </div>
        </Card>
      </div>

      <div className="grid grid-2">
        <Card
          title="Recent conversations"
          hint="S03 · ChannelMessageManager - latest messages across the channels"
          actions={
            <Button size="sm" variant="ghost" onClick={() => navigate('/collaboration')}>
              Open chat
            </Button>
          }
        >
          {messages.length === 0 ? (
            <Empty>No message yet.</Empty>
          ) : (
            <div className="list">
              {messages.slice(0, 5).map((message) => (
                <div className="list-row" key={message.id}>
                  <Avatar name={message.authorName} size="sm" />
                  <div className="grow">
                    <div className="title">
                      {message.authorName} <span className="soft">in #{message.channelName}</span>
                    </div>
                    <div className="sub">{renderMentions(message.body.slice(0, 120))}</div>
                  </div>
                  <span className="soft" style={{ fontSize: 11 }}>
                    {relativeTime(message.postedAt)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card
          title="Meetings"
          hint="S03 · MeetingOrchestrator + AIMeetingAssistant"
          actions={
            <Button size="sm" variant="ghost" onClick={() => navigate('/meetings')}>
              Open meetings
            </Button>
          }
        >
          {meetings.length === 0 ? (
            <Empty>No meeting scheduled.</Empty>
          ) : (
            <div className="list">
              {meetings.slice(0, 4).map((meeting) => (
                <div className="list-row" key={meeting.id}>
                  <Badge
                    tone={
                      meeting.status === 'ENDED'
                        ? 'ok'
                        : meeting.status === 'IN_PROGRESS'
                          ? 'warn'
                          : 'info'
                    }
                  >
                    {meeting.status}
                  </Badge>
                  <div className="grow">
                    <div className="title">{meeting.title}</div>
                    <div className="sub">
                      host {meeting.hostName} · {relativeTime(meeting.startAt)} ·{' '}
                      {meeting.transcriptSegments} transcript segments
                    </div>
                  </div>
                  {meeting.hasSummary && <Badge tone="s04">AI summary</Badge>}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <Card
        title="Next deadlines"
        hint="S02 · tasks ordered by due date, with the overdue flag of the display panel"
      >
        {open.length === 0 ? (
          <Empty>Nothing open. Enjoy the calm.</Empty>
        ) : (
          <div className="list">
            {open
              .slice()
              .sort((a, b) => (a.dueDate ?? '').localeCompare(b.dueDate ?? ''))
              .slice(0, 6)
              .map((task) => (
                <div className="list-row" key={task.id}>
                  <Badge tone={task.overdue ? 'danger' : 'default'}>{task.code}</Badge>
                  <div className="grow">
                    <div className="title">{task.title}</div>
                    <div className="sub">
                      {task.assigneeName ?? 'unassigned'} · {task.statusLabel} · {task.cycleName ?? 'no cycle'}
                    </div>
                  </div>
                  <span className="soft" style={{ fontSize: 12 }}>
                    due {task.dueDate ?? '—'}
                  </span>
                </div>
              ))}
          </div>
        )}
      </Card>

      <Card title="Sub-system coverage" hint="How the four modules map onto the user stories">
        <div className="grid grid-2">
          {(meta?.modules ?? []).map((module) => (
            <div className="list-row" key={module.id}>
              <Badge tone={module.id.toLowerCase() as 's01'}>{module.id}</Badge>
              <div className="grow">
                <div className="title">{module.name}</div>
                <div className="sub">{module.description}</div>
                <div className="chip-row" style={{ marginTop: 6 }}>
                  {module.userStories.map((story) => (
                    <span className="badge" key={story}>
                      {story}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}
