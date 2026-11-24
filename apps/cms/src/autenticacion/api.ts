import axios from 'axios'

export interface Usuario {
  idUsuario: string
  nombres: string
  correoElectronico: string
  roles: string[]
}

export class ErrorApi extends Error {
  codigoEstado: number
  campos?: Record<string, string>
  constructor(codigoEstado: number, mensaje: string, campos?: Record<string, string>) {
    super(mensaje)
    this.codigoEstado = codigoEstado
    this.campos = campos
  }
}

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
      const datos = error.response?.data
      const estado = error.response?.status ?? 0
      if (estado === 401 && !ruta.startsWith('/api/auth/')) window.dispatchEvent(new Event('sesion-vencida'))
      throw new ErrorApi(estado, datos?.mensaje || (error.response ? 'No se pudo completar la solicitud.' : 'No se pudo conectar con el servidor. Inténtalo de nuevo.'), datos?.campos)
    }
    throw error
  }
}

export function mensajeError(error: unknown) {
  if (error instanceof ErrorApi) return Object.values(error.campos || {}).join(' ') || error.message
  return error instanceof Error ? error.message : 'No se pudo completar la operación.'
}

export const apiAutenticacion = {
  obtenerUsuarioActual: () => solicitar<Usuario>('GET', '/api/auth/me'),
  iniciarSesion: (correo: string, contrasena: string, recordarme = false) => solicitar<void>('POST', '/api/auth/login', { email: correo, password: contrasena, recordarme }),
  registrar: (nombres: string, correoElectronico: string, contrasena: string) => solicitar<Usuario>('POST', '/api/auth/registro', { nombres, correoElectronico, password: contrasena }),
  cerrarSesion: () => solicitar<void>('POST', '/logout'),
  // 201 = cuenta nueva (devuelve el usuario); 204 = cuenta existente (sin cuerpo). Ver docs/revision/google-login-frontend.md
  continuarConGoogle: (idToken: string, recordarme = false) => solicitar<Usuario | ''>('POST', '/api/auth/google', { idToken, recordarme }),
  vincularGoogle: (idToken: string) => solicitar<void>('POST', '/api/auth/google/vinculacion', { idToken }),
}
