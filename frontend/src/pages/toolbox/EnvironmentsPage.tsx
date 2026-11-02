import { useCallback, useEffect, useState } from 'react'
import { api, type EnvironmentView, type IntegrationStatus } from '../../api/client'
import { useToast } from '../../app/ToastContext'
import { Badge, Button, Card, Empty, Field, Modal, Stat, formatDateTime, relativeTime } from '../../components/ui'

/** Readable message for anything the API client throws. */
const errorText = (err: unknown): string => (err instanceof Error ? err.message : String(err))

const IMAGES = ['sstc/dev-env:1.0', 'sstc/backend:1.0', 'sstc/frontend:1.0', 'postgres:14-alpine']

/** State tone shared by the table and the status lookup panel. */
const stateTone = (state: string): 'ok' | 'warn' | 'danger' | 'default' => {
  if (state === 'RUNNING') return 'ok'
  if (state === 'STOPPED') return 'warn'
  if (state === 'FAILED') return 'danger'
  return 'default'
}

/**
 * US05 · Toolbox.ContainerRuntime · DevelopmentToolsService + ContainerRuntimeAdapter
 *
 * Two distinct things are shown side by side: the *request acceptance* of
 * {@code RouteRequest(request)} / {@code StartEnvironment(request)} (the integer result code, 0 on
 * success) and the *runtime state* of the container (RUNNING / STOPPED / FAILED).
 * {@code GetServiceStatus(serviceID)} delegates to {@code GetEnvironmentStatus(containerID)} through
 * the service-to-container association retained by the runtime.
 */
export function EnvironmentsPage() {
  const toast = useToast()

  const [environments, setEnvironments] = useState<EnvironmentView[]>([])
  const [integrations, setIntegrations] = useState<IntegrationStatus | null>(null)
  const [busy, setBusy] = useState(false)

  // ---- start environment modal
  const [showStart, setShowStart] = useState(false)
  const [serviceId, setServiceId] = useState('')
  const [image, setImage] = useState('sstc/dev-env:1.0')
  const [cpuLimit, setCpuLimit] = useState('1')
  const [memoryMb, setMemoryMb] = useState('1024')
  const [ports, setPorts] = useState('8080:8080')

  // ---- status lookup panel
  const [lookupServiceId, setLookupServiceId] = useState('')
  const [lookupContainerId, setLookupContainerId] = useState('')
  const [lookupResult, setLookupResult] = useState<Record<string, string> | null>(null)
  const [heartbeatRunning, setHeartbeatRunning] = useState<number | null>(null)

  const loadIntegrations = useCallback(async () => {
    try {
      setIntegrations(await api.integrations())
    } catch (err) {
      toast.push(`Integrations unavailable: ${errorText(err)}`, 'error')
    }
  }, [toast])

  const loadEnvironments = useCallback(async () => {
    try {
      setEnvironments(await api.environments())
    } catch (err) {
      toast.push(`Could not load environments: ${errorText(err)}`, 'error')
    }
  }, [toast])

  useEffect(() => {
    void loadEnvironments()
    void loadIntegrations()
  }, [loadEnvironments, loadIntegrations])

  /** StartEnvironment(request) — request accepted (result code 0) then the container starts. */
  const startEnvironment = async () => {
    if (!serviceId.trim()) {
      toast.push('A service id is required', 'error')
      return
    }
    setBusy(true)
    try {
      const started = await api.startEnvironment({
        serviceId: serviceId.trim(),
        image,
        cpuLimit: Number(cpuLimit) || 1,
        memoryMb: Number(memoryMb) || 1024,
        ports
      })
      toast.push(
        `Request accepted (result code 0) · ${started.serviceId} → ${started.containerId} is ${started.state}`,
        'ok'
      )
      setShowStart(false)
      setServiceId('')
      await loadEnvironments()
      await loadIntegrations()
    } catch (err) {
      toast.push(`Start rejected: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const stopEnvironment = async (containerId: string) => {
    setBusy(true)
    try {
      const stopped = await api.stopEnvironment(containerId)
      toast.push(`${stopped.containerId} is now ${stopped.state}`, 'ok')
      await loadEnvironments()
      await loadIntegrations()
    } catch (err) {
      toast.push(`Stop failed: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const sendHeartbeat = async () => {
    setBusy(true)
    try {
      const result = await api.heartbeat()
      setHeartbeatRunning(result.running)
      toast.push(`Heartbeat refreshed ${result.running} running environment(s)`, 'ok')
      await loadEnvironments()
    } catch (err) {
      toast.push(`Heartbeat failed: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  /** GetServiceStatus(serviceID) / GetEnvironmentStatus(containerID). */
  const lookupStatus = async () => {
    if (!lookupServiceId.trim() && !lookupContainerId.trim()) {
      toast.push('Provide a service id or a container id', 'error')
      return
    }
    setBusy(true)
    try {
      const result = await api.environmentStatus({
        serviceId: lookupServiceId.trim() || undefined,
        containerId: lookupContainerId.trim() || undefined
      })
      setLookupResult(result)
      toast.push(`Runtime state: ${result.state ?? 'UNKNOWN'}`, 'ok')
    } catch (err) {
      toast.push(`Status lookup failed: ${errorText(err)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  const running = integrations?.environmentsRunning ?? environments.filter((item) => item.state === 'RUNNING').length
  const total = integrations?.environmentsTotal ?? environments.length

  return (
    <div className="grid" style={{ gap: 16 }}>
      {/* US05 header block */}
      <div>
        <h1 style={{ margin: 0, fontSize: 20, letterSpacing: '-0.4px' }}>Dev environments</h1>
        <div className="muted" style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.4px' }}>
          US05 · Toolbox.ContainerRuntime · DevelopmentToolsService + ContainerRuntimeAdapter
        </div>
      </div>

      <div className="grid grid-4">
        <Stat
          label="Running / total"
          value={`${running} / ${total}`}
          delta="max environments of the runtime"
          accent="s04"
        />
        <Stat label="Container runtime" value={integrations?.containerRuntime ?? '—'} accent="s04" />
        <Stat
          label="Heartbeat"
          value={heartbeatRunning === null ? '—' : heartbeatRunning}
          delta="running containers refreshed by the last heartbeat"
          accent="s04"
        />
        <Stat
          label="Registered services"
          value={environments.length}
          delta="serviceID → containerID associations"
          accent="s04"
        />
      </div>

      <Card
        title="Containerised environments"
        hint="Request acceptance (result code 0) is separate from the runtime state of the container."
        actions={
          <div className="toolbar">
            <Button variant="ghost" disabled={busy} onClick={() => void sendHeartbeat()}>
              Heartbeat
            </Button>
            <Button variant="primary" onClick={() => setShowStart(true)}>
              Start environment
            </Button>
          </div>
        }
        padded={false}
      >
        {environments.length === 0 ? (
          <Empty>No environments yet — start one to create its container.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Service</th>
                  <th>Container</th>
                  <th>Image</th>
                  <th>State</th>
                  <th>CPU</th>
                  <th>Memory</th>
                  <th>Ports</th>
                  <th>Owner</th>
                  <th>Uptime</th>
                  <th>Last heartbeat</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {environments.map((environment) => (
                  <tr key={environment.id}>
                    <td className="mono">{environment.serviceId}</td>
                    <td className="mono">{environment.containerId}</td>
                    <td className="mono soft">{environment.image}</td>
                    <td>
                      <Badge tone={stateTone(environment.state)}>{environment.state}</Badge>
                    </td>
                    <td>{environment.cpuLimit} vCPU</td>
                    <td>{environment.memoryMb} MB</td>
                    <td className="mono">{environment.ports ?? '—'}</td>
                    <td>{environment.ownerName ?? '—'}</td>
                    <td>{environment.uptimeMinutes} min</td>
                    <td title={formatDateTime(environment.lastHeartbeat)}>
                      {relativeTime(environment.lastHeartbeat)}
                    </td>
                    <td>
                      <Button
                        size="sm"
                        variant="danger"
                        disabled={busy || environment.state !== 'RUNNING'}
                        onClick={() => void stopEnvironment(environment.containerId)}
                      >
                        Stop
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
        <Card
          title="Status lookup"
          hint="GetServiceStatus(serviceID) delegates to GetEnvironmentStatus(containerID) using the retained association."
        >
          <div className="grid grid-2">
            <Field label="Service id">
              <input
                className="input"
                value={lookupServiceId}
                onChange={(event) => setLookupServiceId(event.target.value)}
                placeholder="svc-toolbox"
              />
            </Field>
            <Field label="Container id">
              <input
                className="input"
                value={lookupContainerId}
                onChange={(event) => setLookupContainerId(event.target.value)}
                placeholder="ctr-1a2b3c4d5e6f"
              />
            </Field>
          </div>
          <div className="toolbar">
            <Button variant="primary" disabled={busy} onClick={() => void lookupStatus()}>
              Look up status
            </Button>
            <Button
              variant="ghost"
              disabled={busy}
              onClick={() => {
                setLookupServiceId('')
                setLookupContainerId('')
                setLookupResult(null)
              }}
            >
              Clear
            </Button>
          </div>
          {lookupResult && (
            <div className="list" style={{ marginTop: 14 }}>
              {Object.entries(lookupResult).map(([key, value]) => (
                <div className="list-row" key={key}>
                  <span className="muted mono">{key}</span>
                  <div className="topbar-spacer" />
                  <Badge tone={key === 'state' ? stateTone(value) : 'default'}>{value}</Badge>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card title="Runtime contract">
          <div className="muted" style={{ fontSize: 13, lineHeight: 1.6 }}>
            <p style={{ marginTop: 0 }}>
              <code>StartEnvironment(request)</code> only reports that the request was accepted
              (<code>int 0</code>). The container then moves through the runtime states shown in the
              table: <code>RUNNING</code>, <code>STOPPED</code> or <code>FAILED</code>.
            </p>
            <p>
              A non-zero result code means the runtime refused the request — for example when the
              configured maximum number of environments is already running.
            </p>
            <p style={{ marginBottom: 0 }}>
              <code>GetServiceStatus(serviceID)</code> keeps the serviceID → containerID association and
              delegates to <code>GetEnvironmentStatus(containerID)</code>; both paths answer with the
              same state string.
            </p>
          </div>
        </Card>
      </div>

      {showStart && (
        <Modal
          title="Start environment"
          onClose={() => setShowStart(false)}
          footer={
            <>
              <Button variant="ghost" onClick={() => setShowStart(false)}>
                Cancel
              </Button>
              <Button variant="primary" disabled={busy} onClick={() => void startEnvironment()}>
                Start environment
              </Button>
            </>
          }
        >
          <Field label="Service id">
            <input
              className="input"
              value={serviceId}
              onChange={(event) => setServiceId(event.target.value)}
              placeholder="svc-toolbox"
            />
          </Field>
          <Field label="Image">
            <select className="select" value={image} onChange={(event) => setImage(event.target.value)}>
              {IMAGES.map((candidate) => (
                <option key={candidate} value={candidate}>
                  {candidate}
                </option>
              ))}
            </select>
          </Field>
          <div className="grid grid-2">
            <Field label="CPU limit (vCPU)">
              <input
                className="input"
                type="number"
                min="0.5"
                step="0.5"
                value={cpuLimit}
                onChange={(event) => setCpuLimit(event.target.value)}
              />
            </Field>
            <Field label="Memory (MB)">
              <input
                className="input"
                type="number"
                min="128"
                step="128"
                value={memoryMb}
                onChange={(event) => setMemoryMb(event.target.value)}
              />
            </Field>
          </div>
          <Field label="Port mapping">
            <input
              className="input"
              value={ports}
              onChange={(event) => setPorts(event.target.value)}
              placeholder="8080:8080"
            />
          </Field>
        </Modal>
      )}
    </div>
  )
}
