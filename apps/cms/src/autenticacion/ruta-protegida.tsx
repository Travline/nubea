import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useSesion } from './sesion'

export function RutaProtegida() {
  const { usuario, estado, actualizar } = useSesion()
  const ubicacion = useLocation()
  if (estado === 'cargando') return <main className="p-8" role="status">Comprobando tu sesión...</main>
  if (estado === 'error') return <main className="mx-auto max-w-lg space-y-4 p-8"><p role="alert">No pudimos comprobar tu sesión. Revisa tu conexión e inténtalo de nuevo.</p><Button onClick={() => void actualizar().catch(() => {})}>Reintentar</Button></main>
  if (!usuario) return <Navigate to="/iniciar-sesion" replace state={{ from: ubicacion.pathname + ubicacion.search }} />
  return <Outlet />
}
