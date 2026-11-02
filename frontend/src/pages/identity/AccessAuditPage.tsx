import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, type AccessAuditView, type OperationLogView } from '../../api/client'
import { useToast } from '../../app/ToastContext'
import { Badge, Button, Card, Empty, Stat, formatDateTime } from '../../components/ui'

/**
 * S01 · US04 - AuthorizationAuditController
 * Every decision taken by the authorization service, plus the unified platform trail
 * written by the S02 Operation Logger.
 */
export function AccessAuditPage() {
  const { push } = useToast()
  const [audits, setAudits] = useState<AccessAuditView[]>([])
  const [operations, setOperations] = useState<OperationLogView[]>([])
  const [platform, setPlatform] = useState<Record<string, unknown>[]>([])
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [auditList, operationList, platformList] = await Promise.all([
        api.accessAudits(120),
        api.operations(60),
        api.audits({ limit: 60 })
      ])
      setAudits(auditList)
      setOperations(operationList)
      setPlatform(platformList)
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to load the audit trail', 'error')
    } finally {
      setLoading(false)
    }
  }, [push])

  useEffect(() => {
    void load()
  }, [load])

  const filtered = useMemo(() => {
    if (!filter.trim()) return audits
    const needle = filter.trim().toLowerCase()
    return audits.filter(
      (audit) =>
        audit.permissionCode?.toLowerCase().includes(needle) ||
        audit.resource?.toLowerCase().includes(needle) ||
        audit.username?.toLowerCase().includes(needle) ||
        audit.decision?.toLowerCase().includes(needle)
    )
  }, [audits, filter])

  const stats = useMemo(() => {
    const allow = audits.filter((audit) => audit.decision === 'ALLOW').length
    const deny = audits.filter((audit) => audit.decision === 'DENY').length
    const avg =
      audits.length === 0
        ? 0
        : Math.round(
            audits.reduce((sum, audit) => sum + (audit.durationMs ?? 0), 0) / audits.length
          )
    return { allow, deny, avg }
  }, [audits])

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-4">
        <Stat label="Decisions" value={audits.length} accent="s01" delta="Last 120 authorization checks" />
        <Stat label="Allowed" value={stats.allow} accent="s02" delta="Access granted" />
        <Stat label="Denied" value={stats.deny} accent="s03" delta="Access rejected" />
        <Stat label="Avg latency" value={`${stats.avg} ms`} accent="s04" delta="Per protected call" />
      </div>

      <Card
        title="Access audit"
        hint="US04 · AuthorizationAuditController records the checked permission, the decision and the reason"
        actions={
          <>
            <input
              className="input"
              style={{ width: 220 }}
              placeholder="Filter by user, permission, decision…"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
            <Button size="sm" onClick={() => void load()}>
              Refresh
            </Button>
          </>
        }
        padded={false}
      >
        {loading ? (
          <div className="card-body">
            <div className="skeleton" style={{ height: 200 }} />
          </div>
        ) : filtered.length === 0 ? (
          <Empty>No access decision recorded yet. Browse the platform to generate some.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Account</th>
                  <th>Permission</th>
                  <th>Resource</th>
                  <th>Decision</th>
                  <th>Reason</th>
                  <th>Latency</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((audit) => (
                  <tr key={audit.id}>
                    <td className="soft">{formatDateTime(audit.createdAt)}</td>
                    <td>{audit.username ?? <span className="soft">anonymous</span>}</td>
                    <td className="mono">{audit.permissionCode}</td>
                    <td className="mono soft">{audit.resource}</td>
                    <td>
                      <Badge tone={audit.decision === 'ALLOW' ? 'ok' : 'danger'}>{audit.decision}</Badge>
                    </td>
                    <td className="soft">{audit.reason}</td>
                    <td className="soft">{audit.durationMs ?? 0} ms</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="grid grid-2">
        <Card
          title="Operation log"
          hint="US04 · TaskDashboard.OperationLogger - full lifecycle events of the task controller"
          padded={false}
        >
          <div className="table-wrap" style={{ maxHeight: 420 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Target</th>
                  <th>Detail</th>
                </tr>
              </thead>
              <tbody>
                {operations.map((operation) => (
                  <tr key={operation.id}>
                    <td className="soft">{formatDateTime(operation.createdAt)}</td>
                    <td>{operation.actorName}</td>
                    <td>
                      <Badge tone="s02">{operation.action}</Badge>
                    </td>
                    <td className="mono soft">
                      {operation.targetType} {operation.targetId}
                    </td>
                    <td className="soft">{operation.detail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        <Card
          title="Unified platform trail"
          hint="Cross-cutting audit shared by S01–S04 (module, action, outcome)"
          padded={false}
        >
          <div className="table-wrap" style={{ maxHeight: 420 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Module</th>
                  <th>Action</th>
                  <th>Actor</th>
                  <th>Outcome</th>
                </tr>
              </thead>
              <tbody>
                {platform.map((entry, index) => (
                  <tr key={String(entry.id ?? index)}>
                    <td className="soft">{formatDateTime(String(entry.createdAt ?? ''))}</td>
                    <td>
                      <Badge tone={String(entry.module ?? '').toLowerCase() as 's01'}>{String(entry.module ?? '')}</Badge>
                    </td>
                    <td className="mono">{String(entry.action ?? '')}</td>
                    <td>{String(entry.actorName ?? 'system')}</td>
                    <td>
                      <Badge tone={entry.outcome === 'SUCCESS' ? 'ok' : 'danger'}>
                        {String(entry.outcome ?? '')}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </div>
  )
}
