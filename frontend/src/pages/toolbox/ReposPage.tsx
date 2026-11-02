import { useCallback, useEffect, useState } from 'react'
import { api, type CommitView, type IntegrationStatus, type RepoView } from '../../api/client'
import { useToast } from '../../app/ToastContext'
import { Badge, Button, Card, Empty, Field, Modal, Stat, formatDateTime, relativeTime } from '../../components/ui'

/** Readable message for anything the API client throws. */
const errorText = (err: unknown): string => (err instanceof Error ? err.message : String(err))

const LANGUAGES = ['Java', 'TypeScript', 'Python', 'SQL', 'Go']

/**
 * US03 · Toolbox.VersionControl · VersionControlService + GitRepositoryAdapter
 *
 * Publishing a change is the documented two step contract: {@code Commit(changes)} creates the
 * commit record and {@code Push(repoID, changes)} publishes it through the Git adapter, which
 * answers with an integer result code where {@code 0} means success.
 */
export function ReposPage() {
  const toast = useToast()

  const [repos, setRepos] = useState<RepoView[]>([])
  const [recent, setRecent] = useState<CommitView[]>([])
  const [integrations, setIntegrations] = useState<IntegrationStatus | null>(null)
  const [selected, setSelected] = useState<RepoView | null>(null)
  const [commits, setCommits] = useState<CommitView[]>([])
  const [busy, setBusy] = useState(false)

  // ---- publish change form
  const [message, setMessage] = useState('')
  const [branch, setBranch] = useState('')

  // ---- new repository modal
  const [showNew, setShowNew] = useState(false)
  const [newName, setNewName] = useState('')
  const [newLanguage, setNewLanguage] = useState('Java')
  const [newDescription, setNewDescription] = useState('')

  const loadIntegrations = useCallback(async () => {
    try {
      setIntegrations(await api.integrations())
    } catch (err) {
      toast.push(`Integrations unavailable: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadRecent = useCallback(async () => {
    try {
      setRecent(await api.recentCommits())
    } catch (err) {
      toast.push(`Could not load recent commits: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadRepos = useCallback(async () => {
    try {
      setRepos(await api.repos())
    } catch (err) {
      toast.push(`Could not load repositories: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const openRepo = useCallback(
    async (id: number) => {
      try {
        const [repo, history] = await Promise.all([api.repo(id), api.commits(id)])
        setSelected(repo)
        setCommits(history)
        setBranch(repo.defaultBranch)
      } catch (err) {
        toast.push(`Could not open repository: ${errorText(err)}`, 'error')
      }
    },
    [toast]
  )

  useEffect(() => {
    void loadRepos()
    void loadRecent()
    void loadIntegrations()
  }, [loadRepos, loadRecent, loadIntegrations])

  /** Commit(changes) then Push(repoID, changes). */
  const publishChange = async () => {
    if (!selected) return
    if (!message.trim()) {
      toast.push('A commit message is required', 'error')
      return
    }
    setBusy(true)
    try {
      const commit = await api.pushCommit(selected.id, {
        message: message.trim(),
        branch: branch.trim() || selected.defaultBranch
      })
      toast.push(
        `Pushed ${commit.shortSha} to ${commit.branch} · result code 0 (success)`,
        'ok'
      )
      setMessage('')
      const [repo, history] = await Promise.all([api.repo(selected.id), api.commits(selected.id)])
      setSelected(repo)
      setCommits(history)
      await loadRepos()
      await loadRecent()
    } catch (err) {
      toast.push(`Push failed (non-zero result code): ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const createRepo = async () => {
    if (!newName.trim()) {
      toast.push('A repository name is required', 'error')
      return
    }
    try {
      const created = await api.createRepo({
        name: newName.trim(),
        language: newLanguage,
        description: newDescription
      })
      toast.push(`Repository “${created.slug}” created`, 'ok')
      setShowNew(false)
      setNewName('')
      setNewLanguage('Java')
      setNewDescription('')
      await loadRepos()
      await loadIntegrations()
      await openRepo(created.id)
    } catch (err) {
      toast.push(`Create failed: ${errorText(err)}`, 'error')
    }
  }

  const visibilityTone = (visibility: string): 'ok' | 'warn' | 'default' => {
    if (visibility === 'PUBLIC') return 'ok'
    if (visibility === 'INTERNAL') return 'warn'
    return 'default'
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      {/* US03 header block */}
      <div>
        <h1 style={{ margin: 0, fontSize: 20, letterSpacing: '-0.4px' }}>Git repositories</h1>
        <div className="muted" style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.4px' }}>
          US03 · Toolbox.VersionControl · VersionControlService + GitRepositoryAdapter
        </div>
      </div>

      <div className="grid grid-4">
        <Stat label="Repositories" value={repos.length} accent="s04" />
        <Stat label="Git provider" value={integrations?.gitProvider ?? '—'} accent="s04" />
        <Stat
          label="Commits loaded"
          value={selected ? commits.length : recent.length}
          delta={selected ? `on ${selected.slug}` : 'recent across all repos'}
          accent="s04"
        />
        <Stat
          label="Open pull requests"
          value={repos.reduce((total, repo) => total + repo.openPullRequests, 0)}
          delta="across every repository"
          accent="s04"
        />
      </div>

      <Card
        title="Repositories"
        hint="Repositories mirrored by the GitRepositoryAdapter, with their default branch and history size."
        actions={
          <Button variant="primary" onClick={() => setShowNew(true)}>
            New repository
          </Button>
        }
        padded={false}
      >
        {repos.length === 0 ? (
          <Empty>No repositories yet — create the first one.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Provider</th>
                  <th>Language</th>
                  <th>Visibility</th>
                  <th>Default branch</th>
                  <th>Commits</th>
                  <th>Last commit</th>
                  <th>Stars</th>
                  <th>PRs</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {repos.map((repo) => (
                  <tr key={repo.id} onClick={() => void openRepo(repo.id)} style={{ cursor: 'pointer' }}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{repo.name}</div>
                      <div className="soft mono">{repo.slug}</div>
                    </td>
                    <td>{repo.provider}</td>
                    <td>{repo.language ?? '—'}</td>
                    <td>
                      <Badge tone={visibilityTone(repo.visibility)}>{repo.visibility}</Badge>
                    </td>
                    <td className="mono">{repo.defaultBranch}</td>
                    <td>{repo.commitCount}</td>
                    <td>
                      <div>{repo.lastCommitMessage ?? '—'}</div>
                      <div className="soft">{relativeTime(repo.lastCommitAt)}</div>
                    </td>
                    <td>★ {repo.stars}</td>
                    <td>{repo.openPullRequests}</td>
                    <td>
                      <Button size="sm" onClick={() => void openRepo(repo.id)}>
                        History
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
            title={selected ? `Commit history · ${selected.slug}` : 'Commit history'}
            hint="GetHistory(repoID, branch) rendered from the commit records."
            padded={false}
          >
            {!selected ? (
              <Empty>Select a repository to load its commit history.</Empty>
            ) : commits.length === 0 ? (
              <Empty>This repository has no commits yet.</Empty>
            ) : (
              <div className="card-body">
                <div className="timeline">
                  {commits.map((commit) => (
                    <div className="timeline-item" key={commit.id}>
                      <div className="toolbar">
                        <span className="mono">{commit.shortSha}</span>
                        <Badge tone="s04">{commit.branch}</Badge>
                        <div className="topbar-spacer" />
                        <span className="soft" style={{ fontSize: 12 }} title={formatDateTime(commit.committedAt)}>
                          {relativeTime(commit.committedAt)}
                        </span>
                      </div>
                      <div style={{ fontWeight: 600, marginTop: 4 }}>{commit.message}</div>
                      <div className="muted" style={{ fontSize: 12 }}>
                        {commit.authorName ?? '—'} · +{commit.additions} / −{commit.deletions} ·{' '}
                        {commit.changedFiles} file{commit.changedFiles === 1 ? '' : 's'} changed
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </Card>

          {selected && (
            <Card
              title="Publish change"
              hint="Commit(changes) followed by Push(repoID, changes); the adapter answers with an integer result code, 0 = success."
            >
              <Field label="Commit message">
                <input
                  className="input"
                  value={message}
                  onChange={(event) => setMessage(event.target.value)}
                  placeholder="Add toolbox integration tests"
                />
              </Field>
              <Field label="Branch">
                <input
                  className="input"
                  value={branch}
                  onChange={(event) => setBranch(event.target.value)}
                  placeholder={selected.defaultBranch}
                />
              </Field>
              <div className="toolbar">
                <Button variant="primary" disabled={busy} onClick={() => void publishChange()}>
                  Publish change
                </Button>
                <Button variant="ghost" disabled={busy} onClick={() => void openRepo(selected.id)}>
                  Reload history
                </Button>
              </div>
              <div className="muted" style={{ fontSize: 12, marginTop: 10 }}>
                The simulated Git adapter returns <code>0</code> on success and a non-zero symbolic code
                when the push is rejected; the new commit is appended to the branch above.
              </div>
            </Card>
          )}
        </div>

        <Card title="Recent commits" hint="Latest activity across every repository." padded={false}>
          {recent.length === 0 ? (
            <Empty>No commits recorded yet.</Empty>
          ) : (
            <div className="card-body">
              <div className="list">
                {recent.map((commit) => (
                  <div className="list-row" key={commit.id}>
                    <div className="grow">
                      <div className="title">{commit.message}</div>
                      <div className="sub">
                        <span className="mono">{commit.shortSha}</span> · {commit.authorName ?? '—'} ·{' '}
                        {commit.branch}
                      </div>
                    </div>
                    <span className="soft" style={{ fontSize: 12 }}>
                      {relativeTime(commit.committedAt)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </Card>
      </div>

      {showNew && (
        <Modal
          title="New repository"
          onClose={() => setShowNew(false)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setShowNew(false)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={() => void createRepo()}>
                Create repository
              </Button>
            </>
          }
        >
          <Field label="Name">
            <input
              className="input"
              value={newName}
              onChange={(event) => setNewName(event.target.value)}
              placeholder="toolbox-service"
            />
          </Field>
          <Field label="Primary language">
            <select
              className="select"
              value={newLanguage}
              onChange={(event) => setNewLanguage(event.target.value)}
            >
              {LANGUAGES.map((language) => (
                <option key={language} value={language}>
                  {language}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Description">
            <textarea
              className="textarea"
              rows={3}
              value={newDescription}
              onChange={(event) => setNewDescription(event.target.value)}
              placeholder="S04 development tools container"
            />
          </Field>
        </Modal>
      )}
    </div>
  )
}
