import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { enlaceSesion, ErrorSolicitud, solicitar } from './api'

type Linea = { idPresentacion: string; producto: string; opciones: string; cantidad: number; precio: number; disponible: boolean; valida: boolean }
type Carrito = { items: Linea[]; total: number }
type Direccion = { id: string; alias: string | null; direccion: string; distrito: string; provincia: string; departamento: string; principal: boolean }
const precio = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)

export function CarritoTienda() {
  const { slug = '' } = useParams()
  const base = `/api/compra/tiendas/${encodeURIComponent(slug)}/carrito`
  const [carrito, setCarrito] = useState<Carrito | null>(null)
  const [error, setError] = useState('')
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
  async function modificar(item: Linea, cantidad: number) {
    setOcupado(true); setError('')
    try { setCarrito(await solicitar<Carrito>(`${base}/items/${item.idPresentacion}`, cantidad ? 'PUT' : 'DELETE', cantidad ? { cantidad } : undefined)) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo actualizar el carrito.') }
    finally { setOcupado(false) }
  }
  return <main className="contenedor"><Link to={`/${slug}`}>← Volver a la tienda</Link><h1>Tu carrito</h1><p><Link to={`/${slug}/direcciones`}>Mis direcciones</Link></p>{!sesion ? <p>Para usar el carrito, <a href={enlaceSesion(slug)}>inicia sesión o crea una cuenta</a>.</p> : <>{error && <p role="alert">{error}</p>}{!carrito && !error && <p role="status">Cargando carrito...</p>}{carrito && (carrito.items.length === 0 ? <p>Tu carrito está vacío.</p> : <><ul>{carrito.items.map(item => <li className="linea-carrito" key={item.idPresentacion}><div><strong>{item.producto}</strong><p>{item.opciones}</p><p>{precio(item.precio)} por unidad</p>{(!item.valida || !item.disponible) && <p role="status">Esta presentación o la cantidad solicitada no está disponible.</p>}</div><div><label>Cantidad <input type="number" min="1" max="999" value={item.cantidad} disabled={ocupado} onChange={e => { const n = Number(e.target.value); if (Number.isInteger(n) && n >= 1 && n <= 999) void modificar(item, n) }} /></label><button disabled={ocupado} onClick={() => void modificar(item, 0)}>Quitar</button></div></li>)}</ul><h2>Dirección de entrega</h2>{direcciones.length ? <label>Selecciona una dirección <select value={direccion} onChange={e => setDireccion(e.target.value)}><option value="">Selecciona una dirección</option>{direcciones.map(d => <option key={d.id} value={d.id}>{d.alias ? `${d.alias}: ` : ''}{d.direccion}, {d.distrito}, {d.provincia}, {d.departamento}</option>)}</select></label> : <p>Agrega una dirección para tu compra.</p>}<p><Link to={`/${slug}/direcciones`}>Agregar o editar direcciones</Link></p><h2>Resumen</h2><p className="precio">Total: {precio(carrito.total)}</p><p>El total incluye impuestos. Los precios y la disponibilidad se verificarán al finalizar la compra.</p><p>La finalización de compra aún no está disponible.</p></>)}</>}</main>
}
