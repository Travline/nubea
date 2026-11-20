import type { EstadoPago } from './pago'
import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { actualizarCarrito, enlaceSesion, ErrorSolicitud, solicitar } from './api'
import { MapPin, ShoppingBag, Trash2 } from 'lucide-react'
import { MarcoTienda, Volver } from './diseno'
import { precio } from './api'

type Linea = { idPresentacion: string; idProducto: string; producto: string; opciones: string; cantidad: number; precio: number; disponible: boolean; valida: boolean }
type Carrito = { items: Linea[]; total: number }
type Direccion = { id: string; alias: string | null; direccion: string; distrito: string; provincia: string; departamento: string; principal: boolean }

export function CarritoTienda() {
  const { slug = '' } = useParams()
  const navegar = useNavigate()
  const guardado = `checkout:${slug}`
  const [pendiente, setPendiente] = useState(() => sessionStorage.getItem(guardado) !== null)
  const base = `/api/compra/tiendas/${encodeURIComponent(slug)}/carrito`
  const [carrito, setCarrito] = useState<Carrito | null>(null)
  const [error, setError] = useState('')
  const [checkout, setCheckout] = useState<{ disponible: boolean; ambiente: string | null }>({ disponible: false, ambiente: null })
  const [sesion, setSesion] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  const [direcciones, setDirecciones] = useState<Direccion[]>([])
  const [direccion, setDireccion] = useState('')
  const cargar = useCallback(async () => {
    try { setError(''); setCarrito(await solicitar<Carrito>(base)); setSesion(true) }
    catch (e) { if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false); else setError(e instanceof Error ? e.message : 'No se pudo cargar el carrito.') }
  }, [base])
  useEffect(() => { void Promise.resolve().then(cargar) }, [cargar])
  useEffect(() => {
    let activo = true
    solicitar<Direccion[]>('/api/cliente/direcciones').then(datos => {
      if (!activo) return
      setDirecciones(datos)
      setDireccion(datos.find(d => d.principal)?.id || '')
    }).catch(e => {
      if (activo && !(e instanceof ErrorSolicitud && e.estado === 401)) setError(e instanceof Error ? e.message : 'No se pudieron cargar las direcciones.')
    })
    return () => { activo = false }
  }, [])
  useEffect(() => {
    let activo = true
    solicitar<{ disponible: boolean; ambiente: string | null }>(`/api/compra/tiendas/${encodeURIComponent(slug)}/checkout`)
      .then(datos => { if (activo) setCheckout(datos) }).catch(() => { if (activo) setCheckout({ disponible: false, ambiente: null }) })
    return () => { activo = false }
  }, [slug])
  async function modificar(item: Linea, cantidad: number) {
    setOcupado(true); setError('')
    try { setCarrito(await solicitar<Carrito>(`${base}/items/${item.idPresentacion}`, cantidad ? 'PUT' : 'DELETE', cantidad ? { cantidad } : undefined)); actualizarCarrito() }
    catch (e) { if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false); else setError(e instanceof Error ? e.message : 'No se pudo actualizar el carrito.') }
    finally { setOcupado(false) }
  }
  async function pagar() {
    if (!carrito) return
    setOcupado(true); setError('')
    try {
      const previo = sessionStorage.getItem(guardado)
      const cuerpo = previo ? JSON.parse(previo) as { idDireccion: string; totalEsperado: number; clave: string } : {
        idDireccion: direccion, totalEsperado: carrito.total, clave: crypto.randomUUID(),
      }
      sessionStorage.setItem(guardado, JSON.stringify(cuerpo)); setPendiente(true)
      const pago = await solicitar<EstadoPago>(`/api/compra/tiendas/${encodeURIComponent(slug)}/checkout`, 'POST', cuerpo)
      sessionStorage.removeItem(guardado); setPendiente(false); actualizarCarrito()
      if (pago.checkoutUrl) window.location.assign(pago.checkoutUrl)
      else navegar(`/${slug}/pedidos/${pago.idPedido}`)
    } catch (e) {
      // Una respuesta de negocio rechazada no creó una compra; un fallo de red conserva la clave.
      if (e instanceof ErrorSolicitud && [400, 404, 422].includes(e.estado)) { sessionStorage.removeItem(guardado); setPendiente(false) }
      setError(e instanceof Error ? e.message : 'No se pudo confirmar el inicio del pago. Reintenta con la misma solicitud.')
    } finally { setOcupado(false) }
  }
  return <MarcoTienda slug={slug}><div className="pagina-interior"><Volver to={`/${slug}`}>Volver a la tienda</Volver><h1>Tu carrito</h1><p><Link className="enlace-flecha" to={`/${slug}/direcciones`}><MapPin size={16} /> Mis direcciones</Link></p>{!sesion ? <p>Para usar el carrito, <Link to={enlaceSesion(slug)}>inicia sesión o crea una cuenta</Link>.</p> : <>{error && <p role="alert">{error}</p>}{pendiente && <p>Hay una solicitud de compra sin confirmar. <button disabled={ocupado || !carrito} onClick={() => void pagar()}>Recuperar compra</button></p>}{!carrito && !error && <p role="status">Cargando carrito...</p>}{carrito && (carrito.items.length === 0 ? <div className="estado-vacio"><ShoppingBag size={26} /><p>Tu carrito está vacío.</p><Link to={`/${slug}`}>Explorar productos</Link></div> : <><ul className="lista-panel">{carrito.items.map(item => <li className="linea-carrito" key={item.idPresentacion}><div><Link to={`/${slug}/productos/${item.idProducto}`}><strong>{item.producto}</strong></Link><p>{item.opciones}</p><p>{precio(item.precio)} por unidad · Subtotal: {precio(item.precio * item.cantidad)}</p>{(!item.valida || !item.disponible) && <p role="status">Esta presentación o la cantidad solicitada no está disponible.</p>}</div><div><label>Cantidad <input type="number" min="1" max="999" value={item.cantidad} disabled={ocupado} onChange={e => { const n = Number(e.target.value); if (Number.isInteger(n) && n >= 1 && n <= 999) void modificar(item, n) }} /></label><button disabled={ocupado} onClick={() => void modificar(item, 0)}><Trash2 size={16} /> Quitar</button></div></li>)}</ul><h2>Dirección de entrega</h2>{direcciones.length ? <label>Selecciona una dirección <select value={direccion} onChange={e => setDireccion(e.target.value)}><option value="">Selecciona una dirección</option>{direcciones.map(d => <option key={d.id} value={d.id}>{d.alias ? `${d.alias}: ` : ''}{d.direccion}, {d.distrito}, {d.provincia}, {d.departamento}</option>)}</select></label> : <p>Agrega una dirección para tu compra.</p>}<p><Link to={`/${slug}/direcciones`}>Agregar o editar direcciones</Link></p><h2>Resumen</h2><p className="precio">Total: {precio(carrito.total)}</p><p>El total incluye impuestos. Los precios y la disponibilidad se verificarán al finalizar la compra.</p>{checkout.ambiente === 'PRUEBA' && <p>Esta tienda está en modo de prueba: no se mueve dinero real.</p>}{!checkout.disponible && <p>Los pagos aún no están disponibles en esta tienda.</p>}<button disabled={!checkout.disponible || ocupado || !direccion || carrito.items.some(i => !i.valida || !i.disponible)} onClick={() => void pagar()}>{ocupado ? 'Preparando pago…' : 'Pagar con Mercado Pago'}</button></>)}</>}</div></MarcoTienda>
}
