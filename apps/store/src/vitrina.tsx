import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowRight, ChevronLeft, ChevronRight, ImageOff, ShoppingBag } from 'lucide-react'
import { agregarAlCarrito, enlaceSesion, ErrorSolicitud } from './api'
import { MarcoTienda, Volver } from './diseno'
import { precio } from './api'
import { plantillaDisponible } from './plantillas'

type Presentacion = { id: string; precio: number; precioAnterior?: number | null; disponible: boolean; opciones: { nombre: string; valor: string }[] }
// Porcentaje de descuento cuando la presentación tiene precio especial menor al precio base.
const descuento = (p?: Presentacion) => p?.precioAnterior && p.precioAnterior > p.precio ? Math.round((1 - p.precio / p.precioAnterior) * 100) : 0
type Producto = { id: string; categoriaId: string; nombre: string; descripcion: string | null; imagenes: string[]; presentaciones: Presentacion[] }
type Datos = { nombre: string; plantilla: string; titulo: string | null; descripcion: string | null; mostrarCarrusel: boolean; banners: { url: string; enlace: string | null }[]; categorias: { id: string; nombre: string; slug: string }[]; colecciones: { id: string; nombre: string; slug: string; productos: string[] }[]; productos: Producto[] }
const api = import.meta.env.VITE_API_URL?.trim() || (import.meta.env.DEV ? 'http://localhost:8080' : '')

export function Vitrina() {
  const { slug = '', idProducto } = useParams()
  const [datos, setDatos] = useState<Datos | null>(null)
  const [error, setError] = useState('')
  const [intento, setIntento] = useState(0)
  const [banner, setBanner] = useState(0)
  const [categoria, setCategoria] = useState('')
  const [coleccion, setColeccion] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState('nombre')
  useEffect(() => {
    const controlador = new AbortController()
    void Promise.resolve().then(() => { if (!controlador.signal.aborted) { setDatos(null); setError(''); setBanner(0); setCategoria(''); setColeccion(''); setBusqueda(''); setOrden('nombre') } })
    fetch(`${api}/api/publico/tiendas/${encodeURIComponent(slug)}`, { signal: controlador.signal })
      .then(res => { if (!res.ok) throw new Error(res.status === 404 ? 'Esta tienda no existe o no está disponible.' : 'No se pudo cargar la tienda.'); return res.json() as Promise<Datos> })
      .then(setDatos).catch(e => { if (e.name !== 'AbortError') setError(e.message === 'Failed to fetch' ? 'No se pudo conectar con la tienda.' : e.message) })
    return () => controlador.abort()
  }, [slug, intento])
  if (error) return <MarcoTienda slug={slug}><div className="estado"><h1>Tienda no disponible</h1><p role="alert">{error}</p><button className="boton boton-primario" onClick={() => setIntento(i => i + 1)}>Intentar nuevamente</button></div></MarcoTienda>
  if (!datos) return <main className="contenedor estado" role="status">Cargando tienda...</main>
  if (!plantillaDisponible(datos.plantilla)) return <MarcoTienda slug={slug} nombre={datos.nombre}><div className="estado"><h1>{datos.nombre}</h1><p>La plantilla de esta tienda no está disponible.</p></div></MarcoTienda>
  const producto = datos.productos.find(p => p.id === idProducto)
  const actual = datos.banners[banner]
  const seleccionColeccion = (datos.colecciones || []).find(c => c.id === coleccion)
  const precioDisponible = (p: Producto) => Math.min(...p.presentaciones.filter(v => v.disponible).map(v => v.precio))
  const productos = datos.productos.filter(p => (!categoria || p.categoriaId === categoria) && (!coleccion || seleccionColeccion?.productos.includes(p.id)) && p.nombre.toLocaleLowerCase('es').includes(busqueda.trim().toLocaleLowerCase('es'))).sort((a, b) => {
    if (orden === 'nombre') return a.nombre.localeCompare(b.nombre, 'es')
    const pa = precioDisponible(a), pb = precioDisponible(b)
    if (!Number.isFinite(pa) || !Number.isFinite(pb)) return Number.isFinite(pa) ? -1 : Number.isFinite(pb) ? 1 : a.nombre.localeCompare(b.nombre, 'es')
    return (orden === 'precio-asc' ? pa - pb : pb - pa) || a.nombre.localeCompare(b.nombre, 'es')
  })
  return <MarcoTienda slug={slug} nombre={datos.nombre} plantilla={datos.plantilla}>{idProducto ? producto ? <Detalle key={producto.id} producto={producto} slug={slug} categoria={datos.categorias.find(c => c.id === producto.categoriaId)?.nombre} /> : <div className="estado"><h1>Producto no encontrado</h1><Volver to={`/${slug}`}>Volver a la tienda</Volver></div> : <>
    <section className={`portada-tienda ${datos.mostrarCarrusel && actual ? 'con-imagen' : 'sin-imagen'}`} aria-label="Novedades">
      {datos.mostrarCarrusel && actual && <div className="portada-fondo">{actual.enlace ? <a href={actual.enlace}><img src={actual.url} alt={`Banner ${banner + 1} de ${datos.banners.length}`} /></a> : <img src={actual.url} alt={`Banner ${banner + 1} de ${datos.banners.length}`} />}</div>}
      <div className="portada-contenido"><p className="etiqueta">Bienvenido a {datos.nombre}</p><h1>{datos.titulo || datos.nombre}</h1>{datos.descripcion && <p className="introduccion-texto">{datos.descripcion}</p>}<a className="enlace-flecha" href="#productos">Explorar productos <ArrowRight size={17} /></a></div>
      {datos.mostrarCarrusel && actual && datos.banners.length > 1 && <div className="controles"><button className="boton-icono" aria-label="Banner anterior" onClick={() => setBanner((banner + datos.banners.length - 1) % datos.banners.length)}><ChevronLeft size={19} /></button><span aria-live="polite">{banner + 1} / {datos.banners.length}</span><button className="boton-icono" aria-label="Banner siguiente" onClick={() => setBanner((banner + 1) % datos.banners.length)}><ChevronRight size={19} /></button></div>}
    </section>
    <section id="productos" className="catalogo"><div className="seccion-cabecera"><div><p className="etiqueta">Descubre la colección</p><h2>Nuestros productos</h2></div><p>{productos.length} {productos.length === 1 ? 'producto' : 'productos'}</p></div><div className="herramientas-catalogo"><label>Buscar productos<input type="search" value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar por nombre" /></label><label>Ordenar por<select value={orden} onChange={e => setOrden(e.target.value)}><option value="nombre">Nombre</option><option value="precio-asc">Menor precio</option><option value="precio-desc">Mayor precio</option></select></label>{(datos.colecciones || []).length > 0 && <label>Colección<select value={coleccion} onChange={e => setColeccion(e.target.value)}><option value="">Todas</option>{datos.colecciones.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}</select></label>}{(categoria || coleccion || busqueda || orden !== 'nombre') && <button onClick={() => { setCategoria(''); setColeccion(''); setBusqueda(''); setOrden('nombre') }}>Limpiar filtros</button>}</div>{datos.categorias.length > 0 && <div className="filtros" aria-label="Filtrar por categoría"><button className={!categoria ? 'activo' : ''} aria-pressed={!categoria} onClick={() => setCategoria('')}>Todos</button>{datos.categorias.map(c => <button key={c.id} className={categoria === c.id ? 'activo' : ''} aria-pressed={categoria === c.id} onClick={() => setCategoria(c.id)}>{c.nombre}</button>)}</div>}{productos.length === 0 ? <div className="estado-vacio"><ShoppingBag size={26} /><p>{datos.productos.length ? 'No hay productos que coincidan con los filtros.' : 'Esta tienda aún no tiene productos publicados.'}</p></div> : <div className="productos">{productos.map(p => {
      const disponibles = p.presentaciones.filter(v => v.disponible)
      const categoriaNombre = datos.categorias.find(c => c.id === p.categoriaId)?.nombre
      const oferta = disponibles.length ? disponibles.reduce((a, b) => b.precio < a.precio ? b : a) : undefined
      return <Link className="tarjeta" to={`/${slug}/productos/${p.id}`} key={p.id}><div className="tarjeta-media"><Foto producto={p} />{p.imagenes[1] && <img className="foto foto-alterna" src={p.imagenes[1]} alt="" />}{p.presentaciones.length > 0 && !disponibles.length && <span className="insignia">Agotado</span>}{oferta && descuento(oferta) > 0 && <span className="insignia-oferta">-{descuento(oferta)}%</span>}</div><div className="tarjeta-contenido">{categoriaNombre && <p className="tarjeta-categoria">{categoriaNombre}</p>}<h3>{p.nombre}</h3><div className="tarjeta-pie"><span className="precio-grupo">{oferta && descuento(oferta) > 0 && <s className="precio-anterior">{precio(oferta.precioAnterior!)}</s>}<strong>{disponibles.length ? `${disponibles.length > 1 ? 'Desde ' : ''}${precio(Math.min(...disponibles.map(v => v.precio)))}` : p.presentaciones.length ? 'Agotado' : 'Sin presentaciones'}</strong></span><ArrowRight size={17} aria-hidden="true" /></div></div></Link>
    })}</div>}</section>
  </>}</MarcoTienda>
}
function Foto({ producto, indice = 0 }: { producto: Producto; indice?: number }) { return producto.imagenes[indice] ? <img className="foto" src={producto.imagenes[indice]} alt={producto.nombre} /> : <div className="foto sin-foto"><ImageOff size={28} /><span>Sin imagen</span></div> }
function Detalle({ producto, slug, categoria }: { producto: Producto; slug: string; categoria?: string }) {
  const [seleccion, setSeleccion] = useState(producto.presentaciones.find(p => p.disponible)?.id ?? producto.presentaciones[0]?.id ?? '')
  const [imagen, setImagen] = useState(0)
  const [mensaje, setMensaje] = useState('')
  const [ocupado, setOcupado] = useState(false)
  const [necesitaSesion, setNecesitaSesion] = useState(false)
  const presentacion = producto.presentaciones.find(p => p.id === seleccion)
  // Si todas las presentaciones tienen opciones (talla, color...), se eligen con botones por variante.
  const usaVariantes = producto.presentaciones.length > 0 && producto.presentaciones.every(p => p.opciones.length > 0)
  return <section className="pagina-detalle"><Volver to={`/${slug}`}>Volver a la tienda</Volver><div className="detalle"><div><Foto producto={producto} indice={imagen} />{producto.imagenes.length > 1 && <div className="miniaturas" aria-label="Imágenes del producto">{producto.imagenes.map((url, i) => <button key={`${url}-${i}`} className={imagen === i ? 'activo' : ''} aria-label={`Ver imagen ${i + 1}`} aria-pressed={imagen === i} onClick={() => setImagen(i)}><img src={url} alt="" /></button>)}</div>}</div><div className="detalle-info">{categoria && <p className="etiqueta">{categoria}</p>}<h1>{producto.nombre}</h1>{producto.descripcion && <p className="descripcion">{producto.descripcion}</p>}{producto.presentaciones.length > 0 ? <>{usaVariantes ? <SelectorVariantes presentaciones={producto.presentaciones} seleccion={seleccion} alElegir={setSeleccion} /> : producto.presentaciones.length > 1 && <><label htmlFor="presentacion">Presentación</label><select id="presentacion" value={seleccion} onChange={e => setSeleccion(e.target.value)}>{producto.presentaciones.map(p => <option key={p.id} value={p.id}>{p.opciones.map(o => `${o.nombre}: ${o.valor}`).join(' · ') || 'Única'} — {precio(p.precio)}{descuento(p) > 0 ? ` (antes ${precio(p.precioAnterior!)})` : ''}{!p.disponible ? ' · Agotado' : ''}</option>)}</select></>}<p className="precio">{presentacion && descuento(presentacion) > 0 && <s className="precio-anterior">{precio(presentacion.precioAnterior!)}</s>}{presentacion && precio(presentacion.precio)}{presentacion && descuento(presentacion) > 0 && <span className="insignia-descuento">-{descuento(presentacion)}%</span>}</p><p className={`disponibilidad ${presentacion?.disponible ? '' : 'agotado'}`}>{presentacion?.disponible ? 'Disponible' : 'Agotado'}</p><button className="boton boton-primario boton-compra" disabled={!presentacion?.disponible || ocupado} onClick={() => { setOcupado(true); setMensaje(''); setNecesitaSesion(false); void agregarAlCarrito(slug, seleccion).then(() => setMensaje('Producto agregado al carrito.')).catch((e: unknown) => { if (e instanceof ErrorSolicitud && e.estado === 401) { setNecesitaSesion(true); setMensaje('Inicia sesión para usar el carrito.') } else setMensaje(e instanceof Error ? e.message : 'No se pudo agregar al carrito.') }).finally(() => setOcupado(false)) }}><ShoppingBag size={18} />{ocupado ? 'Agregando...' : 'Agregar al carrito'}</button>{mensaje && <p className="aviso" role="status">{mensaje} {necesitaSesion && <Link to={enlaceSesion(slug)}>Iniciar sesión</Link>} <Link to={`/${slug}/carrito`}>Ver carrito</Link></p>}</> : <p>Sin presentaciones disponibles.</p>}</div></div></section>
}

// ---------- Selector de variantes (talla, color, etc.) ----------
const coloresConocidos: Record<string, string> = { rojo: '#d32f2f', negro: '#111111', blanco: '#ffffff', azul: '#1e4fd8', celeste: '#5bb8f0', verde: '#2e8b57', amarillo: '#f4c430', naranja: '#f08a24', rosado: '#f48fb1', rosa: '#f48fb1', morado: '#7b3fa0', lila: '#b39ddb', gris: '#8a8a8a', marron: '#7b4a2a', 'marrón': '#7b4a2a', cafe: '#6f4e37', 'café': '#6f4e37', beige: '#e8d8bd', crema: '#f3ead8', dorado: '#c9a96e', plateado: '#c0c0c0', vino: '#7b1e2b', turquesa: '#30c5c0', fucsia: '#e3268f', mostaza: '#d4a017', 'verde oliva': '#6b7a3a', 'azul marino': '#1b2a4a' }
const colorDe = (valor: string) => coloresConocidos[valor.trim().toLowerCase()]
const esVarianteColor = (nombre: string) => /colou?r/i.test(nombre)

function SelectorVariantes({ presentaciones, seleccion, alElegir }: { presentaciones: Presentacion[]; seleccion: string; alElegir: (id: string) => void }) {
  const actual = presentaciones.find(p => p.id === seleccion) ?? presentaciones[0]
  const elegido = (nombre: string) => actual?.opciones.find(o => o.nombre === nombre)?.valor
  // Variantes y sus valores en el orden en que aparecen.
  const variantes: { nombre: string; valores: string[] }[] = []
  for (const p of presentaciones) for (const o of p.opciones) {
    let v = variantes.find(x => x.nombre === o.nombre)
    if (!v) { v = { nombre: o.nombre, valores: [] }; variantes.push(v) }
    if (!v.valores.includes(o.valor)) v.valores.push(o.valor)
  }
  // Presentación que tendría ese valor manteniendo lo demás elegido (si no existe, la primera que lo tenga).
  function buscar(nombre: string, valor: string) {
    const deseado = Object.fromEntries(variantes.map(v => [v.nombre, v.nombre === nombre ? valor : elegido(v.nombre)]))
    const coincide = (p: Presentacion) => p.opciones.every(o => deseado[o.nombre] === o.valor) && p.opciones.length === variantes.filter(v => deseado[v.nombre]).length
    const conValor = presentaciones.filter(p => p.opciones.some(o => o.nombre === nombre && o.valor === valor))
    return { exacta: presentaciones.find(coincide), alternativa: conValor.find(p => p.disponible) ?? conValor[0] }
  }
  return <div className="selector-variantes">{variantes.map(v => {
    const esColor = esVarianteColor(v.nombre)
    return <div className="variante-grupo" key={v.nombre} role="group" aria-label={v.nombre}>
      <p className="variante-titulo">{v.nombre.charAt(0) + v.nombre.slice(1).toLowerCase()}: <strong>{elegido(v.nombre) ?? 'Elige una opción'}</strong></p>
      <div className="variante-opciones">{v.valores.map(valor => {
        const { exacta, alternativa } = buscar(v.nombre, valor)
        const destino = exacta ?? alternativa
        const activo = elegido(v.nombre) === valor
        const agotado = exacta ? !exacta.disponible : !alternativa?.disponible
        const color = esColor ? colorDe(valor) : undefined
        return <button type="button" key={valor} aria-pressed={activo} title={agotado ? `${valor} · Agotado` : valor} className={`opcion-variante${activo ? ' activo' : ''}${agotado ? ' agotada' : ''}${color ? ' con-color' : ''}`} onClick={() => destino && alElegir(destino.id)}>
          {color && <span className="muestra-color" style={{ background: color }} aria-hidden="true" />}{valor}
        </button>
      })}</div>
    </div>
  })}</div>
}
