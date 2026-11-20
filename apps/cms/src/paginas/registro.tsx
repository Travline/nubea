import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useSesion } from '@/autenticacion/sesion'
import { ErrorApi, apiAutenticacion } from '@/autenticacion/api'

export function PaginaRegistro() {
  const [nombres, setNombres] = useState('')
  const [correo, establecerCorreo] = useState('')
  const [contrasena, establecerContrasena] = useState('')
  const [error, establecerError] = useState('')
  const [ocupado, establecerOcupado] = useState(false)
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const retorno = new URLSearchParams(ubicacion.search).get('retorno')
  const retornoSeguro = retorno && /^\/tienda\/[a-z0-9]+(?:-[a-z0-9]+)*(?:\/carrito)?\/?$/.test(retorno) ? retorno : null
  const { actualizar } = useSesion()
  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault(); establecerError(''); establecerOcupado(true)
    try {
      await apiAutenticacion.registrar(nombres, correo, contrasena)
      try {
        await apiAutenticacion.iniciarSesion(correo, contrasena)
        await actualizar()
        if (retornoSeguro) window.location.assign(retornoSeguro)
        else navegar('/panel', { replace: true })
      } catch {
        navegar(`/iniciar-sesion${retornoSeguro ? `?retorno=${encodeURIComponent(retornoSeguro)}` : ''}`, { replace: true, state: { registroCompletado: true } })
      }
    } catch (err) {
      if (err instanceof ErrorApi) establecerError(Object.values(err.campos || {}).join(' ') || err.message)
      else establecerError('No se pudo crear la cuenta.')
    } finally { establecerOcupado(false) }
  }
  return <main className="flex min-h-screen items-center justify-center px-4 py-32">
    <Card className="w-full max-w-md">
      <CardHeader><CardTitle className="text-2xl">Crea tu cuenta</CardTitle><p className="text-muted-foreground text-sm">Empieza a formar parte de Nubea.</p></CardHeader>
      <CardContent>
        <form onSubmit={enviar} className="space-y-5">
          <div className="grid gap-2"><Label htmlFor="nombres">Nombres</Label><Input id="nombres" autoComplete="given-name" maxLength={100} value={nombres} onChange={e => setNombres(e.target.value)} required /></div>
          <div className="grid gap-2"><Label htmlFor="email">Correo</Label><Input id="email" type="email" autoComplete="email" maxLength={100} value={correo} onChange={e => establecerCorreo(e.target.value)} required /></div>
          <div className="grid gap-2"><Label htmlFor="password">Contraseña</Label><Input id="password" type="password" autoComplete="new-password" minLength={8} maxLength={128} value={contrasena} onChange={e => establecerContrasena(e.target.value)} required /><p className="text-sm text-muted-foreground">Debe tener al menos 8 caracteres.</p></div>
          {error && <p role="alert" className="text-destructive text-sm">{error}</p>}
          <Button type="submit" className="w-full" disabled={ocupado}>{ocupado ? 'Creando cuenta...' : 'Crear cuenta'}</Button>
        </form>
        <p className="mt-5 text-sm text-muted-foreground">¿Ya tienes cuenta? <Link className="text-primary underline" to={`/iniciar-sesion${retornoSeguro ? `?retorno=${encodeURIComponent(retornoSeguro)}` : ""}`}>Inicia sesión</Link></p>
      </CardContent>
    </Card>
  </main>
}
