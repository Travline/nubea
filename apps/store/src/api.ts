const api = import.meta.env.VITE_API_URL?.trim() || (import.meta.env.DEV ? 'http://localhost:8080' : '')

/**
 * Error de la API. {@code codigo} es el campo `error` del backend (p. ej. TIENDA_NO_DISPONIBLE, DUPLICADO) y
 * {@code campos} el mapa campo → mensaje para mostrar junto a cada campo del formulario.
 */
export class ErrorSolicitud extends Error {
  estado: number
  codigo?: string
  campos: Record<string, string>
  constructor(estado: number, mensaje: string, codigo?: string, campos?: Record<string, string> | null) {
    super(mensaje)
    this.estado = estado
    this.codigo = codigo
    this.campos = campos ?? {}
  }
}

export async function solicitar<T>(ruta: string, metodo = 'GET', cuerpo?: object): Promise<T> {
  const headers: Record<string, string> = {}
  if (metodo !== 'GET') {
    const respuesta = await fetch(`${api}/api/auth/csrf`, { credentials: 'include' })
    if (!respuesta.ok) throw new ErrorSolicitud(respuesta.status, 'No se pudo preparar la solicitud.')
    const csrf: { headerName: string; token: string } = await respuesta.json()
    headers[csrf.headerName] = csrf.token
    if (cuerpo) headers['Content-Type'] = 'application/json'
  }
  const respuesta = await fetch(`${api}${ruta}`, { method: metodo, credentials: 'include', headers, body: cuerpo ? JSON.stringify(cuerpo) : undefined })
  if (!respuesta.ok) {
    if (respuesta.status === 401 && !ruta.startsWith('/api/auth/')) window.dispatchEvent(new Event('tienda-sesion-vencida'))
    let mensaje = 'No se pudo completar la solicitud.'
    let codigo: string | undefined
    let campos: Record<string, string> | null = null
    try {
      const datos = await respuesta.json()
      mensaje = datos.mensaje || mensaje
      codigo = datos.error
      campos = datos.campos
    } catch { /* Respuesta sin JSON */ }
    throw new ErrorSolicitud(respuesta.status, mensaje, codigo, campos)
  }
  if (respuesta.status === 204) return undefined as T
  return respuesta.json() as Promise<T>
}

/** Mensaje general para mostrar: los mensajes por campo, si los hay, o el mensaje de la respuesta. */
export function mensajeDeError(e: unknown, porDefecto = 'No se pudo completar la operación.') {
  if (e instanceof ErrorSolicitud) return Object.values(e.campos).join(' ') || e.message
  return e instanceof Error ? e.message : porDefecto
}

/** Mensaje de un campo concreto, para mostrarlo junto a ese campo. */
export function errorDeCampo(e: unknown, campo: string) {
  return e instanceof ErrorSolicitud ? e.campos[campo] : undefined
}

/** La tienda existe pero no está activa (suspendida, cerrada o en preparación). */
export function esTiendaNoDisponible(e: unknown) {
  return e instanceof ErrorSolicitud && e.codigo === 'TIENDA_NO_DISPONIBLE'
}

export function enlaceSesion(slug: string) { return `/${encodeURIComponent(slug)}/iniciar-sesion?retorno=${encodeURIComponent(window.location.pathname + window.location.search)}` }
export function actualizarSesion() { window.dispatchEvent(new Event('tienda-sesion')) }
export function actualizarCarrito() { window.dispatchEvent(new Event('tienda-carrito')) }

export async function agregarAlCarrito(slug: string, id: string) { const resultado = await solicitar(`/api/compra/tiendas/${encodeURIComponent(slug)}/carrito/items/${id}`, 'POST', { cantidad: 1 }); actualizarCarrito(); return resultado }

export const precio = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)
