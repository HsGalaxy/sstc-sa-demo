/**
 * Typed client of the STC Platform REST API.
 *
 * The dashboard talks to the Spring Boot backend either through the Vite dev proxy
 * (development) or from the same origin (production build served by the jar).
 */

const TOKEN_KEY = 'stc.token'
const USER_KEY = 'stc.user'

export interface SessionUser {
  id: number
  username: string
  displayName: string
  email: string
  jobTitle?: string
  avatarColor?: string
  role: 'TEAM_MEMBER' | 'SENIOR_MANAGER' | 'OVERALL_LEAD'
  roleLabel?: string
  status: string
  initials?: string
  permissions: string[]
  lastLoginAt?: string
}

export interface TaskView {
  id: number
  code: string
  title: string
  description?: string
  status: 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE'
  statusLabel: string
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  assigneeId?: number
  assigneeName?: string
  assigneeInitials?: string
  reporterName?: string
  cycleId?: number
  cycleName?: string
  okrId?: number
  okrObjective?: string
  dueDate?: string
  overdue: boolean
  loggedHours: number
  estimateHours: number
  blocked: boolean
  tags: string[]
  attachmentCount: number
  updatedAt?: string
}

export interface BoardColumn {
  status: TaskView['status']
  label: string
  tasks: TaskView[]
  count: number
}

export interface CycleView {
  id: number
  name: string
  goal?: string
  startDate: string
  endDate: string
  active: boolean
  totalTasks: number
  doneTasks: number
  progress: number
}

export interface KeyResultView {
  id: number
  okrId: number
  title: string
  targetValue: number
  currentValue: number
  unit: string
  ownerId?: number
  ratio: number
}

export interface OkrView {
  id: number
  objective: string
  cycleId?: number
  ownerId?: number
  ownerName?: string
  progress: number
  status: string
  weight: number
  keyResults: KeyResultView[]
}

export interface FileRecordView {
  id: number
  name: string
  bucket: 'DESIGN' | 'DEVELOPMENT' | 'DOCUMENT' | 'REPORT' | 'OTHER'
  taskId?: number
  taskTitle?: string
  ownerId?: number
  ownerName?: string
  contentType?: string
  sizeBytes: number
  checksum?: string
  currentVersion: number
  archived: boolean
  uploadedAt: string
  versionCount: number
}

export interface FileVersionView {
  id: number
  fileId: number
  version: number
  sizeBytes: number
  checksum?: string
  changeNote?: string
  authorId?: number
  authorName?: string
  createdOn: string
  current: boolean
}

export interface OperationLogView {
  id: number
  actorId?: number
  actorName?: string
  action: string
  targetType: string
  targetId?: string
  detail?: string
  outcome: string
  source?: string
  createdAt: string
}

export interface SnapshotView {
  id: number
  snapshotDate: string
  tasksCompleted: number
  tasksOpen: number
  overdueTasks: number
  onTimeRate: number
  qualityScore: number
  okrScore: number
  throughput: number
}

export interface MemberScore {
  userId: number
  name: string
  initials: string
  roleLabel?: string
  completed: number
  open: number
  overdue: number
  onTimeRate: number
  qualityScore: number
  okrScore: number
  totalScore: number
  grade: string
}

export interface Bottleneck {
  type: string
  label: string
  detail: string
  severity: string
  taskId?: number
}

export interface PerformanceView {
  trend: SnapshotView[]
  members: MemberScore[]
  teamOkrScore: number
  teamOnTimeRate: number
  openTasks: number
  overdueTasks: number
  completedThisCycle: number
  averageThroughput: number
  bottlenecks: Bottleneck[]
}

export interface ChannelView {
  id: number
  name: string
  topic?: string
  purpose?: string
  privateChannel: boolean
  memberCount: number
  messageCount: number
  lastActivity?: string
}

export interface MentionView {
  id: number
  messageId: number
  channelId: number
  targetUserId: number
  targetName?: string
  token: string
  alerted: boolean
  createdAt: string
}

export interface MessageView {
  id: number
  channelId: number
  channelName?: string
  authorId: number
  authorName?: string
  authorInitials?: string
  body: string
  kind: string
  postedAt: string
  edited: boolean
  attachmentName?: string
  replyToId?: number
  mentions: MentionView[]
}

export interface NotificationView {
  id: number
  recipientId: number
  kind: string
  priority: string
  title: string
  body?: string
  link?: string
  read: boolean
  deliveredAt: string
  readAt?: string
}

export interface DigestItemView {
  id: number
  userId: number
  notificationId: number
  rankScore: number
  reason?: string
  bucket: string
  dismissed: boolean
  notification?: NotificationView
}

export interface MeetingView {
  id: number
  title: string
  agenda?: string
  channelId?: number
  hostId: number
  hostName?: string
  startAt: string
  endAt?: string
  status: string
  joinCode: string
  participantIds: number[]
  hasSummary: boolean
  transcriptSegments: number
}

export interface TranscriptView {
  id: number
  meetingId: number
  speakerId?: number
  speakerName?: string
  offsetSeconds: number
  text: string
}

export interface SummaryView {
  id: number
  meetingId: number
  overview: string
  decisions: string[]
  actionItems: string[]
  model: string
  confidence: number
  generatedAt: string
}

export interface DocumentView {
  id: number
  title: string
  content?: string
  ownerId: number
  ownerName?: string
  template?: string
  revision: number
  wordCount: number
  lastEditedAt: string
  officeApi?: string
}

export interface DocEditView {
  id: number
  documentId: number
  editorId: number
  editorName?: string
  operation: string
  payload?: string
  resultCode: number
  quotaRemaining: number
  appliedAt: string
}

export interface SlideView {
  id: number
  presentationId: number
  slideNo: number
  title?: string
  bullets: string[]
  notes?: string
  layout?: string
}

export interface PresentationView {
  id: number
  title: string
  theme?: string
  ownerId: number
  ownerName?: string
  slideCount: number
  revision: number
  lastEditedAt: string
  officeApi?: string
  slides: SlideView[]
}

export interface RepoView {
  id: number
  name: string
  slug: string
  provider: string
  remoteUrl?: string
  defaultBranch: string
  language?: string
  visibility: string
  description?: string
  stars: number
  openPullRequests: number
  lastSyncAt?: string
  commitCount: number
  lastCommitMessage?: string
  lastCommitAt?: string
}

export interface CommitView {
  id: number
  repoId: number
  sha: string
  shortSha: string
  branch: string
  message: string
  authorId?: number
  authorName?: string
  committedAt: string
  additions: number
  deletions: number
  changedFiles: number
}

export interface EnvironmentView {
  id: number
  serviceId: string
  containerId: string
  image: string
  state: string
  cpuLimit: number
  memoryMb: number
  ports?: string
  ownerId?: number
  ownerName?: string
  startedAt?: string
  lastHeartbeat?: string
  uptimeMinutes: number
}

export interface ToolboxFileView {
  id: number
  name: string
  folder: string
  contentType?: string
  sizeBytes: number
  checksum?: string
  ownerId?: number
  ownerName?: string
  status: string
  storageBackend?: string
  validationError?: string
  uploadedAt: string
  shared: boolean
  shareLinkCount: number
}

export interface ShareLinkView {
  id: number
  fileId: number
  fileName?: string
  token: string
  url: string
  expiresAt: string
  createdBy?: number
  createdByName?: string
  revoked: boolean
  accessCount: number
  expired: boolean
}

export interface IntegrationStatus {
  officeProvider: string
  officeQuotaRemaining: number
  officeQuotaTotal: number
  gitProvider: string
  repositories: number
  containerRuntime: string
  environmentsRunning: number
  environmentsTotal: number
  storageProvider: string
  storageUsedBytes: number
  maxUploadMb: number
}

export interface RoleView {
  id: number
  code: string
  name: string
  description?: string
  builtIn: boolean
  roleType: string
  permissionCodes: string[]
  memberCount: number
}

export interface PermissionView {
  id: number
  code: string
  name: string
  module: string
  description?: string
}

export interface PolicyView {
  id: number
  userId: number
  permissionCode: string
  effect: 'GRANT' | 'REVOKE'
  reason?: string
  updatedAt: string
}

export interface AccountEventView {
  id: number
  userId: number
  eventType: string
  detail?: string
  actorId?: number
  createdAt: string
}

export interface AccessAuditView {
  id: number
  userId?: number
  username?: string
  permissionCode: string
  resource?: string
  method?: string
  decision: string
  reason?: string
  durationMs?: number
  createdAt: string
}

export interface PlatformMeta {
  name: string
  subtitle: string
  version: string
  caseStudy: string
  actors: string[]
  modules: { id: string; name: string; description: string; userStories: string[] }[]
  stack: string[]
}

export class ApiError extends Error {
  status: number
  code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}

export function getCachedUser(): SessionUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SessionUser
  } catch {
    return null
  }
}

export function setCachedUser(user: SessionUser | null) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })

  const text = await response.text()
  const payload = text ? safeParse(text) : null

  if (!response.ok) {
    const message = (payload && (payload.message || payload.error)) || response.statusText
    if (response.status === 401) {
      setToken(null)
    }
    throw new ApiError(response.status, String(message), payload?.code)
  }
  return payload as T
}

function safeParse(text: string): any {
  try {
    return JSON.parse(text)
  } catch {
    return { message: text }
  }
}

const qs = (params: Record<string, unknown>) => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    search.set(key, String(value))
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export const api = {
  // ---------------------------------------------------------------- meta
  health: () => request<{ status: string; time: string }>('GET', '/api/health'),
  meta: () => request<PlatformMeta>('GET', '/api/meta'),

  // ---------------------------------------------------------------- S01
  login: (username: string, password: string) =>
    request<{ token: string; expiresAt: string; user: SessionUser }>('POST', '/api/auth/login', {
      username,
      password
    }),
  logout: () => request<{ status: string }>('POST', '/api/auth/logout'),
  me: () => request<SessionUser>('GET', '/api/identity/me'),
  myPermissions: () =>
    request<{ username: string; role: string; permissions: string[] }>('GET', '/api/identity/permissions'),
  users: () => request<SessionUser[]>('GET', '/api/users'),
  createUser: (payload: Record<string, unknown>) => request<SessionUser>('POST', '/api/users', payload),
  updateUser: (id: number, payload: Record<string, unknown>) =>
    request<SessionUser>('PUT', `/api/users/${id}`, payload),
  deactivateUser: (id: number) => request<SessionUser>('DELETE', `/api/users/${id}`),
  assignRole: (id: number, payload: { roleCode: string; adminPrivilege?: boolean }) =>
    request<SessionUser>('POST', `/api/users/${id}/role`, payload),
  revokeRole: (id: number) => request<SessionUser>('DELETE', `/api/users/${id}/role`),
  roles: () => request<RoleView[]>('GET', '/api/roles'),
  permissions: () => request<PermissionView[]>('GET', '/api/permissions'),
  policies: (userId: number) => request<PolicyView[]>('GET', `/api/users/${userId}/policies`),
  setPolicy: (userId: number, payload: { permissionCode: string; effect: string; reason?: string }) =>
    request<PolicyView>('POST', `/api/users/${userId}/policies`, payload),
  revokePolicy: (policyId: number) => request<{ status: string }>('DELETE', `/api/policies/${policyId}`),
  accountEvents: (limit = 30) => request<AccountEventView[]>('GET', `/api/account-events${qs({ limit })}`),
  accessAudits: (limit = 60) => request<AccessAuditView[]>('GET', `/api/access-audits${qs({ limit })}`),

  // ---------------------------------------------------------------- S02
  board: () => request<BoardColumn[]>('GET', '/api/tasks/board'),
  taskSummary: () => request<Record<string, number | string>>('GET', '/api/tasks/summary'),
  tasks: (params: { status?: string; assigneeId?: number; cycleId?: number; keyword?: string } = {}) =>
    request<TaskView[]>('GET', `/api/tasks${qs(params)}`),
  task: (id: number) => request<TaskView>('GET', `/api/tasks/${id}`),
  createTask: (payload: Record<string, unknown>) => request<TaskView>('POST', '/api/tasks', payload),
  updateTask: (id: number, payload: Record<string, unknown>) =>
    request<TaskView>('PUT', `/api/tasks/${id}`, payload),
  moveTask: (id: number, status: string, position?: number) =>
    request<TaskView>('PATCH', `/api/tasks/${id}/move${qs({ status, position })}`),
  deleteTask: (id: number) => request<{ status: string }>('DELETE', `/api/tasks/${id}`),
  cycles: () => request<CycleView[]>('GET', '/api/cycles'),
  createCycle: (payload: Record<string, unknown>) => request<CycleView>('POST', '/api/cycles', payload),
  okrs: () => request<OkrView[]>('GET', '/api/okrs'),
  createOkr: (payload: Record<string, unknown>) => request<OkrView>('POST', '/api/okrs', payload),
  addKeyResult: (okrId: number, payload: Record<string, unknown>) =>
    request<KeyResultView>('POST', `/api/okrs/${okrId}/key-results`, payload),
  updateKeyResult: (id: number, currentValue: number) =>
    request<KeyResultView>('PATCH', `/api/key-results/${id}${qs({ currentValue })}`),
  performance: () => request<PerformanceView>('GET', '/api/performance'),
  captureSnapshot: () => request<{ status: string }>('POST', '/api/performance/snapshot'),
  operations: (limit = 60, action?: string) =>
    request<OperationLogView[]>('GET', `/api/operations${qs({ limit, action })}`),
  taskHistory: (id: number) => request<OperationLogView[]>('GET', `/api/tasks/${id}/history`),

  // task files (File Tracker)
  files: (params: { bucket?: string; taskId?: number; ownerId?: number; keyword?: string; archived?: boolean } = {}) =>
    request<FileRecordView[]>('GET', `/api/files${qs(params)}`),
  file: (id: number) => request<FileRecordView>('GET', `/api/files/${id}`),
  fileVersions: (id: number) => request<FileVersionView[]>('GET', `/api/files/${id}/versions`),
  uploadTaskFile: (payload: Record<string, unknown>) => request<FileRecordView>('POST', '/api/files', payload),
  addFileVersion: (id: number, payload: { changeNote: string; sizeBytes?: number }) =>
    request<FileVersionView>('POST', `/api/files/${id}/versions`, payload),
  archiveFile: (id: number, archived: boolean) =>
    request<FileRecordView>('PATCH', `/api/files/${id}/archive${qs({ archived })}`),
  rollbackFile: (id: number, version: number) =>
    request<FileRecordView>('POST', `/api/files/${id}/rollback${qs({ version })}`),
  fileStatistics: () => request<Record<string, { count: number; bytes: number } | number>>(
    'GET',
    '/api/files/statistics'
  ),

  // ---------------------------------------------------------------- S03
  channels: () => request<ChannelView[]>('GET', '/api/channels'),
  createChannel: (payload: Record<string, unknown>) => request<ChannelView>('POST', '/api/channels', payload),
  channelMessages: (id: number, limit = 60) =>
    request<MessageView[]>('GET', `/api/channels/${id}/messages${qs({ limit })}`),
  publishMessage: (id: number, payload: { body: string; kind?: string; attachmentName?: string }) =>
    request<MessageView>('POST', `/api/channels/${id}/messages`, payload),
  recentMessages: () => request<MessageView[]>('GET', '/api/messages/recent'),
  mentions: () => request<MentionView[]>('GET', '/api/mentions'),
  notifications: () => request<NotificationView[]>('GET', '/api/notifications'),
  unreadCount: () => request<{ unread: number }>('GET', '/api/notifications/unread-count'),
  markNotificationRead: (id: number) => request<NotificationView>('PATCH', `/api/notifications/${id}/read`),
  markAllRead: () => request<{ updated: number }>('POST', '/api/notifications/read-all'),
  digest: () => request<DigestItemView[]>('GET', '/api/digest'),
  meetings: () => request<MeetingView[]>('GET', '/api/meetings'),
  meeting: (id: number) => request<MeetingView>('GET', `/api/meetings/${id}`),
  createMeeting: (payload: Record<string, unknown>) => request<MeetingView>('POST', '/api/meetings', payload),
  changeMeetingStatus: (id: number, status: string) =>
    request<MeetingView>('PATCH', `/api/meetings/${id}/status${qs({ status })}`),
  transcript: (id: number) => request<TranscriptView[]>('GET', `/api/meetings/${id}/transcript`),
  summarizeMeeting: (id: number) => request<SummaryView>('POST', `/api/meetings/${id}/summary`),
  meetingSummary: (id: number) => request<SummaryView | null>('GET', `/api/meetings/${id}/summary`),

  // ---------------------------------------------------------------- S04
  documents: () => request<DocumentView[]>('GET', '/api/toolbox/documents'),
  document: (id: number) => request<DocumentView>('GET', `/api/toolbox/documents/${id}`),
  createDocument: (payload: Record<string, unknown>) =>
    request<DocumentView>('POST', '/api/toolbox/documents', payload),
  editDocument: (id: number, payload: { operation: string; payload?: string }) =>
    request<DocumentView>('POST', `/api/toolbox/documents/${id}/edits`, payload),
  documentEdits: (id: number) => request<DocEditView[]>('GET', `/api/toolbox/documents/${id}/edits`),
  presentations: () => request<PresentationView[]>('GET', '/api/toolbox/presentations'),
  presentation: (id: number) => request<PresentationView>('GET', `/api/toolbox/presentations/${id}`),
  createPresentation: (payload: Record<string, unknown>) =>
    request<PresentationView>('POST', '/api/toolbox/presentations', payload),
  updateSlide: (id: number, payload: Record<string, unknown>, slideId?: number) =>
    request<PresentationView>('POST', `/api/toolbox/presentations/${id}/slides${qs({ slideId })}`, payload),
  repos: () => request<RepoView[]>('GET', '/api/toolbox/repos'),
  repo: (id: number) => request<RepoView>('GET', `/api/toolbox/repos/${id}`),
  createRepo: (payload: Record<string, unknown>) => request<RepoView>('POST', '/api/toolbox/repos', payload),
  commits: (repoId: number) => request<CommitView[]>('GET', `/api/toolbox/repos/${repoId}/commits`),
  pushCommit: (repoId: number, payload: { message: string; branch?: string }) =>
    request<CommitView>('POST', `/api/toolbox/repos/${repoId}/commits`, payload),
  recentCommits: () => request<CommitView[]>('GET', '/api/toolbox/commits/recent'),
  environments: () => request<EnvironmentView[]>('GET', '/api/toolbox/environments'),
  startEnvironment: (payload: Record<string, unknown>) =>
    request<EnvironmentView>('POST', '/api/toolbox/environments', payload),
  stopEnvironment: (containerId: string) =>
    request<EnvironmentView>('PATCH', `/api/toolbox/environments/${containerId}/stop`),
  environmentStatus: (params: { serviceId?: string; containerId?: string }) =>
    request<Record<string, string>>('GET', `/api/toolbox/environments/status${qs(params)}`),
  heartbeat: () => request<{ running: number }>('POST', '/api/toolbox/environments/heartbeat'),
  toolboxFiles: () => request<ToolboxFileView[]>('GET', '/api/toolbox/files'),
  uploadToolboxFile: (payload: Record<string, unknown>) =>
    request<ToolboxFileView>('POST', '/api/toolbox/files', payload),
  fileStatus: (id: number) => request<{ fileId: number; status: string }>(
    'GET',
    `/api/toolbox/files/${id}/status`
  ),
  createShareLink: (id: number, payload: { expiresInDays?: number } = {}) =>
    request<ShareLinkView>('POST', `/api/toolbox/files/${id}/links`, payload),
  shareLinks: (id: number) => request<ShareLinkView[]>('GET', `/api/toolbox/files/${id}/links`),
  allShareLinks: () => request<ShareLinkView[]>('GET', '/api/toolbox/links'),
  revokeShareLink: (id: number) => request<ShareLinkView>('DELETE', `/api/toolbox/links/${id}`),
  storage: () => request<Record<string, unknown>>('GET', '/api/toolbox/storage'),
  integrations: () => request<IntegrationStatus>('GET', '/api/toolbox/integrations'),

  // ---------------------------------------------------------------- audit
  audits: (params: { limit?: number; module?: string; action?: string } = {}) =>
    request<Record<string, unknown>[]>('GET', `/api/audits${qs(params)}`)
}
