import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ErrorApi, apiAutenticacion } from '@/autenticacion/api'
import { useSesion } from '@/autenticacion/sesion'

export function PaginaInicioSesion() {
  const [correo, establecerCorreo] = useState('')
  const [contrasena, establecerContrasena] = useState('')
  const [error, establecerError] = useState('')
  const [ocupado, establecerOcupado] = useState(false)
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const { actualizar } = useSesion()
  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault(); establecerError(''); establecerOcupado(true)
    try {
      await apiAutenticacion.iniciarSesion(correo, contrasena)
      await actualizar()
      navegar('/dashboard', { replace: true })
    } catch (err) {
      establecerError(err instanceof ErrorApi && err.codigoEstado === 401 ? 'El correo o la contraseña son incorrectos.' : err instanceof Error ? err.message : 'No se pudo iniciar sesión.')
    } finally { establecerOcupado(false) }
  }
  return <main className="flex min-h-screen items-center justify-center px-4 py-32">
    <Card className="w-full max-w-md">
      <CardHeader><CardTitle className="text-2xl">Ingresa a tu cuenta</CardTitle><p className="text-muted-foreground text-sm">Accede a tu espacio en Nubea.</p></CardHeader>
      <CardContent>
        {ubicacion.state?.registroCompletado && <p role="status" className="mb-4 text-sm">Tu cuenta fue creada. Inicia sesión para continuar.</p>}
        <form onSubmit={enviar} className="space-y-5">
          <div className="grid gap-2"><Label htmlFor="email">Correo</Label><Input id="email" type="email" autoComplete="email" value={correo} onChange={e => establecerCorreo(e.target.value)} required /></div>
          <div className="grid gap-2"><Label htmlFor="password">Contraseña</Label><Input id="password" type="password" autoComplete="current-password" value={contrasena} onChange={e => establecerContrasena(e.target.value)} required /></div>
          {error && <p role="alert" className="text-destructive text-sm">{error}</p>}
          <Button type="submit" className="w-full" disabled={ocupado}>{ocupado ? 'Ingresando...' : 'Ingresar'}</Button>
        </form>
        <p className="mt-5 text-sm text-muted-foreground">¿Aún no tienes cuenta? <Link className="text-primary underline" to="/register">Crea una cuenta</Link></p>
      </CardContent>
    </Card>
  </main>
}
