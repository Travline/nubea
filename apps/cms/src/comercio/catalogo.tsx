import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, ArrowUpDown, Boxes, Check, CircleCheck, CircleX, Sparkles, ChevronDown, Eye, EyeOff, ImageOff, ImagePlus, Layers, Package, Pencil, Plus, RefreshCw, Search, SlidersHorizontal, Star, Tags, Trash2, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { ContextoTienda } from '@/paginas/panel'
import { api } from './datos'
import type { Categoria, Coleccion, Producto, Variante, Valor, Presentacion, Imagen, Inventario, PaginaInventario, RequisitosPublicacion } from './datos'
import { Aviso, ErrorCampo, EstadoCarga, ZonaEdicion, conError, useCarga, useOperacion, useSoloLectura } from './comunes'
import { claseTono, etiquetaProducto, etiquetaVersion } from './estados'
import { ControlStock } from './inventario'

type Elemento = { id: string; nombre: string; slug?: string; activa?: boolean }
const soles = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)
function EstadoProducto({ estado, className = '' }: { estado: string; className?: string }) {
 const e = etiquetaProducto(estado)
 return <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold backdrop-blur ${claseTono[e.tono].clase} ${className}`}>{e.texto}</span>
}
const claseEntrada = 'h-11 w-full rounded-xl border border-input bg-background px-3.5 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20'

const pestanasCatalogo = [['productos', 'Productos', Package], ['categorias', 'Categorías', Tags], ['colecciones', 'Colecciones', Layers], ['variantes', 'Opciones', SlidersHorizontal]] as const
export function Catalogo() {
 const { apiBase, base } = useOutletContext<ContextoTienda>(); const [seccion, setSeccion] = useState('productos')
 return <div className="space-y-6">
  <div className="flex flex-wrap items-center justify-between gap-3">
   <div><h2 className="text-xl font-semibold tracking-tight">Catálogo</h2><p className="text-sm text-muted-foreground">Organiza tus productos, categorías, colecciones y opciones.</p></div>
   <div className="flex flex-wrap rounded-xl border bg-card p-1" role="tablist" aria-label="Secciones del catálogo">
    {pestanasCatalogo.map(([valor, nombre, Icono]) => <button key={valor} type="button" role="tab" aria-selected={seccion === valor} onClick={() => setSeccion(valor)} className={`inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg px-3.5 text-sm font-medium transition ${seccion === valor ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`}><Icono className="size-4" />{nombre}</button>)}
   </div>
  </div>
  {seccion === 'productos' ? <Productos apiBase={apiBase} base={base} irA={setSeccion} /> : seccion === 'variantes' ? <Variantes apiBase={apiBase} base={base} /> : <Listas key={seccion} apiBase={apiBase} base={base} tipo={seccion as 'categorias' | 'colecciones'} />}
 </div>
}

// Imagen principal del producto (se carga por tarjeta).
function ImagenProducto({ ruta, nombre }: { ruta: string; nombre: string }) {
 const { datos } = useCarga(() => api.leer<Imagen[]>(`${ruta}/imagenes`), [ruta])
 const imagen = datos?.find(i => i.esPrincipal) ?? [...(datos ?? [])].sort((a, b) => a.orden - b.orden)[0]
 if (!datos) return <div className="absolute inset-0 animate-pulse bg-muted" />
 return imagen ? <img src={imagen.url} alt={nombre} loading="lazy" className="absolute inset-0 size-full object-cover transition-transform duration-500 group-hover:scale-105" />
  : <div className="absolute inset-0 grid place-items-center bg-[linear-gradient(135deg,var(--muted),transparent)] text-muted-foreground"><div className="flex flex-col items-center gap-1.5"><ImageOff className="size-7 opacity-60" /><span className="text-xs">Sin imagen</span></div></div>
}

function Productos({ apiBase, base, irA }: { apiBase: string; base: string; irA: (seccion: string) => void }) {
 const navegar = useNavigate(); const op = useOperacion(['nombre', 'descripcion', 'precioBase', 'idCategoria', 'sku', 'stockInicial', 'stockMinimo']); const [nombre, setNombre] = useState(''); const [descripcion, setDescripcion] = useState(''); const [precio, setPrecio] = useState(''); const [categoria, setCategoria] = useState(''); const [sku, setSku] = useState(''); const [stock, setStock] = useState(''); const [stockMinimo, setStockMinimo] = useState('')
 const soloLectura = useSoloLectura(); const [mostrarCrear, setMostrarCrear] = useState(false); const [busqueda, setBusqueda] = useState(''); const [filtroEstado, setFiltroEstado] = useState(''); const [filtroCategoria, setFiltroCategoria] = useState('')
 const { datos, error, recargar } = useCarga(async () => { const [productos, categorias] = await Promise.all([api.leer<Producto[]>(`${apiBase}/productos`), api.leer<Categoria[]>(`${apiBase}/categorias`)]); return { productos, categorias } }, [apiBase])
 if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
 const hayCategorias = datos.categorias.some(c => c.activa)
 const nombreCategoria = (id: string) => datos.categorias.find(c => c.idCategoria === id)?.nombre
 const filtrados = datos.productos.filter(p => (!filtroEstado || p.estado === filtroEstado) && (!filtroCategoria || p.idCategoria === filtroCategoria) && p.nombre.toLowerCase().includes(busqueda.trim().toLowerCase()))
 const cuenta = (estado: string) => datos.productos.filter(p => p.estado === estado).length
 const formularioVisible = !soloLectura && (mostrarCrear || datos.productos.length === 0)

 return <div className="space-y-6">
  {/* Resumen */}
  <div className="grid gap-3 sm:grid-cols-4">
   {([['', 'Todos', datos.productos.length], ['PUBLICADO', 'Visibles en tu tienda', cuenta('PUBLICADO')], ['BORRADOR', 'Borradores', cuenta('BORRADOR')], ['INACTIVO', 'Ocultos', cuenta('INACTIVO')]] as const).map(([valor, texto, total]) => <button key={texto} type="button" onClick={() => setFiltroEstado(valor)} className={`cursor-pointer rounded-2xl border bg-card p-4 text-left transition hover:border-primary/40 ${filtroEstado === valor ? 'border-primary ring-3 ring-primary/15' : ''}`}><p className="text-sm text-muted-foreground">{texto}</p><p className="mt-1 text-2xl font-semibold tracking-tight">{total}</p></button>)}
  </div>

  {/* Crear producto */}
  {formularioVisible ? <ZonaEdicion><section className="rounded-2xl border bg-card">
   <div className="flex items-start justify-between gap-3 border-b px-6 py-4">
    <div className="flex items-center gap-3"><span className="grid size-10 place-items-center rounded-xl bg-primary/10 text-primary"><Plus className="size-5" /></span><div><h3 className="font-semibold">Nuevo producto</h3><p className="text-sm text-muted-foreground">Luego podrás añadir fotos, versiones y unidades.</p></div></div>
    {datos.productos.length > 0 && <button type="button" aria-label="Cerrar" onClick={() => setMostrarCrear(false)} className="grid size-8 cursor-pointer place-items-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground"><X className="size-4" /></button>}
   </div>
   {!hayCategorias ? <div className="flex flex-wrap items-center justify-between gap-3 px-6 py-5"><p className="text-sm text-muted-foreground">Primero crea una categoría para poder agregar productos.</p><Button type="button" variant="outline" onClick={() => irA('categorias')}><Tags className="size-4" />Crear categoría</Button></div>
   : <form className="grid gap-4 p-6 sm:grid-cols-2" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { const producto = await api.crear<Producto>(`${apiBase}/productos`, { nombre, descripcion, precioBase: Number(precio), idCategoria: categoria }); if (stock !== '') await api.crear<Presentacion>(`${apiBase}/productos/${producto.idProducto}/presentaciones`, { sku: sku.trim() || undefined, precioOverride: null, idsValores: [], stockInicial: Number(stock), stockMinimo: Number(stockMinimo || 0) }); navegar(`${base}/catalogo/productos/${producto.idProducto}`) }) }}>
    <label className="grid gap-1.5 text-sm font-medium">Nombre<input className={claseEntrada + conError(op.errorDe('nombre'))} required maxLength={150} value={nombre} onChange={e => setNombre(e.target.value)} placeholder="Ej. Camisa de lino" /><ErrorCampo mensaje={op.errorDe('nombre')} /></label>
    <label className="grid gap-1.5 text-sm font-medium">Categoría<select className={`${claseEntrada} cursor-pointer${conError(op.errorDe('idCategoria'))}`} required value={categoria} onChange={e => setCategoria(e.target.value)}><option value="">Selecciona una categoría</option>{datos.categorias.filter(c => c.activa).map(c => <option value={c.idCategoria} key={c.idCategoria}>{c.nombre}</option>)}</select><ErrorCampo mensaje={op.errorDe('idCategoria')} /></label>
    <label className="grid gap-1.5 text-sm font-medium">Precio base<span className="relative"><span className="pointer-events-none absolute top-1/2 left-3.5 -translate-y-1/2 text-sm text-muted-foreground">S/</span><input className={`${claseEntrada} pl-9${conError(op.errorDe('precioBase'))}`} type="number" min="0" step="0.01" required value={precio} onChange={e => setPrecio(e.target.value)} placeholder="0.00" /></span><ErrorCampo mensaje={op.errorDe('precioBase')} /></label>
    <label className="grid gap-1.5 text-sm font-medium">Descripción<input className={claseEntrada + conError(op.errorDe('descripcion'))} value={descripcion} onChange={e => setDescripcion(e.target.value)} placeholder="Breve descripción (opcional)" /><ErrorCampo mensaje={op.errorDe('descripcion')} /></label>
    <div className="rounded-xl border border-dashed bg-muted/30 p-4 sm:col-span-2">
     <p className="flex items-center gap-2 text-sm font-semibold"><Boxes className="size-4 text-primary" />Unidades iniciales <span className="font-normal text-muted-foreground">(opcional)</span></p>
     <p className="mt-1 text-xs text-muted-foreground">Si indicas las unidades se crea una versión única con ellas. Si vendes tallas o colores, déjalo vacío y añade las versiones después.</p>
     <div className="mt-3 grid gap-3 sm:grid-cols-3">
      <CampoCodigoInterno valor={sku} cambiar={setSku} error={op.errorDe('sku')} />
      <label className="grid gap-1.5 text-sm font-medium">Unidades disponibles<input className={claseEntrada + conError(op.errorDe('stockInicial'))} type="number" min="0" step="1" value={stock} onChange={e => setStock(e.target.value)} placeholder="Ej. 20" /><ErrorCampo mensaje={op.errorDe('stockInicial')} /></label>
      <label className="grid gap-1.5 text-sm font-medium">Avisarme cuando queden<input className={claseEntrada + conError(op.errorDe('stockMinimo'))} type="number" min="0" step="1" value={stockMinimo} onChange={e => setStockMinimo(e.target.value)} placeholder="Ej. 3" disabled={stock === ''} /><ErrorCampo mensaje={op.errorDe('stockMinimo')} /></label>
     </div>
    </div>
    <div className="flex flex-wrap items-center justify-end gap-3 sm:col-span-2"><Aviso error={op.error} /><Button type="submit" className="h-11 rounded-xl px-5" disabled={op.ocupado}>{op.ocupado ? 'Creando...' : <>Crear y continuar <ArrowRight className="size-4" /></>}</Button></div>
   </form>}
  </section></ZonaEdicion> : null}

  {/* Barra de herramientas */}
  {datos.productos.length > 0 && <div className="flex flex-wrap items-center gap-3">
   <label className="relative min-w-56 flex-1"><span className="sr-only">Buscar producto</span><Search className="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-muted-foreground" /><input className={`${claseEntrada} bg-card pl-11`} placeholder="Buscar producto por nombre..." value={busqueda} onChange={e => setBusqueda(e.target.value)} /></label>
   <label className="flex h-11 items-center gap-2 rounded-xl border border-input bg-card pr-2 pl-4 text-sm"><span className="text-muted-foreground">Categoría:</span><select className="h-full cursor-pointer bg-transparent font-medium outline-none" value={filtroCategoria} onChange={e => setFiltroCategoria(e.target.value)}><option value="">Todas</option>{datos.categorias.map(c => <option key={c.idCategoria} value={c.idCategoria}>{c.nombre}</option>)}</select></label>
   {!formularioVisible && <ZonaEdicion><Button type="button" className="h-11 gap-2 rounded-xl px-5" onClick={() => setMostrarCrear(true)}><Plus className="size-4" />Nuevo producto</Button></ZonaEdicion>}
  </div>}

  {/* Productos */}
  {datos.productos.length === 0 ? (soloLectura ? <p className="rounded-2xl border bg-card px-6 py-10 text-center text-sm text-muted-foreground">Esta tienda aún no tiene productos.</p> : null) : filtrados.length === 0 ? <p className="rounded-2xl border bg-card px-6 py-10 text-center text-sm text-muted-foreground">Ningún producto coincide con los filtros.</p>
  : <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">{filtrados.map(p => <Link key={p.idProducto} to={`${base}/catalogo/productos/${p.idProducto}`} className="group flex flex-col overflow-hidden rounded-2xl border bg-card transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-[0_18px_40px_-24px_rgba(37,99,235,0.45)]">
    <div className="relative aspect-[4/3] overflow-hidden bg-muted"><ImagenProducto ruta={`${apiBase}/productos/${p.idProducto}`} nombre={p.nombre} /><EstadoProducto estado={p.estado} className="absolute top-3 left-3" /></div>
    <div className="flex flex-1 flex-col gap-1 p-4">
     {nombreCategoria(p.idCategoria) && <p className="text-[11px] font-semibold tracking-[0.12em] text-muted-foreground uppercase">{nombreCategoria(p.idCategoria)}</p>}
     <h3 className="line-clamp-2 font-semibold">{p.nombre}</h3>
     {p.descripcion && <p className="line-clamp-2 text-sm text-muted-foreground">{p.descripcion}</p>}
     <div className="mt-auto flex items-center justify-between pt-3"><span className="text-lg font-semibold">{soles(p.precioBase)}</span><span className="inline-flex items-center gap-1 text-sm font-medium text-primary opacity-0 transition group-hover:opacity-100">Editar <ArrowRight className="size-4" /></span></div>
    </div>
   </Link>)}</div>}
 </div>
}
// Código interno (SKU) de una versión: opcional al crear, el backend genera uno único si se deja vacío.
function CampoCodigoInterno({ valor, cambiar, error, obligatorio = false }: { valor: string; cambiar: (v: string) => void; error: string; obligatorio?: boolean }) {
 return <label className="grid gap-1.5 text-sm font-medium"><span>Código interno {!obligatorio && <span className="font-normal text-muted-foreground">(opcional)</span>}</span><input className={claseEntrada + conError(error)} maxLength={80} required={obligatorio} value={valor} onChange={e => cambiar(e.target.value)} placeholder={obligatorio ? '' : 'Ej. CAM-LIN-M'} />{error ? <ErrorCampo mensaje={error} /> : !obligatorio && <span className="text-xs font-normal text-muted-foreground">Código interno, se genera solo si lo dejas vacío.</span>}</label>
}

// Fila compacta de producto (foto, nombre, precio y estado) usada en categorías, colecciones y variantes.
function TarjetaMini({ producto: p, apiBase, base, detalle, accion }: { producto: Producto; apiBase: string; base: string; detalle?: ReactNode; accion?: ReactNode }) {
 return <div className="group flex items-center gap-3 rounded-xl border bg-background p-2.5 pr-3 transition hover:border-primary/40">
  <Link to={`${base}/catalogo/productos/${p.idProducto}`} className="flex min-w-0 flex-1 items-center gap-3">
   <span className="relative size-14 shrink-0 overflow-hidden rounded-lg bg-muted"><ImagenProducto ruta={`${apiBase}/productos/${p.idProducto}`} nombre={p.nombre} /></span>
   <span className="min-w-0 flex-1"><span className="block truncate text-sm font-semibold">{p.nombre}</span><span className="text-xs text-muted-foreground">{soles(p.precioBase)}</span>{detalle}</span>
  </Link>
  <EstadoProducto estado={p.estado} className="hidden sm:inline-flex" />
  {accion}
 </div>
}

function Vacio({ icono, titulo, texto }: { icono: ReactNode; titulo: string; texto: string }) {
 return <div className="rounded-2xl border border-dashed bg-card px-6 py-12 text-center"><span className="mx-auto grid size-12 place-items-center rounded-2xl bg-primary/10 text-primary">{icono}</span><p className="mt-3 font-medium">{titulo}</p><p className="mt-1 text-sm text-muted-foreground">{texto}</p></div>
}

function ListaLateral<T>({ elementos, seleccionado, alElegir, clave, render, busqueda, alBuscar, placeholder }: { elementos: T[]; seleccionado: string; alElegir: (id: string) => void; clave: (e: T) => string; render: (e: T) => ReactNode; busqueda: string; alBuscar: (v: string) => void; placeholder: string }) {
 return <aside className="h-fit overflow-hidden rounded-2xl border bg-card">
  <label className="relative block border-b p-3"><span className="sr-only">{placeholder}</span><Search className="pointer-events-none absolute top-1/2 left-6 size-4 -translate-y-1/2 text-muted-foreground" /><input className="h-10 w-full rounded-lg bg-muted/60 pr-3 pl-9 text-sm outline-none focus:ring-3 focus:ring-primary/20" placeholder={placeholder} value={busqueda} onChange={e => alBuscar(e.target.value)} /></label>
  <ul className="max-h-[520px] overflow-y-auto p-2">{elementos.length === 0 ? <li className="px-3 py-6 text-center text-sm text-muted-foreground">Sin resultados</li> : elementos.map(e => <li key={clave(e)}><button type="button" onClick={() => alElegir(clave(e))} className={`flex w-full cursor-pointer items-center gap-3 rounded-xl px-3 py-2.5 text-left transition ${seleccionado === clave(e) ? 'bg-primary/10 text-primary' : 'hover:bg-muted'}`}>{render(e)}</button></li>)}</ul>
 </aside>
}

function Listas({ apiBase, base, tipo }: { apiBase: string; base: string; tipo: 'categorias' | 'colecciones' }) {
 const singular = tipo === 'categorias' ? 'categoría' : 'colección'
 const [nombre, setNombre] = useState(''); const [edicion, setEdicion] = useState(''); const [seleccion, setSeleccion] = useState(''); const [busqueda, setBusqueda] = useState(''); const op = useOperacion(['nombre', 'slug'])
 const { datos, error, recargar } = useCarga(async () => {
  const [items, productos] = await Promise.all([
   tipo === 'categorias'
    ? api.leer<Categoria[]>(`${apiBase}/${tipo}`).then(l => l.map(c => ({ id: c.idCategoria, nombre: c.nombre, slug: c.slug, activa: c.activa })))
    : api.leer<Coleccion[]>(`${apiBase}/${tipo}`).then(l => l.map(c => ({ id: c.idColeccion, nombre: c.nombre, slug: c.slug, activa: c.activa }))),
   api.leer<Producto[]>(`${apiBase}/productos`),
  ])
  return { items: items as Elemento[], productos }
 }, [apiBase, tipo])
 function limpiar() { setNombre(''); setEdicion(''); op.limpiar() }
 async function guardar(e: FormEvent) { e.preventDefault(); await op.ejecutar(async () => { const cuerpo = { nombre } /* la dirección web la genera el backend y se conserva al editar */; if (edicion) await api.actualizar(`${apiBase}/${tipo}/${edicion}`, cuerpo); else await api.crear(`${apiBase}/${tipo}`, cuerpo); limpiar(); recargar() }) }
 function editar(item: Elemento) { setNombre(item.nombre); setEdicion(item.id); op.limpiar(); window.scrollTo({ top: 0, behavior: 'smooth' }) }
 if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
 const visibles = datos.items.filter(i => i.nombre.toLowerCase().includes(busqueda.trim().toLowerCase()))
 const actual = datos.items.find(i => i.id === seleccion) ?? datos.items[0]
 const conteo = (id: string) => datos.productos.filter(p => p.idCategoria === id).length

 return <div className="space-y-6">
  <ZonaEdicion><section className="rounded-2xl border bg-card p-5">
   <div className="mb-4 flex items-center justify-between gap-3"><h3 className="font-semibold">{edicion ? `Editar ${singular}` : `Nueva ${singular}`}</h3>{edicion && <Button type="button" variant="ghost" size="sm" onClick={limpiar}><X className="size-4" />Cancelar</Button>}</div>
   <form className="grid items-start gap-3 sm:grid-cols-[1fr_auto]" onSubmit={e => void guardar(e)}>
    <label className="grid gap-1.5 text-sm font-medium">Nombre<input className={claseEntrada + conError(op.errorDe('nombre') || op.errorDe('slug'))} required maxLength={100} value={nombre} onChange={e => setNombre(e.target.value)} placeholder={tipo === 'categorias' ? 'Ej. Polos' : 'Ej. Verano 2026'} /><ErrorCampo mensaje={op.errorDe('nombre') || op.errorDe('slug')} />{!op.errorDe('nombre') && !op.errorDe('slug') && <span className="text-xs font-normal text-muted-foreground">La dirección web se genera sola a partir del nombre.</span>}</label>
    <Button type="submit" className="h-11 rounded-xl px-5 sm:mt-[26px]" disabled={op.ocupado}>{edicion ? 'Guardar cambios' : <><Plus className="size-4" />Crear</>}</Button>
   </form>
   <div className="mt-3"><Aviso error={op.error} /></div>
  </section></ZonaEdicion>

  {datos.items.length === 0 ? <Vacio icono={tipo === 'categorias' ? <Tags className="size-6" /> : <Layers className="size-6" />} titulo={`Aún no tienes ${tipo}`} texto={tipo === 'categorias' ? 'Las categorías agrupan tus productos (por ejemplo: Polos, Pantalones).' : 'Las colecciones destacan productos juntos (por ejemplo: Verano, Ofertas).'} />
  : <div className="grid gap-5 lg:grid-cols-[300px_1fr]">
   <ListaLateral elementos={visibles} seleccionado={actual.id} alElegir={setSeleccion} clave={i => i.id} busqueda={busqueda} alBuscar={setBusqueda} placeholder={`Buscar ${singular}...`} render={i => <>
    <span className={`size-2 shrink-0 rounded-full ${i.activa ? 'bg-emerald-500' : 'bg-zinc-400'}`} />
    <span className="min-w-0 flex-1"><span className="block truncate text-sm font-medium">{i.nombre}</span><span className="block truncate text-xs text-muted-foreground">/{i.slug}</span></span>
    {tipo === 'categorias' && <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">{conteo(i.id)}</span>}
   </>} />
   <DetalleGrupo key={actual.id} apiBase={apiBase} base={base} tipo={tipo} item={actual} productos={datos.productos} alEditar={() => editar(actual)} alCambiar={recargar} />
  </div>}
 </div>
}

function DetalleGrupo({ apiBase, base, tipo, item, productos, alEditar, alCambiar }: { apiBase: string; base: string; tipo: 'categorias' | 'colecciones'; item: Elemento; productos: Producto[]; alEditar: () => void; alCambiar: () => void }) {
 const op = useOperacion(); const [agregar, setAgregar] = useState('')
 const esColeccion = tipo === 'colecciones'
 const enColeccion = useCarga(() => esColeccion ? api.leer<Producto[]>(`${apiBase}/colecciones/${item.id}/productos`) : Promise.resolve([] as Producto[]), [apiBase, tipo, item.id])
 const lista = esColeccion ? enColeccion.datos : productos.filter(p => p.idCategoria === item.id)
 const disponibles = esColeccion && enColeccion.datos ? productos.filter(p => !enColeccion.datos!.some(c => c.idProducto === p.idProducto)) : []
 return <section className="rounded-2xl border bg-card">
  <div className="flex flex-wrap items-start justify-between gap-4 border-b p-5">
   <div className="min-w-0">
    <div className="flex flex-wrap items-center gap-2.5"><h3 className="truncate text-xl font-semibold tracking-tight">{item.nombre}</h3><span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${item.activa ? 'bg-emerald-500/12 text-emerald-700 dark:text-emerald-400' : 'bg-zinc-500/15 text-zinc-600 dark:text-zinc-400'}`}><span className={`size-1.5 rounded-full ${item.activa ? 'bg-emerald-500' : 'bg-zinc-400'}`} />{item.activa ? 'Activa' : 'Inactiva'}</span></div>
    <p className="mt-1 text-sm text-muted-foreground"><span title="Dirección web (se genera sola)">Dirección web: /{item.slug}</span> · {lista ? `${lista.length} ${lista.length === 1 ? 'producto' : 'productos'}` : 'Cargando...'}</p>
   </div>
   <ZonaEdicion className="flex gap-2">
    <Button variant="outline" size="sm" onClick={alEditar}><Pencil className="size-4" />Editar</Button>
    <Button variant="outline" size="sm" disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { await api.modificar(`${apiBase}/${tipo}/${item.id}/estado?activa=${!item.activa}`); alCambiar() })}>{item.activa ? 'Desactivar' : 'Activar'}</Button>
   </ZonaEdicion>
  </div>
  <div className="space-y-4 p-5">
   {esColeccion && enColeccion.datos && <ZonaEdicion className="flex flex-wrap gap-2">
    <select className={`${claseEntrada} min-w-56 flex-1 cursor-pointer`} value={agregar} onChange={e => setAgregar(e.target.value)} aria-label="Producto para agregar"><option value="">{disponibles.length ? 'Elige un producto para agregar...' : 'Todos tus productos ya están en esta colección'}</option>{disponibles.map(p => <option key={p.idProducto} value={p.idProducto}>{p.nombre}</option>)}</select>
    <Button className="h-11 rounded-xl" disabled={!agregar || op.ocupado} onClick={() => void op.ejecutar(async () => { await api.crear(`${apiBase}/colecciones/${item.id}/productos/${agregar}`); setAgregar(''); enColeccion.recargar() })}><Plus className="size-4" />Agregar</Button>
   </ZonaEdicion>}
   <Aviso error={op.error || enColeccion.error} />
   {!lista ? <div className="space-y-2">{[0, 1, 2].map(i => <div key={i} className="h-[76px] animate-pulse rounded-xl bg-muted" />)}</div>
   : lista.length === 0 ? <p className="rounded-xl border border-dashed px-5 py-8 text-center text-sm text-muted-foreground">{esColeccion ? 'Esta colección aún no tiene productos. Agrégalos con el selector de arriba.' : 'Ningún producto usa esta categoría. Elige la categoría al crear o editar un producto.'}</p>
   : <div className="grid gap-2.5 xl:grid-cols-2">{lista.map(p => <TarjetaMini key={p.idProducto} producto={p} apiBase={apiBase} base={base} accion={esColeccion ? <ZonaEdicion><button type="button" aria-label={`Quitar ${p.nombre} de la colección`} title="Quitar de la colección" disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { await api.eliminar(`${apiBase}/colecciones/${item.id}/productos/${p.idProducto}`); enColeccion.recargar() })} className="grid size-8 shrink-0 cursor-pointer place-items-center rounded-lg text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive disabled:cursor-not-allowed disabled:opacity-40"><X className="size-4" /></button></ZonaEdicion> : undefined} />)}</div>}
  </div>
 </section>
}

function Variantes({ apiBase, base }: { apiBase: string; base: string }) {
 const [nombre, setNombre] = useState(''); const [edicion, setEdicion] = useState(''); const [seleccion, setSeleccion] = useState(''); const [busqueda, setBusqueda] = useState(''); const op = useOperacion(['nombre'])
 const { datos, error, recargar } = useCarga(() => api.leer<Variante[]>(`${apiBase}/variantes`), [apiBase])
 if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
 const visibles = datos.filter(v => v.nombre.toLowerCase().includes(busqueda.trim().toLowerCase()))
 const actual = datos.find(v => v.idVariante === seleccion) ?? datos[0]
 return <div className="space-y-6">
  <ZonaEdicion><section className="rounded-2xl border bg-card p-5">
   <div className="mb-1 flex items-center justify-between gap-3"><h3 className="font-semibold">{edicion ? 'Editar opción' : 'Nueva opción'}</h3>{edicion && <Button type="button" variant="ghost" size="sm" onClick={() => { setEdicion(''); setNombre('') }}><X className="size-4" />Cancelar</Button>}</div>
   <p className="mb-4 text-sm text-muted-foreground">Opciones reutilizables como <strong className="font-medium text-foreground">Talla</strong> o <strong className="font-medium text-foreground">Color</strong>. Luego agrega sus valores (S, M, L…).</p>
   <form className="flex flex-wrap gap-3" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { if (edicion) await api.actualizar(`${apiBase}/variantes/${edicion}`, { nombre }); else await api.crear(`${apiBase}/variantes`, { nombre }); setNombre(''); setEdicion(''); recargar() }) }}>
    <label className="grid min-w-56 flex-1 gap-1"><input className={claseEntrada + conError(op.errorDe('nombre'))} aria-label="Nombre de la opción" placeholder="Ej. Talla" required value={nombre} onChange={e => setNombre(e.target.value)} /><ErrorCampo mensaje={op.errorDe('nombre')} /></label>
    <Button type="submit" className="h-11 rounded-xl px-5" disabled={op.ocupado}>{edicion ? 'Guardar cambios' : <><Plus className="size-4" />Crear opción</>}</Button>
   </form>
   <div className="mt-3"><Aviso error={op.error} /></div>
  </section></ZonaEdicion>
  {datos.length === 0 ? <Vacio icono={<SlidersHorizontal className="size-6" />} titulo="Aún no tienes opciones" texto="Crea una opción como Talla o Color para usarla en las versiones de tus productos." />
  : <div className="grid gap-5 lg:grid-cols-[300px_1fr]">
   <ListaLateral elementos={visibles} seleccionado={actual.idVariante} alElegir={setSeleccion} clave={v => v.idVariante} busqueda={busqueda} alBuscar={setBusqueda} placeholder="Buscar opción..." render={v => <><SlidersHorizontal className="size-4 shrink-0 opacity-60" /><span className="truncate text-sm font-medium">{v.nombre}</span></>} />
   <section className="rounded-2xl border bg-card">
    <div className="flex flex-wrap items-start justify-between gap-4 border-b p-5">
     <div><h3 className="text-xl font-semibold tracking-tight">{actual.nombre}</h3><p className="mt-1 text-sm text-muted-foreground">Haz clic en un valor para editarlo.</p></div>
     <ZonaEdicion><Button variant="outline" size="sm" onClick={() => { setNombre(actual.nombre); setEdicion(actual.idVariante); window.scrollTo({ top: 0, behavior: 'smooth' }) }}><Pencil className="size-4" />Editar nombre</Button></ZonaEdicion>
    </div>
    <div className="space-y-6 p-5">
     <ValoresVariante key={actual.idVariante} apiBase={apiBase} variante={actual} />
     <ProductosConVariante key={`p-${actual.idVariante}`} apiBase={apiBase} base={base} variante={actual} />
    </div>
   </section>
  </div>}
 </div>
}

function ProductosConVariante({ apiBase, base, variante }: { apiBase: string; base: string; variante: Variante }) {
 const { datos, error, recargar } = useCarga(async () => {
  const [valores, productos] = await Promise.all([api.leer<Valor[]>(`${apiBase}/variantes/${variante.idVariante}/valores`), api.leer<Producto[]>(`${apiBase}/productos`)])
  const presentaciones = await Promise.all(productos.map(p => api.leer<Presentacion[]>(`${apiBase}/productos/${p.idProducto}/presentaciones`).catch(() => [] as Presentacion[])))
  const nombres = new Map(valores.map(v => [v.idValor, v.valor]))
  return productos.map((producto, i) => ({ producto, valores: [...new Set(presentaciones[i].flatMap(p => p.idsValores).filter(id => nombres.has(id)).map(id => nombres.get(id)!))] })).filter(x => x.valores.length > 0)
 }, [apiBase, variante.idVariante])
 return <div>
  <h4 className="mb-3 text-sm font-semibold">Productos que usan {variante.nombre.toLowerCase()} {datos && <span className="font-normal text-muted-foreground">({datos.length})</span>}</h4>
  {error ? <EstadoCarga error={error} recargar={recargar} /> : !datos ? <div className="space-y-2">{[0, 1].map(i => <div key={i} className="h-[76px] animate-pulse rounded-xl bg-muted" />)}</div>
  : datos.length === 0 ? <p className="rounded-xl border border-dashed px-5 py-8 text-center text-sm text-muted-foreground">Ningún producto usa esta opción todavía. Se asigna al crear las versiones de un producto.</p>
  : <div className="grid gap-2.5 xl:grid-cols-2">{datos.map(({ producto, valores }) => <TarjetaMini key={producto.idProducto} producto={producto} apiBase={apiBase} base={base} detalle={<span className="mt-1 flex flex-wrap gap-1">{valores.map(v => <span key={v} className="rounded-md bg-muted px-1.5 py-0.5 text-[11px] font-medium">{v}</span>)}</span>} />)}</div>}
 </div>
}
function ValoresVariante({ apiBase, variante }: { apiBase: string; variante: Variante }) {
 const ruta = `${apiBase}/variantes/${variante.idVariante}/valores`; const [valor, setValor] = useState(''); const [edicion, setEdicion] = useState(''); const op = useOperacion(['valor']); const { datos, error, recargar } = useCarga(() => api.leer<Valor[]>(ruta), [ruta])
 return <div className="space-y-3">
  <h4 className="text-sm font-semibold">Valores {datos && <span className="font-normal text-muted-foreground">({datos.length})</span>}</h4>
  {error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : datos.length === 0 ? <p className="text-sm text-muted-foreground">Aún no tiene valores.</p>
  : <ZonaEdicion className="flex flex-wrap gap-2">{datos.map(v => <button type="button" key={v.idValor} onClick={() => { setValor(v.valor); setEdicion(v.idValor) }} className={`group inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-sm font-medium transition hover:border-primary/50 ${edicion === v.idValor ? 'border-primary bg-primary/10 text-primary' : 'bg-background'}`}>{v.valor}<Pencil className="size-3 opacity-0 transition group-hover:opacity-60" /></button>)}</ZonaEdicion>}
  <ZonaEdicion><form className="flex flex-wrap gap-2" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { if (edicion) await api.actualizar(`${ruta}/${edicion}`, { valor }); else await api.crear(ruta, { valor }); setValor(''); setEdicion(''); recargar() }) }}>
   <label className="grid min-w-48 flex-1 gap-1"><input className={claseEntrada + conError(op.errorDe('valor'))} aria-label={`Valor de ${variante.nombre}`} placeholder={edicion ? 'Nuevo texto del valor' : 'Añadir un valor, ej. M'} required value={valor} onChange={e => setValor(e.target.value)} /><ErrorCampo mensaje={op.errorDe('valor')} /></label>
   <Button type="submit" variant={edicion ? 'default' : 'outline'} className="h-11 rounded-xl" disabled={op.ocupado}>{edicion ? 'Guardar valor' : <><Plus className="size-4" />Añadir valor</>}</Button>
   {edicion && <Button type="button" variant="ghost" className="h-11 rounded-xl" onClick={() => { setEdicion(''); setValor('') }}>Cancelar</Button>}
  </form></ZonaEdicion>
  <Aviso error={op.error} />
 </div>
}
function Tarjeta({ titulo, descripcion, accion, children, className = '' }: { titulo: string; descripcion?: string; accion?: ReactNode; children: ReactNode; className?: string }) {
 return <section className={`rounded-2xl border bg-card ${className}`}>
  <div className="flex flex-wrap items-start justify-between gap-3 border-b px-5 py-4"><div><h3 className="font-semibold">{titulo}</h3>{descripcion && <p className="mt-0.5 text-sm text-muted-foreground">{descripcion}</p>}</div>{accion}</div>
  <div className="p-5">{children}</div>
 </section>
}

export function DetalleProducto() {
 const { apiBase, base, tienda } = useOutletContext<ContextoTienda>(); const { idProducto = '' } = useParams(); const ruta = `${apiBase}/productos/${idProducto}`
 const { datos, error, recargar } = useCarga(async () => { const [producto, categorias, presentaciones, imagenes, colecciones, asociadas, variantes] = await Promise.all([api.leer<Producto>(ruta), api.leer<Categoria[]>(`${apiBase}/categorias`), api.leer<Presentacion[]>(`${ruta}/presentaciones`), api.leer<Imagen[]>(`${ruta}/imagenes`), api.leer<Coleccion[]>(`${apiBase}/colecciones`), api.leer<Coleccion[]>(`${ruta}/colecciones`), api.leer<Variante[]>(`${apiBase}/variantes`)]); const valores = await Promise.all(variantes.map(async v => ({ ...v, valores: await api.leer<Valor[]>(`${apiBase}/variantes/${v.idVariante}/valores`) }))); const inventario = await api.leer<PaginaInventario>(`${apiBase}/inventario?page=0&size=100&texto=${encodeURIComponent(producto.nombre)}`).catch(() => null); const stock: Record<string, Inventario> = Object.fromEntries((inventario?.contenido ?? []).filter(i => presentaciones.some(p => p.idPresentacion === i.idPresentacion)).map(i => [i.idPresentacion, i])); return { producto, categorias, presentaciones, imagenes, colecciones, asociadas, valores, stock } }, [ruta])
 if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
 const { producto } = datos
 const principal = datos.imagenes.find(i => i.esPrincipal) ?? datos.imagenes[0]
 return <div className="space-y-6">
  {/* Cabecera del producto */}
  <div className="flex flex-wrap items-center gap-4">
   <Link to={`${base}/catalogo`} aria-label="Volver al catálogo" className="grid size-10 shrink-0 place-items-center rounded-xl border bg-card text-muted-foreground transition hover:text-foreground"><ArrowLeft className="size-5" /></Link>
   <span className="relative size-14 shrink-0 overflow-hidden rounded-xl border bg-muted">{principal ? <img src={principal.url} alt="" className="size-full object-cover" /> : <ImageOff className="absolute top-1/2 left-1/2 size-5 -translate-x-1/2 -translate-y-1/2 text-muted-foreground" />}</span>
   <div className="min-w-0 flex-1">
    <p className="text-sm text-muted-foreground"><Link to={`${base}/catalogo`} className="hover:text-primary">Catálogo</Link> / Productos</p>
    <div className="flex flex-wrap items-center gap-2.5"><h2 className="truncate text-2xl font-semibold tracking-tight">{producto.nombre}</h2><EstadoProducto estado={producto.estado} /></div>
   </div>
   <p className="text-2xl font-semibold tracking-tight">{soles(producto.precioBase)}</p>
  </div>

  <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
   <div className="space-y-6">
    <ProductoFormulario key={JSON.stringify(producto)} producto={producto} categorias={datos.categorias} ruta={ruta} apiBase={apiBase} presentaciones={datos.presentaciones} stock={datos.stock} recargar={recargar} />
    <Imagenes ruta={ruta} imagenes={datos.imagenes} recargar={recargar} />
    <Presentaciones ruta={ruta} apiBase={apiBase} presentaciones={datos.presentaciones} valores={datos.valores} imagenes={datos.imagenes} stock={datos.stock} recargar={recargar} />
   </div>
   <div className="space-y-6 lg:sticky lg:top-24">
    <EstadoPublicacion key={JSON.stringify([producto, tienda.estado, datos.presentaciones, datos.stock, datos.categorias])} producto={producto} ruta={ruta} recargar={recargar} />
    <ColeccionesProducto apiBase={apiBase} idProducto={idProducto} colecciones={datos.colecciones} asociadas={datos.asociadas} recargar={recargar} />
    <Tarjeta titulo="Resumen">
     <dl className="space-y-2.5 text-sm">
      {([['Categoría', datos.categorias.find(c => c.idCategoria === producto.idCategoria)?.nombre ?? '—'], ['Precio base', soles(producto.precioBase)], ['Versiones', `${datos.presentaciones.filter(p => p.estado === 'ACTIVA').length} a la venta de ${datos.presentaciones.length}`], ['Unidades disponibles', String(Object.values(datos.stock).reduce((s, i) => s + (i.stockActual ?? 0), 0))], ['Imágenes', String(datos.imagenes.length)], ['Colecciones', String(datos.asociadas.length)]] as const).map(([k, v]) => <div key={k} className="flex justify-between gap-3"><dt className="text-muted-foreground">{k}</dt><dd className="text-right font-medium">{v}</dd></div>)}
     </dl>
     <p className="mt-4 text-xs text-muted-foreground">Las unidades de cada versión también se ajustan en <Link to={`${base}/inventario`} className="font-medium text-primary hover:underline">Inventario</Link>.</p>
    </Tarjeta>
   </div>
  </div>
 </div>
}

function ProductoFormulario({ producto, categorias, ruta, apiBase, presentaciones, stock, recargar }: { producto: Producto; categorias: Categoria[]; ruta: string; apiBase: string; presentaciones: Presentacion[]; stock: Record<string, Inventario>; recargar: () => void }) {
 const unica = presentaciones.length === 1 ? stock[presentaciones[0].idPresentacion] : undefined
 const total = Object.values(stock).reduce((s, i) => s + (i.stockActual ?? 0), 0)
 const [nombre, setNombre] = useState(producto.nombre); const [descripcion, setDescripcion] = useState(producto.descripcion ?? ''); const [precio, setPrecio] = useState(String(producto.precioBase)); const [categoria, setCategoria] = useState(producto.idCategoria); const op = useOperacion(['nombre', 'descripcion', 'precioBase', 'idCategoria']); const [guardado, setGuardado] = useState(false)
 const cambiado = nombre !== producto.nombre || descripcion !== (producto.descripcion ?? '') || precio !== String(producto.precioBase) || categoria !== producto.idCategoria
 return <Tarjeta titulo="Información del producto" descripcion="Nombre, precio, categoría y unidades de tu producto.">
  <ZonaEdicion><form className="grid gap-4 sm:grid-cols-2" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { await api.actualizar(ruta, { nombre, descripcion, precioBase: Number(precio), idCategoria: categoria }); setGuardado(true); recargar() }) }}>
   <label className="grid gap-1.5 text-sm font-medium sm:col-span-2">Nombre<input className={claseEntrada + conError(op.errorDe('nombre'))} required maxLength={150} value={nombre} onChange={e => { setGuardado(false); setNombre(e.target.value) }} /><ErrorCampo mensaje={op.errorDe('nombre')} /></label>
   <label className="grid gap-1.5 text-sm font-medium sm:col-span-2">Descripción<textarea className={`${claseEntrada} h-24 resize-y py-2.5${conError(op.errorDe('descripcion'))}`} value={descripcion} onChange={e => { setGuardado(false); setDescripcion(e.target.value) }} placeholder="Cuenta qué hace especial a este producto." /><ErrorCampo mensaje={op.errorDe('descripcion')} /></label>
   <label className="grid gap-1.5 text-sm font-medium">Precio base<span className="relative"><span className="pointer-events-none absolute top-1/2 left-3.5 -translate-y-1/2 text-sm text-muted-foreground">S/</span><input className={`${claseEntrada} pl-9${conError(op.errorDe('precioBase'))}`} type="number" min="0" step="0.01" required value={precio} onChange={e => { setGuardado(false); setPrecio(e.target.value) }} /></span><ErrorCampo mensaje={op.errorDe('precioBase')} /></label>
   <label className="grid gap-1.5 text-sm font-medium">Categoría<select className={`${claseEntrada} cursor-pointer${conError(op.errorDe('idCategoria'))}`} value={categoria} onChange={e => { setGuardado(false); setCategoria(e.target.value) }}>{categorias.map(c => <option key={c.idCategoria} value={c.idCategoria}>{c.nombre}{c.activa ? '' : ' (inactiva)'}</option>)}</select><ErrorCampo mensaje={op.errorDe('idCategoria')} /></label>
   <div className="flex flex-wrap items-center justify-end gap-3 sm:col-span-2">
    {guardado && !cambiado && <span className="inline-flex items-center gap-1.5 text-sm text-emerald-600 dark:text-emerald-400"><Check className="size-4" />Cambios guardados</span>}
    <Button type="submit" className="h-11 rounded-xl px-5" disabled={op.ocupado || !cambiado}>{op.ocupado ? 'Guardando...' : 'Guardar cambios'}</Button>
   </div>
  </form></ZonaEdicion>
  <div className="mt-3"><Aviso error={op.error} /></div>
  <div className="mt-4 rounded-xl border bg-muted/30 p-4">
   <p className="mb-2 flex items-center gap-2 text-sm font-semibold"><Boxes className="size-4 text-primary" />Unidades</p>
   {unica && !unica.sinRegistroInventario ? <ControlStock apiBase={apiBase} idPresentacion={unica.idPresentacion} inventario={unica} nombre={producto.nombre} alCambiar={recargar} alRecargar={recargar} />
   : presentaciones.length === 0 ? <p className="text-sm text-muted-foreground">Este producto aún no tiene versión. Añade una abajo, en "Versiones del producto", con sus unidades iniciales.</p>
   : <p className="text-sm text-muted-foreground"><strong className="text-foreground">{total} disponibles</strong> en {presentaciones.length} versiones. Ajusta las unidades de cada una abajo, en "Versiones del producto".</p>}
  </div>
 </Tarjeta>
}

const textoRequisito: Record<string, string> = {
 TIENDA_ACTIVA: 'Tu tienda está abierta',
 CATEGORIA_ACTIVA: 'Su categoría está activa',
 VERSION_ACTIVA: 'Tiene al menos una versión a la venta',
 VERSION_CON_UNIDADES: 'Alguna versión a la venta tiene unidades disponibles',
}

function EstadoPublicacion({ producto, ruta, recargar }: { producto: Producto; ruta: string; recargar: () => void }) {
 const op = useOperacion()
 const requisitos = useCarga(() => api.leer<RequisitosPublicacion>(`${ruta}/requisitos`), [ruta])
 const textos: Record<string, string> = { PUBLICADO: 'Tus clientes lo ven en tu tienda.', BORRADOR: 'Solo tú lo ves. Publícalo cuando esté listo.', INACTIVO: 'Oculto: tus clientes no lo ven.' }
 const pendientes = requisitos.datos?.requisitos.filter(r => !r.cumplido) ?? []
 const publicable = !!requisitos.datos?.publicable
 const motivo = !requisitos.datos ? 'Comprobando si está listo para publicar...' : pendientes[0]?.mensaje ?? ''
 const cambiar = (estado: string) => void op.ejecutar(async () => { await api.modificar(`${ruta}/estado?estado=${estado}`); recargar() })
 return <Tarjeta titulo="Estado">
  <div className="flex items-center gap-2.5"><EstadoProducto estado={producto.estado} /><span className="text-sm text-muted-foreground">{textos[producto.estado] ?? ''}</span></div>

  <div className="mt-4 rounded-xl border bg-muted/30 p-3.5">
   <p className="text-sm font-semibold">Listo para publicar</p>
   {requisitos.error ? <div className="mt-2"><EstadoCarga error={requisitos.error} recargar={requisitos.recargar} /></div>
   : !requisitos.datos ? <div className="mt-2 space-y-2">{[0, 1, 2, 3].map(i => <div key={i} className="h-4 animate-pulse rounded bg-muted" />)}</div>
   : <ul className="mt-2 space-y-2">{requisitos.datos.requisitos.map(r => <li key={r.codigo} className="flex gap-2 text-sm">
     {r.cumplido ? <CircleCheck className="mt-0.5 size-4 shrink-0 text-emerald-600 dark:text-emerald-400" /> : <CircleX className="mt-0.5 size-4 shrink-0 text-amber-600 dark:text-amber-400" />}
     <span className={r.cumplido ? '' : 'text-muted-foreground'}>{r.cumplido ? textoRequisito[r.codigo] ?? r.mensaje : r.mensaje}</span>
    </li>)}</ul>}
  </div>

  <ZonaEdicion className="mt-4 grid gap-2">
   {producto.estado !== 'PUBLICADO' && <>
    <Button className="h-10 w-full rounded-xl" disabled={op.ocupado || !publicable} aria-describedby={!publicable ? 'motivo-publicar' : undefined} onClick={() => cambiar('PUBLICADO')}><Eye className="size-4" />Publicar</Button>
    {!publicable && motivo && <p id="motivo-publicar" className="text-xs text-muted-foreground">{motivo}</p>}
   </>}
   {producto.estado !== 'INACTIVO' && <Button variant="outline" className="h-10 w-full rounded-xl" disabled={op.ocupado} onClick={() => cambiar('INACTIVO')}><EyeOff className="size-4" />Ocultar</Button>}
   {producto.estado !== 'BORRADOR' && <Button variant="outline" className="h-10 w-full rounded-xl" disabled={op.ocupado} onClick={() => cambiar('BORRADOR')}>Pasar a borrador</Button>}
  </ZonaEdicion>
  <div className="mt-3"><Aviso error={op.error} /></div>
 </Tarjeta>
}

type VarianteConValores = Variante & { valores: Valor[] }
function SelectorValores({ variantes, seleccion, cambiar }: { variantes: VarianteConValores[]; seleccion: Record<string, string>; cambiar: (v: Record<string, string>) => void }) { return <div className="grid gap-3 sm:grid-cols-3">{variantes.map(v => <label className="grid gap-1.5 text-sm font-medium" key={v.idVariante}>{v.nombre}<select className={`${claseEntrada} cursor-pointer`} value={seleccion[v.idVariante] ?? ''} onChange={e => cambiar({ ...seleccion, [v.idVariante]: e.target.value })}><option value="">Sin seleccionar</option>{v.valores.map(x => <option key={x.idValor} value={x.idValor}>{x.valor}</option>)}</select></label>)}</div> }

function Presentaciones({ ruta, apiBase, presentaciones, valores, imagenes, stock, recargar }: { ruta: string; apiBase: string; presentaciones: Presentacion[]; valores: VarianteConValores[]; imagenes: Imagen[]; stock: Record<string, Inventario>; recargar: () => void }) {
 const [stockInicial, setStockInicial] = useState(''); const [stockMinimoInicial, setStockMinimoInicial] = useState(''); const [sku, setSku] = useState(''); const [precio, setPrecio] = useState(''); const [seleccion, setSeleccion] = useState<Record<string,string>>({}); const op = useOperacion(['sku', 'precioOverride', 'idsValores', 'stockInicial', 'stockMinimo']); const [mostrarNueva, setMostrarNueva] = useState(false); const [mostrarGenerador, setMostrarGenerador] = useState(false)
 const formularioVisible = mostrarNueva || presentaciones.length === 0
 return <Tarjeta titulo="Versiones del producto" descripcion="Cada combinación que vendes (por ejemplo talla M, color azul), con su precio y sus unidades." accion={<ZonaEdicion className="flex gap-2">{valores.some(v => v.valores.length > 0) && <Button variant="outline" size="sm" onClick={() => setMostrarGenerador(g => !g)}><Sparkles className="size-4" />Generar combinaciones</Button>}{!formularioVisible && <Button variant="outline" size="sm" onClick={() => setMostrarNueva(true)}><Plus className="size-4" />Añadir</Button>}</ZonaEdicion>}>
  <div className="space-y-4">
   {mostrarGenerador && <GeneradorCombinaciones ruta={ruta} variantes={valores} presentaciones={presentaciones} alCerrar={() => setMostrarGenerador(false)} alTerminar={() => { setMostrarGenerador(false); recargar() }} />}
   {formularioVisible && <ZonaEdicion><form className="space-y-4 rounded-xl border border-dashed bg-muted/30 p-4" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { await api.crear<Presentacion>(`${ruta}/presentaciones`, { sku: sku.trim() || undefined, precioOverride: precio === '' ? null : Number(precio), idsValores: Object.values(seleccion).filter(Boolean), stockInicial: stockInicial === '' ? undefined : Number(stockInicial), stockMinimo: stockMinimoInicial === '' ? undefined : Number(stockMinimoInicial) }); setSku(''); setPrecio(''); setStockInicial(''); setStockMinimoInicial(''); setSeleccion({}); setMostrarNueva(false); recargar() }) }}>
    <div className="flex items-center justify-between gap-3"><p className="text-sm font-semibold">Nueva versión</p>{presentaciones.length > 0 && <button type="button" aria-label="Cerrar" onClick={() => setMostrarNueva(false)} className="grid size-7 cursor-pointer place-items-center rounded-lg text-muted-foreground hover:bg-muted"><X className="size-4" /></button>}</div>
    <div className="grid gap-3 sm:grid-cols-2">
     <CampoCodigoInterno valor={sku} cambiar={setSku} error={op.errorDe('sku')} />
     <label className="grid gap-1.5 text-sm font-medium"><span>Precio especial <span className="font-normal text-muted-foreground">(opcional)</span></span><span className="relative"><span className="pointer-events-none absolute top-1/2 left-3.5 -translate-y-1/2 text-sm text-muted-foreground">S/</span><input className={`${claseEntrada} pl-9${conError(op.errorDe('precioOverride'))}`} type="number" min="0" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} placeholder="Usa el precio base" /></span><ErrorCampo mensaje={op.errorDe('precioOverride')} /></label>
    </div>
    {valores.length > 0 ? <SelectorValores variantes={valores} seleccion={seleccion} cambiar={setSeleccion} /> : <p className="text-xs text-muted-foreground">Sin opciones: esta será la versión única del producto.</p>}
    <ErrorCampo mensaje={op.errorDe('idsValores')} />
    <div className="grid gap-3 sm:grid-cols-2">
     <label className="grid gap-1.5 text-sm font-medium"><span>Unidades iniciales <span className="font-normal text-muted-foreground">(opcional)</span></span><input className={claseEntrada + conError(op.errorDe('stockInicial'))} type="number" min="0" step="1" value={stockInicial} onChange={e => setStockInicial(e.target.value)} placeholder="Ej. 20" /><ErrorCampo mensaje={op.errorDe('stockInicial')} /></label>
     <label className="grid gap-1.5 text-sm font-medium"><span>Avisarme cuando queden <span className="font-normal text-muted-foreground">(opcional)</span></span><input className={claseEntrada + conError(op.errorDe('stockMinimo'))} type="number" min="0" step="1" value={stockMinimoInicial} onChange={e => setStockMinimoInicial(e.target.value)} placeholder="Ej. 3" /><ErrorCampo mensaje={op.errorDe('stockMinimo')} /></label>
    </div>
    <div className="flex justify-end"><Button type="submit" className="h-10 rounded-xl" disabled={op.ocupado}><Plus className="size-4" />Añadir versión</Button></div>
    <Aviso error={op.error} />
   </form></ZonaEdicion>}
   {presentaciones.length > 0 && <ListaPresentaciones presentaciones={presentaciones} variantes={valores} stock={stock} fila={(p, ocultar) => <PresentacionFila key={`${p.idPresentacion}-${p.sku}-${p.precioOverride}-${p.estado}-${p.idsValores.join(",")}-${stock[p.idPresentacion]?.stockActual}-${stock[p.idPresentacion]?.stockMinimo}`} presentacion={p} ruta={`${ruta}/presentaciones/${p.idPresentacion}`} apiBase={apiBase} variantes={valores} imagenes={imagenes} inventario={stock[p.idPresentacion]} ocultarVariante={ocultar} recargar={recargar} />} />}
  </div>
 </Tarjeta>
}

function PresentacionFila({ presentacion, ruta, apiBase, variantes, imagenes, inventario, ocultarVariante, recargar }: { presentacion: Presentacion; ruta: string; apiBase: string; variantes: VarianteConValores[]; imagenes: Imagen[]; inventario?: Inventario; ocultarVariante?: string; recargar: () => void }) {
 const unidades = inventario?.stockActual ?? null
 const stockBajo = inventario && !inventario.sinRegistroInventario && (inventario.stockActual ?? 0) <= (inventario.stockMinimo ?? 0)
 const [sku, setSku] = useState(presentacion.sku); const [precio, setPrecio] = useState(presentacion.precioOverride === null ? '' : String(presentacion.precioOverride)); const [seleccion, setSeleccion] = useState<Record<string,string>>(() => Object.fromEntries(variantes.flatMap(variante => { const valor = variante.valores.find(v => presentacion.idsValores.includes(v.idValor)); return valor ? [[variante.idVariante, valor.idValor]] : [] }))); const op = useOperacion(['sku', 'precioOverride', 'idsValores']); const asignadas = useCarga(() => api.leer<Imagen[]>(`${apiBase}/presentaciones/${presentacion.idPresentacion}/imagenes`), [apiBase, presentacion.idPresentacion])
 const [abierta, setAbierta] = useState(false)
 const activa = presentacion.estado === 'ACTIVA'
 const etiquetas = variantes.filter(v => v.idVariante !== ocultarVariante).flatMap(v => v.valores.filter(x => presentacion.idsValores.includes(x.idValor)).map(x => `${v.nombre.charAt(0) + v.nombre.slice(1).toLowerCase()}: ${x.valor}`))
 const miniatura = asignadas.datos?.[0]
 return <div className={activa ? 'bg-background' : 'bg-muted/40'}>
  <button type="button" onClick={() => setAbierta(a => !a)} className="flex w-full cursor-pointer flex-wrap items-center gap-3 px-3.5 py-2.5 text-left transition hover:bg-muted/40">
   <span className="relative size-9 shrink-0 overflow-hidden rounded-lg bg-muted">{miniatura ? <img src={miniatura.url} alt="" className="size-full object-cover" /> : <Package className="absolute top-1/2 left-1/2 size-4 -translate-x-1/2 -translate-y-1/2 text-muted-foreground" />}</span>
   <span className="min-w-0 flex-1">
    <span className="flex flex-wrap items-center gap-1.5">{etiquetas.length ? etiquetas.map(t => <span key={t} className="inline-flex items-center gap-1.5 rounded-md bg-muted px-2 py-0.5 text-xs font-medium">{muestra(t.split(': ')[1]) && <span className="size-2.5 rounded-full border border-black/15" style={{ background: muestra(t.split(': ')[1]) }} />}{t}</span>) : !ocultarVariante && <span className="text-sm font-medium">Versión única</span>}</span>
    <span className="mt-0.5 block truncate font-mono text-[11px] text-muted-foreground">{presentacion.sku}</span>
   </span>
   {unidades !== null && <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${unidades === 0 ? 'bg-red-500/12 text-red-600 dark:text-red-400' : stockBajo ? 'bg-amber-500/15 text-amber-700 dark:text-amber-400' : 'bg-muted text-foreground'}`}>{unidades === 0 ? 'Agotado' : `${unidades} ${unidades === 1 ? 'disponible' : 'disponibles'}`}{(inventario?.stockReservado ?? 0) > 0 && ` · ${inventario!.stockReservado} ${inventario!.stockReservado === 1 ? 'apartada' : 'apartadas'}`}</span>}
   <span className="text-right"><span className="block font-semibold">{soles(presentacion.precioEfectivo)}</span>{presentacion.precioOverride !== null && <span className="text-[11px] text-muted-foreground">precio especial</span>}</span>
   <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${claseTono[etiquetaVersion(presentacion.estado).tono].clase}`}>{etiquetaVersion(presentacion.estado).texto}</span>
   <ChevronDown className={`size-4 text-muted-foreground transition-transform ${abierta ? 'rotate-180' : ''}`} />
  </button>
  {abierta && <div className="space-y-5 border-t p-4">
   {inventario && !inventario.sinRegistroInventario && <ControlStock apiBase={apiBase} idPresentacion={presentacion.idPresentacion} inventario={inventario} nombre={etiquetas.join(', ') || presentacion.sku} alCambiar={recargar} alRecargar={recargar} />}
   <ZonaEdicion className="block space-y-5"><form className="grid items-start gap-3 sm:grid-cols-[1fr_1fr_auto]" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { await api.actualizar(ruta, { sku, precioOverride: precio === '' ? null : Number(precio) }); recargar() }) }}>
    <CampoCodigoInterno valor={sku} cambiar={setSku} error={op.errorDe('sku')} obligatorio />
    <label className="grid gap-1.5 text-sm font-medium">Precio especial<span className="relative"><span className="pointer-events-none absolute top-1/2 left-3.5 -translate-y-1/2 text-sm text-muted-foreground">S/</span><input className={`${claseEntrada} pl-9${conError(op.errorDe('precioOverride'))}`} type="number" min="0" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} placeholder="Precio base" /></span><ErrorCampo mensaje={op.errorDe('precioOverride')} /></label>
    <Button type="submit" className="h-11 rounded-xl sm:mt-[26px]" disabled={op.ocupado}>Guardar</Button>
   </form>
   {variantes.length > 0 && <div className="space-y-3"><p className="text-sm font-semibold">Combinación de opciones</p><SelectorValores variantes={variantes} seleccion={seleccion} cambiar={setSeleccion} /><ErrorCampo mensaje={op.errorDe('idsValores')} /><Button variant="outline" size="sm" disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { await api.actualizar(`${ruta}/valores`, Object.values(seleccion).filter(Boolean)); recargar() })}>Guardar combinación</Button></div>}
   {imagenes.length > 0 && <div className="space-y-3"><p className="text-sm font-semibold">Fotos de esta versión <span className="font-normal text-muted-foreground">(toca para asignar o quitar)</span></p>
    {asignadas.error || !asignadas.datos ? <EstadoCarga error={asignadas.error} recargar={asignadas.recargar} /> : <div className="flex flex-wrap gap-2">{imagenes.map(imagen => { const vinculada = asignadas.datos!.some(a => a.idImagen === imagen.idImagen); return <button type="button" key={imagen.idImagen} aria-pressed={vinculada} disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { const r = `${apiBase}/presentaciones/${presentacion.idPresentacion}/imagenes/${imagen.idImagen}`; if (vinculada) await api.eliminar(r); else await api.crear(r); asignadas.recargar() })} className={`relative size-16 cursor-pointer overflow-hidden rounded-lg border-2 transition ${vinculada ? 'border-primary' : 'border-transparent opacity-60 hover:opacity-100'}`}><img src={imagen.url} alt={`Imagen ${imagen.orden}`} className="size-full object-cover" />{vinculada && <span className="absolute top-1 right-1 grid size-5 place-items-center rounded-full bg-primary text-primary-foreground"><Check className="size-3" strokeWidth={3} /></span>}</button> })}</div>}
   </div>}
   <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-4"><Aviso error={op.error} /><Button type="button" variant="outline" size="sm" className="ml-auto" disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { await api.modificar(`${ruta}/estado?estado=${activa ? 'INACTIVA' : 'ACTIVA'}`); recargar() })}>{activa ? 'Pausar la venta' : 'Poner a la venta'}</Button></div>
   </ZonaEdicion>
  </div>}
 </div>
}

function Imagenes({ ruta, imagenes, recargar }: { ruta: string; imagenes: Imagen[]; recargar: () => void }) {
 const op = useOperacion(); const base = `${ruta}/imagenes`
 const ordenadas = [...imagenes].sort((a, b) => Number(b.esPrincipal) - Number(a.esPrincipal) || a.orden - b.orden)
 function subir(archivo: File) { const datos = new FormData(); datos.append('archivo', archivo); void op.ejecutar(async () => { await api.crear(`${base}?orden=${Math.max(0, ...imagenes.map(i => i.orden)) + 1}&esPrincipal=${imagenes.length === 0}`, datos); recargar() }) }
 const accionImagen = 'grid size-8 cursor-pointer place-items-center rounded-lg bg-white/90 text-zinc-800 shadow transition hover:bg-white disabled:opacity-40'
 return <Tarjeta titulo="Fotos" descripcion="La foto principal es la que se ve en el catálogo. Pasa el mouse sobre una foto para ver sus opciones.">
  <ZonaEdicion><div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">
   {ordenadas.map((i, indice) => <div key={i.idImagen} className={`group relative aspect-square overflow-hidden rounded-xl border bg-muted ${indice === 0 ? 'sm:col-span-2 sm:row-span-2' : ''}`}>
    <img src={i.url} alt={`Imagen ${i.orden}`} className="size-full object-cover" />
    {i.esPrincipal && <span className="absolute top-2 left-2 inline-flex items-center gap-1 rounded-full bg-primary px-2 py-0.5 text-[11px] font-semibold text-primary-foreground"><Star className="size-3 fill-current" />Principal</span>}
    <span className="absolute bottom-2 left-2 rounded-md bg-black/55 px-1.5 py-0.5 text-[11px] font-medium text-white">#{i.orden}</span>
    <div className="absolute top-2 right-2 flex flex-col gap-1.5 opacity-0 transition group-hover:opacity-100 focus-within:opacity-100">
     {!i.esPrincipal && <button type="button" title="Hacer principal" aria-label="Hacer principal" className={accionImagen} disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { await api.modificar(`${base}/${i.idImagen}/principal`); recargar() })}><Star className="size-4" /></button>}
     <label title="Reemplazar" className={accionImagen}><RefreshCw className="size-4" /><span className="sr-only">Reemplazar</span><input type="file" className="sr-only" accept="image/*" onChange={e => { const archivo = e.target.files?.[0]; if (!archivo) return; const datos = new FormData(); datos.append('archivo', archivo); void op.ejecutar(async () => { await api.actualizar(`${base}/${i.idImagen}/archivo`, datos); recargar() }); e.target.value = '' }} /></label>
     <button type="button" title="Cambiar orden" aria-label="Cambiar orden" className={accionImagen} disabled={op.ocupado} onClick={() => { const orden = window.prompt('Nuevo orden de la imagen', String(i.orden)); if (orden && /^\d+$/.test(orden) && Number(orden) > 0) void op.ejecutar(async () => { await api.actualizar(`${base}/${i.idImagen}`, { orden: Number(orden), esPrincipal: i.esPrincipal }); recargar() }) }}><ArrowUpDown className="size-4" /></button>
     <button type="button" title="Eliminar" aria-label="Eliminar" className={`${accionImagen} hover:text-red-600`} disabled={op.ocupado} onClick={() => { if (window.confirm('¿Eliminar esta imagen?')) void op.ejecutar(async () => { await api.eliminar(`${base}/${i.idImagen}`); recargar() }) }}><Trash2 className="size-4" /></button>
    </div>
   </div>)}
   <label className={`flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed text-center text-sm text-muted-foreground transition hover:border-primary hover:bg-primary/5 hover:text-primary ${op.ocupado ? 'pointer-events-none opacity-60' : ''} ${imagenes.length === 0 ? 'col-span-2 aspect-[3/1] sm:col-span-3 xl:col-span-4' : 'aspect-square'}`}>
    <ImagePlus className="size-7" />
    <span className="font-medium">{op.ocupado ? 'Subiendo...' : imagenes.length === 0 ? 'Sube la primera foto' : 'Añadir foto'}</span>
    {imagenes.length === 0 && <span className="text-xs">JPG o PNG. Será la foto principal.</span>}
    <input type="file" accept="image/*" className="sr-only" disabled={op.ocupado} onChange={e => { const archivo = e.target.files?.[0]; if (archivo) subir(archivo); e.target.value = '' }} />
   </label>
  </div></ZonaEdicion>
  <div className="mt-3"><Aviso error={op.error} /></div>
 </Tarjeta>
}

function ColeccionesProducto({ apiBase, idProducto, colecciones, asociadas, recargar }: { apiBase: string; idProducto: string; colecciones: Coleccion[]; asociadas: Coleccion[]; recargar: () => void }) {
 const op = useOperacion()
 return <Tarjeta titulo="Colecciones" descripcion="Toca una colección para agregar o quitar el producto.">
  {colecciones.length === 0 ? <p className="text-sm text-muted-foreground">Crea colecciones en la pestaña Colecciones del catálogo para asignarlas aquí.</p>
  : <ZonaEdicion className="flex flex-wrap gap-2">{colecciones.map(c => { const asignada = asociadas.some(a => a.idColeccion === c.idColeccion); return <button type="button" key={c.idColeccion} aria-pressed={asignada} disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { const ruta = `${apiBase}/colecciones/${c.idColeccion}/productos/${idProducto}`; if (asignada) await api.eliminar(ruta); else await api.crear(ruta); recargar() })} className={`inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-medium transition ${asignada ? 'border-primary bg-primary text-primary-foreground' : 'bg-background hover:border-primary/50'}`}>{asignada ? <Check className="size-3.5" strokeWidth={3} /> : <Plus className="size-3.5" />}{c.nombre}{!c.activa && <span className="opacity-70">(inactiva)</span>}</button> })}</ZonaEdicion>}
  <div className="mt-3"><Aviso error={op.error} /></div>
 </Tarjeta>
}

// Crea de una vez una presentación por cada combinación de valores elegidos (ej. S/M/L × Rojo/Negro).
function GeneradorCombinaciones({ ruta, variantes, presentaciones, alCerrar, alTerminar }: { ruta: string; variantes: VarianteConValores[]; presentaciones: Presentacion[]; alCerrar: () => void; alTerminar: () => void }) {
 const [elegidos, setElegidos] = useState<Record<string, string[]>>({}); const [precio, setPrecio] = useState(''); const [stock, setStock] = useState(''); const [stockMinimo, setStockMinimo] = useState(''); const [prefijo, setPrefijo] = useState('PROD'); const [progreso, setProgreso] = useState(''); const op = useOperacion()
 const conValores = variantes.filter(v => (elegidos[v.idVariante] ?? []).length > 0)
 const combinaciones = conValores.reduce<Valor[][]>((acc, v) => acc.flatMap(c => v.valores.filter(x => elegidos[v.idVariante].includes(x.idValor)).map(x => [...c, x])), [[]]).filter(c => c.length > 0)
 const existe = (c: Valor[]) => presentaciones.some(p => p.idsValores.length === c.length && c.every(x => p.idsValores.includes(x.idValor)))
 const nuevas = combinaciones.filter(c => !existe(c))
 const alternar = (idVariante: string, idValor: string) => setElegidos(e => { const lista = e[idVariante] ?? []; return { ...e, [idVariante]: lista.includes(idValor) ? lista.filter(x => x !== idValor) : [...lista, idValor] } })
 const limpiarSku = (t: string) => t.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().replace(/[^A-Z0-9]+/g, '-').replace(/^-+|-+$/g, '')
 return <div className="space-y-4 rounded-xl border border-primary/30 bg-primary/5 p-4">
  <div className="flex items-start justify-between gap-3"><div><p className="flex items-center gap-2 text-sm font-semibold"><Sparkles className="size-4 text-primary" />Generar combinaciones</p><p className="mt-0.5 text-xs text-muted-foreground">Marca los valores que vendes. Se creará una versión por cada combinación (las que ya existen se omiten).</p></div><button type="button" aria-label="Cerrar" onClick={alCerrar} className="grid size-7 cursor-pointer place-items-center rounded-lg text-muted-foreground hover:bg-muted"><X className="size-4" /></button></div>
  {variantes.filter(v => v.valores.length > 0).map(v => <div key={v.idVariante}>
   <p className="mb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">{v.nombre}</p>
   <div className="flex flex-wrap gap-2">{v.valores.map(x => { const activo = (elegidos[v.idVariante] ?? []).includes(x.idValor); return <button type="button" key={x.idValor} aria-pressed={activo} onClick={() => alternar(v.idVariante, x.idValor)} className={`inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-medium transition ${activo ? 'border-primary bg-primary text-primary-foreground' : 'bg-background hover:border-primary/50'}`}>{activo && <Check className="size-3.5" strokeWidth={3} />}{x.valor}</button> })}</div>
  </div>)}
  <div className="grid gap-3 sm:grid-cols-4">
   <label className="grid gap-1.5 text-sm font-medium">Prefijo del código interno<input className={claseEntrada} value={prefijo} onChange={e => setPrefijo(limpiarSku(e.target.value))} maxLength={30} /></label>
   <label className="grid gap-1.5 text-sm font-medium">Precio especial<input className={claseEntrada} type="number" min="0" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} placeholder="Precio base" /></label>
   <label className="grid gap-1.5 text-sm font-medium">Unidades c/u<input className={claseEntrada} type="number" min="0" step="1" value={stock} onChange={e => setStock(e.target.value)} placeholder="Ej. 10" /></label>
   <label className="grid gap-1.5 text-sm font-medium">Avisarme cuando queden<input className={claseEntrada} type="number" min="0" step="1" value={stockMinimo} onChange={e => setStockMinimo(e.target.value)} placeholder="Ej. 2" /></label>
  </div>
  {nuevas.length > 0 && <div className="flex flex-wrap gap-1.5">{nuevas.slice(0, 24).map(c => <span key={c.map(x => x.idValor).join('-')} className="rounded-md bg-background px-2 py-1 font-mono text-[11px]">{prefijo || 'PROD'}-{c.map(x => limpiarSku(x.valor)).join('-')}</span>)}{nuevas.length > 24 && <span className="px-2 py-1 text-xs text-muted-foreground">y {nuevas.length - 24} más…</span>}</div>}
  <div className="flex flex-wrap items-center justify-between gap-3">
   <p className="text-sm text-muted-foreground">{progreso || (combinaciones.length ? `${nuevas.length} nuevas de ${combinaciones.length} combinaciones` : 'Elige al menos un valor.')}</p>
   <Button className="h-10 rounded-xl" disabled={op.ocupado || nuevas.length === 0} onClick={() => void op.ejecutar(async () => {
    let hechas = 0
    for (const c of nuevas) {
     setProgreso(`Creando ${hechas + 1} de ${nuevas.length}...`)
     await api.crear<Presentacion>(`${ruta}/presentaciones`, { sku: `${prefijo || 'PROD'}-${c.map(x => limpiarSku(x.valor)).join('-')}`, precioOverride: precio === '' ? null : Number(precio), idsValores: c.map(x => x.idValor), stockInicial: stock === '' ? undefined : Number(stock), stockMinimo: stockMinimo === '' ? undefined : Number(stockMinimo) })
     hechas++
    }
    setProgreso(''); alTerminar()
   })}><Sparkles className="size-4" />{op.ocupado ? 'Creando...' : `Crear ${nuevas.length} ${nuevas.length === 1 ? 'versión' : 'versiones'}`}</Button>
  </div>
  <Aviso error={op.error} />
 </div>
}

const coloresMuestra: Record<string, string> = { rojo: '#d32f2f', negro: '#111', blanco: '#fff', azul: '#1e4fd8', celeste: '#5bb8f0', verde: '#2e8b57', amarillo: '#f4c430', naranja: '#f08a24', rosado: '#f48fb1', rosa: '#f48fb1', morado: '#7b3fa0', gris: '#8a8a8a', marron: '#7b4a2a', 'marrón': '#7b4a2a', beige: '#e8d8bd', crema: '#f3ead8', dorado: '#c9a96e', plateado: '#c0c0c0', vino: '#7b1e2b', turquesa: '#30c5c0', fucsia: '#e3268f' }
const muestra = (valor?: string) => valor ? coloresMuestra[valor.trim().toLowerCase()] : undefined

// Lista de presentaciones agrupada por una variante (ej. por Talla), con búsqueda y grupos plegables.
function ListaPresentaciones({ presentaciones, variantes, stock, fila }: { presentaciones: Presentacion[]; variantes: VarianteConValores[]; stock: Record<string, Inventario>; fila: (p: Presentacion, ocultarVariante?: string) => ReactNode }) {
 const usadas = variantes.filter(v => new Set(presentaciones.flatMap(p => p.idsValores.filter(id => v.valores.some(x => x.idValor === id)))).size > 1)
 const [agrupar, setAgrupar] = useState(usadas[0]?.idVariante ?? '')
 const [busqueda, setBusqueda] = useState(''); const [cerrados, setCerrados] = useState<string[]>([])
 const variante = usadas.find(v => v.idVariante === agrupar)
 const termino = busqueda.trim().toLowerCase()
 const texto = (p: Presentacion) => [p.sku, ...variantes.flatMap(v => v.valores.filter(x => p.idsValores.includes(x.idValor)).map(x => x.valor))].join(' ').toLowerCase()
 const filtradas = presentaciones.filter(p => !termino || texto(p).includes(termino))
 const orden = (p: Presentacion) => variantes.map(v => String(v.valores.findIndex(x => p.idsValores.includes(x.idValor)) + 100)).join('')
 const ordenadas = [...filtradas].sort((a, b) => orden(a).localeCompare(orden(b)))
 const grupos: { clave: string; titulo: string; lista: Presentacion[] }[] = variante
  ? [...variante.valores.map(x => ({ clave: x.idValor, titulo: x.valor, lista: ordenadas.filter(p => p.idsValores.includes(x.idValor)) })), { clave: 'sin', titulo: `Sin ${variante.nombre.toLowerCase()}`, lista: ordenadas.filter(p => !variante.valores.some(x => p.idsValores.includes(x.idValor))) }].filter(g => g.lista.length > 0)
  : [{ clave: 'todas', titulo: '', lista: ordenadas }]
 const unidades = (lista: Presentacion[]) => lista.filter(p => p.estado === 'ACTIVA').reduce((s, p) => s + (stock[p.idPresentacion]?.stockActual ?? 0), 0)
 const alternar = (clave: string) => setCerrados(c => c.includes(clave) ? c.filter(x => x !== clave) : [...c, clave])
 return <div className="space-y-3">
  {presentaciones.length > 3 && <div className="flex flex-wrap items-center gap-2">
   <label className="relative min-w-48 flex-1"><span className="sr-only">Buscar versión</span><Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" /><input className="h-9 w-full rounded-lg border border-input bg-background pr-3 pl-9 text-sm outline-none focus:border-primary" placeholder="Buscar código, talla, color..." value={busqueda} onChange={e => setBusqueda(e.target.value)} /></label>
   {usadas.length > 0 && <div className="flex items-center gap-1 rounded-lg border bg-background p-0.5 text-xs"><span className="px-2 text-muted-foreground">Agrupar:</span>{[...usadas.map(v => [v.idVariante, v.nombre.charAt(0) + v.nombre.slice(1).toLowerCase()] as const), ['', 'Ninguno'] as const].map(([id, nombre]) => <button key={id || 'ninguno'} type="button" aria-pressed={agrupar === id} onClick={() => setAgrupar(id)} className={`h-7 cursor-pointer rounded-md px-2.5 font-medium transition ${agrupar === id ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted'}`}>{nombre}</button>)}</div>}
   <span className="text-xs text-muted-foreground">{filtradas.length} de {presentaciones.length}</span>
  </div>}
  {filtradas.length === 0 ? <p className="rounded-xl border border-dashed px-4 py-6 text-center text-sm text-muted-foreground">Ninguna versión coincide.</p>
  : grupos.map(g => {
   const cerrado = cerrados.includes(g.clave)
   return <div key={g.clave} className="overflow-hidden rounded-xl border">
    {g.titulo && <button type="button" onClick={() => alternar(g.clave)} className="flex w-full cursor-pointer items-center gap-3 bg-muted/50 px-4 py-2.5 text-left transition hover:bg-muted">
     <ChevronDown className={`size-4 text-muted-foreground transition-transform ${cerrado ? '-rotate-90' : ''}`} />
     {muestra(g.titulo) && <span className="size-3.5 rounded-full border border-black/15" style={{ background: muestra(g.titulo) }} />}
     <span className="text-sm font-semibold">{variante ? `${variante.nombre.charAt(0) + variante.nombre.slice(1).toLowerCase()}: ` : ''}{g.titulo}</span>
     <span className="ml-auto flex items-center gap-3 text-xs text-muted-foreground"><span>{g.lista.length} {g.lista.length === 1 ? 'versión' : 'versiones'}</span><span className="rounded-full bg-background px-2 py-0.5 font-semibold text-foreground">{unidades(g.lista)} disponibles</span></span>
    </button>}
    {!cerrado && <div className="divide-y">{g.lista.map(p => fila(p, variante?.idVariante))}</div>}
   </div>
  })}
 </div>
}
