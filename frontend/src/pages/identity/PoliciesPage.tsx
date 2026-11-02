import { useCallback, useEffect, useState } from 'react'
import { api, type PermissionView, type PolicyView, type RoleView, type SessionUser } from '../../api/client'
import { useAuth } from '../../app/AuthContext'
import { useToast } from '../../app/ToastContext'
import { Badge, Button, Card, Empty, Field, Stat } from '../../components/ui'

const MODULE_TONE: Record<string, 's01' | 's02' | 's03' | 's04'> = {
  S01: 's01',
  S02: 's02',
  S03: 's03',
  S04: 's04'
}

/**
 * S01 · US02 / US03 - RoleAssignmentController + PermissionPolicyController
 * Role matrix and per-account permission overrides.
 */
export function PoliciesPage() {
  const { can } = useAuth()
  const { push } = useToast()
  const [roles, setRoles] = useState<RoleView[]>([])
  const [permissions, setPermissions] = useState<PermissionView[]>([])
  const [users, setUsers] = useState<SessionUser[]>([])
  const [policies, setPolicies] = useState<PolicyView[]>([])
  const [selectedUser, setSelectedUser] = useState<number | null>(null)
  const [draft, setDraft] = useState({ permissionCode: '', effect: 'GRANT', reason: '' })
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [roleList, permissionList, userList] = await Promise.all([
        api.roles(),
        api.permissions(),
        api.users()
      ])
      setRoles(roleList)
      setPermissions(permissionList)
      setUsers(userList)
      setSelectedUser((previous) => previous ?? userList[0]?.id ?? null)
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to load the policy data', 'error')
    } finally {
      setLoading(false)
    }
  }, [push])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (selectedUser == null) {
      setPolicies([])
      return
    }
    api
      .policies(selectedUser)
      .then(setPolicies)
      .catch(() => setPolicies([]))
  }, [selectedUser])

  const grouped = permissions.reduce<Record<string, PermissionView[]>>((accumulator, permission) => {
    const key = permission.module ?? 'OTHER'
    accumulator[key] = accumulator[key] ?? []
    accumulator[key].push(permission)
    return accumulator
  }, {})

  const applyPolicy = async () => {
    if (selectedUser == null || !draft.permissionCode) {
      push('Select an account and a permission first', 'error')
      return
    }
    try {
      await api.setPolicy(selectedUser, draft)
      push(`${draft.effect} ${draft.permissionCode} applied`, 'ok')
      setPolicies(await api.policies(selectedUser))
      setDraft({ permissionCode: '', effect: 'GRANT', reason: '' })
    } catch (error) {
      push(error instanceof Error ? error.message : 'Unable to apply the policy', 'error')
    }
  }

  const selected = users.find((user) => user.id === selectedUser)

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="grid grid-3">
        <Stat label="Roles" value={roles.length} accent="s01" delta="Built-in role matrix" />
        <Stat label="Permissions" value={permissions.length} accent="s01" delta="Across S01–S04" />
        <Stat label="Policy overrides" value={policies.length} accent="s01" delta="Per-account GRANT / REVOKE" />
      </div>

      <Card
        title="Role matrix"
        hint="US02 · RoleAssignmentController - every role with the permissions bound to it"
        padded={false}
      >
        {loading ? (
          <div className="card-body">
            <div className="skeleton" style={{ height: 160 }} />
          </div>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Type</th>
                  <th>Members</th>
                  <th>Permissions</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {roles.map((role) => (
                  <tr key={role.id}>
                    <td>
                      <div style={{ fontWeight: 600 }}>{role.name}</div>
                      <div className="soft mono">{role.code}</div>
                    </td>
                    <td>
                      <Badge tone={MODULE_TONE[role.roleType === 'OVERALL_LEAD' ? 'S01' : 'S02'] ?? 'default'}>
                        {role.roleType}
                      </Badge>
                    </td>
                    <td>{role.memberCount}</td>
                    <td>
                      <div className="chip-row">
                        {role.permissionCodes.slice(0, 6).map((code) => (
                          <span className="badge" key={code}>
                            {code}
                          </span>
                        ))}
                        {role.permissionCodes.length > 6 && (
                          <Badge tone="info">+{role.permissionCodes.length - 6}</Badge>
                        )}
                      </div>
                    </td>
                    <td className="soft">{role.description}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <div className="grid grid-2-1">
        <Card
          title="Permission catalogue"
          hint="US03 · the fine grained permissions a policy can grant or revoke"
        >
          {Object.entries(grouped).map(([module, items]) => (
            <div key={module} style={{ marginBottom: 14 }}>
              <div className="nav-group-title" style={{ paddingLeft: 0 }}>
                {module}
              </div>
              <div className="list">
                {items.map((permission) => (
                  <div className="list-row" key={permission.id}>
                    <Badge tone={MODULE_TONE[module] ?? 'default'}>{permission.module}</Badge>
                    <div className="grow">
                      <div className="title mono">{permission.code}</div>
                      <div className="sub">{permission.name}</div>
                    </div>
                    <Button
                      size="sm"
                      variant="ghost"
                      disabled={!can('S01.POLICY.MANAGE')}
                      onClick={() => setDraft({ ...draft, permissionCode: permission.code })}
                    >
                      Use in policy
                    </Button>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </Card>

        <Card
          title="Policy overrides"
          hint="US03 · PermissionPolicyController - GRANT and REVOKE per account"
        >
          <Field label="Account">
            <select
              className="select"
              value={selectedUser ?? ''}
              onChange={(event) => setSelectedUser(Number(event.target.value))}
            >
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.displayName} · {user.roleLabel ?? user.role}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Permission code">
            <input
              className="input mono"
              value={draft.permissionCode}
              placeholder="S02.PERFORMANCE.VIEW"
              onChange={(event) => setDraft({ ...draft, permissionCode: event.target.value })}
            />
          </Field>
          <Field label="Effect">
            <select
              className="select"
              value={draft.effect}
              onChange={(event) => setDraft({ ...draft, effect: event.target.value })}
            >
              <option value="GRANT">GRANT</option>
              <option value="REVOKE">REVOKE</option>
            </select>
          </Field>
          <Field label="Reason">
            <input
              className="input"
              value={draft.reason}
              placeholder="Why this override exists"
              onChange={(event) => setDraft({ ...draft, reason: event.target.value })}
            />
          </Field>
          <Button variant="primary" disabled={!can('S01.POLICY.MANAGE')} onClick={applyPolicy}>
            Apply policy
          </Button>
          {!can('S01.POLICY.MANAGE') && (
            <div className="soft" style={{ marginTop: 8, fontSize: 12 }}>
              Your role cannot edit policies (S01.POLICY.MANAGE required).
            </div>
          )}

          <div className="nav-group-title" style={{ paddingLeft: 0, marginTop: 18 }}>
            CURRENT OVERRIDES {selected ? `· ${selected.displayName}` : ''}
          </div>
          {policies.length === 0 ? (
            <Empty>No override for this account.</Empty>
          ) : (
            <div className="list">
              {policies.map((policy) => (
                <div className="list-row" key={policy.id}>
                  <Badge tone={policy.effect === 'GRANT' ? 'ok' : 'danger'}>{policy.effect}</Badge>
                  <div className="grow">
                    <div className="title mono">{policy.permissionCode}</div>
                    <div className="sub">{policy.reason ?? 'no reason recorded'}</div>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={!can('S01.POLICY.MANAGE')}
                    onClick={async () => {
                      try {
                        await api.revokePolicy(policy.id)
                        push('Policy removed', 'ok')
                        if (selectedUser != null) setPolicies(await api.policies(selectedUser))
                      } catch (error) {
                        push(error instanceof Error ? error.message : 'Unable to remove the policy', 'error')
                      }
                    }}
                  >
                    Remove
                  </Button>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
