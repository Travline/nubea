import type { EstadoPago } from './pago'
import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { actualizarCarrito, enlaceSesion, ErrorSolicitud, errorDeCampo, esTiendaNoDisponible, mensajeDeError, solicitar } from './api'
import { avisoProductosApartados, motivoLineaNoDisponible } from './estados'
import { TiendaNoDisponible } from './vitrina'
import { MapPin, ShoppingBag, Trash2 } from 'lucide-react'
import { MarcoTienda, Volver } from './diseno'
import { precio } from './api'
import { FormularioDireccion, textoDireccion, type Direccion } from './direcciones'

type Linea = { idPresentacion: string; idProducto: string; producto: string; opciones: string; cantidad: number; precio: number; disponible: boolean; valida: boolean; unidadesDisponibles?: number }
/** totalAPagar, subtotal e igv cuentan solo las líneas que se pueden comprar (mismo cálculo que el checkout). */
type Carrito = { items: Linea[]; total: number; subtotal: number | null; igv: number | null; tasaIgv: number | null; totalAPagar: number }
const comprable = (l: Linea) => l.disponible && l.valida

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
  const [agregando, setAgregando] = useState(false)
  const [direccionesCargadas, setDireccionesCargadas] = useState(false)
  const [noDisponible, setNoDisponible] = useState(false)
  const [erroresLinea, setErroresLinea] = useState<Record<string, string>>({})
  const [confirmando, setConfirmando] = useState(false)
  const cargar = useCallback(async () => {
    try { setError(''); setCarrito(await solicitar<Carrito>(base)); setSesion(true) }
    catch (e) {
      if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false)
      else if (esTiendaNoDisponible(e)) setNoDisponible(true)
      else setError(mensajeDeError(e, 'No se pudo cargar el carrito.'))
    }
  }, [base])
  useEffect(() => { void Promise.resolve().then(cargar) }, [cargar])
  useEffect(() => {
    let activo = true
    solicitar<Direccion[]>('/api/cliente/direcciones').then(datos => {
      if (!activo) return
      setDirecciones(datos)
      setDireccionesCargadas(true)
      setAgregando(datos.length === 0)
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
    setOcupado(true); setError(''); setErroresLinea(actuales => ({ ...actuales, [item.idPresentacion]: '' }))
    try { setCarrito(await solicitar<Carrito>(`${base}/items/${item.idPresentacion}`, cantidad ? 'PUT' : 'DELETE', cantidad ? { cantidad } : undefined)); actualizarCarrito() }
    catch (e) {
      if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false)
      else if (esTiendaNoDisponible(e)) setNoDisponible(true)
      // El error de cantidad se muestra junto a la línea; el resto, arriba.
      else if (errorDeCampo(e, 'cantidad')) setErroresLinea(actuales => ({ ...actuales, [item.idPresentacion]: errorDeCampo(e, 'cantidad')! }))
      else setError(mensajeDeError(e, 'No se pudo actualizar el carrito.'))
    }
    finally { setOcupado(false) }
  }
  async function pagar() {
    if (!carrito) return
    setOcupado(true); setError('')
    try {
      const previo = sessionStorage.getItem(guardado)
      const cuerpo = previo ? JSON.parse(previo) as { idDireccion: string; totalEsperado: number; clave: string } : {
        // Checkout parcial: se cobran solo las líneas disponibles (totalAPagar); las demás quedan en el carrito.
        idDireccion: direccion, totalEsperado: carrito.totalAPagar, clave: crypto.randomUUID(),
      }
      sessionStorage.setItem(guardado, JSON.stringify(cuerpo)); setPendiente(true)
      const pago = await solicitar<EstadoPago>(`/api/compra/tiendas/${encodeURIComponent(slug)}/checkout`, 'POST', cuerpo)
      sessionStorage.removeItem(guardado); setPendiente(false); actualizarCarrito()
      if (pago.checkoutUrl) window.location.assign(pago.checkoutUrl)
      else navegar(`/${slug}/pedidos/${pago.idPedido}`, { state: { productosApartados: pago.productosApartados ?? 0 } })
    } catch (e) {
      // Una respuesta de negocio rechazada no creó una compra; un fallo de red conserva la clave.
      if (e instanceof ErrorSolicitud && [400, 404, 422].includes(e.estado)) { sessionStorage.removeItem(guardado); setPendiente(false) }
      if (esTiendaNoDisponible(e)) setNoDisponible(true)
      else setError(mensajeDeError(e, 'No se pudo confirmar el inicio del pago. Reintenta con la misma solicitud.'))
      setConfirmando(false); void cargar()
    } finally { setOcupado(false) }
  }
  const unidades = carrito ? carrito.items.reduce((total, item) => total + item.cantidad, 0) : 0
  const lineasComprables = carrito ? carrito.items.filter(comprable) : []
  const unidadesComprables = lineasComprables.reduce((total, item) => total + item.cantidad, 0)
  const apartados = carrito ? carrito.items.length - lineasComprables.length : 0
  const soloPorStock = !!carrito && carrito.items.every(l => comprable(l) || l.valida)
  const direccionElegida = direcciones.find(d => d.id === direccion)
  const puedePagar = checkout.disponible && !ocupado && !!direccion && lineasComprables.length > 0
  if (noDisponible) return <TiendaNoDisponible slug={slug} />
  return <MarcoTienda slug={slug}><div className="pagina-interior pagina-carrito">
    <Volver to={`/${slug}`}>Volver a la tienda</Volver>
    <div className="carrito-cabecera">
      <h1>Tu carrito</h1>
      <Link className="enlace-secundario" to={`/${slug}/direcciones`}><MapPin size={16} /> Mis direcciones</Link>
    </div>
    {!sesion ? <div className="aviso-tarjeta"><p>Para usar el carrito, <Link to={enlaceSesion(slug)}>inicia sesión o crea una cuenta</Link>.</p></div> : <>
      {error && <p role="alert" className="mensaje-error">{error}</p>}
      {pendiente && <div className="aviso-tarjeta"><p>Hay una solicitud de compra sin confirmar.</p><button disabled={ocupado || !carrito} onClick={() => void pagar()}>Recuperar compra</button></div>}
      {!carrito && !error && <p role="status" className="cargando">Cargando carrito...</p>}
      {carrito && (carrito.items.length === 0 ? <div className="estado-vacio"><ShoppingBag size={26} /><p>Tu carrito está vacío.</p><Link className="boton boton-primario" to={`/${slug}`}>Explorar productos</Link></div> : <div className="carrito-diseno">
        <div className="carrito-principal">
          <section className="bloque">
            <h2 className="bloque-titulo">Productos <span>{unidades} {unidades === 1 ? 'unidad' : 'unidades'}</span></h2>
            <ul className="lista-panel">{carrito.items.map(item => <li className={`linea-carrito linea-producto${comprable(item) ? '' : ' linea-no-disponible'}`} key={item.idPresentacion}>
              <div className="linea-info">
                <Link to={`/${slug}/productos/${item.idProducto}`}><strong>{item.producto}</strong></Link>
                {item.opciones && <p className="linea-opciones">{item.opciones}</p>}
                <p>{precio(item.precio)} por unidad</p>
                {!comprable(item) && <p role="status" className="linea-alerta">{motivoLineaNoDisponible(item)} · No se incluye en el total</p>}
                {erroresLinea[item.idPresentacion] && <p role="alert" className="error-campo">{erroresLinea[item.idPresentacion]}</p>}
              </div>
              <div className="linea-acciones">
                <label className="campo-cantidad">Cantidad <input type="number" min="1" max="999" value={item.cantidad} disabled={ocupado} onChange={e => { const n = Number(e.target.value); if (Number.isInteger(n) && n >= 1 && n <= 999) void modificar(item, n) }} /></label>
                <strong className="linea-subtotal">{comprable(item) ? precio(item.precio * item.cantidad) : <s>{precio(item.precio * item.cantidad)}</s>}</strong>
                <button className="boton-quitar" disabled={ocupado} onClick={() => void modificar(item, 0)}><Trash2 size={16} /> Quitar</button>
              </div>
            </li>)}</ul>
          </section>
          <section className="bloque">
            <h2 className="bloque-titulo">Dirección de entrega</h2>
            {direcciones.length > 0 && <label className="campo-select">Selecciona una dirección <select value={direccion} onChange={e => setDireccion(e.target.value)}><option value="">Selecciona una dirección</option>{direcciones.map(d => <option key={d.id} value={d.id}>{d.alias ? `${d.alias}: ` : ''}{textoDireccion(d)}</option>)}</select></label>}
            {direccionesCargadas && !agregando && <button type="button" className="boton-secundario" onClick={() => setAgregando(true)}>Agregar dirección</button>}
            {agregando && <FormularioDireccion alGuardar={d => { setDirecciones(actuales => [d, ...actuales.map(a => d.principal ? { ...a, principal: false } : a)]); setDireccion(d.id); setAgregando(false) }} alCancelar={direcciones.length ? () => setAgregando(false) : undefined} />}
            <p className="enlace-pequeno"><Link to={`/${slug}/direcciones`}>Administrar direcciones</Link></p>
          </section>
        </div>
        <aside className="carrito-resumen">
          <h2 className="bloque-titulo">Resumen</h2>
          <div className="resumen-fila"><span>Productos a comprar</span><span>{unidadesComprables}</span></div>
          {carrito.subtotal !== null && <div className="resumen-fila"><span>Subtotal</span><span>{precio(carrito.subtotal)}</span></div>}
          {carrito.igv !== null && <div className="resumen-fila"><span>IGV{carrito.tasaIgv !== null ? ` (${Number(carrito.tasaIgv)}%)` : ''}</span><span>{precio(carrito.igv)}</span></div>}
          <div className="resumen-fila resumen-total"><span>Total a pagar</span><span>{precio(carrito.totalAPagar)}</span></div>
          <p className="resumen-nota">Los precios incluyen IGV. Se verificarán al finalizar la compra.</p>
          {apartados > 0 && <p className="resumen-aviso">{avisoProductosApartados(apartados, soloPorStock, false)}</p>}
          {checkout.ambiente === 'PRUEBA' && <p className="resumen-aviso">Esta tienda está en modo de prueba: no se mueve dinero real.</p>}
          {!checkout.disponible && <p className="resumen-aviso">Los pagos aún no están disponibles en esta tienda.</p>}
          {!direccion && lineasComprables.length > 0 && <p className="resumen-nota">Elige una dirección de entrega para continuar.</p>}
          <button className="boton boton-primario boton-pagar" disabled={!puedePagar} onClick={() => setConfirmando(true)}>Continuar al pago</button>
        </aside>
      </div>)}
      {confirmando && carrito && <div className="confirmacion-fondo" role="dialog" aria-modal="true" aria-labelledby="titulo-confirmacion">
        <div className="confirmacion-compra bloque">
          <h2 id="titulo-confirmacion" className="bloque-titulo">Revisa tu compra</h2>
          <ul className="confirmacion-lista">{lineasComprables.map(l => <li key={l.idPresentacion}><span>{l.cantidad} × {l.producto}{l.opciones ? ` · ${l.opciones}` : ''}</span><span>{precio(l.precio * l.cantidad)}</span></li>)}</ul>
          {direccionElegida && <p className="confirmacion-direccion"><strong>Entrega en:</strong> {direccionElegida.alias ? `${direccionElegida.alias}: ` : ''}{textoDireccion(direccionElegida)}</p>}
          {carrito.subtotal !== null && <div className="resumen-fila"><span>Subtotal</span><span>{precio(carrito.subtotal)}</span></div>}
          {carrito.igv !== null && <div className="resumen-fila"><span>IGV{carrito.tasaIgv !== null ? ` (${Number(carrito.tasaIgv)}%)` : ''}</span><span>{precio(carrito.igv)}</span></div>}
          <div className="resumen-fila resumen-total"><span>Total a pagar</span><span>{precio(carrito.totalAPagar)}</span></div>
          {apartados > 0 && <p className="resumen-aviso">{avisoProductosApartados(apartados, soloPorStock, false)}</p>}
          {checkout.ambiente === 'PRUEBA' && <p className="resumen-aviso">Compra de prueba: no se mueve dinero real.</p>}
          <div className="confirmacion-acciones">
            <button type="button" className="boton-secundario" disabled={ocupado} onClick={() => setConfirmando(false)}>Volver al carrito</button>
            <button type="button" className="boton boton-primario boton-pagar" disabled={ocupado} onClick={() => void pagar()}>{ocupado ? 'Preparando pago…' : 'Pagar con Mercado Pago'}</button>
          </div>
        </div>
      </div>}
    </>}
  </div></MarcoTienda>
}
