import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, type BoardColumn, type CycleView, type SessionUser, type TaskView } from '../api/client'
import { useToast } from '../app/ToastContext'
import { Avatar, Badge, Button, Card, Empty, Field, Modal, Stat, formatDate } from '../components/ui'

const PRIORITY_TONE: Record<string, 'default' | 'info' | 'warn' | 'danger'> = {
  LOW: 'default',
  MEDIUM: 'info',
  HIGH: 'warn',
  URGENT: 'danger'
}

const COLUMN_ACCENT: Record<string, string> = {
  TODO: '#98a2b3',
  IN_PROGRESS: '#2e90fa',
  REVIEW: '#f79009',
  DONE: '#12b76a'
}

/**
 * S02 · US03 / US05 - TaskDashboard.ModificationScheduler + DisplayPanel
 * Kanban board with drag and drop between the four columns of the case study
 * ("To Do", "In Progress", "Done" plus the review column of the team).
 */
export function BoardPage() {
  const { push } = useToast()
  const [columns, setColumns] = useState<BoardColumn[]>([])
  const [cycles, setCycles] = useState<CycleView[]>([])
  const [members, setMembers] = useState<SessionUser[]>([])
  const [loading, setLoading] = useState(true)
  const [dragging, setDragging] = useState<TaskView | null>(null)
  const [dragOver, setDragOver] = useState<string | null>(null)
  const [detail, setDetail] = useState<TaskView | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [filterAssignee, setFilterAssignee] = useState<number | ''>('')
  const [keyword, setKeyword] = useState('')
  const [form, setForm] = useState({
    title: '',
    description: '',
    status: 'TODO',
    priority: 'MEDIUM',
    assigneeId: '',
    cycleId: '',
    dueDate: '',
    estimateHours: '4',
    tags: ''
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [board, cycleList, userList] = await Promise.all([api.board(), api.cycles(), api.users()])
      setColumns(board)
      setCycles(cycleList)
      setMembers(userList)
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to load the board', 'error')
    } finally {
      setLoading(false)
    }
  }, [push])

  useEffect(() => {
    void load()
  }, [load])

  const visibleColumns = useMemo(() => {
    return columns.map((column) => ({
      ...column,
      tasks: column.tasks.filter((task) => {
        if (filterAssignee !== '' && task.assigneeId !== filterAssignee) return false
        if (keyword.trim() && !`${task.code} ${task.title}`.toLowerCase().includes(keyword.trim().toLowerCase())) {
          return false
        }
        return true
      })
    }))
  }, [columns, filterAssignee, keyword])

  const totals = useMemo(() => {
    const all = columns.flatMap((column) => column.tasks)
    return {
      total: all.length,
      done: all.filter((task) => task.status === 'DONE').length,
      overdue: all.filter((task) => task.overdue).length,
      blocked: all.filter((task) => task.blocked).length
    }
  }, [columns])

  const move = async (task: TaskView, status: TaskView['status']) => {
    if (task.status === status) return
    try {
      await api.moveTask(task.id, status)
      push(`${task.code} moved to ${status.replace('_', ' ').toLowerCase()}`, 'ok')
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to move the task', 'error')
    }
  }

  const createTask = async () => {
    try {
      await api.createTask({
        title: form.title,
        description: form.description,
        status: form.status,
        priority: form.priority,
        assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
        cycleId: form.cycleId ? Number(form.cycleId) : null,
        dueDate: form.dueDate || null,
        estimateHours: form.estimateHours ? Number(form.estimateHours) : 0,
        tags: form.tags
      })
      push('Task created through ModificationScheduler', 'ok')
      setShowCreate(false)
      setForm({
        title: '',
        description: '',
        status: 'TODO',
        priority: 'MEDIUM',
        assigneeId: '',
        cycleId: '',
        dueDate: '',
        estimateHours: '4',
        tags: ''
      })
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to create the task', 'error')
    }
  }

  const saveDetail = async () => {
    if (!detail) return
    try {
      await api.updateTask(detail.id, {
        title: detail.title,
        description: detail.description,
        priority: detail.priority,
        assigneeId: detail.assigneeId,
        dueDate: detail.dueDate,
        loggedHours: detail.loggedHours,
        estimateHours: detail.estimateHours,
        blocked: detail.blocked,
        tags: detail.tags.join(',')
      })
      push(`${detail.code} updated and logged by OperationLogger`, 'ok')
      setDetail(null)
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to update the task', 'error')
    }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-4">
        <Stat label="Tasks" value={totals.total} accent="s02" delta="All cycles" />
        <Stat label="Done" value={totals.done} accent="s02" delta={`${totals.total ? Math.round((totals.done / totals.total) * 100) : 0}% completion`} />
        <Stat label="Overdue" value={totals.overdue} accent="s03" delta="Flagged by the bottleneck scan" />
        <Stat label="Blocked" value={totals.blocked} accent="s04" delta="Needs a manager decision" />
      </div>

      <Card
        title="Kanban board"
        hint="US03 · drag a card to change its status (ModificationScheduler) · US05 · DisplayPanel renders the live board"
        actions={
          <>
            <select
              className="select"
              style={{ width: 180 }}
              value={filterAssignee}
              onChange={(event) => setFilterAssignee(event.target.value === '' ? '' : Number(event.target.value))}
            >
              <option value="">All assignees</option>
              {members.map((member) => (
                <option key={member.id} value={member.id}>
                  {member.displayName}
                </option>
              ))}
            </select>
            <input
              className="input"
              style={{ width: 180 }}
              placeholder="Search code or title"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <Button size="sm" onClick={() => void load()}>
              Refresh
            </Button>
            <Button variant="primary" size="sm" onClick={() => setShowCreate(true)}>
              New task
            </Button>
          </>
        }
      >
        {loading ? (
          <div className="board">
            {[0, 1, 2, 3].map((index) => (
              <div className="skeleton" key={index} style={{ height: 260 }} />
            ))}
          </div>
        ) : (
          <div className="board">
            {visibleColumns.map((column) => (
              <div
                key={column.status}
                className={`board-column ${dragOver === column.status ? 'drag-over' : ''}`}
                onDragOver={(event) => {
                  event.preventDefault()
                  setDragOver(column.status)
                }}
                onDragLeave={() => setDragOver((current) => (current === column.status ? null : current))}
                onDrop={async () => {
                  setDragOver(null)
                  if (dragging) await move(dragging, column.status)
                  setDragging(null)
                }}
              >
                <div className="board-column-head">
                  <span
                    className="priority"
                    style={{ background: COLUMN_ACCENT[column.status], width: 8, height: 8 }}
                  />
                  {column.label}
                  <span className="count">{column.tasks.length}</span>
                </div>

                {column.tasks.length === 0 && <div className="empty">Drop a task here</div>}

                {column.tasks.map((task) => (
                  <article
                    className="task-card"
                    key={task.id}
                    draggable
                    onDragStart={() => setDragging(task)}
                    onDragEnd={() => setDragging(null)}
                    onClick={() => setDetail(task)}
                  >
                    <div className="title">{task.title}</div>
                    <div className="meta">
                      <span className="mono soft">{task.code}</span>
                      <span className={`priority ${task.priority}`} />
                      <span>{task.priority}</span>
                      {task.blocked && <Badge tone="danger">blocked</Badge>}
                      {task.overdue && <Badge tone="warn">overdue</Badge>}
                    </div>
                    <div className="meta" style={{ marginTop: 8 }}>
                      <span className="person">
                        <Avatar name={task.assigneeName} size="sm" />
                        <span>{task.assigneeName ?? 'unassigned'}</span>
                      </span>
                      <span style={{ marginLeft: 'auto' }}>{formatDate(task.dueDate)}</span>
                    </div>
                    {(task.attachmentCount > 0 || task.tags.length > 0) && (
                      <div className="chip-row" style={{ marginTop: 8 }}>
                        {task.attachmentCount > 0 && <Badge tone="info">📎 {task.attachmentCount}</Badge>}
                        {task.tags.slice(0, 3).map((tag) => (
                          <span className="badge" key={tag}>
                            {tag}
                          </span>
                        ))}
                      </div>
                    )}
                  </article>
                ))}
              </div>
            ))}
          </div>
        )}
      </Card>

      <Card title="Cycles" hint="Delivery cycles grouping the tasks and their deadlines" padded={false}>
        {cycles.length === 0 ? (
          <Empty>No cycle defined.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Cycle</th>
                  <th>Goal</th>
                  <th>Window</th>
                  <th>Progress</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {cycles.map((cycle) => (
                  <tr key={cycle.id}>
                    <td style={{ fontWeight: 600 }}>{cycle.name}</td>
                    <td className="soft">{cycle.goal}</td>
                    <td className="soft">
                      {formatDate(cycle.startDate)} → {formatDate(cycle.endDate)}
                    </td>
                    <td>
                      <div className="progress-row" style={{ marginBottom: 0 }}>
                        <div style={{ flex: 1, minWidth: 120 }}>
                          <div className="bar">
                            <span style={{ width: `${cycle.progress}%` }} />
                          </div>
                        </div>
                        <span className="value">
                          {cycle.doneTasks}/{cycle.totalTasks}
                        </span>
                      </div>
                    </td>
                    <td>
                      <Badge tone={cycle.active ? 'ok' : 'default'}>
                        {cycle.active ? 'ACTIVE' : 'CLOSED'}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {showCreate && (
        <Modal
          title="New task"
          onClose={() => setShowCreate(false)}
          footer={
            <>
              <Button onClick={() => setShowCreate(false)}>Cancel</Button>
              <Button variant="primary" onClick={createTask}>
                Create task
              </Button>
            </>
          }
        >
          <Field label="Title">
            <input
              className="input"
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
            />
          </Field>
          <Field label="Description">
            <textarea
              className="textarea"
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
            />
          </Field>
          <div className="grid grid-2">
            <Field label="Column">
              <select
                className="select"
                value={form.status}
                onChange={(event) => setForm({ ...form, status: event.target.value })}
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="REVIEW">Review</option>
                <option value="DONE">Done</option>
              </select>
            </Field>
            <Field label="Priority">
              <select
                className="select"
                value={form.priority}
                onChange={(event) => setForm({ ...form, priority: event.target.value })}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </Field>
            <Field label="Assignee">
              <select
                className="select"
                value={form.assigneeId}
                onChange={(event) => setForm({ ...form, assigneeId: event.target.value })}
              >
                <option value="">Unassigned</option>
                {members.map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.displayName}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Cycle">
              <select
                className="select"
                value={form.cycleId}
                onChange={(event) => setForm({ ...form, cycleId: event.target.value })}
              >
                <option value="">No cycle</option>
                {cycles.map((cycle) => (
                  <option key={cycle.id} value={cycle.id}>
                    {cycle.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Due date">
              <input
                className="input"
                type="date"
                value={form.dueDate}
                onChange={(event) => setForm({ ...form, dueDate: event.target.value })}
              />
            </Field>
            <Field label="Estimate (hours)">
              <input
                className="input"
                type="number"
                value={form.estimateHours}
                onChange={(event) => setForm({ ...form, estimateHours: event.target.value })}
              />
            </Field>
          </div>
          <Field label="Tags (comma separated)">
            <input
              className="input"
              value={form.tags}
              onChange={(event) => setForm({ ...form, tags: event.target.value })}
            />
          </Field>
        </Modal>
      )}

      {detail && (
        <Modal
          title={`${detail.code} · ${detail.title}`}
          onClose={() => setDetail(null)}
          wide
          footer={
            <>
              <Button
                variant="danger"
                onClick={async () => {
                  try {
                    await api.deleteTask(detail.id)
                    push(`${detail.code} deleted`, 'ok')
                    setDetail(null)
                    await load()
                  } catch (error) {
                    push(error instanceof Error ? error.message : 'Unable to delete the task', 'error')
                  }
                }}
              >
                Delete
              </Button>
              <div className="topbar-spacer" />
              <Button onClick={() => setDetail(null)}>Close</Button>
              <Button variant="primary" onClick={saveDetail}>
                Save changes
              </Button>
            </>
          }
        >
          <div className="grid grid-2">
            <Field label="Title">
              <input
                className="input"
                value={detail.title}
                onChange={(event) => setDetail({ ...detail, title: event.target.value })}
              />
            </Field>
            <Field label="Priority">
              <select
                className="select"
                value={detail.priority}
                onChange={(event) =>
                  setDetail({ ...detail, priority: event.target.value as TaskView['priority'] })
                }
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </Field>
            <Field label="Assignee">
              <select
                className="select"
                value={detail.assigneeId ?? ''}
                onChange={(event) =>
                  setDetail({ ...detail, assigneeId: event.target.value ? Number(event.target.value) : undefined })
                }
              >
                <option value="">Unassigned</option>
                {members.map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.displayName}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Due date">
              <input
                className="input"
                type="date"
                value={detail.dueDate ?? ''}
                onChange={(event) => setDetail({ ...detail, dueDate: event.target.value })}
              />
            </Field>
            <Field label="Logged hours">
              <input
                className="input"
                type="number"
                step="0.5"
                value={detail.loggedHours}
                onChange={(event) => setDetail({ ...detail, loggedHours: Number(event.target.value) })}
              />
            </Field>
            <Field label="Estimate hours">
              <input
                className="input"
                type="number"
                step="0.5"
                value={detail.estimateHours}
                onChange={(event) => setDetail({ ...detail, estimateHours: Number(event.target.value) })}
              />
            </Field>
          </div>
          <Field label="Description">
            <textarea
              className="textarea"
              value={detail.description ?? ''}
              onChange={(event) => setDetail({ ...detail, description: event.target.value })}
            />
          </Field>
          <div className="chip-row" style={{ marginBottom: 12 }}>
            <Badge tone={PRIORITY_TONE[detail.priority]}>{detail.priority}</Badge>
            <Badge>{detail.statusLabel}</Badge>
            {detail.cycleName && <Badge tone="info">{detail.cycleName}</Badge>}
            {detail.okrObjective && <Badge tone="s02">{detail.okrObjective}</Badge>}
            <Badge tone={detail.blocked ? 'danger' : 'ok'}>
              {detail.blocked ? 'blocked' : 'not blocked'}
            </Badge>
          </div>
          <label className="person" style={{ gap: 8 }}>
            <input
              type="checkbox"
              checked={detail.blocked}
              onChange={(event) => setDetail({ ...detail, blocked: event.target.checked })}
            />
            <span>Flag this task as blocked (feeds the bottleneck list of the display panel)</span>
          </label>
        </Modal>
      )}
    </div>
  )
}
