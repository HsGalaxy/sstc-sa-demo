import type { ReactNode } from 'react'

/** Small, dependency free UI kit shared by every sub-system page. */

export function Card({
  title,
  hint,
  actions,
  children,
  padded = true,
  className = ''
}: {
  title?: ReactNode
  hint?: ReactNode
  actions?: ReactNode
  children: ReactNode
  padded?: boolean
  className?: string
}) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-head">
          <div>
            <h3>{title}</h3>
            {hint && <div className="hint">{hint}</div>}
          </div>
          <div className="topbar-spacer" />
          {actions}
        </header>
      )}
      <div className={padded ? 'card-body' : ''}>{children}</div>
    </section>
  )
}

export function Stat({
  label,
  value,
  delta,
  accent
}: {
  label: string
  value: ReactNode
  delta?: ReactNode
  accent?: 's01' | 's02' | 's03' | 's04'
}) {
  return (
    <div className={`card stat stat-accent ${accent ?? ''}`}>
      <span className="label">{label}</span>
      <span className="value">{value}</span>
      {delta && <span className="delta">{delta}</span>}
    </div>
  )
}

export function Badge({
  children,
  tone = 'default'
}: {
  children: ReactNode
  tone?: 'default' | 'solid' | 'ok' | 'warn' | 'danger' | 'info' | 's01' | 's02' | 's03' | 's04'
}) {
  return <span className={`badge ${tone === 'default' ? '' : tone}`}>{children}</span>
}

export function Button({
  children,
  onClick,
  variant = 'default',
  size,
  disabled,
  title,
  type = 'button'
}: {
  children: ReactNode
  onClick?: () => void
  variant?: 'default' | 'primary' | 'ghost' | 'danger'
  size?: 'sm'
  disabled?: boolean
  title?: string
  type?: 'button' | 'submit'
}) {
  return (
    <button
      type={type}
      title={title}
      disabled={disabled}
      onClick={onClick}
      className={`btn ${variant === 'default' ? '' : variant} ${size === 'sm' ? 'sm' : ''}`}
    >
      {children}
    </button>
  )
}

export function Field({
  label,
  children
}: {
  label: string
  children: ReactNode
}) {
  return (
    <div className="field">
      <label>{label}</label>
      {children}
    </div>
  )
}

export function Avatar({ name, color, size }: { name?: string; color?: string; size?: 'sm' | 'lg' }) {
  const initials = (name ?? '?')
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
  return (
    <span
      className={`avatar ${size === 'sm' ? 'sm' : size === 'lg' ? 'lg' : ''}`}
      style={{ background: color ?? '#4f7cff' }}
      title={name}
    >
      {initials}
    </span>
  )
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>
}

export function Modal({
  title,
  onClose,
  children,
  footer,
  wide
}: {
  title: string
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  wide?: boolean
}) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal"
        style={wide ? { maxWidth: 820 } : undefined}
        onClick={(event) => event.stopPropagation()}
      >
        <header className="modal-head">
          <h3>{title}</h3>
          <div className="topbar-spacer" />
          <Button variant="ghost" size="sm" onClick={onClose}>
            ✕
          </Button>
        </header>
        <div className="modal-body">{children}</div>
        {footer && <footer className="modal-foot">{footer}</footer>}
      </div>
    </div>
  )
}

export function Bar({ value, tone }: { value: number; tone?: string }) {
  const clamped = Math.max(0, Math.min(100, value))
  return (
    <div className="bar">
      <span
        style={{
          width: `${clamped}%`,
          background: tone ?? 'linear-gradient(90deg, #4f7cff, #22a06b)'
        }}
      />
    </div>
  )
}

export function ProgressRow({ label, value }: { label: string; value: number }) {
  return (
    <div className="progress-row">
      <span className="label">{label}</span>
      <div style={{ flex: 1 }}>
        <Bar value={value} />
      </div>
      <span className="value">{Math.round(value)}%</span>
    </div>
  )
}

/** Minimal dependency free sparkline used by the performance panel. */
export function Sparkline({
  values,
  width = 320,
  height = 70,
  color = '#4f7cff',
  label
}: {
  values: number[]
  width?: number
  height?: number
  color?: string
  label?: string
}) {
  if (values.length === 0) {
    return <Empty>No indicator history yet.</Empty>
  }
  const max = Math.max(...values, 1)
  const min = Math.min(...values, 0)
  const span = max - min || 1
  const step = values.length > 1 ? width / (values.length - 1) : width
  const points = values.map((value, index) => {
    const x = index * step
    const y = height - ((value - min) / span) * (height - 10) - 5
    return `${x.toFixed(1)},${y.toFixed(1)}`
  })
  return (
    <svg viewBox={`0 0 ${width} ${height}`} width="100%" height={height} role="img" aria-label={label}>
      <polyline
        points={points.join(' ')}
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinejoin="round"
        strokeLinecap="round"
      />
      {points.map((point, index) => {
        const [x, y] = point.split(',')
        return <circle key={index} cx={x} cy={y} r="2.6" fill="#fff" stroke={color} strokeWidth="1.6" />
      })}
    </svg>
  )
}

export function formatBytes(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${(bytes / Math.pow(1024, index)).toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

export function formatDateTime(value?: string): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function formatDate(value?: string): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString(undefined, { month: 'short', day: '2-digit' })
}

export function relativeTime(value?: string): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const diff = Date.now() - date.getTime()
  const minutes = Math.round(diff / 60000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  const days = Math.round(hours / 24)
  if (days < 30) return `${days} d ago`
  return formatDate(value)
}

/** Renders @mention tokens with the highlight used in the chat view. */
export function renderMentions(body: string) {
  const parts = body.split(/(@[a-zA-Z0-9._-]+)/g)
  return parts.map((part, index) =>
    part.startsWith('@') ? (
      <span className="mention-chip" key={index}>
        {part}
      </span>
    ) : (
      <span key={index}>{part}</span>
    )
  )
}
