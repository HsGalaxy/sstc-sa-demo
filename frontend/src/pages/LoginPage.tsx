import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../app/AuthContext'
import { Avatar, Button, Field } from '../components/ui'

const DEMO_ACCOUNTS = [
  { username: 'lead', password: 'Lead@123', name: 'Alex Lead', role: 'Overall Lead · S01', color: '#4f7cff' },
  {
    username: 'manager',
    password: 'Manager@123',
    name: 'Nina Manager',
    role: 'Senior Manager · S02 files',
    color: '#22a06b'
  },
  {
    username: 'yunteng',
    password: 'Member@123',
    name: 'Yunteng Hou',
    role: 'Team Member · S04 Toolbox',
    color: '#f2994a'
  },
  {
    username: 'jinghui',
    password: 'Member@123',
    name: 'Jinghui Huang',
    role: 'Team Member · S02 Dashboard',
    color: '#9b51e0'
  },
  {
    username: 'changyang',
    password: 'Member@123',
    name: 'Changyang Qian',
    role: 'Team Member · S03 Collaboration',
    color: '#2d9cdb'
  },
  { username: 'hao', password: 'Member@123', name: 'Hao Tan', role: 'Team Member · S01 Identity', color: '#eb5757' }
]

export function LoginPage() {
  const { signIn, signingIn, error, user, meta } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('lead')
  const [password, setPassword] = useState('Lead@123')

  if (user) return <Navigate to="/" replace />

  const submit = async () => {
    try {
      await signIn(username.trim(), password)
      navigate('/', { replace: true })
    } catch {
      /* the error is rendered below */
    }
  }

  return (
    <div className="login-page">
      <section className="login-hero">
        <div className="brand-badge" style={{ color: '#8ab4ff' }}>
          <span className="brand-dot" />
          UTS · SOFTWARE ARCHITECTURE · GROUP 1
        </div>
        <h1>Small Team Collaboration Platform</h1>
        <p>
          Full stack reference implementation of the C4 architecture designed for the
          &ldquo;Smart Working Management App (small teams)&rdquo; case study: four sub-systems, one
          shared dashboard, a Java backend and a relational database behind it.
        </p>
        <div className="hero-modules">
          {(meta?.modules ?? []).map((module) => (
            <div className="hero-module" key={module.id}>
              <div className="id">{module.id}</div>
              <div className="name">{module.name}</div>
              <div className="desc">{module.description}</div>
            </div>
          ))}
        </div>
        <p style={{ fontSize: 12, color: '#7d8db1' }}>
          Stack: {(meta?.stack ?? ['Java', 'Spring Boot', 'H2', 'React']).join(' · ')}
        </p>
      </section>

      <section className="login-panel">
        <div className="login-card">
          <h2>Sign in</h2>
          <div className="sub">Use a seeded demo account, or pick one below to fill the form.</div>

          <Field label="Username or email">
            <input
              className="input"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              placeholder="lead"
              autoComplete="username"
            />
          </Field>
          <Field label="Password">
            <input
              className="input"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') void submit()
              }}
              autoComplete="current-password"
            />
          </Field>

          {error && (
            <div className="badge danger" style={{ marginBottom: 12 }}>
              {error}
            </div>
          )}

          <Button variant="primary" onClick={submit} disabled={signingIn}>
            {signingIn ? 'Signing in…' : 'Sign in'}
          </Button>

          <div className="demo-accounts">
            <div className="nav-group-title" style={{ padding: '0 0 8px' }}>
              SEEDED DEMO ACCOUNTS
            </div>
            {DEMO_ACCOUNTS.map((account) => (
              <div
                className="demo-account"
                key={account.username}
                onClick={() => {
                  setUsername(account.username)
                  setPassword(account.password)
                }}
              >
                <Avatar name={account.name} color={account.color} size="sm" />
                <div className="who">
                  <div className="n">{account.name}</div>
                  <div className="r">{account.role}</div>
                </div>
                <span className="mono soft">{account.username}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}
