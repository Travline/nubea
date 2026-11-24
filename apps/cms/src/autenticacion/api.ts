import axios from 'axios'

export interface Usuario {
  idUsuario: string
  nombres: string
  correoElectronico: string
  roles: string[]
}

/**
 * Error de la API con el formato del backend: {status, error, mensaje, campos?}.
 * {@code codigo} es el campo {@code error} (DATOS_INVALIDOS, CONFLICTO, DUPLICADO, REGLA_NEGOCIO…), o SIN_PERMISO (403),
 * que el backend envía sin cuerpo.
 */
export class ErrorApi extends Error {
  codigoEstado: number
  codigo?: string
  campos?: Record<string, string>
  constructor(codigoEstado: number, mensaje: string, campos?: Record<string, string>, codigo?: string) {
    super(mensaje)
    this.codigoEstado = codigoEstado
    this.campos = campos
    this.codigo = codigo
  }
}

export const SIN_PERMISO = 'SIN_PERMISO'
const MENSAJE_SIN_PERMISO = 'Ya no tienes permiso de vendedor para hacer esto. Si crees que es un error, escribe a soporte de Nubea.'

const urlApi = import.meta.env.VITE_API_URL?.trim() || (import.meta.env.DEV ? 'http://localhost:8080' : '')
const cliente = axios.create({ baseURL: urlApi, withCredentials: true, withXSRFToken: false })

type Metodo = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
export async function solicitar<T>(metodo: Metodo, ruta: string, cuerpo?: object): Promise<T> {
  if (!urlApi) throw new ErrorApi(0, 'No se ha configurado la dirección de la API.')
  try {
    const cabeceras: Record<string, string> = {}
    if (metodo !== 'GET') {
      const csrf = await cliente.get<{ headerName: string; token: string }>('/api/auth/csrf')
      cabeceras[csrf.data.headerName] = csrf.data.token
    }
    const respuesta = await cliente.request<T>({ method: metodo, url: ruta, data: cuerpo, headers: cabeceras })
    return respuesta.data
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const datos = error.response?.data as { mensaje?: string; error?: string; campos?: Record<string, string> } | undefined
      const estado = error.response?.status ?? 0
      const esAuth = ruta.startsWith('/api/auth/') || ruta === '/logout'
      if (estado === 401 && !esAuth) window.dispatchEvent(new Event('sesion-vencida'))
      // 403 en el panel: se perdió el rol de vendedor o la tienda no es suya. El backend no envía un texto útil.
      if (estado === 403 && !esAuth) throw new ErrorApi(403, ruta.startsWith('/api/tiendas/') ? MENSAJE_SIN_PERMISO : 'No tienes permiso para hacer esto.', undefined, SIN_PERMISO)
      // 409 sin campos en una tienda (p. ej. la suspendieron con el panel abierto): el panel vuelve a leer su estado
      // para mostrar el aviso de solo lectura. El mensaje lo da el backend.
      if (estado === 409 && !datos?.campos && ruta.startsWith('/api/tiendas/')) window.dispatchEvent(new Event('tienda-no-modificable'))
      throw new ErrorApi(estado, datos?.mensaje || (error.response ? 'No se pudo completar la solicitud.' : 'No se pudo conectar con el servidor. Inténtalo de nuevo.'), datos?.campos, datos?.error)
    }
    throw error
  }
}

/** Texto de un error para mostrarlo en un único aviso (une los mensajes de los campos). */
export function mensajeError(error: unknown) {
  if (error instanceof ErrorApi) return Object.values(error.campos || {}).join(' ') || error.message
  return error instanceof Error ? error.message : 'No se pudo completar la operación.'
}

/** Mensaje del backend para un campo concreto, o ''. */
export function errorDeCampo(error: unknown, campo: string) {
  return error instanceof ErrorApi ? error.campos?.[campo] ?? '' : ''
}

export const apiAutenticacion = {
  obtenerUsuarioActual: () => solicitar<Usuario>('GET', '/api/auth/me'),
  iniciarSesion: (correo: string, contrasena: string, recordarme = false) => solicitar<void>('POST', '/api/auth/login', { email: correo, password: contrasena, recordarme }),
  registrar: (nombres: string, correoElectronico: string, contrasena: string) => solicitar<Usuario>('POST', '/api/auth/registro', { nombres, correoElectronico, password: contrasena }),
  cerrarSesion: () => solicitar<void>('POST', '/logout'),
}
