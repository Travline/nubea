import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorSolicitud, mensajeDeError, solicitar } from './api'

/*
 * "Continuar con Google" con Google Identity Services (docs/revision/google-login-frontend.md).
 * El ID token se envía tal cual al backend y se descarta: no se decodifica ni se guarda.
 * Sin VITE_GOOGLE_CLIENT_ID no se muestra nada. El login con Google no crea la relación con la tienda:
 * se crea con el primer "Agregar al carrito", igual que con el login local.
 */
const clienteGoogle = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? ''
const googleConfigurado = clienteGoogle !== ''

type Gis = { accounts: { id: { initialize: (opciones: object) => void; renderButton: (elemento: HTMLElement, opciones: object) => void } } }
declare global { interface Window { google?: Gis } }

let carga: Promise<Gis> | null = null
function cargarGis(): Promise<Gis> {
  if (window.google?.accounts?.id) return Promise.resolve(window.google)
  carga ??= new Promise<Gis>((resolver, rechazar) => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    script.onload = () => window.google?.accounts?.id ? resolver(window.google) : rechazar(new Error('Google Identity Services no cargó'))
    script.onerror = () => { carga = null; rechazar(new Error('Google Identity Services no cargó')) }
    document.head.appendChild(script)
  })
  return carga
}

function BotonGoogle({ registro, alRecibirToken, ocupado }: { registro: boolean; alRecibirToken: (idToken: string) => void; ocupado: boolean }) {
  const contenedor = useRef<HTMLDivElement>(null)
  const callback = useRef(alRecibirToken)
  useEffect(() => { callback.current = alRecibirToken })
  const [fallo, setFallo] = useState(false)
  useEffect(() => {
    if (!googleConfigurado) return
    let activo = true
    cargarGis().then(gis => {
      const elemento = contenedor.current
      if (!activo || !elemento) return
      gis.accounts.id.initialize({ client_id: clienteGoogle, callback: (r: { credential?: string }) => { if (r.credential) callback.current(r.credential) }, ux_mode: 'popup', auto_select: false })
      gis.accounts.id.renderButton(elemento, { type: 'standard', theme: 'outline', size: 'large', text: registro ? 'signup_with' : 'continue_with', locale: 'es', width: Math.min(400, Math.max(220, elemento.offsetWidth)) })
    }, () => { if (activo) setFallo(true) })
    return () => { activo = false }
  }, [registro])
  if (fallo) return <p role="status">No se pudo cargar el botón de Google. Usa tu correo y contraseña.</p>
  return <div aria-busy={ocupado} className={`acceso-google-boton${ocupado ? ' ocupado' : ''}`} ref={contenedor} />
}

type Accion = 'contrasena' | 'registro' | 'reintentar' | 'no-disponible'
function interpretarError(e: unknown): { mensaje: string; accion?: Accion } {
  if (!(e instanceof ErrorSolicitud)) return { mensaje: 'No se pudo continuar con Google. Inténtalo de nuevo.' }
  const deCampo = e.campos.correoElectronico || e.campos.idToken
  switch (e.estado) {
    case 409: return { mensaje: deCampo || e.message, accion: e.campos.correoElectronico ? 'contrasena' : undefined }
    case 422: return { mensaje: deCampo || e.message, accion: 'registro' }
    case 401: return e.codigo === 'GOOGLE_TOKEN_INVALIDO' ? { mensaje: e.message, accion: 'reintentar' } : { mensaje: 'No se pudo iniciar sesión.' }
    case 503: return { mensaje: e.message || 'El inicio de sesión con Google no está disponible en este momento', accion: 'no-disponible' }
    default: return { mensaje: mensajeDeError(e) }
  }
}

/**
 * Botón de Google con sus mensajes. {@code alEntrar} se llama con la sesión ya iniciada:
 * 201 (cuenta nueva, con cuerpo) o 204 (cuenta existente).
 */
export function AccesoConGoogle({ registro = false, recordarme = false, enlaceContrasena, enlaceRegistro, alEntrar, alUsarContrasena }: { registro?: boolean; recordarme?: boolean; enlaceContrasena: string; enlaceRegistro: string; alEntrar: (cuentaNueva: boolean) => Promise<void> | void; alUsarContrasena?: () => void }) {
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<{ mensaje: string; accion?: Accion } | null>(null)
  if (!googleConfigurado) return null
  async function recibir(idToken: string) {
    setOcupado(true); setError(null)
    try {
      const datos = await solicitar<unknown>('/api/auth/google', 'POST', { idToken, recordarme })
      await alEntrar(!!datos)
    } catch (e) { setError(interpretarError(e)) }
    finally { setOcupado(false) }
  }
  return <div className="acceso-google">
    <p className="separador-acceso"><span>o</span></p>
    {error?.accion !== 'no-disponible' && <BotonGoogle registro={registro} alRecibirToken={t => void recibir(t)} ocupado={ocupado} />}
    {ocupado && <p role="status">Verificando tu cuenta de Google...</p>}
    {error && <div role="alert" className="aviso-google">
      <p>{error.mensaje}</p>
      {error.accion === 'contrasena' && (alUsarContrasena ? <button type="button" className="boton-enlace" onClick={alUsarContrasena}>Iniciar sesión con contraseña</button> : <Link to={enlaceContrasena}>Iniciar sesión con contraseña</Link>)}
      {error.accion === 'registro' && (registro ? <p>Puedes crear tu cuenta con correo y contraseña en el formulario de arriba.</p> : <Link to={enlaceRegistro}>Crear una cuenta con correo y contraseña</Link>)}
      {error.accion === 'reintentar' && <p>Vuelve a pulsar el botón de Google para intentarlo de nuevo.</p>}
      {error.accion === 'no-disponible' && <p>Mientras tanto, usa tu correo y contraseña.</p>}
    </div>}
  </div>
}
