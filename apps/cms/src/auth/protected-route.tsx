import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useSession } from './session'

export function ProtectedRoute() {
  const { user, status, refresh } = useSession()
  const location = useLocation()
  if (status === 'loading') return <main className="p-8" role="status">Comprobando tu sesión...</main>
  if (status === 'error') return <main className="mx-auto max-w-lg space-y-4 p-8"><p role="alert">No pudimos comprobar tu sesión. Revisa tu conexión e inténtalo de nuevo.</p><Button onClick={() => void refresh()}>Reintentar</Button></main>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
