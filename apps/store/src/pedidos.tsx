import { PagoPedido } from './pago'
import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { enlaceSesion, ErrorSolicitud, mensajeDeError, solicitar } from './api'
import { Package } from 'lucide-react'
import { MarcoTienda, Volver } from './diseno'
import { precio } from './api'
import { avisoProductosApartados, claseEstadoPedido, textoEstadoPedido } from './estados'

type Pedido = { id: string; estado: string; fecha: string; subtotal: number; impuesto: number; total: number; numero: number }
type Detalle = { pedido: Pedido; receptor: string; telefono: string | null; referencia: string | null; direccion: string; distrito: string | null; provincia: string | null; departamento: string | null; items: { producto: string; presentacion: string | null; cantidad: number; precio: number; total: number }[] }

/** Nunca se muestra el id interno: "Pedido #1001". */
const titulo = (p: Pedido) => `Pedido #${p.numero}`
const fecha = (iso: string) => new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium' }).format(new Date(iso))

function EtiquetaEstado({ estado }: { estado: string }) {
  return <span className={`etiqueta-estado ${claseEstadoPedido(estado)}`}>{textoEstadoPedido(estado)}</span>
}

export function Pedidos() {
  const { slug = '', idPedido } = useParams()
  const ubicacion = useLocation()
  const productosApartados = (ubicacion.state as { productosApartados?: number } | null)?.productosApartados ?? 0
  const [lista, setLista] = useState<Pedido[] | null>(null)
  const [detalle, setDetalle] = useState<Detalle | null>(null)
  const [error, setError] = useState('')
  const [sesion, setSesion] = useState(true)
  useEffect(() => {
    let activo = true
    void Promise.resolve().then(() => { if (activo) { setLista(null); setDetalle(null); setError('') } })
    // Los pedidos propios se ven aunque la tienda ya no esté disponible.
    const base = `/api/compra/tiendas/${encodeURIComponent(slug)}/pedidos`
    const peticion = idPedido ? solicitar<Detalle>(`${base}/${encodeURIComponent(idPedido)}`) : solicitar<Pedido[]>(base)
    peticion.then(d => { if (!activo) return; if (Array.isArray(d)) setLista(d); else setDetalle(d); setSesion(true) }).catch((e: unknown) => { if (!activo) return; if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false); else setError(mensajeDeError(e, 'No se pudieron cargar tus pedidos.')) })
    return () => { activo = false }
  }, [slug, idPedido])
  return <MarcoTienda slug={slug}><div className="pagina-interior">
    <Volver to={`/${slug}`}>Volver a la tienda</Volver>
    <h1>Mis pedidos</h1>
    {!sesion ? <p>Para consultar tus pedidos, <Link to={enlaceSesion(slug)}>inicia sesión</Link>.</p>
      : error ? <p role="alert">{error}</p>
      : detalle ? <>
        <Volver to={`/${slug}/pedidos`}>Todos mis pedidos</Volver>
        <h2>{titulo(detalle.pedido)} <EtiquetaEstado estado={detalle.pedido.estado} /></h2>
        <p className="resumen-nota">Realizado el {fecha(detalle.pedido.fecha)}</p>
        {productosApartados > 0 && <p className="aviso-tarjeta">{avisoProductosApartados(productosApartados, true, true)}</p>}
        <PagoPedido key={detalle.pedido.id} slug={slug} pedido={detalle.pedido.id} />
        <section className="bloque">
          <h3 className="bloque-titulo">Productos</h3>
          <ul>{detalle.items.map((item, i) => <li key={i}>{item.cantidad} × {item.producto}{item.presentacion ? ` · ${item.presentacion}` : ''} — {precio(item.total)}</li>)}</ul>
          <p>Subtotal: {precio(detalle.pedido.subtotal)} · IGV: {precio(detalle.pedido.impuesto)}</p>
          <p className="precio">Total: {precio(detalle.pedido.total)}</p>
        </section>
        <section className="bloque">
          <h3 className="bloque-titulo">Entrega</h3>
          <p>{detalle.receptor}</p>
          <p>{[detalle.direccion, detalle.distrito, detalle.provincia, detalle.departamento].filter(Boolean).join(', ')}</p>
          {detalle.referencia && <p>Referencia: {detalle.referencia}</p>}
          {detalle.telefono && <p>Teléfono: {detalle.telefono}</p>}
        </section>
      </>
      : lista ? lista.length
        ? <ul className="lista-panel">{lista.map(p => <li className="linea-carrito" key={p.id}><Link to={`/${slug}/pedidos/${p.id}`}>{titulo(p)}</Link><span>{fecha(p.fecha)} · <EtiquetaEstado estado={p.estado} /> · {precio(p.total)}</span></li>)}</ul>
        : <div className="estado-vacio"><Package size={26} /><p>Aún no tienes pedidos en esta tienda.</p></div>
      : <p role="status">Cargando pedidos...</p>}
  </div></MarcoTienda>
}
