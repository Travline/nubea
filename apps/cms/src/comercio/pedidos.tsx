import { MapaLugar } from './mapa'
import { PagoPedido } from './pago-pedido'
import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import type { ContextoTienda } from '@/paginas/panel'
import { api } from './datos'
import { Aviso, Bloque, EstadoCarga, useCarga, useOperacion } from './comunes'
import { claseTono, etiquetaPedido } from './estados'

type Pedido = { id: string; numero?: number | null; estado: string; fecha: string; subtotal: number; impuesto: number; total: number }
type Detalle = { pedido: Pedido; receptor: string; telefono: string | null; direccion: string; referencia: string | null; departamento: string; provincia: string; distrito: string; placeId: string | null; items: { producto: string; presentacion: string | null; cantidad: number; precio: number; total: number }[] }
/** "Pedido #1001": número corto por tienda; nunca el identificador largo. */
const tituloPedido = (p: Pedido) => p.numero ? `Pedido #${p.numero}` : 'Pedido'
function EstadoPedido({ estado }: { estado: string }) { const e = etiquetaPedido(estado); return <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${claseTono[e.tono].clase}`}>{e.texto}</span> }
const precio = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)
export function PedidosTienda() {
  const { apiBase } = useOutletContext<ContextoTienda>()
  const { datos, error, recargar } = useCarga(() => api.leer<Pedido[]>(`${apiBase}/pedidos`), [apiBase])
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const op = useOperacion()
  return <Bloque titulo="Pedidos de la tienda"><Aviso error={op.error} />{detalle ? <div className="space-y-3"><Button variant="outline" onClick={() => setDetalle(null)}>Volver a pedidos</Button><div className="flex flex-wrap items-center gap-2.5"><h3 className="text-lg font-semibold">{tituloPedido(detalle.pedido)}</h3><EstadoPedido estado={detalle.pedido.estado} /></div><p className="text-sm text-muted-foreground">{new Date(detalle.pedido.fecha).toLocaleString('es-PE', { dateStyle: 'long', timeStyle: 'short' })}</p><PagoPedido key={detalle.pedido.id} base={apiBase} pedido={detalle.pedido.id} total={detalle.pedido.total} /><p>Receptor: {detalle.receptor} {detalle.telefono && `· ${detalle.telefono}`}</p><p>{[detalle.direccion, detalle.distrito, detalle.provincia, detalle.departamento].filter(Boolean).join(', ')} {detalle.referencia && `· ${detalle.referencia}`}</p>{detalle.placeId && <MapaLugar placeId={detalle.placeId} />}<ul>{detalle.items.map((item, i) => <li key={i}>{item.producto} {item.presentacion} · {item.cantidad} × {precio(item.precio)} = {precio(item.total)}</li>)}</ul><p>Subtotal: {precio(detalle.pedido.subtotal)} · IGV: {precio(detalle.pedido.impuesto)} · Total: {precio(detalle.pedido.total)}</p>{detalle.pedido.estado === 'PENDIENTE_PAGO' && <p className="text-sm text-muted-foreground">Mientras el pedido espera el pago, sus unidades quedan apartadas en tu inventario. Si el pago no llega a tiempo, el pedido se cancela y las unidades vuelven a estar disponibles.</p>}</div> : error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : datos.length === 0 ? <p>La tienda aún no tiene pedidos.</p> : datos.map(p => <div className="flex flex-wrap items-center justify-between gap-3 rounded border p-3" key={p.id}><div className="space-y-1"><div className="flex flex-wrap items-center gap-2"><strong>{tituloPedido(p)}</strong><EstadoPedido estado={p.estado} /></div><p className="text-sm text-muted-foreground">{precio(p.total)} · {new Date(p.fecha).toLocaleDateString('es-PE')}</p></div><Button variant="outline" disabled={op.ocupado} onClick={() => void op.ejecutar(async () => setDetalle(await api.leer<Detalle>(`${apiBase}/pedidos/${p.id}`)))}>Ver detalle</Button></div>)}</Bloque>
}
