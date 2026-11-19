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

const cliente = axios.create({ withCredentials: true, withXSRFToken: false, headers: { 'Cache-Control': 'no-store' } })

async function solicitar<T>(ruta: string, cuerpo?: object, cabeceras?: Record<string, string>): Promise<T> {
  try {
    const respuesta = cuerpo === undefined && !cabeceras
      ? await cliente.get<T>(ruta)
      : await cliente.post<T>(ruta, cuerpo, { headers: cabeceras })
    return respuesta.data
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const cuerpo = error.response?.data
      throw new ErrorApi(error.response?.status ?? 0, cuerpo?.mensaje || (error.response ? 'No se pudo completar la solicitud.' : 'No se pudo conectar con el servidor. Inténtalo de nuevo.'), cuerpo?.campos)
    }
    throw error
  }
}

async function escribir<T>(ruta: string, cuerpo?: object): Promise<T> {
  const csrf = await solicitar<{ headerName: string; token: string }>('/api/auth/csrf')
  return solicitar<T>(ruta, cuerpo, { [csrf.headerName]: csrf.token })
}

export const apiAutenticacion = {
  obtenerUsuarioActual: () => solicitar<Usuario>('/api/auth/me'),
  iniciarSesion: (correo: string, contrasena: string) => escribir<void>('/api/auth/login', { email: correo, password: contrasena }),
  registrar: (nombres: string, correoElectronico: string, contrasena: string) => escribir<Usuario>('/api/auth/registro', { nombres, correoElectronico, password: contrasena }),
  cerrarSesion: () => escribir<void>('/logout'),
}
