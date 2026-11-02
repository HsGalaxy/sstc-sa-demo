import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  api,
  type FileRecordView,
  type FileVersionView,
  type TaskView
} from '../api/client'
import { useToast } from '../app/ToastContext'
import {
  Badge,
  Button,
  Card,
  Empty,
  Field,
  Modal,
  Stat,
  formatBytes,
  formatDateTime,
  relativeTime
} from '../components/ui'

const BUCKET_TONE: Record<string, 's01' | 's02' | 's03' | 's04'> = {
  DESIGN: 's01',
  DEVELOPMENT: 's02',
  DOCUMENT: 's03',
  REPORT: 's04',
  OTHER: 's04'
}

const BUCKETS = ['DESIGN', 'DEVELOPMENT', 'DOCUMENT', 'REPORT', 'OTHER'] as const

/**
 * S02 · US01 / US02 - TaskDashboard.FileQueryController + FileBucket + HistoricalVersionController
 * Multi condition file query, classified buckets, version history and rollback.
 */
export function FileTrackerPage() {
  const { push } = useToast()
  const [files, setFiles] = useState<FileRecordView[]>([])
  const [tasks, setTasks] = useState<TaskView[]>([])
  const [statistics, setStatistics] = useState<Record<string, { count: number; bytes: number } | number>>({})
  const [bucket, setBucket] = useState('')
  const [keyword, setKeyword] = useState('')
  const [taskId, setTaskId] = useState('')
  const [loading, setLoading] = useState(true)
  const [selected, setSelected] = useState<FileRecordView | null>(null)
  const [versions, setVersions] = useState<FileVersionView[]>([])
  const [showUpload, setShowUpload] = useState(false)
  const [form, setForm] = useState({
    name: '',
    bucket: 'DOCUMENT',
    taskId: '',
    contentType: 'application/pdf',
    sizeBytes: '250000',
    changeNote: 'Initial upload'
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [fileList, taskList, stats] = await Promise.all([
        api.files({
          bucket: bucket || undefined,
          keyword: keyword || undefined,
          taskId: taskId ? Number(taskId) : undefined
        }),
        api.tasks(),
        api.fileStatistics()
      ])
      setFiles(fileList)
      setTasks(taskList)
      setStatistics(stats)
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to query the file index', 'error')
    } finally {
      setLoading(false)
    }
  }, [bucket, keyword, taskId, push])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!selected) {
      setVersions([])
      return
    }
    api
      .fileVersions(selected.id)
      .then(setVersions)
      .catch(() => setVersions([]))
  }, [selected])

  const totalBytes = useMemo(() => files.reduce((sum, file) => sum + file.sizeBytes, 0), [files])

  const upload = async () => {
    try {
      const created = await api.uploadTaskFile({
        name: form.name,
        bucket: form.bucket,
        taskId: form.taskId ? Number(form.taskId) : null,
        contentType: form.contentType,
        sizeBytes: Number(form.sizeBytes),
        changeNote: form.changeNote
      })
      push(`${created.name} stored as version 1`, 'ok')
      setShowUpload(false)
      setForm({ ...form, name: '' })
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to upload the file', 'error')
    }
  }

  const addVersion = async (file: FileRecordView) => {
    try {
      const version = await api.addFileVersion(file.id, {
        changeNote: `Revision ${file.currentVersion + 1} after review`,
        sizeBytes: file.sizeBytes + 2048
      })
      push(`${file.name} is now at version ${version.version}`, 'ok')
      await load()
      if (selected && selected.id === file.id) setVersions(await api.fileVersions(file.id))
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to add a version', 'error')
    }
  }

  const rollback = async (file: FileRecordView, version: number) => {
    try {
      await api.rollbackFile(file.id, version)
      push(`${file.name} rolled back to version ${version}`, 'ok')
      await load()
      setVersions(await api.fileVersions(file.id))
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to roll back', 'error')
    }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-4">
        <Stat label="Indexed files" value={files.length} accent="s02" delta="Matching the current filter" />
        <Stat label="Total size" value={formatBytes(totalBytes)} accent="s02" delta="Across the buckets" />
        <Stat
          label="Versions tracked"
          value={files.reduce((sum, file) => sum + file.versionCount, 0)}
          accent="s03"
          delta="HistoricalVersionController"
        />
        <Stat
          label="Archived"
          value={files.filter((file) => file.archived).length}
          accent="s04"
          delta="Hidden from the default query"
        />
      </div>

      <Card
        title="File query"
        hint="US01 · FileQueryController - multi condition lookup over the category and task buckets"
        actions={
          <>
            <select
              className="select"
              style={{ width: 160 }}
              value={bucket}
              onChange={(event) => setBucket(event.target.value)}
            >
              <option value="">All buckets</option>
              {BUCKETS.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
            <select
              className="select"
              style={{ width: 200 }}
              value={taskId}
              onChange={(event) => setTaskId(event.target.value)}
            >
              <option value="">All tasks</option>
              {tasks.map((task) => (
                <option key={task.id} value={task.id}>
                  {task.code} · {task.title.slice(0, 32)}
                </option>
              ))}
            </select>
            <input
              className="input"
              style={{ width: 200 }}
              placeholder="Search file name"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
            <Button size="sm" onClick={() => void load()}>
              Run query
            </Button>
            <Button variant="primary" size="sm" onClick={() => setShowUpload(true)}>
              Upload file
            </Button>
          </>
        }
        padded={false}
      >
        {loading ? (
          <div className="card-body">
            <div className="skeleton" style={{ height: 220 }} />
          </div>
        ) : files.length === 0 ? (
          <Empty>No file matches the query. Upload one to create the first version.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>File</th>
                  <th>Bucket</th>
                  <th>Task</th>
                  <th>Owner</th>
                  <th>Size</th>
                  <th>Version</th>
                  <th>Uploaded</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {files.map((file) => (
                  <tr key={file.id} onClick={() => setSelected(file)} style={{ cursor: 'pointer' }}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{file.name}</div>
                      <div className="soft mono">{file.contentType}</div>
                    </td>
                    <td>
                      <Badge tone={BUCKET_TONE[file.bucket] ?? 'default'}>{file.bucket}</Badge>
                    </td>
                    <td className="soft">{file.taskTitle ?? '—'}</td>
                    <td>{file.ownerName ?? '—'}</td>
                    <td className="soft">{formatBytes(file.sizeBytes)}</td>
                    <td>
                      <Badge tone="info">v{file.currentVersion}</Badge>
                      <span className="soft" style={{ marginLeft: 6 }}>
                        ({file.versionCount})
                      </span>
                    </td>
                    <td className="soft">{relativeTime(file.uploadedAt)}</td>
                    <td onClick={(event) => event.stopPropagation()}>
                      <div className="chip-row">
                        <Button size="sm" variant="ghost" onClick={() => addVersion(file)}>
                          New version
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={async () => {
                            try {
                              await api.archiveFile(file.id, !file.archived)
                              push(
                                `${file.name} ${file.archived ? 'restored' : 'archived'}`,
                                'ok'
                              )
                              await load()
                            } catch (error) {
                              push(
                                error instanceof Error ? error.message : 'Unable to archive the file',
                                'error'
                              )
                            }
                          }}
                        >
                          {file.archived ? 'Restore' : 'Archive'}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="grid grid-2">
        <Card
          title={selected ? `Version history · ${selected.name}` : 'Version history'}
          hint="US01 · HistoricalVersionController - roll back to any previous edition"
        >
          {!selected ? (
            <Empty>Select a file to inspect its version history.</Empty>
          ) : versions.length === 0 ? (
            <Empty>No version recorded.</Empty>
          ) : (
            <div className="timeline">
              {versions.map((version) => (
                <div className="timeline-item" key={version.id}>
                  <div className="person">
                    <Badge tone={version.current ? 'ok' : 'default'}>v{version.version}</Badge>
                    <span style={{ fontWeight: 600 }}>{version.changeNote}</span>
                    {version.current && <Badge tone="ok">current</Badge>}
                  </div>
                  <div className="sub muted">
                    {version.authorName} · {formatBytes(version.sizeBytes)} ·{' '}
                    {formatDateTime(version.createdOn)}
                  </div>
                  <div className="soft mono" style={{ fontSize: 11 }}>
                    sha {version.checksum}
                  </div>
                  {!version.current && (
                    <Button size="sm" variant="ghost" onClick={() => rollback(selected, version.version)}>
                      Roll back to v{version.version}
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card title="Bucket statistics" hint="US02 · FileBucket classification of the stored attachments">
          {Object.entries(statistics)
            .filter(([key]) => key !== 'totalFiles')
            .map(([key, value]) => {
              const entry = value as { count: number; bytes: number }
              return (
                <div className="progress-row" key={key}>
                  <span className="label">
                    <Badge tone={BUCKET_TONE[key] ?? 'default'}>{key}</Badge>
                  </span>
                  <div style={{ flex: 1 }}>
                    <div className="bar">
                      <span
                        style={{
                          width: `${
                            files.length ? Math.round(((entry?.count ?? 0) / files.length) * 100) : 0
                          }%`
                        }}
                      />
                    </div>
                  </div>
                  <span className="value">{entry?.count ?? 0}</span>
                </div>
              )
            })}
          <div className="soft" style={{ marginTop: 10, fontSize: 12 }}>
            Total indexed files: {String(statistics.totalFiles ?? files.length)}
          </div>
        </Card>
      </div>

      {showUpload && (
        <Modal
          title="Upload a task attachment"
          onClose={() => setShowUpload(false)}
          footer={
            <>
              <Button onClick={() => setShowUpload(false)}>Cancel</Button>
              <Button variant="primary" onClick={upload}>
                Store as version 1
              </Button>
            </>
          }
        >
          <Field label="File name">
            <input
              className="input"
              value={form.name}
              placeholder="design-review.pdf"
              onChange={(event) => setForm({ ...form, name: event.target.value })}
            />
          </Field>
          <div className="grid grid-2">
            <Field label="Bucket">
              <select
                className="select"
                value={form.bucket}
                onChange={(event) => setForm({ ...form, bucket: event.target.value })}
              >
                {BUCKETS.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Task">
              <select
                className="select"
                value={form.taskId}
                onChange={(event) => setForm({ ...form, taskId: event.target.value })}
              >
                <option value="">Not attached</option>
                {tasks.map((task) => (
                  <option key={task.id} value={task.id}>
                    {task.code} · {task.title.slice(0, 30)}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Content type">
              <input
                className="input"
                value={form.contentType}
                onChange={(event) => setForm({ ...form, contentType: event.target.value })}
              />
            </Field>
            <Field label="Size (bytes)">
              <input
                className="input"
                type="number"
                value={form.sizeBytes}
                onChange={(event) => setForm({ ...form, sizeBytes: event.target.value })}
              />
            </Field>
          </div>
          <Field label="Change note">
            <input
              className="input"
              value={form.changeNote}
              onChange={(event) => setForm({ ...form, changeNote: event.target.value })}
            />
          </Field>
        </Modal>
      )}
    </div>
  )
}
