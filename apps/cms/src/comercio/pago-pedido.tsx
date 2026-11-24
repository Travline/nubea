import { useEffect, useState } from 'react'
import { api } from './datos'
import { Button } from '@/components/ui/button'
import { mensajeError } from '@/autenticacion/api'
import { ZonaEdicion } from './comunes'
import { etiquetaPago, textoAmbientePago } from './estados'

type Pago = { estado: string; estadoPago: string | null; ambiente: string; incidencia: string | null; referencia: string | null }
export function PagoPedido({ base, pedido, total }: { base: string; pedido: string; total: number }) {
  const [pago, setPago] = useState<Pago | null>(null)
  const [error, setError] = useState('')
  const [solicitud] = useState<{ clave: string; reponerStock: boolean } | null>(() => {
    try { return JSON.parse(sessionStorage.getItem(`reembolso:${pedido}`) || 'null') } catch { return null }
  })
  const [enviada, setEnviada] = useState(solicitud !== null)
  const [reponer, setReponer] = useState(solicitud?.reponerStock || false)
  const [confirmar, setConfirmar] = useState(false)
  const [ocupado, setOcupado] = useState(false)
  const [clave] = useState(() => solicitud?.clave || crypto.randomUUID())
  useEffect(() => {
    let activo = true
    let temporizador: ReturnType<typeof setTimeout>
    async function cargar() {
      try {
        const datos = await api.leer<Pago>(`${base}/pedidos/${pedido}/pago`)
        if (activo) { setPago(datos); temporizador = setTimeout(() => void cargar(), 10000) }
      } catch { if (activo) setError('No se pudo consultar el estado del pago.') }
    }
    void cargar()
    return () => { activo = false; clearTimeout(temporizador) }
  }, [base, pedido])
  async function devolver() {
    setOcupado(true); setError('')
    sessionStorage.setItem(`reembolso:${pedido}`, JSON.stringify({ clave, reponerStock: reponer })); setEnviada(true)
    try {
      const datos = await api.crear<Pago>(`${base}/pedidos/${pedido}/reembolsos`, { clave, reponerStock: reponer })
      setPago(datos); setConfirmar(false)
    } catch (e) { setError(mensajeError(e) || 'No se pudo confirmar la devolución. Reintenta para consultar la misma operación.') }
    finally { setOcupado(false) }
  }
  return <section className="space-y-3" aria-live="polite">{error && <p role="alert">{error}</p>}{pago && <>
    <p>Pago: <strong>{etiquetaPago(pago.estadoPago).texto}</strong>{textoAmbientePago(pago.ambiente) && <> · {textoAmbientePago(pago.ambiente)}{pago.ambiente === 'PRUEBA' && ' (sin dinero real)'}</>}</p>
    {pago.referencia && <p>Referencia en Mercado Pago: {pago.referencia}</p>}
    {pago.incidencia && <p>Requiere atención: {pago.incidencia}. Comprueba la vinculación y la operación en Mercado Pago.</p>}
    {pago.estadoPago === 'APROBADO' && !confirmar && <ZonaEdicion><Button onClick={() => setConfirmar(true)}>Devolver el dinero del pedido</Button></ZonaEdicion>}
    {confirmar && <div className="space-y-3 rounded border p-3"><p>Se devolverán {new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(total)} al comprador.</p>
      <label><input type="checkbox" checked={reponer} disabled={ocupado || enviada} onChange={e => setReponer(e.target.checked)} /> Volver a poner las unidades a la venta</label>
      <div className="flex gap-3"><Button disabled={ocupado} onClick={() => void devolver()}>{ocupado ? 'Procesando…' : 'Confirmar devolución'}</Button><Button variant="outline" disabled={ocupado} onClick={() => setConfirmar(false)}>Volver</Button></div></div>}
  </>}</section>
}
