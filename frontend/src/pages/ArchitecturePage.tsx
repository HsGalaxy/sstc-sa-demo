import { useEffect, useState } from 'react'
import { api, type PlatformMeta } from '../api/client'
import { useAuth } from '../app/AuthContext'
import { Badge, Card, Empty, Stat, formatDateTime } from '../components/ui'

interface ModuleDoc {
  id: string
  tone: 's01' | 's02' | 's03' | 's04'
  name: string
  container: string
  components: string[]
  entities: string[]
  endpoints: string[]
  stories: string[]
}

const MODULES: ModuleDoc[] = [
  {
    id: 'S01',
    tone: 's01',
    name: 'Identity, Role & Access Management',
    container: 'UserRoleManager',
    components: [
      'AccountController (Account Service)',
      'RoleAssignmentController (Role & Permission API)',
      'PermissionPolicyController (Role & Permission API)',
      'AuthorizationAuditController (Identity API + Authorization Service)'
    ],
    entities: [
      'usr_account',
      'usr_role',
      'usr_permission',
      'usr_role_permission',
      'usr_user_role',
      'usr_permission_policy',
      'usr_account_event',
      'usr_access_audit',
      'usr_session_token'
    ],
    endpoints: [
      'POST /api/auth/login · POST /api/auth/logout · GET /api/identity/me',
      'GET|POST|PUT|DELETE /api/users',
      'GET /api/roles · GET /api/permissions',
      'POST|DELETE /api/users/{id}/role',
      'GET|POST /api/users/{id}/policies · DELETE /api/policies/{id}',
      'GET /api/account-events · GET /api/access-audits'
    ],
    stories: ['US01 accounts', 'US02 roles', 'US03 policies', 'US04 access audit']
  },
  {
    id: 'S02',
    tone: 's02',
    name: 'Task Dashboard',
    container: 'TaskDashboard',
    components: [
      'ModificationScheduler (Task Controller)',
      'OperationLogger (Task Controller)',
      'FileQueryController / FileBucket / HistoricalVersionController (File Tracker)',
      'DisplayPanel / PerformanceCalculator (Task Assessment)'
    ],
    entities: [
      'td_task',
      'td_cycle',
      'td_okr',
      'td_key_result',
      'td_file_record',
      'td_file_version',
      'td_operation_log',
      'td_performance_snapshot'
    ],
    endpoints: [
      'GET /api/tasks/board · GET /api/tasks/summary · GET /api/tasks',
      'POST /api/tasks · PUT /api/tasks/{id} · PATCH /api/tasks/{id}/move',
      'GET /api/cycles · POST /api/cycles',
      'GET /api/okrs · POST /api/okrs · POST /api/okrs/{id}/key-results',
      'GET /api/files · POST /api/files · POST /api/files/{id}/versions · POST /api/files/{id}/rollback',
      'GET /api/performance · GET /api/operations'
    ],
    stories: ['US01 file query', 'US02 upload', 'US03 task CRUD', 'US04 operation log', 'US05 display panel', 'US06 score']
  },
  {
    id: 'S03',
    tone: 's03',
    name: 'Team Collaboration',
    container: 'TeamCollaboration',
    components: [
      'ChannelMessageManager (Messaging Service)',
      'MentionDetector (Messaging Service)',
      'MeetingOrchestrator / AIMeetingAssistant (Meeting Service)',
      'NotificationDispatcher / SmartDigestEngine (Notification Service)'
    ],
    entities: [
      'tc_channel',
      'tc_channel_member',
      'tc_message',
      'tc_mention',
      'tc_notification',
      'tc_digest_item',
      'tc_meeting',
      'tc_meeting_transcript',
      'tc_meeting_summary'
    ],
    endpoints: [
      'GET|POST /api/channels · GET|POST /api/channels/{id}/messages',
      'GET /api/messages/recent · GET /api/mentions',
      'GET /api/notifications · PATCH /api/notifications/{id}/read · POST /api/notifications/read-all',
      'GET /api/digest',
      'GET|POST /api/meetings · PATCH /api/meetings/{id}/status',
      'GET /api/meetings/{id}/transcript · GET|POST /api/meetings/{id}/summary'
    ],
    stories: ['US01 meetings', 'US02 group chat', 'US03 mentions', 'US04 notifications', 'US05 AI summary', 'US06 digest']
  },
  {
    id: 'S04',
    tone: 's04',
    name: 'Toolbox',
    container: 'Toolbox',
    components: [
      'WordDocumentBuilder / PPTPresentationBuilder / OfficeAPIAdapter (Office Tools)',
      'DevelopmentToolsController / VersionControlService / GitRepositoryAdapter (Development Tools)',
      'ContainerRuntimeAdapter (Development Tools)',
      'FileUploadHandler / StorageAdapter / ShareLinkService / FileManagementController (File Management)'
    ],
    entities: [
      'tb_doc_document',
      'tb_doc_edit',
      'tb_presentation',
      'tb_slide',
      'tb_git_repo',
      'tb_commit_record',
      'tb_dev_environment',
      'tb_toolbox_file',
      'tb_share_link'
    ],
    endpoints: [
      'GET|POST /api/toolbox/documents · POST /api/toolbox/documents/{id}/edits',
      'GET|POST /api/toolbox/presentations · POST /api/toolbox/presentations/{id}/slides',
      'GET|POST /api/toolbox/repos · GET|POST /api/toolbox/repos/{id}/commits',
      'GET|POST /api/toolbox/environments · PATCH /api/toolbox/environments/{id}/stop',
      'GET|POST /api/toolbox/files · POST /api/toolbox/files/{id}/links · DELETE /api/toolbox/links/{id}',
      'GET /api/toolbox/integrations · GET /api/toolbox/storage'
    ],
    stories: ['US01 documents', 'US02 presentations', 'US03 version control', 'US04 project files', 'US05 container runtime']
  }
]

/**
 * Architecture map of the running system: how the C4 design of the project maps onto the
 * deployed backend (packages, entities, REST endpoints) and onto the sequence diagrams that
 * were used to "execute" the architecture in the SA module.
 */
export function ArchitecturePage() {
  const { meta } = useAuth()
  const [health, setHealth] = useState<{ status: string; time: string } | null>(null)
  const [remoteMeta, setRemoteMeta] = useState<PlatformMeta | null>(null)
  const [audits, setAudits] = useState<Record<string, unknown>[]>([])

  useEffect(() => {
    let alive = true
    Promise.all([api.health(), api.meta(), api.audits({ limit: 12 })])
      .then(([healthValue, metaValue, auditList]) => {
        if (!alive) return
        setHealth(healthValue)
        setRemoteMeta(metaValue)
        setAudits(auditList)
      })
      .catch(() => undefined)
    return () => {
      alive = false
    }
  }, [])

  const platform = remoteMeta ?? meta

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-4">
        <Stat label="Sub-systems" value={MODULES.length} accent="s01" delta="S01–S04 of the case study" />
        <Stat
          label="Level-3 components"
          value={MODULES.reduce((sum, module) => sum + module.components.length, 0)}
          accent="s02"
          delta="Each one implemented as a service or adapter"
        />
        <Stat
          label="Persistent entities"
          value={MODULES.reduce((sum, module) => sum + module.entities.length, 0)}
          accent="s03"
          delta="JPA entities behind the H2 database"
        />
        <Stat
          label="Backend status"
          value={health?.status ?? '…'}
          accent="s04"
          delta={health ? `checked ${formatDateTime(health.time)}` : 'calling /api/health'}
        />
      </div>

      <Card
        title="C4 → code map"
        hint="Level 3 containers and components of the design, and where they live in the implementation"
      >
        <div className="grid grid-2">
          {MODULES.map((module) => (
            <div key={module.id} className="list-row" style={{ alignItems: 'flex-start' }}>
              <Badge tone={module.tone}>{module.id}</Badge>
              <div className="grow">
                <div className="title">{module.name}</div>
                <div className="sub">Container: {module.container}</div>

                <div className="nav-group-title" style={{ paddingLeft: 0 }}>
                  LEVEL 3 COMPONENTS
                </div>
                <div className="list">
                  {module.components.map((component) => (
                    <div className="sub" key={component}>
                      · {component}
                    </div>
                  ))}
                </div>

                <div className="nav-group-title" style={{ paddingLeft: 0 }}>
                  DATABASE TABLES
                </div>
                <div className="chip-row">
                  {module.entities.map((entity) => (
                    <span className="badge mono" key={entity}>
                      {entity}
                    </span>
                  ))}
                </div>

                <div className="nav-group-title" style={{ paddingLeft: 0 }}>
                  REST ENDPOINTS
                </div>
                <div className="list">
                  {module.endpoints.map((endpoint) => (
                    <div className="sub mono" key={endpoint}>
                      {endpoint}
                    </div>
                  ))}
                </div>

                <div className="chip-row" style={{ marginTop: 8 }}>
                  {module.stories.map((story) => (
                    <Badge key={story} tone={module.tone}>
                      {story}
                    </Badge>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <div className="grid grid-2">
        <Card
          title="Architecture execution"
          hint="Sequence diagrams used to test the architecture against the functional requirements"
        >
          <div className="list">
            {[
              ['US01', 'Update a Word document', 'UpdateDocument → GetQuota → RunOfficeOperation (alt: quota exhausted)'],
              ['US02', 'Update a presentation slide', 'UpdateSlide → GetQuota → RunOfficeOperation (alt: quota exhausted)'],
              ['US03', 'Commit and publish a change set', 'GetHistory → Pull (opt) → Commit → Push (opt: commit = 0)'],
              ['US04 · A', 'Upload and store a project file', 'UploadFile → WriteFile (alt: validation) → GetFileStatus (opt)'],
              ['US04 · B', 'Generate a file sharing link', 'GetFileStatus → CreateShareLink (opt: available) → GetFileStatus'],
              ['US05', 'Start a development environment', 'RouteRequest → StartEnvironment → GetServiceStatus → GetEnvironmentStatus']
            ].map(([story, title, flow]) => (
              <div className="list-row" key={story}>
                <Badge tone="s04">{story}</Badge>
                <div className="grow">
                  <div className="title">{title}</div>
                  <div className="sub mono">{flow}</div>
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card
          title="Platform facts"
          hint="Runtime configuration exposed by /api/meta and /api/health"
        >
          <div className="list">
            <div className="list-row">
              <Badge>Case study</Badge>
              <div className="grow">
                <div className="title">{platform?.caseStudy ?? '—'}</div>
                <div className="sub">Actors: {(platform?.actors ?? []).join(' · ')}</div>
              </div>
            </div>
            <div className="list-row">
              <Badge>Stack</Badge>
              <div className="grow">
                <div className="title">{(platform?.stack ?? []).join(' · ')}</div>
                <div className="sub">Single jar serves the REST API and this dashboard</div>
              </div>
            </div>
            <div className="list-row">
              <Badge>Database</Badge>
              <div className="grow">
                <div className="title">H2 file database (JPA / Hibernate)</div>
                <div className="sub">jdbc:h2:file:./data/sstc · console at /h2-console</div>
              </div>
            </div>
            <div className="list-row">
              <Badge>Docs</Badge>
              <div className="grow">
                <div className="title">OpenAPI UI</div>
                <div className="sub mono">/swagger-ui.html · /v3/api-docs</div>
              </div>
            </div>
          </div>

          <div className="nav-group-title" style={{ paddingLeft: 0 }}>
            LATEST AUDIT ENTRIES
          </div>
          {audits.length === 0 ? (
            <Empty>No audit entry yet.</Empty>
          ) : (
            <div className="timeline">
              {audits.slice(0, 6).map((entry, index) => (
                <div className="timeline-item" key={String(entry.id ?? index)}>
                  <div style={{ fontSize: 13, fontWeight: 600 }}>
                    <Badge tone={String(entry.module ?? '').toLowerCase() as 's01'}>
                      {String(entry.module ?? '')}
                    </Badge>{' '}
                    {String(entry.action ?? '')}
                  </div>
                  <div className="sub muted">{String(entry.detail ?? '')}</div>
                  <div className="soft" style={{ fontSize: 11 }}>
                    {String(entry.actorName ?? 'system')} · {formatDateTime(String(entry.createdAt ?? ''))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
