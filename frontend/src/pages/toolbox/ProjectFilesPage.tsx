import { useCallback, useEffect, useState } from 'react'
import {
  api,
  type IntegrationStatus,
  type ShareLinkView,
  type ToolboxFileView
} from '../../api/client'
import { useToast } from '../../app/ToastContext'
import { Badge, Button, Card, Empty, Field, Modal, Stat, formatBytes, formatDateTime, relativeTime } from '../../components/ui'

/** Readable message for anything the API client throws. */
const errorText = (err: unknown): string => (err instanceof Error ? err.message : String(err))

/** The storage panel arrives as a loose map, so it is narrowed once here. */
interface StorageSummary {
  files: number
  bytes: number
  shared: number
  links: number
  provider: string
  maxUploadMb: number
}

const toNumber = (value: unknown, fallback = 0): number => {
  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

const toText = (value: unknown, fallback = '—'): string =>
  typeof value === 'string' && value.length > 0 ? value : fallback

const readStorage = (raw: Record<string, unknown>): StorageSummary => ({
  files: toNumber(raw.files),
  bytes: toNumber(raw.bytes),
  shared: toNumber(raw.shared),
  links: toNumber(raw.links),
  provider: toText(raw.provider),
  maxUploadMb: toNumber(raw.maxUploadMb, 25)
})

const statusTone = (status: string): 'ok' | 'danger' | 'warn' | 'default' => {
  if (status === 'VALIDATED' || status === 'VALIDATED_SHARED') return 'ok'
  if (status === 'REJECTED') return 'danger'
  if (status === 'ARCHIVED') return 'warn'
  return 'default'
}

/**
 * US04 · Toolbox.ProjectFileManagement · FileUploadHandler + StorageAdapter + ShareLinkService
 *
 * UploadFile(file, content) validates the record internally (name, size limit, executable
 * extensions) and only then calls StorageAdapter.WriteFile(file, content); CreateShareLink(file,
 * expiry) is accepted only for a file whose status is VALIDATED, otherwise it answers with a
 * non-success result code.
 */
export function ProjectFilesPage() {
  const toast = useToast()

  const [files, setFiles] = useState<ToolboxFileView[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [links, setLinks] = useState<ShareLinkView[]>([])
  const [allLinks, setAllLinks] = useState<ShareLinkView[]>([])
  const [storage, setStorage] = useState<StorageSummary | null>(null)
  const [integrations, setIntegrations] = useState<IntegrationStatus | null>(null)
  const [fileStatus, setFileStatus] = useState<{ fileId: number; status: string } | null>(null)
  const [expiresInDays, setExpiresInDays] = useState('7')
  const [busy, setBusy] = useState(false)

  // ---- upload modal
  const [showUpload, setShowUpload] = useState(false)
  const [upName, setUpName] = useState('')
  const [upFolder, setUpFolder] = useState('/design')
  const [upSizeMb, setUpSizeMb] = useState('2.5')
  const [upContentType, setUpContentType] = useState('application/pdf')

  const selectedFile = files.find((file) => file.id === selectedId) ?? null
  const maxUploadMb = storage?.maxUploadMb ?? integrations?.maxUploadMb ?? 25

  const loadIntegrations = useCallback(async () => {
    try {
      setIntegrations(await api.integrations())
    } catch (err) {
      toast.push(`Integrations unavailable: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadStorage = useCallback(async () => {
    try {
      setStorage(readStorage(await api.storage()))
    } catch (err) {
      toast.push(`Storage panel unavailable: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadAllLinks = useCallback(async () => {
    try {
      setAllLinks(await api.allShareLinks())
    } catch (err) {
      toast.push(`Could not load share links: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadFiles = useCallback(async () => {
    try {
      setFiles(await api.toolboxFiles())
    } catch (err) {
      toast.push(`Could not load project files: ${errorText(err)}`, 'error')
    }
  }, [toast])

  /** Select a file and load the share links registered for it. */
  const openFile = useCallback(
    async (id: number) => {
      setSelectedId(id)
      setFileStatus(null)
      try {
        setLinks(await api.shareLinks(id))
      } catch (err) {
        toast.push(`Could not load share links: ${errorText(err)}`, 'error')
      }
    },
    [toast]
  )

  useEffect(() => {
    void loadFiles()
    void loadAllLinks()
    void loadStorage()
    void loadIntegrations()
  }, [loadFiles, loadAllLinks, loadStorage, loadIntegrations])

  const refreshAfterMutation = async () => {
    await loadFiles()
    await loadAllLinks()
    await loadStorage()
  }

  /** UploadFile(file, content) — validation errors come back as a non-success result code. */
  const uploadFile = async () => {
    if (!upName.trim()) {
      toast.push('A file name is required', 'error')
      return
    }
    const sizeMb = Number(upSizeMb)
    if (!Number.isFinite(sizeMb) || sizeMb <= 0) {
      toast.push('The size must be a positive number of MB', 'error')
      return
    }
    setBusy(true)
    try {
      const created = await api.uploadToolboxFile({
        name: upName.trim(),
        folder: upFolder.trim() || '/',
        sizeBytes: Math.round(sizeMb * 1024 * 1024),
        contentType: upContentType
      })
      toast.push(
        `Upload accepted (result code 0) · ${created.name} stored in ${created.storageBackend ?? 'storage'}`,
        'ok'
      )
      setShowUpload(false)
      setUpName('')
      await refreshAfterMutation()
      await openFile(created.id)
    } catch (err) {
      toast.push(`Upload rejected (non-success result code): ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  /** GetFileStatus(file). */
  const checkStatus = async (file: ToolboxFileView) => {
    setBusy(true)
    try {
      const result = await api.fileStatus(file.id)
      setFileStatus(result)
      toast.push(`${file.name}: ${result.status}`, 'ok')
      await refreshAfterMutation()
    } catch (err) {
      toast.push(`Status check failed: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  /** CreateShareLink(file, expiry) — accepted only while the file is VALIDATED. */
  const createLink = async (file: ToolboxFileView) => {
    setBusy(true)
    try {
      const link = await api.createShareLink(file.id, { expiresInDays: Number(expiresInDays) || 7 })
      toast.push(`Share link created · token ${link.token.slice(0, 12)}…`, 'ok')
      await refreshAfterMutation()
      await openFile(file.id)
    } catch (err) {
      toast.push(`Share link refused (non-success result code): ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const revokeLink = async (link: ShareLinkView) => {
    setBusy(true)
    try {
      const revoked = await api.revokeShareLink(link.id)
      toast.push(`Link ${revoked.token.slice(0, 12)}… revoked`, 'ok')
      await refreshAfterMutation()
      if (selectedId !== null) await openFile(selectedId)
    } catch (err) {
      toast.push(`Revoke failed: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      {/* US04 header block */}
      <div>
        <h1 style={{ margin: 0, fontSize: 20, letterSpacing: '-0.4px' }}>Project files</h1>
        <div className="muted" style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.4px' }}>
          US04 · Toolbox.ProjectFileManagement · FileUploadHandler + StorageAdapter + ShareLinkService
        </div>
      </div>

      <div className="grid grid-4">
        <Stat label="Files" value={storage?.files ?? files.length} accent="s04" />
        <Stat
          label="Stored bytes"
          value={formatBytes(storage?.bytes ?? integrations?.storageUsedBytes ?? 0)}
          delta={`provider ${storage?.provider ?? integrations?.storageProvider ?? '—'}`}
          accent="s04"
        />
        <Stat
          label="Shared files"
          value={storage?.shared ?? files.filter((file) => file.shared).length}
          delta={`${storage?.links ?? allLinks.length} share links`}
          accent="s04"
        />
        <Stat label="Max upload" value={`${maxUploadMb} MB`} delta="files above the limit are rejected" accent="s04" />
      </div>

      <Card
        title="Project files"
        hint={`UploadFile(file, content) validates internally, then StorageAdapter.WriteFile(file, content) stores the content. Executables and files above ${maxUploadMb} MB are rejected.`}
        actions={
          <Button variant="primary" onClick={() => setShowUpload(true)}>
            Upload file
          </Button>
        }
        padded={false}
      >
        {files.length === 0 ? (
          <Empty>No project files yet — upload the first one.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Folder</th>
                  <th>Size</th>
                  <th>Owner</th>
                  <th>Status</th>
                  <th>Storage</th>
                  <th>Uploaded</th>
                  <th>Shared</th>
                  <th>Links</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {files.map((file) => (
                  <tr
                    key={file.id}
                    onClick={() => void openFile(file.id)}
                    style={{ cursor: 'pointer', background: file.id === selectedId ? '#fafbff' : undefined }}
                  >
                    <td>
                      <div style={{ fontWeight: 600 }}>{file.name}</div>
                      <div className="soft mono">{file.contentType ?? 'application/octet-stream'}</div>
                      {file.validationError && (
                        <div className="soft" style={{ fontSize: 11, color: '#b42318' }}>
                          {file.validationError}
                        </div>
                      )}
                    </td>
                    <td className="mono">{file.folder}</td>
                    <td>{formatBytes(file.sizeBytes)}</td>
                    <td>{file.ownerName ?? '—'}</td>
                    <td>
                      <Badge tone={statusTone(file.status)}>{file.status}</Badge>
                    </td>
                    <td className="mono soft">{file.storageBackend ?? '—'}</td>
                    <td title={formatDateTime(file.uploadedAt)}>{relativeTime(file.uploadedAt)}</td>
                    <td>
                      <Badge tone={file.shared ? 'info' : 'default'}>{file.shared ? 'shared' : 'private'}</Badge>
                    </td>
                    <td>{file.shareLinkCount}</td>
                    <td>
                      <Button size="sm" onClick={() => void openFile(file.id)}>
                        Open
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="grid grid-2-1">
        <div className="grid" style={{ gap: 16 }}>
          <Card
            title={selectedFile ? `Share links · ${selectedFile.name}` : 'Share links'}
            hint="CreateShareLink(file, expiry) is accepted only for a VALIDATED file; other statuses answer with a non-success result code."
            actions={
              selectedFile ? (
                <div className="toolbar">
                  <Badge tone={statusTone(fileStatus?.status ?? selectedFile.status)}>
                    {fileStatus?.status ?? selectedFile.status}
                  </Badge>
                  <Button size="sm" disabled={busy} onClick={() => void checkStatus(selectedFile)}>
                    Check status
                  </Button>
                </div>
              ) : undefined
            }
            padded={false}
          >
            {!selectedFile ? (
              <Empty>Select a file to inspect and create its share links.</Empty>
            ) : (
              <>
                <div className="card-body" style={{ paddingBottom: 12 }}>
                  <div className="toolbar">
                    <Field label="Link expiry (days)">
                      <input
                        className="input"
                        type="number"
                        min="1"
                        value={expiresInDays}
                        onChange={(event) => setExpiresInDays(event.target.value)}
                      />
                    </Field>
                    <div className="topbar-spacer" />
                    <Button
                      variant="primary"
                      disabled={busy || selectedFile.status !== 'VALIDATED'}
                      onClick={() => void createLink(selectedFile)}
                    >
                      Create share link
                    </Button>
                  </div>
                  {selectedFile.status !== 'VALIDATED' && (
                    <div className="muted" style={{ fontSize: 12 }}>
                      Status {selectedFile.status}: CreateShareLink(file, expiry) is only allowed for a
                      VALIDATED file, so the request returns a non-success result code.
                    </div>
                  )}
                </div>
                {links.length === 0 ? (
                  <Empty>No share links for this file yet.</Empty>
                ) : (
                  <div className="table-wrap">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Token</th>
                          <th>Expires</th>
                          <th>State</th>
                          <th>Accesses</th>
                          <th>Created by</th>
                          <th />
                        </tr>
                      </thead>
                      <tbody>
                        {links.map((link) => (
                          <tr key={link.id}>
                            <td className="mono">{link.token.slice(0, 16)}…</td>
                            <td title={formatDateTime(link.expiresAt)}>{formatDateTime(link.expiresAt)}</td>
                            <td>
                              <div className="chip-row">
                                {link.expired && <Badge tone="warn">expired</Badge>}
                                {link.revoked && <Badge tone="danger">revoked</Badge>}
                                {!link.expired && !link.revoked && <Badge tone="ok">active</Badge>}
                              </div>
                            </td>
                            <td>{link.accessCount}</td>
                            <td>{link.createdByName ?? '—'}</td>
                            <td>
                              <Button
                                size="sm"
                                variant="danger"
                                disabled={busy || link.revoked}
                                onClick={() => void revokeLink(link)}
                              >
                                Revoke
                              </Button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}
          </Card>

          <Card
            title="All share links"
            hint="Every ShareLinkService record, including the file it points at."
            padded={false}
          >
            {allLinks.length === 0 ? (
              <Empty>No share links have been created yet.</Empty>
            ) : (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>File</th>
                      <th>Token</th>
                      <th>Expires</th>
                      <th>State</th>
                      <th>Accesses</th>
                      <th>Created by</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {allLinks.map((link) => (
                      <tr key={link.id}>
                        <td>{link.fileName ?? `#${link.fileId}`}</td>
                        <td className="mono">{link.token.slice(0, 16)}…</td>
                        <td>{formatDateTime(link.expiresAt)}</td>
                        <td>
                          <div className="chip-row">
                            {link.revoked ? (
                              <Badge tone="danger">revoked</Badge>
                            ) : link.expired ? (
                              <Badge tone="warn">expired</Badge>
                            ) : (
                              <Badge tone="ok">active</Badge>
                            )}
                          </div>
                        </td>
                        <td>{link.accessCount}</td>
                        <td>{link.createdByName ?? '—'}</td>
                        <td>
                          <Button
                            size="sm"
                            variant="danger"
                            disabled={busy || link.revoked}
                            onClick={() => void revokeLink(link)}
                          >
                            Revoke
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>

        <div className="grid" style={{ gap: 16 }}>
          <Card title="Storage" hint="FileManagementService.storageStatistics().">
            {!storage ? (
              <Empty>Storage statistics unavailable.</Empty>
            ) : (
              <div className="grid" style={{ gap: 10 }}>
                <div className="list-row">
                  <span className="muted">Files</span>
                  <div className="topbar-spacer" />
                  <strong>{storage.files}</strong>
                </div>
                <div className="list-row">
                  <span className="muted">Bytes</span>
                  <div className="topbar-spacer" />
                  <strong>{formatBytes(storage.bytes)}</strong>
                </div>
                <div className="list-row">
                  <span className="muted">Shared files</span>
                  <div className="topbar-spacer" />
                  <strong>{storage.shared}</strong>
                </div>
                <div className="list-row">
                  <span className="muted">Share links</span>
                  <div className="topbar-spacer" />
                  <strong>{storage.links}</strong>
                </div>
                <div className="list-row">
                  <span className="muted">Provider</span>
                  <div className="topbar-spacer" />
                  <Badge tone="s04">{storage.provider}</Badge>
                </div>
                <div className="list-row">
                  <span className="muted">Max upload</span>
                  <div className="topbar-spacer" />
                  <strong>{storage.maxUploadMb} MB</strong>
                </div>
              </div>
            )}
          </Card>

          <Card title="Upload contract">
            <div className="muted" style={{ fontSize: 13, lineHeight: 1.6 }}>
              <p style={{ marginTop: 0 }}>
                <code>UploadFile(file, content)</code> validates the record internally: the name must
                be short, the size must stay under <strong>{maxUploadMb} MB</strong> and executable
                extensions (<code>.exe</code>, <code>.bat</code>, <code>.cmd</code>) are refused with a
                symbolic non-success result code.
              </p>
              <p>
                When validation passes, <code>StorageAdapter.WriteFile(file, content)</code> stores the
                content in the configured backend and the record becomes <code>VALIDATED</code>;
                rejected uploads are kept with the status <code>REJECTED</code> and the validation
                error.
              </p>
              <p style={{ marginBottom: 0 }}>
                <code>CreateShareLink(file, expiry)</code> then issues a token whose access path is
                counted on every download, until the link expires or is revoked.
              </p>
            </div>
          </Card>
        </div>
      </div>

      {showUpload && (
        <Modal
          title="Upload file"
          onClose={() => setShowUpload(false)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setShowUpload(false)}>
                Cancel
              </Button>
              <Button variant="primary" disabled={busy} onClick={() => void uploadFile()}>
                Upload
              </Button>
            </>
          }
        >
          <Field label="File name">
            <input
              className="input"
              value={upName}
              onChange={(event) => setUpName(event.target.value)}
              placeholder="architecture-diagram.pdf"
            />
          </Field>
          <Field label="Folder">
            <input
              className="input"
              value={upFolder}
              onChange={(event) => setUpFolder(event.target.value)}
              placeholder="/design"
            />
          </Field>
          <div className="grid grid-2">
            <Field label="Size (MB)">
              <input
                className="input"
                type="number"
                min="0.1"
                step="0.1"
                value={upSizeMb}
                onChange={(event) => setUpSizeMb(event.target.value)}
              />
            </Field>
            <Field label="Content type">
              <input
                className="input"
                value={upContentType}
                onChange={(event) => setUpContentType(event.target.value)}
              />
            </Field>
          </div>
          <div className="muted" style={{ fontSize: 12 }}>
            Executables are refused and the size limit is {maxUploadMb} MB; the error message from the
            backend is shown as a toast.
          </div>
        </Modal>
      )}
    </div>
  )
}
