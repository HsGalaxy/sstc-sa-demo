import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, type OperationLogView } from '../api/client'
import { useToast } from '../app/ToastContext'
import { Badge, Button, Card, Empty, Stat, formatDateTime } from '../components/ui'

/**
 * S02 · US04 - TaskDashboard.OperationLogger
 * Full lifecycle event log of the task controller: who did what, to which target,
 * with which outcome. This is the evidence behind the "trace historical operations"
 * requirement of the senior manager.
 */
export function OperationLogPage() {
  const { push } = useToast()
  const [logs, setLogs] = useState<OperationLogView[]>([])
  const [action, setAction] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setLogs(await api.operations(150, action || undefined))
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to load the operation log', 'error')
    } finally {
      setLoading(false)
    }
  }, [action, push])

  useEffect(() => {
    void load()
  }, [load])

  const actions = useMemo(() => Array.from(new Set(logs.map((log) => log.action))).sort(), [logs])
  const failures = logs.filter((log) => log.outcome !== 'SUCCESS').length

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-4">
        <Stat label="Events" value={logs.length} accent="s02" delta="Newest first" />
        <Stat label="Distinct actions" value={actions.length} accent="s03" delta="Create, update, move, upload…" />
        <Stat label="Failures" value={failures} accent="s04" delta="Rejected business rules" />
        <Stat
          label="Actors"
          value={new Set(logs.map((log) => log.actorName)).size}
          accent="s01"
          delta="Accounts that produced an event"
        />
      </div>

      <Card
        title="Operation log"
        hint="US04 · OperationLogger records every lifecycle event so a manager can trace history"
        actions={
          <>
            <select
              className="select"
              style={{ width: 200 }}
              value={action}
              onChange={(event) => setAction(event.target.value)}
            >
              <option value="">All actions</option>
              {actions.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <Button size="sm" onClick={() => void load()}>
              Refresh
            </Button>
          </>
        }
        padded={false}
      >
        {loading ? (
          <div className="card-body">
            <div className="skeleton" style={{ height: 240 }} />
          </div>
        ) : logs.length === 0 ? (
          <Empty>No operation recorded yet. Create or move a task to generate events.</Empty>
        ) : (
          <div className="table-wrap" style={{ maxHeight: 620 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Target</th>
                  <th>Detail</th>
                  <th>Source</th>
                  <th>Outcome</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td className="soft">{formatDateTime(log.createdAt)}</td>
                    <td>{log.actorName}</td>
                    <td>
                      <Badge tone="s02">{log.action}</Badge>
                    </td>
                    <td className="mono soft">
                      {log.targetType} {log.targetId}
                    </td>
                    <td className="soft">{log.detail}</td>
                    <td>
                      <Badge tone={log.source === 'API' ? 'info' : 'default'}>{log.source}</Badge>
                    </td>
                    <td>
                      <Badge tone={log.outcome === 'SUCCESS' ? 'ok' : 'danger'}>{log.outcome}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
