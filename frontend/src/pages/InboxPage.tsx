import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, type DigestItemView, type NotificationView } from '../api/client'
import { useAuth } from '../app/AuthContext'
import { useToast } from '../app/ToastContext'
import { Badge, Button, Card, Empty, Stat, relativeTime } from '../components/ui'

/**
 * S03 - Team Collaboration: notification centre and the ranked smart digest.
 *
 * Level-3 mapping:
 *  - US04 TeamCollaboration.NotificationDispatcher - delivered notifications, read state.
 *  - US06 TeamCollaboration.SmartDigestEngine - ranking of the unread items.
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

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

/** Notification kind -> badge tone, so the source of an alert is readable at a glance. */
function kindTone(kind: string): BadgeTone {
  switch (kind) {
    case 'MENTION':
      return 's03'
    case 'TASK':
      return 's02'
    case 'MEETING':
      return 's01'
    case 'FILE':
      return 's04'
    default:
      return 'default'
  }
}

function priorityTone(priority: string): BadgeTone {
  switch (priority) {
    case 'HIGH':
      return 'warn'
    case 'LOW':
      return 'info'
    default:
      return 'default'
  }
}

/** Digest bucket produced by the SmartDigestEngine. */
function bucketTone(bucket: string): BadgeTone {
  switch (bucket) {
    case 'MENTION':
      return 'danger'
    case 'HIGH':
      return 'warn'
    default:
      return 'info'
  }
}

export function InboxPage() {
  const { can } = useAuth()
  const { push } = useToast()
  const navigate = useNavigate()
  const canNotify = can('S03.NOTIFICATION.USE')

  // US04 - notification centre state.
  const [notifications, setNotifications] = useState<NotificationView[]>([])
  const [loading, setLoading] = useState<boolean>(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [markingAll, setMarkingAll] = useState<boolean>(false)

  // US06 - smart digest state.
  const [digest, setDigest] = useState<DigestItemView[]>([])
  const [digestLoading, setDigestLoading] = useState<boolean>(true)

  const loadNotifications = useCallback(async () => {
    setLoading(true)
    try {
      setNotifications(await api.notifications())
    } catch (error) {
      push(errorText(error, 'Could not load your notifications'), 'error')
    } finally {
      setLoading(false)
    }
  }, [push])

  const loadDigest = useCallback(async () => {
    setDigestLoading(true)
    try {
      setDigest(await api.digest())
    } catch (error) {
      push(errorText(error, 'Could not build the smart digest'), 'error')
    } finally {
      setDigestLoading(false)
    }
  }, [push])

  useEffect(() => {
    void loadNotifications()
    void loadDigest()
  }, [loadNotifications, loadDigest])

  const unread = notifications.filter((notification) => !notification.read).length
  const mentionAlerts = notifications.filter(
    (notification) => !notification.read && notification.kind === 'MENTION'
  ).length

  /** US04 - mark one notification as read; the digest only ranks unread items. */
  const markRead = async (notificationId: number) => {
    setBusyId(notificationId)
    try {
      const updated = await api.markNotificationRead(notificationId)
      setNotifications((current) =>
        current.map((notification) => (notification.id === updated.id ? updated : notification))
      )
      await loadDigest()
      push('Notification marked as read', 'ok')
    } catch (error) {
      push(errorText(error, 'The notification could not be marked as read'), 'error')
    } finally {
      setBusyId(null)
    }
  }

  /** US04 - bulk acknowledgement from the notification centre header. */
  const markAllRead = async () => {
    setMarkingAll(true)
    try {
      const result = await api.markAllRead()
      await loadNotifications()
      await loadDigest()
      push(`${result.updated} notification(s) marked as read`, 'ok')
    } catch (error) {
      push(errorText(error, 'The notifications could not be marked as read'), 'error')
    } finally {
      setMarkingAll(false)
    }
  }

  return (
    <div className="grid">
      <div className="grid grid-4">
        <Stat label="Unread" value={unread} accent="s03" delta={`${notifications.length} delivered`} />
        <Stat label="Mentions waiting" value={mentionAlerts} delta="always ranked first" />
        <Stat label="Digest items" value={digest.length} delta="ranked by the digest engine" />
        <Stat
          label="Top rank score"
          value={digest.length > 0 ? digest[0].rankScore.toFixed(1) : '—'}
          delta={digest.length > 0 ? digest[0].bucket : 'no unread items'}
        />
      </div>

      <div className="grid grid-2">
        <Card
          title="Notifications"
          hint="Everything the NotificationDispatcher delivered to your account."
          actions={
            <div className="toolbar">
              <Button size="sm" onClick={() => void loadNotifications()} disabled={loading}>
                {loading ? 'Loading…' : 'Refresh'}
              </Button>
              <Button
                size="sm"
                variant="primary"
                disabled={!canNotify || markingAll || unread === 0}
                title={canNotify ? 'Mark every notification as read' : 'S03.NOTIFICATION.USE is required'}
                onClick={() => void markAllRead()}
              >
                {markingAll ? 'Marking…' : 'Mark all read'}
              </Button>
            </div>
          }
        >
          {loading ? (
            <div className="list">
              {[0, 1, 2].map((index) => (
                <div className="skeleton" key={index} style={{ height: 72 }} />
              ))}
            </div>
          ) : notifications.length === 0 ? (
            <Empty>No notifications yet - mentions, task and meeting updates land here.</Empty>
          ) : (
            <div className="list" style={{ maxHeight: 560, overflowY: 'auto' }}>
              {notifications.map((notification) => {
                const link = notification.link
                return (
                  <div
                    className="list-row"
                    key={notification.id}
                    style={{
                      alignItems: 'flex-start',
                      background: notification.read ? undefined : '#f7f9ff',
                      borderColor: notification.read ? undefined : '#d6e2ff'
                    }}
                  >
                    <div className="grow">
                      <div className="chip-row">
                        <Badge tone={kindTone(notification.kind)}>{notification.kind}</Badge>
                        <Badge tone={priorityTone(notification.priority)}>
                          {notification.priority}
                        </Badge>
                        {notification.read ? (
                          <Badge>read</Badge>
                        ) : (
                          <Badge tone="solid">unread</Badge>
                        )}
                        <span className="soft" style={{ fontSize: 11 }}>
                          {relativeTime(notification.deliveredAt)}
                        </span>
                      </div>
                      <div
                        className="title"
                        style={{ marginTop: 6, fontWeight: notification.read ? 500 : 700 }}
                      >
                        {notification.title}
                      </div>
                      {notification.body ? <div className="sub">{notification.body}</div> : null}
                      {notification.readAt ? (
                        <div className="soft" style={{ fontSize: 11, marginTop: 4 }}>
                          read {relativeTime(notification.readAt)}
                        </div>
                      ) : null}
                    </div>
                    <div className="toolbar" style={{ justifyContent: 'flex-end' }}>
                      {notification.read ? null : (
                        <Button
                          size="sm"
                          disabled={!canNotify || busyId === notification.id}
                          onClick={() => void markRead(notification.id)}
                        >
                          {busyId === notification.id ? 'Saving…' : 'Mark read'}
                        </Button>
                      )}
                      {link ? (
                        <Button size="sm" variant="ghost" onClick={() => navigate(link)}>
                          Open
                        </Button>
                      ) : null}
                    </div>
                  </div>
                )
              })}
            </div>
          )}

          <div className="soft" style={{ fontSize: 11, marginTop: 12 }}>
            US04 · TeamCollaboration.NotificationDispatcher - kind and priority of every delivered
            alert plus the per item and bulk read acknowledgement.
          </div>
        </Card>

        <Card
          title="Smart digest"
          hint="score = base(kind) + 10 × recency · @mentions always rank first."
          actions={
            <Button size="sm" onClick={() => void loadDigest()} disabled={digestLoading}>
              {digestLoading ? 'Ranking…' : 'Rebuild digest'}
            </Button>
          }
        >
          {digestLoading ? (
            <div className="list">
              {[0, 1, 2].map((index) => (
                <div className="skeleton" key={index} style={{ height: 62 }} />
              ))}
            </div>
          ) : digest.length === 0 ? (
            <Empty>Nothing to rank - every notification has been read.</Empty>
          ) : (
            <div className="list" style={{ maxHeight: 560, overflowY: 'auto' }}>
              {digest.map((item) => (
                <div className="list-row" key={item.id} style={{ alignItems: 'flex-start' }}>
                  <div style={{ width: 46, textAlign: 'center', flex: '0 0 auto' }}>
                    <div style={{ fontWeight: 700, fontSize: 16 }}>{item.rankScore.toFixed(1)}</div>
                    <div className="soft" style={{ fontSize: 10 }}>
                      score
                    </div>
                  </div>
                  <div className="grow">
                    <div className="chip-row">
                      <Badge tone={bucketTone(item.bucket)}>{item.bucket}</Badge>
                      <span className="soft" style={{ fontSize: 11 }}>
                        notification #{item.notificationId}
                      </span>
                    </div>
                    <div className="title" style={{ marginTop: 6 }}>
                      {item.notification?.title ?? 'Linked notification'}
                    </div>
                    {item.reason ? <div className="sub">{item.reason}</div> : null}
                    {item.notification ? (
                      <div className="soft" style={{ fontSize: 11, marginTop: 4 }}>
                        {item.notification.kind} · {relativeTime(item.notification.deliveredAt)}
                      </div>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="soft" style={{ fontSize: 12, marginTop: 12 }}>
            Ranking rule (US06): score = base(kind) + 10 × recency, where recency decays from 1.0 to
            0.0 over 24 hours. MENTION has base 90, TASK 70, MEETING 60, FILE 50 and SYSTEM 30, so a
            mention is always delivered first. Only unread items are ranked.
          </div>

          <div className="soft" style={{ fontSize: 11, marginTop: 12 }}>
            US06 · TeamCollaboration.SmartDigestEngine - ranked list with the score, the bucket and
            the reason of every unread item.
          </div>
        </Card>
      </div>
    </div>
  )
}
