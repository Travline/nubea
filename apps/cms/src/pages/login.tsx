import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ApiError, authApi } from '@/auth/api'
import { useSession } from '@/auth/session'

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { refresh } = useSession()
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    try {
      await authApi.login(email, password)
      await refresh()
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401 ? 'El correo o la contraseña son incorrectos.' : err instanceof Error ? err.message : 'No se pudo iniciar sesión.')
    } finally { setBusy(false) }
  }
  return <main className="flex min-h-screen items-center justify-center px-4 py-32">
    <Card className="w-full max-w-md">
      <CardHeader><CardTitle className="text-2xl">Ingresa a tu cuenta</CardTitle><p className="text-muted-foreground text-sm">Accede a tu espacio en Nubea.</p></CardHeader>
      <CardContent>
        {location.state?.registered && <p role="status" className="mb-4 text-sm">Tu cuenta fue creada. Inicia sesión para continuar.</p>}
        <form onSubmit={submit} className="space-y-5">
          <div className="grid gap-2"><Label htmlFor="email">Correo</Label><Input id="email" type="email" autoComplete="email" value={email} onChange={e => setEmail(e.target.value)} required /></div>
          <div className="grid gap-2"><Label htmlFor="password">Contraseña</Label><Input id="password" type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required /></div>
          {error && <p role="alert" className="text-destructive text-sm">{error}</p>}
          <Button type="submit" className="w-full" disabled={busy}>{busy ? 'Ingresando...' : 'Ingresar'}</Button>
        </form>
        <p className="mt-5 text-sm text-muted-foreground">¿Aún no tienes cuenta? <Link className="text-primary underline" to="/register">Crea una cuenta</Link></p>
      </CardContent>
    </Card>
  </main>
}
