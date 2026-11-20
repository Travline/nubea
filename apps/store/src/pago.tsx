import { useEffect, useState } from 'react'
import { ErrorSolicitud, solicitar } from './api'

export type EstadoPago = { idPedido: string; estado: string; estadoPago: string | null; checkoutUrl: string | null; ambiente: string; incidencia: string | null; referencia: string | null; vencimiento: string }
export function PagoPedido({ slug, pedido }: { slug: string; pedido: string }) {
  const [pago, setPago] = useState<EstadoPago | null>(null)
  const [error, setError] = useState('')
  useEffect(() => {
    let activo = true
    let temporizador: ReturnType<typeof setTimeout>
    async function cargar() {
      try {
        const datos = await solicitar<EstadoPago>(`/api/compra/tiendas/${encodeURIComponent(slug)}/pedidos/${pedido}/pago`)
        if (!activo) return
        setPago(datos); setError('')
        if (datos.estado === 'PENDIENTE_PAGO' || datos.estadoPago === 'REEMBOLSO_PENDIENTE') temporizador = setTimeout(() => void cargar(), 5000)
      } catch (e) {
        if (!activo) return
        if (!(e instanceof ErrorSolicitud && e.estado === 404)) setError('No se pudo actualizar el pago. Recarga para volver a consultar.')
      }
    }
    void cargar()
    return () => { activo = false; clearTimeout(temporizador) }
  }, [slug, pedido])
  return <section aria-live="polite">{error && <p role="alert">{error}</p>}{pago && <>
    {pago.ambiente === 'PRUEBA' && <p>Compra de prueba: no mueve dinero real.</p>}
    <p>Pedido: {pago.estado} · Pago: {pago.estadoPago || 'En preparación'}</p>
    {pago.estado === 'PENDIENTE_PAGO' && <p>Estamos esperando la confirmación de Mercado Pago. Puedes cerrar esta página y consultar el pedido después.</p>}
    {pago.checkoutUrl && <a className="boton" href={pago.checkoutUrl}>Continuar en Mercado Pago</a>}
    {pago.incidencia && <p>La confirmación está demorada. El pedido está registrado; no necesitas crear otra compra.</p>}
  </>}</section>
}
