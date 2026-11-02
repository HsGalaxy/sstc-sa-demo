import { useCallback, useEffect, useState } from 'react'
import { api, type OkrView, type PerformanceView } from '../api/client'
import { useToast } from '../app/ToastContext'
import {
  Avatar,
  Badge,
  Bar,
  Button,
  Card,
  Empty,
  Field,
  Modal,
  ProgressRow,
  Sparkline,
  Stat
} from '../components/ui'

const STATUS_TONE: Record<string, 'ok' | 'warn' | 'danger' | 'info'> = {
  ON_TRACK: 'ok',
  ACHIEVED: 'ok',
  AT_RISK: 'warn',
  OFF_TRACK: 'danger'
}

const GRADE_TONE: Record<string, 'ok' | 'info' | 'warn' | 'danger'> = {
  A: 'ok',
  B: 'info',
  C: 'warn',
  D: 'danger'
}

/**
 * S02 · US05 / US06 - TaskDashboard.DisplayPanel + PerformanceCalculator
 * OKR tracking, real time indicators, the bottleneck list and the weighted
 * individual performance score.
 */
export function OkrPage() {
  const { push } = useToast()
  const [okrs, setOkrs] = useState<OkrView[]>([])
  const [performance, setPerformance] = useState<PerformanceView | null>(null)
  const [loading, setLoading] = useState(true)
  const [showOkr, setShowOkr] = useState(false)
  const [okrForm, setOkrForm] = useState({ objective: '', weight: '1.0' })
  const [krTarget, setKrTarget] = useState<{ okrId: number; title: string; target: string } | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [okrList, performanceView] = await Promise.all([api.okrs(), api.performance()])
      setOkrs(okrList)
      setPerformance(performanceView)
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to load the indicators', 'error')
    } finally {
      setLoading(false)
    }
  }, [push])

  useEffect(() => {
    void load()
  }, [load])

  const createOkr = async () => {
    try {
      await api.createOkr({ objective: okrForm.objective, weight: Number(okrForm.weight) })
      push('Objective created', 'ok')
      setShowOkr(false)
      setOkrForm({ objective: '', weight: '1.0' })
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to create the objective', 'error')
    }
  }

  const addKeyResult = async () => {
    if (!krTarget) return
    try {
      await api.addKeyResult(krTarget.okrId, {
        title: krTarget.title,
        targetValue: Number(krTarget.target),
        currentValue: 0,
        unit: '%'
      })
      push('Key result added, OKR progress recalculated', 'ok')
      setKrTarget(null)
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to add the key result', 'error')
    }
  }

  const nudge = async (keyResultId: number, currentValue: number) => {
    try {
      await api.updateKeyResult(keyResultId, Math.min(100, currentValue + 20))
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to update the key result', 'error')
    }
  }

  const trend = performance?.trend ?? []

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-4">
        <Stat
          label="Team OKR score"
          value={`${performance?.teamOkrScore ?? 0}%`}
          accent="s02"
          delta="Average objective progress"
        />
        <Stat
          label="On-time rate"
          value={`${performance?.teamOnTimeRate ?? 0}%`}
          accent="s03"
          delta="Delivered on or before the due date"
        />
        <Stat
          label="Open tasks"
          value={performance?.openTasks ?? 0}
          accent="s04"
          delta={`${performance?.overdueTasks ?? 0} overdue`}
        />
        <Stat
          label="Avg throughput"
          value={`${performance?.averageThroughput ?? 0} h`}
          accent="s01"
          delta="Logged effort per snapshot"
        />
      </div>

      <div className="grid grid-2-1">
        <Card
          title="Objectives and key results"
          hint="US05 · DisplayPanel - process and outcome indicators"
          actions={
            <Button size="sm" variant="primary" onClick={() => setShowOkr(true)}>
              New objective
            </Button>
          }
        >
          {loading ? (
            <div className="skeleton" style={{ height: 200 }} />
          ) : okrs.length === 0 ? (
            <Empty>No objective defined for this cycle.</Empty>
          ) : (
            okrs.map((okr) => (
              <div key={okr.id} style={{ marginBottom: 20 }}>
                <div className="person" style={{ marginBottom: 6 }}>
                  <Badge tone={STATUS_TONE[okr.status] ?? 'default'}>{okr.status.replace('_', ' ')}</Badge>
                  <span style={{ fontWeight: 600 }}>{okr.objective}</span>
                  <span className="soft" style={{ marginLeft: 'auto' }}>
                    owner {okr.ownerName ?? '—'} · weight {okr.weight}
                  </span>
                </div>
                <ProgressRow label="Objective progress" value={okr.progress} />
                {okr.keyResults.map((keyResult) => (
                  <div className="list-row" key={keyResult.id} style={{ marginBottom: 6 }}>
                    <div className="grow">
                      <div className="title">{keyResult.title}</div>
                      <div className="sub">
                        {keyResult.currentValue} / {keyResult.targetValue} {keyResult.unit}
                      </div>
                      <div style={{ marginTop: 6 }}>
                        <Bar value={keyResult.ratio} />
                      </div>
                    </div>
                    <span className="value" style={{ fontWeight: 700 }}>
                      {keyResult.ratio}%
                    </span>
                    <Button size="sm" variant="ghost" onClick={() => nudge(keyResult.id, keyResult.currentValue)}>
                      +20
                    </Button>
                  </div>
                ))}
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setKrTarget({ okrId: okr.id, title: '', target: '100' })}
                >
                  Add key result
                </Button>
              </div>
            ))
          )}
        </Card>

        <Card title="Indicator trend" hint="US05 · daily snapshots captured by the display panel">
          <Sparkline
            values={trend.map((item) => item.onTimeRate)}
            color="#22a06b"
            label="On-time rate"
          />
          <div className="soft" style={{ fontSize: 12, marginBottom: 14 }}>
            On-time rate across the captured snapshots
          </div>
          <Sparkline values={trend.map((item) => item.okrScore)} color="#4f7cff" label="OKR score" />
          <div className="soft" style={{ fontSize: 12, marginBottom: 14 }}>
            OKR score across the captured snapshots
          </div>
          <Sparkline values={trend.map((item) => item.throughput)} color="#9b51e0" label="Throughput" />
          <div className="soft" style={{ fontSize: 12 }}>
            Logged effort (hours) per snapshot
          </div>
          <div style={{ marginTop: 12 }}>
            <Button
              size="sm"
              onClick={async () => {
                try {
                  await api.captureSnapshot()
                  push('Snapshot captured', 'ok')
                  await load()
                } catch (error) {
                  push(error instanceof Error ? error.message : 'Unable to capture a snapshot', 'error')
                }
              }}
            >
              Capture snapshot now
            </Button>
          </div>
        </Card>
      </div>

      <Card
        title="Performance scores"
        hint="US06 · PerformanceCalculator - 0.4 × on-time + 0.3 × quality + 0.3 × OKR, graded A–D"
        padded={false}
      >
        {!performance || performance.members.length === 0 ? (
          <Empty>No scored member yet.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Member</th>
                  <th>Role</th>
                  <th>Completed</th>
                  <th>Open</th>
                  <th>Overdue</th>
                  <th>On-time</th>
                  <th>Quality</th>
                  <th>OKR</th>
                  <th>Score</th>
                  <th>Grade</th>
                </tr>
              </thead>
              <tbody>
                {performance.members.map((member) => (
                  <tr key={member.userId}>
                    <td>
                      <div className="person">
                        <Avatar name={member.name} size="sm" />
                        <span style={{ fontWeight: 600 }}>{member.name}</span>
                      </div>
                    </td>
                    <td className="soft">{member.roleLabel}</td>
                    <td>{member.completed}</td>
                    <td>{member.open}</td>
                    <td>{member.overdue}</td>
                    <td>{member.onTimeRate}%</td>
                    <td>{member.qualityScore}%</td>
                    <td>{member.okrScore}%</td>
                    <td style={{ fontWeight: 700 }}>{member.totalScore}</td>
                    <td>
                      <Badge tone={GRADE_TONE[member.grade] ?? 'default'}>{member.grade}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Card title="Flagged bottlenecks" hint="Automatic bottleneck detection over the open tasks">
        {!performance || performance.bottlenecks.length === 0 ? (
          <Empty>No bottleneck detected.</Empty>
        ) : (
          <div className="list">
            {performance.bottlenecks.map((bottleneck, index) => (
              <div className="list-row" key={`${bottleneck.taskId}-${index}`}>
                <Badge tone={bottleneck.severity === 'HIGH' ? 'danger' : 'warn'}>{bottleneck.type}</Badge>
                <div className="grow">
                  <div className="title">{bottleneck.label}</div>
                  <div className="sub">{bottleneck.detail}</div>
                </div>
                <Badge>{bottleneck.severity}</Badge>
              </div>
            ))}
          </div>
        )}
      </Card>

      {showOkr && (
        <Modal
          title="New objective"
          onClose={() => setShowOkr(false)}
          footer={
            <>
              <Button onClick={() => setShowOkr(false)}>Cancel</Button>
              <Button variant="primary" onClick={createOkr}>
                Create objective
              </Button>
            </>
          }
        >
          <Field label="Objective">
            <input
              className="input"
              value={okrForm.objective}
              onChange={(event) => setOkrForm({ ...okrForm, objective: event.target.value })}
            />
          </Field>
          <Field label="Weight">
            <input
              className="input"
              type="number"
              step="0.1"
              value={okrForm.weight}
              onChange={(event) => setOkrForm({ ...okrForm, weight: event.target.value })}
            />
          </Field>
        </Modal>
      )}

      {krTarget && (
        <Modal
          title="Add key result"
          onClose={() => setKrTarget(null)}
          footer={
            <>
              <Button onClick={() => setKrTarget(null)}>Cancel</Button>
              <Button variant="primary" onClick={addKeyResult}>
                Add key result
              </Button>
            </>
          }
        >
          <Field label="Title">
            <input
              className="input"
              value={krTarget.title}
              onChange={(event) => setKrTarget({ ...krTarget, title: event.target.value })}
            />
          </Field>
          <Field label="Target value (%)">
            <input
              className="input"
              type="number"
              value={krTarget.target}
              onChange={(event) => setKrTarget({ ...krTarget, target: event.target.value })}
            />
          </Field>
        </Modal>
      )}
    </div>
  )
}
