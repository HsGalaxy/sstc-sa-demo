import { useCallback, useEffect, useState } from 'react'
import { api, type DocEditView, type DocumentView, type IntegrationStatus } from '../../api/client'
import { useToast } from '../../app/ToastContext'
import {
  Badge,
  Button,
  Card,
  Empty,
  Field,
  Modal,
  Stat,
  formatDateTime,
  relativeTime
} from '../../components/ui'

/** Readable message for anything the API client throws. */
const errorText = (err: unknown): string => (err instanceof Error ? err.message : String(err))

/** The three edit operations exposed by WordDocumentBuilder.UpdateDocument. */
type EditOperation = 'APPEND' | 'REPLACE' | 'RENAME'

const TEMPLATES = ['blank', 'meeting-notes', 'design-doc', 'report', 'decision-record']

/**
 * US01 · Toolbox.DocumentEditing · WordDocumentBuilder + OfficeAPIAdapter
 *
 * The page mirrors the documented execution view of {@code UpdateDocument}:
 * {@code GetQuota(apiName)} is checked first and {@code RunOfficeOperation(edit)} only runs when
 * quota is available; otherwise the recorded result code is symbolic (-1).
 */
export function DocumentsPage() {
  const toast = useToast()

  const [documents, setDocuments] = useState<DocumentView[]>([])
  const [integrations, setIntegrations] = useState<IntegrationStatus | null>(null)
  const [selected, setSelected] = useState<DocumentView | null>(null)
  const [edits, setEdits] = useState<DocEditView[]>([])
  const [draft, setDraft] = useState('')
  const [renameDraft, setRenameDraft] = useState('')
  const [busy, setBusy] = useState(false)

  const [showNew, setShowNew] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newTemplate, setNewTemplate] = useState('blank')
  const [newContent, setNewContent] = useState('')

  /** Toolbox.OfficeAPIAdapter status panel. */
  const loadIntegrations = useCallback(async () => {
    try {
      setIntegrations(await api.integrations())
    } catch (err) {
      toast.push(`Integrations unavailable: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadDocuments = useCallback(async () => {
    try {
      setDocuments(await api.documents())
    } catch (err) {
      toast.push(`Could not load documents: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const openDocument = useCallback(
    async (id: number) => {
      try {
        const [document, history] = await Promise.all([api.document(id), api.documentEdits(id)])
        setSelected(document)
        setDraft(document.content ?? '')
        setRenameDraft(document.title)
        setEdits(history)
      } catch (err) {
        toast.push(`Could not open document: ${errorText(err)}`, 'error')
      }
    },
    [toast]
  )

  useEffect(() => {
    void loadDocuments()
    void loadIntegrations()
  }, [loadDocuments, loadIntegrations])

  /** UpdateDocument(documentID, edit) followed by a full refresh of the document. */
  const applyEdit = async (operation: EditOperation, payload: string) => {
    if (!selected) return
    setBusy(true)
    try {
      const updated = await api.editDocument(selected.id, { operation, payload })
      setSelected(updated)
      setDraft(updated.content ?? '')
      setRenameDraft(updated.title)
      const history = await api.documentEdits(updated.id)
      setEdits(history)
      const latest = history.length > 0 ? history[0] : null
      if (latest && latest.resultCode !== 0) {
        toast.push(
          `${operation} not applied: resultCode ${latest.resultCode} (quota ${latest.quotaRemaining})`,
          'error'
        )
      } else {
        toast.push(`${operation} applied · revision ${updated.revision}`, 'ok')
      }
      await loadDocuments()
      await loadIntegrations()
    } catch (err) {
      toast.push(`Edit failed: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const createDocument = async () => {
    if (!newTitle.trim()) {
      toast.push('A document title is required', 'error')
      return
    }
    try {
      const created = await api.createDocument({
        title: newTitle.trim(),
        content: newContent,
        template: newTemplate
      })
      toast.push(`Document “${created.title}” created (revision ${created.revision})`, 'ok')
      setShowNew(false)
      setNewTitle('')
      setNewContent('')
      setNewTemplate('blank')
      await loadDocuments()
      await openDocument(created.id)
    } catch (err) {
      toast.push(`Create failed: ${errorText(err)}`, 'error')
    }
  }

  const quotaRemaining = integrations?.officeQuotaRemaining ?? 0
  const quotaTotal = integrations?.officeQuotaTotal ?? 0

  return (
    <div className="grid" style={{ gap: 16 }}>
      {/* US01 header block */}
      <div>
        <h1 style={{ margin: 0, fontSize: 20, letterSpacing: '-0.4px' }}>Documents</h1>
        <div className="muted" style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.4px' }}>
          US01 · Toolbox.DocumentEditing · WordDocumentBuilder + OfficeAPIAdapter
        </div>
      </div>

      <div className="grid grid-4">
        <Stat label="Documents" value={documents.length} accent="s04" />
        <Stat
          label="Office API provider"
          value={integrations?.officeProvider ?? '—'}
          delta="OfficeAPIAdapter"
          accent="s04"
        />
        <Stat
          label="Office quota"
          value={`${quotaRemaining} / ${quotaTotal}`}
          delta="GetQuota(apiName) before every edit"
          accent="s04"
        />
        <Stat
          label="Edits recorded"
          value={edits.length}
          delta={selected ? `for “${selected.title}”` : 'select a document'}
          accent="s04"
        />
      </div>

      <Card
        title="Document library"
        hint="Every document is stored with its owner, template, revision and word count."
        actions={
          <Button variant="primary" onClick={() => setShowNew(true)}>
            New document
          </Button>
        }
        padded={false}
      >
        {documents.length === 0 ? (
          <Empty>No documents yet — create the first one.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Owner</th>
                  <th>Template</th>
                  <th>Rev</th>
                  <th>Words</th>
                  <th>Last edited</th>
                  <th>Office API</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {documents.map((document) => (
                  <tr
                    key={document.id}
                    onClick={() => void openDocument(document.id)}
                    style={{ cursor: 'pointer' }}
                  >
                    <td>
                      <div style={{ fontWeight: 600 }}>{document.title}</div>
                      <div className="soft mono">#{document.id}</div>
                    </td>
                    <td>{document.ownerName ?? '—'}</td>
                    <td>
                      <Badge tone="s04">{document.template ?? 'blank'}</Badge>
                    </td>
                    <td>{document.revision}</td>
                    <td>{document.wordCount}</td>
                    <td title={formatDateTime(document.lastEditedAt)}>
                      {relativeTime(document.lastEditedAt)}
                    </td>
                    <td className="mono soft">{document.officeApi ?? '—'}</td>
                    <td>
                      <Button size="sm" onClick={() => void openDocument(document.id)}>
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

      {selected ? (
        <div className="grid grid-2-1">
          <Card
            title={`Edit “${selected.title}”`}
            hint="UpdateDocument(documentID, edit) → GetQuota(apiName) → RunOfficeOperation(edit)."
            actions={<Badge tone="s04">revision {selected.revision}</Badge>}
          >
            <Field label="Content (editable)">
              <textarea
                className="textarea"
                rows={12}
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
              />
            </Field>
            <div className="toolbar" style={{ marginBottom: 14 }}>
              <Button variant="primary" disabled={busy} onClick={() => void applyEdit('APPEND', draft)}>
                Append text
              </Button>
              <Button disabled={busy} onClick={() => void applyEdit('REPLACE', draft)}>
                Replace content
              </Button>
              <Button variant="ghost" disabled={busy} onClick={() => void openDocument(selected.id)}>
                Reload document
              </Button>
            </div>
            <Field label="Rename — new title">
              <div className="toolbar">
                <input
                  className="input"
                  style={{ flex: 1, minWidth: 220 }}
                  value={renameDraft}
                  onChange={(event) => setRenameDraft(event.target.value)}
                />
                <Button
                  disabled={busy || !renameDraft.trim()}
                  onClick={() => void applyEdit('RENAME', renameDraft.trim())}
                >
                  Rename
                </Button>
              </div>
            </Field>
            <div className="muted" style={{ fontSize: 12 }}>
              {selected.wordCount} words · owner {selected.ownerName ?? '—'} · last edit{' '}
              {relativeTime(selected.lastEditedAt)} · provider {selected.officeApi ?? '—'}
            </div>
          </Card>

          <div className="grid" style={{ gap: 16 }}>
            <Stat
              label="Quota remaining today"
              value={quotaRemaining}
              delta={`of ${quotaTotal} simulated Office API operations`}
              accent="s04"
            />
            <Card title="Quota protocol">
              <div className="muted" style={{ fontSize: 13, lineHeight: 1.6 }}>
                <p style={{ marginTop: 0 }}>
                  <code>UpdateDocument</code> first calls <code>GetQuota(apiName)</code>. Only when
                  quota is available does it call <code>RunOfficeOperation(edit)</code> and bump the
                  document revision.
                </p>
                <p>
                  A <code>resultCode</code> of <code>-1</code> means the simulated Office API quota is
                  exhausted, so the edit is recorded but the document is left untouched. The exact
                  number is documented as symbolic in the sequence diagrams, and the same symbolic
                  code is shown in the edit history below.
                </p>
              </div>
            </Card>
          </div>
        </div>
      ) : (
        <Card title="Editor">
          <Empty>Select a document in the library to open the editor and its edit history.</Empty>
        </Card>
      )}

      <Card
        title="Edit history"
        hint="One row per UpdateDocument call, including the quota bookkeeping of the adapter."
        padded={false}
      >
        {!selected ? (
          <Empty>Select a document to inspect its edit history.</Empty>
        ) : edits.length === 0 ? (
          <Empty>No edits recorded for this document yet.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Operation</th>
                  <th>Editor</th>
                  <th>Result code</th>
                  <th>Quota remaining</th>
                  <th>Applied</th>
                </tr>
              </thead>
              <tbody>
                {edits.map((edit) => (
                  <tr key={edit.id}>
                    <td>
                      <Badge tone={edit.operation === 'RENAME' ? 'info' : 's04'}>{edit.operation}</Badge>
                    </td>
                    <td>{edit.editorName ?? '—'}</td>
                    <td>
                      <Badge tone={edit.resultCode === 0 ? 'ok' : 'danger'}>
                        {edit.resultCode === 0 ? '0 · success' : `${edit.resultCode} · symbolic failure`}
                      </Badge>
                    </td>
                    <td>{edit.quotaRemaining}</td>
                    <td title={formatDateTime(edit.appliedAt)}>{relativeTime(edit.appliedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {showNew && (
        <Modal
          title="New document"
          onClose={() => setShowNew(false)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setShowNew(false)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={() => void createDocument()}>
                Create document
              </Button>
            </>
          }
        >
          <Field label="Title">
            <input
              className="input"
              value={newTitle}
              onChange={(event) => setNewTitle(event.target.value)}
              placeholder="Sprint 4 design notes"
            />
          </Field>
          <Field label="Template">
            <select
              className="select"
              value={newTemplate}
              onChange={(event) => setNewTemplate(event.target.value)}
            >
              {TEMPLATES.map((template) => (
                <option key={template} value={template}>
                  {template}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Initial content">
            <textarea
              className="textarea"
              rows={6}
              value={newContent}
              onChange={(event) => setNewContent(event.target.value)}
              placeholder="Opening paragraph…"
            />
          </Field>
        </Modal>
      )}
    </div>
  )
}
