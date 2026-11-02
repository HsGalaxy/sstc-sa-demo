import { useEffect, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../app/AuthContext'
import { Avatar, Badge, Button } from './ui'

interface NavEntry {
  to: string
  label: string
  icon: string
  tag?: string
}

interface NavGroup {
  title: string
  entries: NavEntry[]
}

const GROUPS: NavGroup[] = [
  {
    title: 'OVERVIEW',
    entries: [
      { to: '/', label: 'Dashboard', icon: '◎' },
      { to: '/architecture', label: 'Architecture map', icon: '⌘' }
    ]
  },
  {
    title: 'S02 · TASK DASHBOARD',
    entries: [
      { to: '/tasks', label: 'Kanban board', icon: '▦', tag: 'US03' },
      { to: '/files', label: 'File tracker', icon: '🗂', tag: 'US01' },
      { to: '/okr', label: 'OKR & performance', icon: '◈', tag: 'US06' },
      { to: '/operations', label: 'Operation log', icon: '⟳', tag: 'US04' }
    ]
  },
  {
    title: 'S03 · COLLABORATION',
    entries: [
      { to: '/collaboration', label: 'Channels & chat', icon: '💬', tag: 'US02' },
      { to: '/meetings', label: 'Meetings & AI summary', icon: '🎙', tag: 'US05' },
      { to: '/inbox', label: 'Notifications & digest', icon: '🔔', tag: 'US06' }
    ]
  },
  {
    title: 'S04 · TOOLBOX',
    entries: [
      { to: '/toolbox/documents', label: 'Documents', icon: '📄', tag: 'US01' },
      { to: '/toolbox/presentations', label: 'Presentations', icon: '📊', tag: 'US02' },
      { to: '/toolbox/repos', label: 'Git repositories', icon: '⎇', tag: 'US03' },
      { to: '/toolbox/environments', label: 'Dev environments', icon: '🐳', tag: 'US05' },
      { to: '/toolbox/files', label: 'Project files', icon: '📦', tag: 'US04' }
    ]
  },
  {
    title: 'S01 · IDENTITY & ACCESS',
    entries: [
      { to: '/identity/users', label: 'Accounts & roles', icon: '👥', tag: 'US01' },
      { to: '/identity/policies', label: 'Permission policies', icon: '🔐', tag: 'US03' },
      { to: '/identity/audit', label: 'Access audit', icon: '🛡', tag: 'US04' }
    ]
  }
]

export function AppShell() {
  const { user, meta, signOut } = useAuth()
  const navigate = useNavigate()
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    let alive = true
    const load = () =>
      api
        .unreadCount()
        .then((value) => {
          if (alive) setUnread(value.unread)
        })
        .catch(() => undefined)
    load()
    const timer = setInterval(load, 30000)
    return () => {
      alive = false
      clearInterval(timer)
    }
  }, [])

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-badge">
            <span className="brand-dot" />
            STC PLATFORM
          </div>
          <h1>Small Team Collaboration</h1>
          <p>{meta?.caseStudy ?? 'Smart Working Management App (small teams)'}</p>
        </div>
        <nav className="sidebar-nav">
          {GROUPS.map((group) => (
            <div key={group.title}>
              <div className="nav-group-title">{group.title}</div>
              {group.entries.map((entry) => (
                <NavLink
                  key={entry.to}
                  to={entry.to}
                  end={entry.to === '/'}
                  className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                >
                  <span className="nav-icon">{entry.icon}</span>
                  <span>{entry.label}</span>
                  {entry.tag && <span className="nav-tag">{entry.tag}</span>}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div>v{meta?.version ?? '1.0.0'} · Java 8 · Spring Boot · H2</div>
          <div style={{ marginTop: 4 }}>{meta?.stack?.slice(0, 3).join(' · ')}</div>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div>
            <h2>STC Platform</h2>
            <div className="subtitle">
              {meta?.subtitle ?? 'Reference implementation of the S01–S04 architecture'}
            </div>
          </div>
          <div className="topbar-spacer" />
          <Button variant="ghost" onClick={() => navigate('/inbox')} title="Notifications">
            🔔 {unread > 0 ? <Badge tone="danger">{unread}</Badge> : null}
          </Button>
          {user && (
            <div className="person">
              <Avatar name={user.displayName} color={user.avatarColor} />
              <div>
                <div style={{ fontSize: 13, fontWeight: 600 }}>{user.displayName}</div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                  {user.roleLabel ?? user.role}
                </div>
              </div>
            </div>
          )}
          <Button
            variant="ghost"
            size="sm"
            onClick={async () => {
              await signOut()
              navigate('/login')
            }}
          >
            Sign out
          </Button>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
