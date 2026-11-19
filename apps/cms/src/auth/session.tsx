import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { ApiError, authApi } from './api'
import type { User } from './api'

type Session = { user: User | null; status: 'loading' | 'ready' | 'error'; refresh: () => Promise<void>; clear: () => void }
const SessionContext = createContext<Session | null>(null)

export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [status, setStatus] = useState<Session['status']>('loading')
  const load = useCallback(async () => {
    try { setUser(await authApi.me()); setStatus('ready') }
    catch (error) {
      setUser(null)
      setStatus(error instanceof ApiError && error.status === 401 ? 'ready' : 'error')
    }
  }, [])
  const refresh = useCallback(async () => { setStatus('loading'); await load() }, [load])
  useEffect(() => {
    let active = true
    authApi.me().then(
      current => { if (active) { setUser(current); setStatus('ready') } },
      error => { if (active) { setUser(null); setStatus(error instanceof ApiError && error.status === 401 ? 'ready' : 'error') } },
    )
    return () => { active = false }
  }, [])
  return <SessionContext.Provider value={{ user, status, refresh, clear: () => { setUser(null); setStatus('ready') } }}>{children}</SessionContext.Provider>
}
export function useSession() {
  const value = useContext(SessionContext)
  if (!value) throw new Error('SessionProvider no disponible')
  return value
}
