import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { actualizarSesion, ErrorSolicitud, solicitar, mensajeDeError } from './api'
import { MarcoTienda, Volver } from './diseno'
import { AccesoConGoogle } from './boton-google'

export function Autenticacion({ registro = false }: { registro?: boolean }) {
  const { slug = '' } = useParams()
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const [nombre, setNombre] = useState('')
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [recordarme, setRecordarme] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [bienvenida, setBienvenida] = useState(false)
  const parametros = new URLSearchParams(ubicacion.search)
  const parametro = parametros.get('retorno') || ''
  const base = `/tienda/${encodeURIComponent(slug)}`
  // Solo se admite una ruta local de esta tienda; nunca una URL externa o protocolo relativo.
  let retorno = base
  try {
    const url = new URL(parametro, window.location.origin)
    if (url.origin === window.location.origin && (url.pathname === base || url.pathname.startsWith(`${base}/`)) && !url.pathname.endsWith('/iniciar-sesion') && !url.pathname.endsWith('/registro')) retorno = url.pathname + url.search
  } catch { /* Se usa la portada de la tienda. */ }
  const destino = retorno.slice('/tienda'.length)
  async function enviar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault(); setError(''); setAviso(''); setOcupado(true)
    try {
      if (registro) await solicitar('/api/auth/registro', 'POST', { nombres: nombre, correoElectronico: correo, password: contrasena })
      try {
        await solicitar('/api/auth/login', 'POST', { email: correo, password: contrasena, recordarme })
        actualizarSesion(); navegar(destino, { replace: true })
      } catch (err) {
        if (registro) setAviso('Tu cuenta fue creada. Inicia sesión para continuar.')
        else setError(err instanceof ErrorSolicitud && err.estado === 401 ? 'El correo o la contraseña son incorrectos.' : err instanceof Error ? err.message : 'No se pudo iniciar sesión.')
      }
    } catch (err) { setError(mensajeDeError(err, 'No se pudo crear la cuenta.')) }
    finally { setOcupado(false) }
  }
  const alterna = `/${slug}/${registro ? 'iniciar-sesion' : 'registro'}?retorno=${encodeURIComponent(retorno)}`
  const enlaceDe = (pagina: string) => `/${slug}/${pagina}?retorno=${encodeURIComponent(retorno)}`
  // Google: 204 entra directo; 201 (cuenta nueva) muestra la bienvenida un momento. Vuelve al mismo retorno validado.
  async function entrarConGoogle(cuentaNueva: boolean) {
    actualizarSesion()
    if (cuentaNueva) { setBienvenida(true); await new Promise(r => setTimeout(r, 1500)) }
    navegar(destino, { replace: true })
  }
  return <MarcoTienda slug={slug}><div className="formulario-cuenta"><Volver to={`/${slug}`}>Volver a la tienda</Volver><h1>{registro ? 'Crea tu cuenta' : 'Inicia sesión'}</h1><p>Usa tu cuenta de Nubea para comprar y consultar tus pedidos.</p>{bienvenida && <p role="status" className="aviso-bienvenida">¡Bienvenido/a a Nubea! Creamos tu cuenta con Google.</p>}{parametros.get('vencida') === '1' && <p role="status">Tu sesión venció. Inicia sesión para continuar.</p>}{aviso ? <><p role="status">{aviso}</p><Link to={alterna}>Iniciar sesión</Link></> : <form onSubmit={e => void enviar(e)}>{registro && <label>Nombres<input autoComplete="given-name" maxLength={100} value={nombre} onChange={e => setNombre(e.target.value)} required /></label>}<label>Correo electrónico<input id="correo-acceso" type="email" autoComplete="email" maxLength={100} value={correo} onChange={e => setCorreo(e.target.value)} required /></label><label>Contraseña<input type="password" autoComplete={registro ? 'new-password' : 'current-password'} minLength={registro ? 8 : undefined} maxLength={128} value={contrasena} onChange={e => setContrasena(e.target.value)} required /></label>{registro && <p>La contraseña debe tener al menos 8 caracteres.</p>}{!registro && <label><input type="checkbox" checked={recordarme} onChange={e => setRecordarme(e.target.checked)} /> Recordarme durante 24 horas</label>}{error && <p role="alert">{error}</p>}<button className="boton boton-primario" disabled={ocupado}>{ocupado ? 'Un momento...' : registro ? 'Crear cuenta' : 'Iniciar sesión'}</button></form>}{!aviso && <AccesoConGoogle registro={registro} recordarme={recordarme} enlaceContrasena={enlaceDe('iniciar-sesion')} enlaceRegistro={enlaceDe('registro')} alEntrar={entrarConGoogle} alUsarContrasena={registro ? undefined : () => document.getElementById('correo-acceso')?.focus()} />}<p>{registro ? '¿Ya tienes cuenta?' : '¿Aún no tienes cuenta?'} <Link to={alterna}>{registro ? 'Inicia sesión' : 'Crea una cuenta'}</Link></p></div></MarcoTienda>
}
