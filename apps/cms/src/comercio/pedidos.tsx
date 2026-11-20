import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import type { ContextoTienda } from '@/paginas/panel'
import { api } from './datos'
import { Aviso, Bloque, EstadoCarga, useCarga, useOperacion } from './comunes'

type Pedido = { id: string; estado: string; fecha: string; subtotal: number; impuesto: number; total: number }
type Detalle = { pedido: Pedido; receptor: string; telefono: string | null; direccion: string; referencia: string | null; departamento: string; provincia: string; distrito: string; items: { producto: string; presentacion: string | null; cantidad: number; precio: number; total: number }[] }
const precio = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)
export function PedidosTienda() {
  const { apiBase } = useOutletContext<ContextoTienda>()
  const { datos, error, recargar } = useCarga(() => api.leer<Pedido[]>(`${apiBase}/pedidos`), [apiBase])
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const op = useOperacion()
  return <Bloque titulo="Pedidos de la tienda"><Aviso error={op.error} />{detalle ? <div className="space-y-3"><Button variant="outline" onClick={() => setDetalle(null)}>Volver a pedidos</Button><h3 className="font-semibold">Pedido {detalle.pedido.id}</h3><p>Estado: {detalle.pedido.estado}</p><p>Receptor: {detalle.receptor} {detalle.telefono && `· ${detalle.telefono}`}</p><p>{detalle.direccion}, {detalle.distrito}, {detalle.provincia}, {detalle.departamento} {detalle.referencia && `· ${detalle.referencia}`}</p><ul>{detalle.items.map((item, i) => <li key={i}>{item.producto} {item.presentacion} · {item.cantidad} × {precio(item.precio)} = {precio(item.total)}</li>)}</ul><p>Subtotal: {precio(detalle.pedido.subtotal)} · IGV: {precio(detalle.pedido.impuesto)} · Total: {precio(detalle.pedido.total)}</p></div> : error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : datos.length === 0 ? <p>La tienda aún no tiene pedidos.</p> : datos.map(p => <div className="flex flex-wrap items-center justify-between gap-3 rounded border p-3" key={p.id}><div><strong>{p.id}</strong><p>{p.estado} · {precio(p.total)} · {new Date(p.fecha).toLocaleDateString('es-PE')}</p></div><Button variant="outline" disabled={op.ocupado} onClick={() => void op.ejecutar(async () => setDetalle(await api.leer<Detalle>(`${apiBase}/pedidos/${p.id}`)))}>Ver detalle</Button></div>)}</Bloque>
}
