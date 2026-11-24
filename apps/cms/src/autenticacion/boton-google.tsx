import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorApi, mensajeError, solicitar } from './api'

/*
 * Inicio de sesión con Google Identity Services (docs/revision/google-login-frontend.md).
 * El ID token se envía tal cual al backend y se descarta: no se decodifica ni se guarda.
 * Sin VITE_GOOGLE_CLIENT_ID no se muestra nada.
 */
const clienteGoogle = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? ''
export const googleConfigurado = clienteGoogle !== ''

type RespuestaGis = { credential?: string }
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

type TextoBoton = 'continue_with' | 'signin_with' | 'signup_with'

/** Botón oficial de Google. Llama a {@code alRecibirToken} con el ID token (response.credential). */
export function BotonGoogle({ texto = 'continue_with', tema = 'filled_black', alRecibirToken, ocupado = false }: { texto?: TextoBoton; tema?: 'outline' | 'filled_black' | 'filled_blue'; alRecibirToken: (idToken: string) => void; ocupado?: boolean }) {
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
      gis.accounts.id.initialize({ client_id: clienteGoogle, callback: (r: RespuestaGis) => { if (r.credential) callback.current(r.credential) }, ux_mode: 'popup', auto_select: false })
      gis.accounts.id.renderButton(elemento, { type: 'standard', theme: tema, size: 'large', text: texto, shape: 'rectangular', locale: 'es', width: Math.min(400, Math.max(220, elemento.offsetWidth)) })
    }, () => { if (activo) setFallo(true) })
    return () => { activo = false }
  }, [texto, tema])
  if (!googleConfigurado) return null
  if (fallo) return <p role="status" className="text-center text-sm opacity-75">No se pudo cargar el botón de Google. Usa tu correo y contraseña.</p>
  return <div aria-busy={ocupado} className={ocupado ? 'pointer-events-none opacity-60' : ''}><div ref={contenedor} className="flex min-h-[44px] w-full justify-center" /></div>
}

/** POST /api/auth/google: 201 (cuenta nueva, con cuerpo) o 204 (cuenta existente, sin cuerpo). */
export async function continuarConGoogle(idToken: string, recordarme = false) {
  const datos = await solicitar<unknown>('POST', '/api/auth/google', { idToken, recordarme })
  return { cuentaNueva: !!datos }
}
export const vincularGoogle = (idToken: string) => solicitar<void>('POST', '/api/auth/google/vinculacion', { idToken })

export type AccionGoogle = 'contrasena' | 'registro' | 'reintentar' | 'no-disponible' | 'sesion'
/** Mensaje y siguiente paso para cada respuesta de error de Google (tabla de la guía). */
export function interpretarErrorGoogle(error: unknown, vinculando = false): { mensaje: string; accion?: AccionGoogle } {
  if (!(error instanceof ErrorApi)) return { mensaje: 'No se pudo continuar con Google. Inténtalo de nuevo.' }
  const deCampo = error.campos?.correoElectronico || error.campos?.idToken
  switch (error.codigoEstado) {
    case 409: return { mensaje: deCampo || error.message, accion: !vinculando && error.campos?.correoElectronico ? 'contrasena' : undefined }
    case 422: return { mensaje: deCampo || error.message, accion: vinculando ? undefined : 'registro' }
    case 401:
      if (error.codigo === 'GOOGLE_TOKEN_INVALIDO') return { mensaje: error.message, accion: 'reintentar' }
      // 401 sin cuerpo: cuenta suspendida o cerrada (login) o sesión vencida (vinculación). No se revela el motivo.
      return vinculando ? { mensaje: 'Tu sesión venció. Vuelve a iniciar sesión.', accion: 'sesion' } : { mensaje: 'No se pudo iniciar sesión.' }
    case 503: return { mensaje: error.message || 'El inicio de sesión con Google no está disponible en este momento', accion: 'no-disponible' }
    default: return { mensaje: mensajeError(error) }
  }
}

/**
 * Bloque "Continuar con Google" de las páginas de acceso: botón, mensajes de error y siguiente paso.
 * {@code alEntrar} se llama con la sesión ya iniciada (201 o 204).
 */
export function AccesoConGoogle({ registro = false, recordarme = false, retorno, alEntrar, alUsarContrasena }: { registro?: boolean; recordarme?: boolean; retorno?: string | null; alEntrar: (cuentaNueva: boolean) => Promise<void> | void; alUsarContrasena?: () => void }) {
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState<{ mensaje: string; accion?: AccionGoogle } | null>(null)
  if (!googleConfigurado) return null
  const consulta = retorno ? `?retorno=${encodeURIComponent(retorno)}` : ''
  async function recibir(idToken: string) {
    setOcupado(true); setError(null)
    try { const { cuentaNueva } = await continuarConGoogle(idToken, recordarme); await alEntrar(cuentaNueva) }
    catch (e) { setError(interpretarErrorGoogle(e)) }
    finally { setOcupado(false) }
  }
  return <div className="grid gap-3">
    {error?.accion !== 'no-disponible' && <BotonGoogle texto={registro ? 'signup_with' : 'continue_with'} alRecibirToken={t => void recibir(t)} ocupado={ocupado} />}
    {ocupado && <p role="status" className="text-center text-[13px] text-white/70">Verificando tu cuenta de Google...</p>}
    {error && <div role="alert" className="grid gap-2 rounded-[12px] border border-red-400/30 bg-red-500/10 px-[14px] py-[10px] text-[13px] text-red-200">
      <p>{error.mensaje}</p>
      {error.accion === 'contrasena' && (alUsarContrasena
        ? <button type="button" onClick={alUsarContrasena} className="w-fit cursor-pointer font-semibold text-[#8fb3ff] hover:underline">Iniciar sesión con contraseña</button>
        : <Link to={`/iniciar-sesion${consulta}`} className="w-fit font-semibold text-[#8fb3ff] hover:underline">Iniciar sesión con contraseña</Link>)}
      {error.accion === 'registro' && !registro && <Link to={`/registro${consulta}`} className="w-fit font-semibold text-[#8fb3ff] hover:underline">Crear una cuenta con correo y contraseña</Link>}
      {error.accion === 'registro' && registro && <p>Puedes crear tu cuenta con correo y contraseña en el formulario de arriba.</p>}
      {error.accion === 'reintentar' && <p>Vuelve a pulsar el botón de Google para intentarlo de nuevo.</p>}
      {error.accion === 'no-disponible' && <p>Mientras tanto, usa tu correo y contraseña.</p>}
    </div>}
  </div>
}

/** Separador "o …" que solo se muestra si Google está configurado. */
export function SiGoogle({ children }: { children: React.ReactNode }) { return googleConfigurado ? <>{children}</> : null }
