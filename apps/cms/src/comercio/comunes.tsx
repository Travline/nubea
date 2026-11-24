import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import type { ReactNode } from 'react'
import { ErrorApi, mensajeError } from '@/autenticacion/api'
import { Button } from '@/components/ui/button'

export const campo = 'w-full rounded-md border border-input bg-background px-3 py-2 text-sm'
export function Aviso({ error }: { error: string }) { return error ? <p role="alert" className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</p> : null }
export function Bloque({ titulo, children }: { titulo: string; children: ReactNode }) { return <section className="space-y-4 rounded-xl border bg-card p-5"><h2 className="text-lg font-semibold">{titulo}</h2>{children}</section> }
/**
 * Ejecuta una acción contra la API y guarda su error.
 * {@code camposEnFormulario}: campos que el formulario muestra junto a su entrada con {@code errorDe}; el resto de
 * mensajes de {@code campos} (o el mensaje general) se muestran en {@code error}, para que nada quede oculto.
 */
export function useOperacion(camposEnFormulario: readonly string[] = []) {
  const [fallo, establecerFallo] = useState<unknown>(null)
  const [ocupado, establecerOcupado] = useState(false)
  async function ejecutar(accion: () => Promise<void>) {
    establecerFallo(null); establecerOcupado(true)
    try { await accion() } catch (err) { establecerFallo(err) } finally { establecerOcupado(false) }
  }
  const campos = fallo instanceof ErrorApi ? fallo.campos ?? {} : {}
  const restantes = Object.entries(campos).filter(([c]) => !camposEnFormulario.includes(c)).map(([, m]) => m)
  const error = !fallo ? '' : restantes.length ? restantes.join(' ') : Object.keys(campos).length ? 'Revisa los campos marcados.' : mensajeError(fallo)
  return { error, ocupado, ejecutar, fallo, errorDe: (campo: string) => campos[campo] ?? '', limpiar: () => establecerFallo(null) }
}
/** Mensaje de error debajo de una entrada. */
export function ErrorCampo({ mensaje }: { mensaje: string }) { return mensaje ? <span role="alert" className="text-xs font-normal text-destructive">{mensaje}</span> : null }
/** Borde rojo para una entrada con error. */
export const conError = (mensaje: string) => mensaje ? ' border-destructive focus:border-destructive focus:ring-destructive/20' : ''
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

/** true si la tienda abierta en el panel está suspendida o cerrada (solo consulta). */
export function useSoloLectura() { return !!useOutletContext<{ soloLectura?: boolean } | undefined>()?.soloLectura }
/** Deshabilita todas las entradas y botones de su interior cuando la tienda no admite cambios. */
export function ZonaEdicion({ children, className = 'contents' }: { children: ReactNode; className?: string }) {
  const soloLectura = useSoloLectura()
  return <fieldset disabled={soloLectura} className={`m-0 min-w-0 border-0 p-0 ${className}`}>{children}</fieldset>
}
