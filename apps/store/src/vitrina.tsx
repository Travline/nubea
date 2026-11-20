import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { agregarAlCarrito, enlaceSesion, ErrorSolicitud } from './api'

type Presentacion = { id: string; precio: number; disponible: boolean; opciones: { nombre: string; valor: string }[] }
type Producto = { id: string; categoriaId: string; nombre: string; descripcion: string | null; imagenes: string[]; presentaciones: Presentacion[] }
type Datos = { nombre: string; plantilla: string; titulo: string | null; descripcion: string | null; mostrarCarrusel: boolean; banners: { url: string; enlace: string | null }[]; categorias: { id: string; nombre: string; slug: string }[]; productos: Producto[] }
const api = import.meta.env.VITE_API_URL?.trim() || (import.meta.env.DEV ? 'http://localhost:8080' : '')
const precio = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)

export function Vitrina() {
  const { slug = '', idProducto } = useParams()
  const [datos, setDatos] = useState<Datos | null>(null)
  const [error, setError] = useState('')
  const [intento, setIntento] = useState(0)
  const [banner, setBanner] = useState(0)
  useEffect(() => {
    const controlador = new AbortController()
    void Promise.resolve().then(() => { if (!controlador.signal.aborted) { setDatos(null); setError(''); setBanner(0) } })
    fetch(`${api}/api/publico/tiendas/${encodeURIComponent(slug)}`, { signal: controlador.signal })
      .then(res => { if (!res.ok) throw new Error(res.status === 404 ? 'Esta tienda no existe o no está disponible.' : 'No se pudo cargar la tienda.'); return res.json() as Promise<Datos> })
      .then(setDatos).catch(e => { if (e.name !== 'AbortError') setError(e.message === 'Failed to fetch' ? 'No se pudo conectar con la tienda.' : e.message) })
    return () => controlador.abort()
  }, [slug, intento])
  if (error) return <main className="contenedor"><h1>Tienda no disponible</h1><p role="alert">{error}</p><button onClick={() => setIntento(i => i + 1)}>Intentar nuevamente</button></main>
  if (!datos) return <main className="contenedor" role="status">Cargando tienda...</main>
  if (datos.plantilla !== 'minimal') return <main className="contenedor"><h1>{datos.nombre}</h1><p>La plantilla de esta tienda no está disponible.</p></main>
  const producto = datos.productos.find(p => p.id === idProducto)
  const actual = datos.banners[banner]
  return <><header><div className="contenedor cabecera"><Link to={`/${slug}`}>{datos.nombre}</Link><nav><Link to={`/${slug}/pedidos`}>Mis pedidos</Link> · <Link to={`/${slug}/carrito`}>Carrito</Link></nav></div></header><main className="contenedor">
    {idProducto ? producto ? <Detalle key={producto.id} producto={producto} slug={slug} /> : <><h1>Producto no encontrado</h1><Link to={`/${slug}`}>Volver a la tienda</Link></> : <>
      <section className="introduccion"><p className="etiqueta">Bienvenido a {datos.nombre}</p><h1>{datos.titulo || datos.nombre}</h1>{datos.descripcion && <p>{datos.descripcion}</p>}</section>
      {datos.mostrarCarrusel && actual && <section className="carrusel" aria-label="Novedades"><div>{actual.enlace ? <a href={actual.enlace}><img src={actual.url} alt={`Banner ${banner + 1}`} /></a> : <img src={actual.url} alt={`Banner ${banner + 1}`} />}</div>{datos.banners.length > 1 && <div className="controles"><button onClick={() => setBanner((banner + datos.banners.length - 1) % datos.banners.length)}>Anterior</button><span>{banner + 1} / {datos.banners.length}</span><button onClick={() => setBanner((banner + 1) % datos.banners.length)}>Siguiente</button></div>}</section>}
      <section><h2>Nuestros productos</h2>{datos.productos.length === 0 ? <p>Esta tienda aún no tiene productos publicados.</p> : <div className="productos">{datos.productos.map(p => <Link className="tarjeta" to={`/${slug}/productos/${p.id}`} key={p.id}><Foto producto={p} /><div><h3>{p.nombre}</h3><p>{p.presentaciones.length ? `Desde ${precio(Math.min(...p.presentaciones.map(v => v.precio)))}` : 'Sin presentaciones disponibles'}</p><small>{p.presentaciones.some(v => v.disponible) ? 'Disponible' : 'Agotado'}</small></div></Link>)}</div>}</section>
    </>}
  </main><footer className="contenedor">{datos.nombre} · Vitrina en Nubea</footer></>
}
function Foto({ producto }: { producto: Producto }) { return producto.imagenes[0] ? <img className="foto" src={producto.imagenes[0]} alt={producto.nombre} /> : <div className="foto sin-foto">Sin imagen</div> }
function Detalle({ producto, slug }: { producto: Producto; slug: string }) {
  const [seleccion, setSeleccion] = useState(producto.presentaciones[0]?.id ?? '')
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [necesitaSesion, setNecesitaSesion] = useState(false)
  const presentacion = producto.presentaciones.find(p => p.id === seleccion)
  return <section><Link to={`/${slug}`}>← Volver a la tienda</Link><div className="detalle"><Foto producto={producto} /><div><h1>{producto.nombre}</h1>{producto.descripcion && <p>{producto.descripcion}</p>}{producto.presentaciones.length > 0 ? <><label htmlFor="presentacion">Presentación</label><select id="presentacion" value={seleccion} onChange={e => setSeleccion(e.target.value)}>{producto.presentaciones.map(p => <option key={p.id} value={p.id}>{p.opciones.map(o => `${o.nombre}: ${o.valor}`).join(' · ') || 'Única'} — {precio(p.precio)}</option>)}</select><p className="precio">{presentacion && precio(presentacion.precio)}</p><p>{presentacion?.disponible ? 'Disponible' : 'Agotado'}</p><button disabled={!presentacion?.disponible || ocupado} onClick={() => { setOcupado(true); setMensaje(''); setNecesitaSesion(false); void agregarAlCarrito(slug, seleccion).then(() => setMensaje('Producto agregado al carrito.')).catch((e: unknown) => { if (e instanceof ErrorSolicitud && e.estado === 401) { setNecesitaSesion(true); setMensaje('Inicia sesión para usar el carrito.') } else setMensaje(e instanceof Error ? e.message : 'No se pudo agregar al carrito.') }).finally(() => setOcupado(false)) }}>Agregar al carrito</button>{mensaje && <p role="status">{mensaje} {necesitaSesion && <a href={enlaceSesion(slug)}>Iniciar sesión · </a>}<Link to={`/${slug}/carrito`}>Ver carrito</Link></p>}</> : <p>Sin presentaciones disponibles.</p>}</div></div></section>
}
