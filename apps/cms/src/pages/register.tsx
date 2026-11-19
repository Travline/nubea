import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ApiError, authApi } from '@/auth/api'

export function RegisterPage() {
  const [nombres, setNombres] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    try {
      await authApi.register(nombres, email, password)
      navigate('/login', { replace: true, state: { registered: true } })
    } catch (err) {
      if (err instanceof ApiError) setError(Object.values(err.fields || {}).join(' ') || err.message)
      else setError('No se pudo crear la cuenta.')
    } finally { setBusy(false) }
  }
  return <main className="flex min-h-screen items-center justify-center px-4 py-32">
    <Card className="w-full max-w-md">
      <CardHeader><CardTitle className="text-2xl">Crea tu cuenta</CardTitle><p className="text-muted-foreground text-sm">Empieza a formar parte de Nubea.</p></CardHeader>
      <CardContent>
        <form onSubmit={submit} className="space-y-5">
          <div className="grid gap-2"><Label htmlFor="nombres">Nombres</Label><Input id="nombres" autoComplete="given-name" maxLength={100} value={nombres} onChange={e => setNombres(e.target.value)} required /></div>
          <div className="grid gap-2"><Label htmlFor="email">Correo</Label><Input id="email" type="email" autoComplete="email" maxLength={100} value={email} onChange={e => setEmail(e.target.value)} required /></div>
          <div className="grid gap-2"><Label htmlFor="password">Contraseña</Label><Input id="password" type="password" autoComplete="new-password" minLength={8} maxLength={128} value={password} onChange={e => setPassword(e.target.value)} required /><p className="text-sm text-muted-foreground">Debe tener al menos 8 caracteres.</p></div>
          {error && <p role="alert" className="text-destructive text-sm">{error}</p>}
          <Button type="submit" className="w-full" disabled={busy}>{busy ? 'Creando cuenta...' : 'Crear cuenta'}</Button>
        </form>
        <p className="mt-5 text-sm text-muted-foreground">¿Ya tienes cuenta? <Link className="text-primary underline" to="/login">Inicia sesión</Link></p>
      </CardContent>
    </Card>
  </main>
}
