import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, type AccountEventView, type PolicyView, type RoleView, type SessionUser } from '../../api/client'
import { useAuth } from '../../app/AuthContext'
import { useToast } from '../../app/ToastContext'
import { Avatar, Badge, Button, Card, Empty, Field, Modal, Stat, formatDateTime, relativeTime } from '../../components/ui'

const ROLE_TONE: Record<string, 's01' | 's02' | 's03' | 's04'> = {
  OVERALL_LEAD: 's01',
  SENIOR_MANAGER: 's02',
  TEAM_MEMBER: 's03'
}

const STATUS_TONE: Record<string, 'ok' | 'warn' | 'danger'> = {
  ACTIVE: 'ok',
  SUSPENDED: 'warn',
  DEACTIVATED: 'danger'
}

/**
 * S01 · US01 - UserRoleManager.AccountController
 * Account administration: create, update, deactivate and view accounts.
 */
export function UsersPage() {
  const { can, user: currentUser, refreshUser } = useAuth()
  const { push } = useToast()
  const [users, setUsers] = useState<SessionUser[]>([])
  const [roles, setRoles] = useState<RoleView[]>([])
  const [events, setEvents] = useState<AccountEventView[]>([])
  const [policies, setPolicies] = useState<PolicyView[]>([])
  const [selected, setSelected] = useState<SessionUser | null>(null)
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [editing, setEditing] = useState<SessionUser | null>(null)
  const [form, setForm] = useState({
    username: '',
    displayName: '',
    email: '',
    password: 'Member@123',
    jobTitle: '',
    role: 'TEAM_MEMBER'
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [userList, roleList, eventList] = await Promise.all([
        api.users(),
        api.roles(),
        api.accountEvents(20)
      ])
      setUsers(userList)
      setRoles(roleList)
      setEvents(eventList)
      setSelected((previous) => {
        if (previous) {
          return userList.find((item) => item.id === previous.id) ?? userList[0] ?? null
        }
        return userList[0] ?? null
      })
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to load accounts', 'error')
    } finally {
      setLoading(false)
    }
  }, [push])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!selected) {
      setPolicies([])
      return
    }
    api
      .policies(selected.id)
      .then(setPolicies)
      .catch(() => setPolicies([]))
  }, [selected])

  const counts = useMemo(() => {
    const active = users.filter((item) => item.status === 'ACTIVE').length
    return {
      total: users.length,
      active,
      leads: users.filter((item) => item.role === 'OVERALL_LEAD').length,
      managers: users.filter((item) => item.role === 'SENIOR_MANAGER').length
    }
  }, [users])

  const createAccount = async () => {
    try {
      await api.createUser(form)
      push(`Account ${form.username} created`, 'ok')
      setShowCreate(false)
      setForm({
        username: '',
        displayName: '',
        email: '',
        password: 'Member@123',
        jobTitle: '',
        role: 'TEAM_MEMBER'
      })
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to create the account', 'error')
    }
  }

  const saveAccount = async () => {
    if (!editing) return
    try {
      await api.updateUser(editing.id, {
        displayName: editing.displayName,
        email: editing.email,
        jobTitle: editing.jobTitle,
        role: editing.role,
        status: editing.status
      })
      push(`Account ${editing.username} updated`, 'ok')
      setEditing(null)
      await load()
      if (currentUser && editing.id === currentUser.id) {
        await refreshUser()
      }
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to update the account', 'error')
    }
  }

  const deactivate = async (account: SessionUser) => {
    try {
      await api.deactivateUser(account.id)
      push(`${account.displayName} deactivated, sessions revoked`, 'ok')
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to deactivate the account', 'error')
    }
  }

  const assignRole = async (account: SessionUser, roleCode: string) => {
    try {
      await api.assignRole(account.id, { roleCode, adminPrivilege: roleCode === 'ROLE_OVERALL_LEAD' })
      push(`Role ${roleCode} assigned to ${account.displayName}`, 'ok')
      await load()
      if (currentUser && account.id === currentUser.id) {
        await refreshUser()
      }
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to assign the role', 'error')
    }
  }

  const revokeRole = async (account: SessionUser) => {
    try {
      await api.revokeRole(account.id)
      push(`Role assignments of ${account.displayName} removed`, 'ok')
      await load()
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to revoke the role', 'error')
    }
  }

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-4">
        <Stat label="Accounts" value={counts.total} accent="s01" delta="S01 · Account Service" />
        <Stat label="Active" value={counts.active} accent="s01" delta={`${counts.total - counts.active} suspended or deactivated`} />
        <Stat label="Overall leads" value={counts.leads} accent="s01" delta="Epic A of S01" />
        <Stat label="Senior managers" value={counts.managers} accent="s01" delta="Task review and OKR" />
      </div>

      <Card
        title="Accounts"
        hint="US01 · UserRoleManager.AccountController - create, update, deactivate and view accounts"
        actions={
          can('S01.ACCOUNT.MANAGE') ? (
            <Button variant="primary" size="sm" onClick={() => setShowCreate(true)}>
              New account
            </Button>
          ) : (
            <Badge tone="warn">read-only role</Badge>
          )
        }
        padded={false}
      >
        {loading ? (
          <div className="card-body">
            <div className="skeleton" style={{ height: 180 }} />
          </div>
        ) : users.length === 0 ? (
          <Empty>No accounts yet.</Empty>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Account</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Permissions</th>
                  <th>Last sign-in</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {users.map((account) => (
                  <tr key={account.id} onClick={() => setSelected(account)} style={{ cursor: 'pointer' }}>
                    <td>
                      <div className="person">
                        <Avatar name={account.displayName} color={account.avatarColor} />
                        <div>
                          <div style={{ fontWeight: 600 }}>{account.displayName}</div>
                          <div className="soft mono">{account.username} · {account.email}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <Badge tone={ROLE_TONE[account.role] ?? 'default'}>
                        {account.roleLabel ?? account.role}
                      </Badge>
                    </td>
                    <td>
                      <Badge tone={STATUS_TONE[account.status] ?? 'default'}>{account.status}</Badge>
                    </td>
                    <td className="soft">{account.permissions?.length ?? 0}</td>
                    <td className="soft">{relativeTime(account.lastLoginAt)}</td>
                    <td onClick={(event) => event.stopPropagation()}>
                      <div className="chip-row">
                        <Button size="sm" variant="ghost" onClick={() => setEditing(account)}>
                          Edit
                        </Button>
                        {can('S01.ROLE.MANAGE') && (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() =>
                              assignRole(
                                account,
                                account.role === 'TEAM_MEMBER' ? 'ROLE_SENIOR_MANAGER' : 'ROLE_TEAM_MEMBER'
                              )
                            }
                          >
                            Toggle role
                          </Button>
                        )}
                        {can('S01.ACCOUNT.MANAGE') && account.status === 'ACTIVE' && (
                          <Button size="sm" variant="danger" onClick={() => deactivate(account)}>
                            Deactivate
                          </Button>
                        )}
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
          title={selected ? `Effective access · ${selected.displayName}` : 'Effective access'}
          hint="US02/US03 · RoleAssignmentController + PermissionPolicyController"
        >
          {!selected ? (
            <Empty>Select an account to inspect its access.</Empty>
          ) : (
            <>
              <div className="chip-row" style={{ marginBottom: 12 }}>
                <Badge tone={ROLE_TONE[selected.role] ?? 'default'}>
                  {selected.roleLabel ?? selected.role}
                </Badge>
                <Badge tone="info">{selected.permissions?.length ?? 0} permissions</Badge>
                {policies.length > 0 && <Badge tone="warn">{policies.length} policy overrides</Badge>}
              </div>
              <div className="chip-row">
                {(selected.permissions ?? []).map((permission) => (
                  <span className="badge" key={permission}>
                    {permission}
                  </span>
                ))}
              </div>

              {policies.length > 0 && (
                <>
                  <div className="nav-group-title" style={{ paddingLeft: 0 }}>
                    POLICY OVERRIDES
                  </div>
                  <div className="list">
                    {policies.map((policy) => (
                      <div className="list-row" key={policy.id}>
                        <Badge tone={policy.effect === 'GRANT' ? 'ok' : 'danger'}>{policy.effect}</Badge>
                        <div className="grow">
                          <div className="title mono">{policy.permissionCode}</div>
                          <div className="sub">{policy.reason ?? 'no reason recorded'}</div>
                        </div>
                        {can('S01.POLICY.MANAGE') && (
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={async () => {
                              try {
                                await api.revokePolicy(policy.id)
                                push('Policy removed', 'ok')
                                setPolicies(await api.policies(selected.id))
                                await load()
                              } catch (error) {
                                push(error instanceof Error ? error.message : 'Unable to remove the policy', 'error')
                              }
                            }}
                          >
                            Remove
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>
                </>
              )}

              {can('S01.ROLE.MANAGE') && (
                <div style={{ marginTop: 14 }}>
                  <Button size="sm" variant="ghost" onClick={() => revokeRole(selected)}>
                    Revoke all role assignments
                  </Button>
                </div>
              )}
            </>
          )}
        </Card>

        <Card title="Account events" hint="US01 · lifecycle events published by the Account Service">
          {events.length === 0 ? (
            <Empty>No account events yet.</Empty>
          ) : (
            <div className="timeline">
              {events.slice(0, 12).map((event) => (
                <div className="timeline-item" key={event.id}>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{event.eventType}</div>
                  <div className="sub muted">{event.detail}</div>
                  <div className="soft" style={{ fontSize: 11 }}>
                    {formatDateTime(event.createdAt)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {showCreate && (
        <Modal
          title="Create account"
          onClose={() => setShowCreate(false)}
          footer={
            <>
              <Button onClick={() => setShowCreate(false)}>Cancel</Button>
              <Button variant="primary" onClick={createAccount}>
                Create account
              </Button>
            </>
          }
        >
          <Field label="Username">
            <input
              className="input"
              value={form.username}
              onChange={(event) => setForm({ ...form, username: event.target.value })}
            />
          </Field>
          <Field label="Display name">
            <input
              className="input"
              value={form.displayName}
              onChange={(event) => setForm({ ...form, displayName: event.target.value })}
            />
          </Field>
          <Field label="Email">
            <input
              className="input"
              value={form.email}
              onChange={(event) => setForm({ ...form, email: event.target.value })}
            />
          </Field>
          <Field label="Initial password">
            <input
              className="input"
              value={form.password}
              onChange={(event) => setForm({ ...form, password: event.target.value })}
            />
          </Field>
          <Field label="Job title">
            <input
              className="input"
              value={form.jobTitle}
              onChange={(event) => setForm({ ...form, jobTitle: event.target.value })}
            />
          </Field>
          <Field label="Role">
            <select
              className="select"
              value={form.role}
              onChange={(event) => setForm({ ...form, role: event.target.value })}
            >
              <option value="TEAM_MEMBER">Team Member</option>
              <option value="SENIOR_MANAGER">Senior Manager</option>
              <option value="OVERALL_LEAD">Overall Lead</option>
            </select>
          </Field>
        </Modal>
      )}

      {editing && (
        <Modal
          title={`Edit ${editing.displayName}`}
          onClose={() => setEditing(null)}
          footer={
            <>
              <Button onClick={() => setEditing(null)}>Cancel</Button>
              <Button variant="primary" onClick={saveAccount}>
                Save changes
              </Button>
            </>
          }
        >
          <Field label="Display name">
            <input
              className="input"
              value={editing.displayName}
              onChange={(event) => setEditing({ ...editing, displayName: event.target.value })}
            />
          </Field>
          <Field label="Email">
            <input
              className="input"
              value={editing.email}
              onChange={(event) => setEditing({ ...editing, email: event.target.value })}
            />
          </Field>
          <Field label="Job title">
            <input
              className="input"
              value={editing.jobTitle ?? ''}
              onChange={(event) => setEditing({ ...editing, jobTitle: event.target.value })}
            />
          </Field>
          <Field label="Role">
            <select
              className="select"
              value={editing.role}
              onChange={(event) =>
                setEditing({ ...editing, role: event.target.value as SessionUser['role'] })
              }
            >
              <option value="TEAM_MEMBER">Team Member</option>
              <option value="SENIOR_MANAGER">Senior Manager</option>
              <option value="OVERALL_LEAD">Overall Lead</option>
            </select>
          </Field>
          <Field label="Status">
            <select
              className="select"
              value={editing.status}
              onChange={(event) => setEditing({ ...editing, status: event.target.value })}
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="SUSPENDED">SUSPENDED</option>
              <option value="DEACTIVATED">DEACTIVATED</option>
            </select>
          </Field>
        </Modal>
      )}
    </div>
  )
}
