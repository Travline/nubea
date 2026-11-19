import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { ErrorApi, apiAutenticacion } from './api'
import type { Usuario } from './api'

type Sesion = { usuario: Usuario | null; estado: 'cargando' | 'listo' | 'error'; actualizar: () => Promise<void>; limpiar: () => void }
const ContextoSesion = createContext<Sesion | null>(null)

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [usuario, establecerUsuario] = useState<Usuario | null>(null)
  const [estado, establecerEstado] = useState<Sesion['estado']>('cargando')
  const cargar = useCallback(async () => {
    try { establecerUsuario(await apiAutenticacion.obtenerUsuarioActual()); establecerEstado('listo') }
    catch (error) {
      establecerUsuario(null)
      establecerEstado(error instanceof ErrorApi && error.codigoEstado === 401 ? 'listo' : 'error')
      throw error
    }
  }, [])
  const actualizar = useCallback(async () => { establecerEstado('cargando'); await cargar() }, [cargar])
  useEffect(() => {
    let activo = true
    apiAutenticacion.obtenerUsuarioActual().then(
      actual => { if (activo) { establecerUsuario(actual); establecerEstado('listo') } },
      error => { if (activo) { establecerUsuario(null); establecerEstado(error instanceof ErrorApi && error.codigoEstado === 401 ? 'listo' : 'error') } },
    )
    return () => { activo = false }
  }, [])
  useEffect(() => {
    const vencer = () => { establecerUsuario(null); establecerEstado('listo') }
    window.addEventListener('sesion-vencida', vencer)
    return () => window.removeEventListener('sesion-vencida', vencer)
  }, [])
  return <ContextoSesion.Provider value={{ usuario, estado, actualizar, limpiar: () => { establecerUsuario(null); establecerEstado('listo') } }}>{children}</ContextoSesion.Provider>
}
export function useSesion() {
  const valor = useContext(ContextoSesion)
  if (!valor) throw new Error('ProveedorSesion no disponible')
  return valor
}
