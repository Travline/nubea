import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LogOut, ShieldCheck, Store, UserRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ThemeToggle } from '@/components/theme-toggle'
import { authApi } from '@/auth/api'
import { useSession } from '@/auth/session'

export function Dashboard() {
  const { user, clear } = useSession()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()
  if (!user) return null
  const isAdmin = user.roles.includes('ADMIN_GENERAL')
  async function logout() {
    setBusy(true); setError('')
    try { await authApi.logout(); clear(); navigate('/login', { replace: true }) }
    catch (err) { setError(err instanceof Error ? err.message : 'No se pudo cerrar la sesión.') }
    finally { setBusy(false) }
  }
  return <div className="min-h-screen bg-muted/30">
    <header className="border-b bg-background"><div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-4">
      <Link to="/" className="flex items-center gap-2 font-bold text-xl"><img className="w-10" src="/assets/logo.webp" alt="" />Nubea</Link>
      <div className="flex items-center gap-2"><ThemeToggle /><Button variant="outline" onClick={() => void logout()} disabled={busy}><LogOut aria-hidden="true" />{busy ? 'Saliendo...' : 'Cerrar sesión'}</Button></div>
    </div></header>
    <main className="mx-auto max-w-6xl space-y-8 px-5 py-12">
      {error && <p role="alert" className="text-destructive">{error}</p>}
      <div><p className="mb-2 text-sm font-medium text-primary">{isAdmin ? 'ADMINISTRACIÓN DE PLATAFORMA' : 'TU ESPACIO EN NUBEA'}</p><h1 className="text-3xl font-semibold tracking-tight">Hola, {user.nombres}</h1><p className="mt-3 max-w-2xl text-muted-foreground">{isAdmin ? 'Bienvenido al espacio de administración general de Nubea.' : 'Bienvenido a tu espacio. Desde aquí podrás gestionar tu experiencia en Nubea.'}</p></div>
      <div className="grid gap-5 md:grid-cols-2">
        <Card><CardHeader><CardTitle className="flex items-center gap-2"><UserRound className="size-5 text-primary" /> Tu cuenta</CardTitle></CardHeader><CardContent className="space-y-2 text-sm"><p>{user.nombres}</p><p className="text-muted-foreground">{user.correoElectronico}</p></CardContent></Card>
        <Card><CardHeader><CardTitle className="flex items-center gap-2">{isAdmin ? <ShieldCheck className="size-5 text-primary" /> : <Store className="size-5 text-primary" />}{isAdmin ? 'Administración general' : 'Tus tiendas'}</CardTitle></CardHeader><CardContent><p className="text-sm text-muted-foreground">{isAdmin ? 'Tu cuenta tiene acceso de administrador de la plataforma.' : 'La gestión y selección de tiendas estarán disponibles próximamente.'}</p></CardContent></Card>
      </div>
    </main>
  </div>
}
