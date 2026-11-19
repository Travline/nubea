import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { mensajeError } from '@/autenticacion/api'
import { Button } from '@/components/ui/button'

export const campo = 'w-full rounded-md border border-input bg-background px-3 py-2 text-sm'
export function Aviso({ error }: { error: string }) { return error ? <p role="alert" className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</p> : null }
export function Bloque({ titulo, children }: { titulo: string; children: ReactNode }) { return <section className="space-y-4 rounded-xl border bg-card p-5"><h2 className="text-lg font-semibold">{titulo}</h2>{children}</section> }
export function useOperacion() {
  const [error, establecerError] = useState('')
  const [ocupado, establecerOcupado] = useState(false)
  async function ejecutar(accion: () => Promise<void>) {
    establecerError(''); establecerOcupado(true)
    try { await accion() } catch (err) { establecerError(mensajeError(err)) } finally { establecerOcupado(false) }
  }
  return { error, ocupado, ejecutar }
}
export function useCarga<T>(cargar: () => Promise<T>, dependencias: string[]) {
  const [resultado, establecerResultado] = useState<{ clave: string; datos: T | null; error: string } | null>(null)
  const [version, establecerVersion] = useState(0)
  const recargar = useCallback(() => establecerVersion(v => v + 1), [])
  const clave = dependencias.join('|')
  useEffect(() => {
    let activo = true
    cargar().then(valor => { if (activo) establecerResultado({ clave, datos: valor, error: '' }) }, err => { if (activo) establecerResultado({ clave, datos: null, error: mensajeError(err) }) })
    return () => { activo = false }
    // La clave identifica los parámetros de la consulta.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave, version])
  function actualizarDatos(actualizar: (datos: T) => T) {
    establecerResultado(actual => actual?.clave === clave && actual.datos !== null
      ? { ...actual, datos: actualizar(actual.datos) }
      : actual)
  }
  return { datos: resultado?.clave === clave ? resultado.datos : null, error: resultado?.clave === clave ? resultado.error : '', recargar, actualizarDatos }
}
export function EstadoCarga({ error, recargar }: { error: string; recargar: () => void }) { return error ? <div className="space-y-3"><Aviso error={error} /><Button variant="outline" onClick={recargar}>Reintentar</Button></div> : <p role="status">Cargando...</p> }
