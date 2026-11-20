const api = import.meta.env.VITE_API_URL?.trim() || (import.meta.env.DEV ? 'http://localhost:8080' : '')
export class ErrorSolicitud extends Error {
  estado: number
  constructor(estado: number, mensaje: string) { super(mensaje); this.estado = estado }
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
    try { const datos = await respuesta.json(); mensaje = datos.mensaje || mensaje } catch { /* Respuesta sin JSON */ }
    throw new ErrorSolicitud(respuesta.status, mensaje)
  }
  if (respuesta.status === 204) return undefined as T
  return respuesta.json() as Promise<T>
}
export function enlaceSesion(slug: string) { return `/${encodeURIComponent(slug)}/iniciar-sesion?retorno=${encodeURIComponent(window.location.pathname + window.location.search)}` }
export function actualizarSesion() { window.dispatchEvent(new Event('tienda-sesion')) }
export function actualizarCarrito() { window.dispatchEvent(new Event('tienda-carrito')) }

export async function agregarAlCarrito(slug: string, id: string) { const resultado = await solicitar(`/api/compra/tiendas/${encodeURIComponent(slug)}/carrito/items/${id}`, 'POST', { cantidad: 1 }); actualizarCarrito(); return resultado }

export const precio = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)
