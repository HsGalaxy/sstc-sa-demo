import { useCallback, useEffect, useRef, useState, type ChangeEvent } from 'react'
import { api, type ChannelView, type MentionView, type MessageView } from '../api/client'
import { useAuth } from '../app/AuthContext'
import { useToast } from '../app/ToastContext'
import {
  Avatar,
  Badge,
  Button,
  Card,
  Empty,
  Field,
  Modal,
  Stat,
  relativeTime,
  renderMentions
} from '../components/ui'

/**
 * S03 - Team Collaboration: channels, group chat and @mentions.
 *
 * Level-3 mapping:
 *  - US02 TeamCollaboration.ChannelMessageManager - channel list, thread, publishing.
 *  - US03 TeamCollaboration.MentionDetector - @mention highlighting and the mention inbox.
 */

/** Seeded demo accounts recognised by the MentionDetector of the backend. */
const MENTION_HINTS: string[] = ['lead', 'manager', 'yunteng', 'jinghui', 'changyang', 'hao']

const AVATAR_COLORS: string[] = ['#4f7cff', '#22a06b', '#f2994a', '#9b51e0', '#2d9cdb', '#eb5757']

/** Stable colour per author so the thread does not flicker between renders. */
function avatarColor(seed: string): string {
  let hash = 0
  for (let index = 0; index < seed.length; index += 1) {
    hash = (hash * 31 + seed.charCodeAt(index)) % 1000003
  }
  return AVATAR_COLORS[hash % AVATAR_COLORS.length]
}

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

/**
 * The UI kit derives initials from a display name; when the API already returns
 * them, the letters are spaced so that Avatar renders exactly those initials.
 */
function avatarLabel(message: MessageView): string {
  const initials = message.authorInitials?.trim()
  if (initials) return initials.split('').join(' ')
  return message.authorName ?? `User ${message.authorId}`
}

export function CollaborationPage() {
  const { can } = useAuth()
  const { push } = useToast()
  const canCollaborate = can('S03.COLLABORATION.USE')

  // US02 - channel list state.
  const [channels, setChannels] = useState<ChannelView[]>([])
  const [channelsLoading, setChannelsLoading] = useState<boolean>(true)
  const [activeChannelId, setActiveChannelId] = useState<number | null>(null)

  // US02 - thread state.
  const [messages, setMessages] = useState<MessageView[]>([])
  const [threadLoading, setThreadLoading] = useState<boolean>(false)
  const [threadNonce, setThreadNonce] = useState<number>(0)
  const [draft, setDraft] = useState<string>('')
  const [sending, setSending] = useState<boolean>(false)
  const threadRef = useRef<HTMLDivElement | null>(null)
  /** Keeps the 10 s poll from pushing the same failure toast over and over. */
  const pollFailed = useRef<boolean>(false)

  // US03 - mention panel state.
  const [mentions, setMentions] = useState<MentionView[]>([])
  const [mentionsLoading, setMentionsLoading] = useState<boolean>(true)

  // US02 - "new channel" modal state.
  const [createOpen, setCreateOpen] = useState<boolean>(false)
  const [newName, setNewName] = useState<string>('')
  const [newTopic, setNewTopic] = useState<string>('')
  const [newPurpose, setNewPurpose] = useState<string>('')
  const [creating, setCreating] = useState<boolean>(false)

  const loadChannels = useCallback(
    async (selectFallback: boolean) => {
      setChannelsLoading(true)
      try {
        const list = await api.channels()
        setChannels(list)
        if (selectFallback) {
          setActiveChannelId((current) =>
            current !== null && list.some((channel) => channel.id === current)
              ? current
              : list.length > 0
                ? list[0].id
                : null
          )
        }
      } catch (error) {
        push(errorText(error, 'Could not load the channel list'), 'error')
      } finally {
        setChannelsLoading(false)
      }
    },
    [push]
  )

  const loadMentions = useCallback(async () => {
    setMentionsLoading(true)
    try {
      setMentions(await api.mentions())
    } catch (error) {
      push(errorText(error, 'Could not load your mentions'), 'error')
    } finally {
      setMentionsLoading(false)
    }
  }, [push])

  useEffect(() => {
    void loadChannels(true)
    void loadMentions()
  }, [loadChannels, loadMentions])

  /**
   * US02 - load the selected channel history and poll it every 10 seconds, so a
   * second browser window shows the messages published by a teammate.
   */
  useEffect(() => {
    if (activeChannelId === null) {
      setMessages([])
      return
    }
    const channelId = activeChannelId
    let alive = true

    const fetchThread = async (quiet: boolean) => {
      if (!quiet) setThreadLoading(true)
      try {
        const list = await api.channelMessages(channelId, 60)
        if (!alive) return
        setMessages(list)
        pollFailed.current = false
      } catch (error) {
        if (!alive) return
        if (!quiet || !pollFailed.current) {
          pollFailed.current = true
          push(errorText(error, 'Could not load the channel history'), 'error')
        }
      } finally {
        if (alive && !quiet) setThreadLoading(false)
      }
    }

    void fetchThread(false)
    const timer = window.setInterval(() => {
      void fetchThread(true)
    }, 10000)

    return () => {
      alive = false
      window.clearInterval(timer)
    }
  }, [activeChannelId, threadNonce, push])

  /** Keep the newest message visible after a send or a poll refresh. */
  useEffect(() => {
    const node = threadRef.current
    if (node) node.scrollTop = node.scrollHeight
  }, [messages])

  const activeChannel = channels.find((channel) => channel.id === activeChannelId) ?? null
  const alertedMentions = mentions.filter((mention) => mention.alerted).length

  const channelNameOf = (channelId: number): string =>
    channels.find((channel) => channel.id === channelId)?.name ?? `channel ${channelId}`

  /** US02 - publish a message; the backend also runs the MentionDetector. */
  const send = async () => {
    const body = draft.trim()
    if (!body || activeChannelId === null) return
    setSending(true)
    try {
      const created = await api.publishMessage(activeChannelId, { body })
      setMessages((current) => [...current, created])
      setDraft('')
      setChannels((current) =>
        current.map((channel) =>
          channel.id === created.channelId
            ? { ...channel, messageCount: channel.messageCount + 1, lastActivity: created.postedAt }
            : channel
        )
      )
      push(`Message published to #${created.channelName ?? channelNameOf(created.channelId)}`, 'ok')
      // US03 - a message carrying @mentions refreshes the mention panel.
      if (created.mentions.length > 0) {
        await loadMentions()
        push(`${created.mentions.length} teammate(s) notified by mention`, 'info')
      }
    } catch (error) {
      push(errorText(error, 'The message could not be published'), 'error')
    } finally {
      setSending(false)
    }
  }

  /** US02 - create a channel and open it straight away. */
  const createChannel = async () => {
    const name = newName.trim()
    if (!name) {
      push('A channel name is required', 'error')
      return
    }
    setCreating(true)
    try {
      const created = await api.createChannel({
        name,
        topic: newTopic.trim() || undefined,
        purpose: newPurpose.trim() || undefined
      })
      await loadChannels(false)
      setActiveChannelId(created.id)
      setCreateOpen(false)
      setNewName('')
      setNewTopic('')
      setNewPurpose('')
      push(`Channel #${created.name} created`, 'ok')
    } catch (error) {
      push(errorText(error, 'The channel could not be created'), 'error')
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="grid">
      <div className="grid grid-4">
        <Stat label="Channels" value={channels.length} accent="s03" />
        <Stat
          label="Members in channel"
          value={activeChannel?.memberCount ?? 0}
          delta={activeChannel ? `#${activeChannel.name}` : 'no channel selected'}
        />
        <Stat label="Messages in thread" value={messages.length} delta="last 60 loaded" />
        <Stat label="My mentions" value={mentions.length} delta={`${alertedMentions} alerted`} />
      </div>

      <div className="grid grid-2-1">
        <Card
          title={activeChannel ? `#${activeChannel.name}` : 'Team channels'}
          hint={
            activeChannel?.topic ??
            'Pick a channel on the left, or create one for a new workstream.'
          }
          actions={
            <Button
              size="sm"
              variant="primary"
              disabled={!canCollaborate}
              title={canCollaborate ? 'Create a channel' : 'S03.COLLABORATION.USE is required'}
              onClick={() => setCreateOpen(true)}
            >
              New channel
            </Button>
          }
        >
          <div className="chat">
            <div className="channel-list">
              {channelsLoading ? (
                [0, 1, 2, 3].map((index) => (
                  <div className="skeleton" key={index} style={{ height: 52 }} />
                ))
              ) : channels.length === 0 ? (
                <Empty>No channels yet - create the first one.</Empty>
              ) : (
                channels.map((channel) => (
                  <div
                    key={channel.id}
                    className={`channel-item ${channel.id === activeChannelId ? 'active' : ''}`}
                    onClick={() => setActiveChannelId(channel.id)}
                  >
                    <div className="name">
                      #{channel.name}
                      {channel.privateChannel ? (
                        <>
                          {' '}
                          <Badge tone="s04">private</Badge>
                        </>
                      ) : null}
                    </div>
                    <div className="meta">{channel.topic ?? 'No topic set'}</div>
                    <div className="meta">
                      {channel.memberCount} members · {channel.messageCount} messages
                    </div>
                    <div className="meta">active {relativeTime(channel.lastActivity)}</div>
                  </div>
                ))
              )}
            </div>

            <div>
              {activeChannelId === null ? (
                <Empty>Select a channel to read the conversation.</Empty>
              ) : threadLoading ? (
                <div className="thread">
                  {[0, 1, 2].map((index) => (
                    <div className="skeleton" key={index} style={{ height: 66 }} />
                  ))}
                </div>
              ) : messages.length === 0 ? (
                <Empty>No messages in this channel yet - say hello to the team.</Empty>
              ) : (
                <div className="thread" ref={threadRef}>
                  {messages.map((message) => {
                    const isSystem = message.kind === 'SYSTEM'
                    return (
                      <div className="msg" key={message.id}>
                        <Avatar
                          name={avatarLabel(message)}
                          color={avatarColor(message.authorName ?? String(message.authorId))}
                        />
                        <div className="bubble" style={isSystem ? { opacity: 0.72 } : undefined}>
                          <div className="head">
                            <span className="name">{message.authorName ?? 'Unknown member'}</span>
                            <span className="time">{relativeTime(message.postedAt)}</span>
                            {message.mentions.length > 0 ? (
                              <Badge tone="s03">
                                {message.mentions.length} mention
                                {message.mentions.length > 1 ? 's' : ''}
                              </Badge>
                            ) : null}
                            {isSystem ? <Badge>system</Badge> : null}
                            {message.edited ? <Badge>edited</Badge> : null}
                          </div>
                          <div
                            className="body"
                            style={isSystem ? { fontStyle: 'italic', color: 'var(--text-muted)' } : undefined}
                          >
                            {renderMentions(message.body)}
                          </div>
                          {message.attachmentName ? (
                            <div className="sub soft" style={{ fontSize: 12, marginTop: 4 }}>
                              📎 {message.attachmentName}
                            </div>
                          ) : null}
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}

              <div className="composer">
                <textarea
                  className="textarea"
                  value={draft}
                  disabled={activeChannelId === null}
                  placeholder={
                    activeChannel
                      ? `Message #${activeChannel.name} - use @username to notify a teammate`
                      : 'Select a channel first'
                  }
                  onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setDraft(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' && !event.shiftKey) {
                      event.preventDefault()
                      void send()
                    }
                  }}
                />
                <Button
                  variant="primary"
                  disabled={sending || draft.trim().length === 0 || activeChannelId === null}
                  onClick={() => void send()}
                >
                  {sending ? 'Sending…' : 'Send'}
                </Button>
              </div>
              <div className="soft" style={{ fontSize: 11, marginTop: 6 }}>
                Typing @username notifies that teammate - the thread polls every 10 seconds, so a
                second browser window shows new messages. Enter sends, Shift+Enter adds a line.
              </div>
            </div>
          </div>

          <div className="soft" style={{ fontSize: 11, marginTop: 12 }}>
            US02 · TeamCollaboration.ChannelMessageManager - channel list, group thread and message
            publishing.
          </div>
        </Card>

        <div className="grid">
          <Card
            title="My mentions"
            hint="Every @mention that targets your account, newest first."
            actions={
              <Button size="sm" onClick={() => void loadMentions()} disabled={mentionsLoading}>
                {mentionsLoading ? 'Loading…' : 'Refresh'}
              </Button>
            }
          >
            {mentionsLoading ? (
              <div className="list">
                {[0, 1, 2].map((index) => (
                  <div className="skeleton" key={index} style={{ height: 48 }} />
                ))}
              </div>
            ) : mentions.length === 0 ? (
              <Empty>No mentions yet - type @username in a channel to notify a teammate.</Empty>
            ) : (
              <div className="list">
                {mentions.map((mention) => (
                  <div className="list-row" key={mention.id}>
                    <div className="grow">
                      <div className="chip-row">
                        <span className="mono" style={{ fontWeight: 700 }}>
                          {mention.token}
                        </span>
                        <Badge tone={mention.alerted ? 'danger' : 'default'}>
                          {mention.alerted ? 'alerted' : 'queued'}
                        </Badge>
                      </div>
                      <div className="sub">
                        #{channelNameOf(mention.channelId)} · {relativeTime(mention.createdAt)}
                      </div>
                    </div>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setActiveChannelId(mention.channelId)}
                    >
                      Open
                    </Button>
                  </div>
                ))}
              </div>
            )}

            <div className="soft" style={{ fontSize: 11, marginTop: 12 }}>
              US03 · TeamCollaboration.MentionDetector - parsing of the @username tokens and the
              targeted alerts raised by the NotificationDispatcher.
            </div>
          </Card>

          <Card title="Mention guide" hint="Seeded demo accounts that can be mentioned.">
            <div className="chip-row">
              {MENTION_HINTS.map((username) => (
                <span className="mention-chip" key={username}>
                  @{username}
                </span>
              ))}
            </div>
            <div className="soft" style={{ fontSize: 12, marginTop: 10 }}>
              A mention to another account creates a MENTION notification and puts the item at the
              top of the smart digest of that teammate.
            </div>
          </Card>
        </div>
      </div>

      {createOpen ? (
        <Modal
          title="New channel"
          onClose={() => setCreateOpen(false)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setCreateOpen(false)}>
                Cancel
              </Button>
              <Button variant="primary" disabled={creating} onClick={() => void createChannel()}>
                {creating ? 'Creating…' : 'Create channel'}
              </Button>
            </>
          }
        >
          <Field label="Name">
            <input
              className="input"
              value={newName}
              placeholder="s03-collaboration"
              onChange={(event: ChangeEvent<HTMLInputElement>) => setNewName(event.target.value)}
            />
          </Field>
          <Field label="Topic">
            <input
              className="input"
              value={newTopic}
              placeholder="Meetings, transcripts and the AI summary"
              onChange={(event: ChangeEvent<HTMLInputElement>) => setNewTopic(event.target.value)}
            />
          </Field>
          <Field label="Purpose">
            <textarea
              className="textarea"
              value={newPurpose}
              placeholder="Why this channel exists and who should join it"
              onChange={(event: ChangeEvent<HTMLTextAreaElement>) => setNewPurpose(event.target.value)}
            />
          </Field>
          <div className="soft" style={{ fontSize: 12 }}>
            Creating a channel also joins your account as the first member
            (TeamCollaboration.ChannelMessageManager).
          </div>
        </Modal>
      ) : null}
    </div>
  )
}
