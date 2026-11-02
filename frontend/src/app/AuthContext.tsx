import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, getCachedUser, getToken, setCachedUser, setToken, type PlatformMeta, type SessionUser } from '../api/client'

interface AuthState {
  user: SessionUser | null
  meta: PlatformMeta | null
  loading: boolean
  signingIn: boolean
  error: string | null
  signIn: (username: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  refreshUser: () => Promise<void>
  can: (permission: string) => boolean
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(() => (getToken() ? getCachedUser() : null))
  const [meta, setMeta] = useState<PlatformMeta | null>(null)
  const [loading, setLoading] = useState(true)
  const [signingIn, setSigningIn] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    api
      .meta()
      .then((value) => {
        if (alive) setMeta(value)
      })
      .catch(() => undefined)

    if (!getToken()) {
      setLoading(false)
      return () => {
        alive = false
      }
    }
    api
      .me()
      .then((value) => {
        if (!alive) return
        setUser(value)
        setCachedUser(value)
      })
      .catch(() => {
        if (!alive) return
        setToken(null)
        setUser(null)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })
    return () => {
      alive = false
    }
  }, [])

  const signIn = useCallback(async (username: string, password: string) => {
    setSigningIn(true)
    setError(null)
    try {
      const session = await api.login(username, password)
      setToken(session.token)
      setCachedUser(session.user)
      setUser(session.user)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sign-in failed')
      throw err
    } finally {
      setSigningIn(false)
    }
  }, [])

  const signOut = useCallback(async () => {
    try {
      await api.logout()
    } catch {
      /* the local session is cleared regardless */
    }
    setToken(null)
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    const value = await api.me()
    setUser(value)
    setCachedUser(value)
  }, [])

  const can = useCallback(
    (permission: string) => {
      if (!user) return false
      if (user.role === 'OVERALL_LEAD') return true
      return user.permissions?.includes(permission) ?? false
    },
    [user]
  )

  const value = useMemo<AuthState>(
    () => ({ user, meta, loading, signingIn, error, signIn, signOut, refreshUser, can }),
    [user, meta, loading, signingIn, error, signIn, signOut, refreshUser, can]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
