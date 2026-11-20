import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { enlaceSesion, ErrorSolicitud, solicitar } from './api'

type Pedido = { id: string; estado: string; fecha: string; subtotal: number; impuesto: number; total: number }
type Detalle = { pedido: Pedido; receptor: string; direccion: string; distrito: string; provincia: string; departamento: string; items: { producto: string; presentacion: string | null; cantidad: number; precio: number; total: number }[] }
const precio = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)
export function Pedidos() {
  const { slug = '', idPedido } = useParams()
  const [lista, setLista] = useState<Pedido[] | null>(null)
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const [error, setError] = useState('')
  const [sesion, setSesion] = useState(true)
  useEffect(() => {
    let activo = true
    void Promise.resolve().then(() => { if (activo) { setLista(null); setDetalle(null); setError('') } })
    const base = `/api/compra/tiendas/${encodeURIComponent(slug)}/pedidos`
    const peticion = idPedido ? solicitar<Detalle>(`${base}/${encodeURIComponent(idPedido)}`) : solicitar<Pedido[]>(base)
    peticion.then(d => { if (!activo) return; if (Array.isArray(d)) setLista(d); else setDetalle(d); setSesion(true) }).catch((e: unknown) => { if (!activo) return; if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false); else setError(e instanceof Error ? e.message : 'No se pudieron cargar tus pedidos.') })
    return () => { activo = false }
  }, [slug, idPedido])
  return <main className="contenedor"><Link to={`/${slug}`}>← Volver a la tienda</Link><h1>Mis pedidos</h1>{!sesion ? <p>Para consultar tus pedidos, <a href={enlaceSesion(slug)}>inicia sesión</a>.</p> : error ? <p role="alert">{error}</p> : detalle ? <><Link to={`/${slug}/pedidos`}>← Todos mis pedidos</Link><h2>Pedido {detalle.pedido.id}</h2><p>Estado: {detalle.pedido.estado}</p><p>Receptor: {detalle.receptor}</p><p>{detalle.direccion}, {detalle.distrito}, {detalle.provincia}, {detalle.departamento}</p><ul>{detalle.items.map((item, i) => <li key={i}>{item.producto} {item.presentacion} · {item.cantidad} × {precio(item.precio)} = {precio(item.total)}</li>)}</ul><p>Subtotal: {precio(detalle.pedido.subtotal)} · IGV: {precio(detalle.pedido.impuesto)}</p><p className="precio">Total: {precio(detalle.pedido.total)}</p></> : lista ? lista.length ? <ul>{lista.map(p => <li className="linea-carrito" key={p.id}><Link to={`/${slug}/pedidos/${p.id}`}>Pedido {p.id}</Link><span>{p.estado} · {precio(p.total)}</span></li>)}</ul> : <p>Aún no tienes pedidos en esta tienda.</p> : <p role="status">Cargando pedidos...</p>}</main>
}
