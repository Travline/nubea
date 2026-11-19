import axios from 'axios'

export interface User {
  idUsuario: string
  nombres: string
  correoElectronico: string
  roles: string[]
}

export class ApiError extends Error {
  status: number
  fields?: Record<string, string>
  constructor(status: number, message: string, fields?: Record<string, string>) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

const client = axios.create({ withCredentials: true, withXSRFToken: false, headers: { 'Cache-Control': 'no-store' } })

async function request<T>(path: string, body?: object, headers?: Record<string, string>): Promise<T> {
  try {
    const response = body === undefined && !headers
      ? await client.get<T>(path)
      : await client.post<T>(path, body, { headers })
    return response.data
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const body = error.response?.data
      throw new ApiError(error.response?.status ?? 0, body?.mensaje || (error.response ? 'No se pudo completar la solicitud.' : 'No se pudo conectar con el servidor. Inténtalo de nuevo.'), body?.campos)
    }
    throw error
  }
}

async function write<T>(path: string, body?: object): Promise<T> {
  const csrf = await request<{ headerName: string; token: string }>('/api/auth/csrf')
  return request<T>(path, body, { [csrf.headerName]: csrf.token })
}

export const authApi = {
  me: () => request<User>('/api/auth/me'),
  login: (email: string, password: string) => write<void>('/api/auth/login', { email, password }),
  register: (nombres: string, correoElectronico: string, password: string) => write<User>('/api/auth/registro', { nombres, correoElectronico, password }),
  logout: () => write<void>('/logout'),
}
