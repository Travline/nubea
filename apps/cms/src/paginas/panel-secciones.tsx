import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, BookOpen, Boxes, Check, ChevronDown, CreditCard, ExternalLink, Eye, LayoutTemplate, Package, Palette, Plus, Search, ShoppingCart, Sparkles, Store } from 'lucide-react'
import { useSesion } from '@/autenticacion/sesion'
import { api, rutaTienda } from '@/comercio/datos'
import type { CuentaPago, Plantilla, Producto, Tienda } from '@/comercio/datos'
import { EstadoCarga, useCarga } from '@/comercio/comunes'
import { MiniaturaEnVivo, ModalVistaPrevia, PLANTILLAS_VISIBLES, infoPlantillas } from '@/components/plantillas/vista-plantillas'

type Pedido = { id: string; estado: string; fecha: string; subtotal: number; impuesto: number; total: number }
const soles = (valor: number) => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor)
const capitalizar = (texto: string) => texto.charAt(0) + texto.slice(1).toLowerCase().replace(/_/g, ' ')
const tarjeta = 'rounded-2xl border bg-card'
const iconoAzul = 'bg-primary/10 text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]'

function Encabezado({ titulo, descripcion, accion }: { titulo: ReactNode; descripcion: string; accion?: ReactNode }) {
  return <div className="flex flex-wrap items-end justify-between gap-4">
    <div><h1 className="text-3xl font-semibold tracking-tight">{titulo}</h1><p className="mt-1.5 text-muted-foreground">{descripcion}</p></div>
    {accion}
  </div>
}

/* =====================================================================
   INICIO: lo primero que ve el vendedor al entrar
   ===================================================================== */
export function InicioVendedor() {
  const { usuario } = useSesion()
  const { datos: tiendas, error, recargar } = useCarga(() => api.leer<Tienda[]>(`/api/tiendas/propietario/${usuario!.idUsuario}`), [usuario!.idUsuario])
  const [idSeleccionada, establecerIdSeleccionada] = useState('')
  const primerNombre = (usuario?.nombres ?? '').trim().split(/\s+/)[0]
  const hoy = new Intl.DateTimeFormat('es-PE', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date())

  return <>
    <Encabezado titulo={<>Hola, {primerNombre} <span className="inline-block">👋</span></>} descripcion={`Hoy es ${hoy}. Este es el resumen de tu negocio.`} />
    {error || !tiendas ? <EstadoCarga error={error} recargar={recargar} />
      : tiendas.length === 0 ? <Bienvenida />
      : <ResumenInicio key={(tiendas.find(t => t.idTienda === idSeleccionada) ?? tiendas[0]).idTienda} tienda={tiendas.find(t => t.idTienda === idSeleccionada) ?? tiendas[0]} tiendas={tiendas} alCambiar={establecerIdSeleccionada} />}
  </>
}

function Bienvenida() {
  return <section className="relative overflow-hidden rounded-3xl border bg-card p-8 sm:p-12">
    <div className="pointer-events-none absolute -top-24 -right-24 size-80 rounded-full bg-primary/10 blur-3xl dark:bg-[#4d7cff]/20" />
    <div className="relative max-w-xl">
      <span className={`grid size-14 place-items-center rounded-2xl ${iconoAzul}`}><Sparkles className="size-7" /></span>
      <h2 className="mt-6 text-2xl font-semibold tracking-tight sm:text-3xl">Bienvenido a Nubea</h2>
      <p className="mt-3 text-muted-foreground">Crea tu primera tienda para empezar a subir productos, elegir una plantilla y recibir pedidos desde tu propio enlace.</p>
      <Link to="/panel/tiendas" className="mt-7 inline-flex h-11 items-center gap-2 rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground transition hover:opacity-90"><Plus className="size-4" />Crear mi primera tienda</Link>
    </div>
  </section>
}

function ResumenInicio({ tienda, tiendas, alCambiar }: { tienda: Tienda; tiendas: Tienda[]; alCambiar: (id: string) => void }) {
  const apiBase = rutaTienda(tienda.idTienda)
  const base = `/panel/tiendas/${tienda.idTienda}`
  const { datos } = useCarga(async () => {
    const [productos, pedidos, cuenta] = await Promise.allSettled([api.leer<Producto[]>(`${apiBase}/productos`), api.leer<Pedido[]>(`${apiBase}/pedidos`), api.leer<CuentaPago>(`${apiBase}/cuenta-pago`)])
    return {
      productos: productos.status === 'fulfilled' ? productos.value : null,
      pedidos: pedidos.status === 'fulfilled' ? pedidos.value : null,
      cuenta: cuenta.status === 'fulfilled' ? cuenta.value : null,
    }
  }, [apiBase])

  const productos = datos?.productos ?? null
  const pedidos = datos?.pedidos ?? null
  const publicados = productos?.filter(p => p.estado === 'PUBLICADO').length ?? 0
  const pagosConectados = datos?.cuenta?.estado === 'ACTIVA'
  const recientes = [...(pedidos ?? [])].sort((a, b) => b.fecha.localeCompare(a.fecha)).slice(0, 5)
  const cargando = !datos
  const valor = (n: number | string | null) => cargando ? <span className="inline-block h-7 w-12 animate-pulse rounded bg-muted" /> : n ?? '—'

  const pasos = [
    { hecho: true, texto: 'Crear tu tienda', ruta: base },
    { hecho: (productos?.length ?? 0) > 0, texto: 'Agregar tu primer producto', ruta: `${base}/catalogo` },
    { hecho: publicados > 0, texto: 'Publicar un producto', ruta: `${base}/catalogo` },
    { hecho: tienda.estado === 'ACTIVA', texto: 'Activar tu tienda', ruta: base },
    { hecho: pagosConectados, texto: 'Conectar Mercado Pago', ruta: `${base}/pagos` },
  ]
  const completados = pasos.filter(p => p.hecho).length

  return <>
    {/* Tienda seleccionada */}
    <section className="relative overflow-hidden rounded-3xl border bg-[linear-gradient(135deg,#1e3a8a_0%,#2f5bff_55%,#4d7cff_100%)] p-6 text-white sm:p-8">
      <div className="pointer-events-none absolute -right-16 -bottom-24 size-72 rounded-full bg-white/15 blur-3xl" />
      <div className="relative flex flex-wrap items-center justify-between gap-5">
        <div className="flex items-center gap-4">
          <span className="grid size-14 place-items-center rounded-2xl bg-white/15 ring-1 ring-white/25 backdrop-blur"><Store className="size-7" /></span>
          <div>
            {tiendas.length > 1
              ? <label className="relative inline-flex items-center"><span className="sr-only">Tienda</span><select value={tienda.idTienda} onChange={e => alCambiar(e.target.value)} className="cursor-pointer appearance-none bg-transparent pr-7 text-2xl font-semibold outline-none [&>option]:text-foreground">{tiendas.map(t => <option key={t.idTienda} value={t.idTienda}>{t.nombre}</option>)}</select><ChevronDown className="pointer-events-none absolute right-0 size-5" /></label>
              : <h2 className="text-2xl font-semibold">{tienda.nombre}</h2>}
            <p className="mt-0.5 text-sm text-white/75">/tienda/{tienda.slug} · {capitalizar(tienda.estado)}</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2.5">
          <a href={`/tienda/${encodeURIComponent(tienda.slug)}`} target="_blank" rel="noreferrer" className="inline-flex h-10 items-center gap-2 rounded-xl bg-white/15 px-4 text-sm font-medium ring-1 ring-white/25 backdrop-blur transition hover:bg-white/25"><ExternalLink className="size-4" />Ver tienda</a>
          <Link to={base} className="inline-flex h-10 items-center gap-2 rounded-xl bg-white px-4 text-sm font-semibold text-[#1e3a8a] transition hover:bg-white/90">Administrar<ArrowRight className="size-4" /></Link>
        </div>
      </div>
    </section>

    {/* Indicadores */}
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <Indicador icono={<Package className="size-5" />} titulo="Productos" valor={valor(productos?.length ?? null)} detalle={productos ? `${publicados} publicados` : ''} />
      <Indicador icono={<ShoppingCart className="size-5" />} titulo="Pedidos" valor={valor(pedidos?.length ?? null)} detalle="En total" />
      <Indicador icono={<span className="text-sm font-bold">S/</span>} titulo="Monto en pedidos" valor={valor(pedidos ? soles(pedidos.reduce((s, p) => s + p.total, 0)) : null)} detalle="Suma de todos tus pedidos" />
      <Indicador icono={<CreditCard className="size-5" />} titulo="Cobros" valor={cargando ? valor(null) : <span className="text-lg">{pagosConectados ? 'Conectado' : 'Sin conectar'}</span>} detalle="Mercado Pago" />
    </div>

    <div className="grid gap-5 lg:grid-cols-[1.4fr_1fr]">
      {/* Pedidos recientes */}
      <section className={`${tarjeta} p-6`}>
        <div className="flex items-center justify-between gap-3"><h2 className="text-lg font-semibold">Pedidos recientes</h2><Link to={`${base}/pedidos`} className="text-sm font-medium text-primary hover:underline dark:text-[#8fb3ff]">Ver todos</Link></div>
        {cargando ? <div className="mt-5 space-y-3">{[0, 1, 2].map(i => <div key={i} className="h-12 animate-pulse rounded-xl bg-muted" />)}</div>
          : recientes.length === 0 ? <div className="mt-5 rounded-xl border border-dashed px-5 py-10 text-center"><ShoppingCart className="mx-auto size-8 text-muted-foreground/60" /><p className="mt-2 text-sm font-medium">Todavía no tienes pedidos</p><p className="mt-1 text-sm text-muted-foreground">Comparte el enlace de tu tienda para recibir los primeros.</p></div>
          : <ul className="mt-4 divide-y">{recientes.map(p => <li key={p.id} className="flex items-center gap-4 py-3">
              <span className={`grid size-10 shrink-0 place-items-center rounded-xl ${iconoAzul}`}><ShoppingCart className="size-[18px]" /></span>
              <div className="min-w-0 flex-1"><p className="truncate text-sm font-medium">Pedido #{p.id.slice(0, 8)}</p><p className="text-xs text-muted-foreground">{new Date(p.fecha).toLocaleDateString('es-PE', { day: 'numeric', month: 'short', year: 'numeric' })}</p></div>
              <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">{capitalizar(p.estado)}</span>
              <span className="w-24 text-right text-sm font-semibold">{soles(p.total)}</span>
            </li>)}</ul>}
      </section>

      {/* Primeros pasos */}
      <section className={`${tarjeta} p-6`}>
        <div className="flex items-center justify-between gap-3"><h2 className="text-lg font-semibold">Primeros pasos</h2><span className="text-sm text-muted-foreground">{completados} de {pasos.length}</span></div>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-muted"><div className="h-full rounded-full bg-primary transition-all duration-500 dark:bg-[#4d7cff]" style={{ width: `${completados / pasos.length * 100}%` }} /></div>
        <ul className="mt-5 space-y-1">{pasos.map(paso => <li key={paso.texto}><Link to={paso.ruta} className="group flex items-center gap-3 rounded-xl px-2 py-2.5 transition hover:bg-muted">
          <span className={`grid size-6 shrink-0 place-items-center rounded-full ${paso.hecho ? 'bg-emerald-500 text-white' : 'border-2 border-border'}`}>{paso.hecho && <Check className="size-3.5" strokeWidth={3} />}</span>
          <span className={`flex-1 text-sm ${paso.hecho ? 'text-muted-foreground line-through' : 'font-medium'}`}>{paso.texto}</span>
          {!paso.hecho && <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />}
        </Link></li>)}</ul>
      </section>
    </div>

    {/* Accesos rápidos */}
    <section>
      <h2 className="mb-3 text-lg font-semibold">Accesos rápidos</h2>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {([[`${base}/catalogo`, 'Agregar producto', 'Sube fotos, precios y presentaciones', Plus], [`${base}/inventario`, 'Revisar inventario', 'Controla el stock de tus productos', Boxes], [`${base}/apariencia`, 'Cambiar apariencia', 'Plantilla, portada y banners', Palette], [`${base}/pagos`, 'Configurar cobros', 'Conecta tu cuenta de Mercado Pago', CreditCard]] as const).map(([ruta, titulo, detalle, Icono]) =>
          <Link key={titulo} to={ruta} className={`${tarjeta} group flex items-start gap-4 p-5 transition hover:border-primary/40`}>
            <span className={`grid size-11 shrink-0 place-items-center rounded-xl ${iconoAzul}`}><Icono className="size-5" /></span>
            <span className="min-w-0"><span className="flex items-center gap-1.5 font-semibold">{titulo}<ArrowRight className="size-4 opacity-0 transition group-hover:translate-x-0.5 group-hover:opacity-100" /></span><span className="mt-0.5 block text-sm text-muted-foreground">{detalle}</span></span>
          </Link>)}
      </div>
    </section>
  </>
}

function Indicador({ icono, titulo, valor, detalle }: { icono: ReactNode; titulo: string; valor: ReactNode; detalle: string }) {
  return <div className={`${tarjeta} p-5`}>
    <div className="flex items-center justify-between"><p className="text-sm text-muted-foreground">{titulo}</p><span className={`grid size-10 place-items-center rounded-xl ${iconoAzul}`}>{icono}</span></div>
    <p className="mt-2 text-2xl font-semibold tracking-tight">{valor}</p>
    <p className="mt-1 text-xs text-muted-foreground">{detalle}</p>
  </div>
}

/* =====================================================================
   PLANTILLAS: galería de diseños disponibles
   ===================================================================== */
export function PlantillasVendedor() {
  const { usuario } = useSesion()
  const { datos, error, recargar } = useCarga(async () => {
    const [plantillas, tiendas] = await Promise.all([api.leer<Plantilla[]>('/api/plantillas'), api.leer<Tienda[]>(`/api/tiendas/propietario/${usuario!.idUsuario}`)])
    return { plantillas, tiendas }
  }, [usuario!.idUsuario])
  const [vistaPrevia, establecerVistaPrevia] = useState<{ codigo: string; nombre: string } | null>(null)

  return <>
    {vistaPrevia && <ModalVistaPrevia {...vistaPrevia} alCerrar={() => establecerVistaPrevia(null)} />}
    <Encabezado titulo="Plantillas" descripcion="Elige el diseño de tu vitrina. Puedes cambiarlo cuando quieras sin perder tus productos." />
    {error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : <>
      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
        {datos.plantillas.filter(p => PLANTILLAS_VISIBLES.includes(p.codigo)).sort((a, b) => PLANTILLAS_VISIBLES.indexOf(a.codigo) - PLANTILLAS_VISIBLES.indexOf(b.codigo)).map(p => {
          const enUso = datos.tiendas.filter(t => t.plantilla === p.codigo)
          const info = infoPlantillas[p.codigo]
          return <article key={p.idPlantilla} className={`${tarjeta} group flex flex-col overflow-hidden transition hover:border-primary/40 hover:shadow-[0_20px_44px_-26px_rgba(77,124,255,0.5)]`}>
            <div className="relative aspect-[16/11] overflow-hidden border-b">
              <div className="h-full transition-transform duration-500 group-hover:scale-[1.03]"><MiniaturaEnVivo codigo={p.codigo} /></div>
              <button type="button" onClick={() => establecerVistaPrevia({ codigo: p.codigo, nombre: p.nombre })} className="absolute inset-0 grid cursor-pointer place-items-center bg-black/0 opacity-0 transition group-hover:bg-black/45 group-hover:opacity-100 focus-visible:bg-black/45 focus-visible:opacity-100" aria-label={`Vista previa de ${p.nombre}`}><span className="inline-flex items-center gap-2 rounded-full bg-white px-4 py-2 text-sm font-semibold text-zinc-900 shadow-lg"><Eye className="size-4" />Vista previa</span></button>
              {enUso.length > 0 && <span className="absolute top-3 right-3 inline-flex items-center gap-1 rounded-full bg-emerald-500 px-2.5 py-1 text-xs font-semibold text-white shadow"><Check className="size-3.5" strokeWidth={3} />En uso</span>}
            </div>
            <div className="flex flex-1 flex-col p-5">
              <div className="flex items-center justify-between gap-2"><h2 className="text-lg font-semibold">{p.nombre}</h2>{info && <span className="rounded-full bg-muted px-2.5 py-0.5 text-xs text-muted-foreground">{info.ideal}</span>}</div>
              <p className="mt-2 text-sm text-muted-foreground">{info?.descripcion ?? 'Diseño disponible para tu tienda.'}</p>
              {enUso.length > 0 && <p className="mt-3 text-xs text-muted-foreground">En uso en: <span className="font-medium text-foreground">{enUso.map(t => t.nombre).join(', ')}</span></p>}
              <div className="mt-auto space-y-2 pt-5">
                <button type="button" onClick={() => establecerVistaPrevia({ codigo: p.codigo, nombre: p.nombre })} className="inline-flex h-10 w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-primary text-sm font-semibold text-primary-foreground transition hover:opacity-90 dark:bg-[#4d7cff] dark:text-[#06101f]"><Eye className="size-4" />Vista previa</button>
                {datos.tiendas.length === 0
                  ? <Link to="/panel/tiendas" className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-xl border text-sm font-semibold transition hover:border-primary/40">Crea una tienda para usarla</Link>
                  : <div className="flex flex-wrap gap-2">{datos.tiendas.map(t => <Link key={t.idTienda} to={`/panel/tiendas/${t.idTienda}/apariencia`} className="inline-flex h-10 flex-1 items-center justify-center gap-2 rounded-xl border border-primary/40 px-3 text-sm font-semibold text-primary transition hover:bg-primary hover:text-primary-foreground dark:border-[#4d7cff]/50 dark:text-[#8fb3ff] dark:hover:bg-[#4d7cff] dark:hover:text-[#06101f]"><Palette className="size-4" />{datos.tiendas.length > 1 ? `Usar en ${t.nombre}` : t.plantilla === p.codigo ? 'Personalizar' : 'Usar plantilla'}</Link>)}</div>}
              </div>
            </div>
          </article>
        })}
      </div>
      <p className="flex items-center gap-2 text-sm text-muted-foreground"><LayoutTemplate className="size-4" />La plantilla se aplica desde la sección <strong className="font-medium text-foreground">Apariencia</strong> de cada tienda.</p>
    </>}
  </>
}

/* =====================================================================
   AYUDA: guía y preguntas frecuentes
   ===================================================================== */
type TemaAyuda = { id: string; titulo: string; icono: typeof Sparkles; preguntas: [string, string][] }
const temasAyuda: TemaAyuda[] = [
  { id: 'primeros-pasos', titulo: 'Primeros pasos', icono: Sparkles, preguntas: [
    ['¿Cómo creo mi tienda?', 'Entra a Mis tiendas y pulsa "Crear tienda". Solo necesitas un nombre; con él se genera el enlace de tu tienda (por ejemplo /tienda/mi-tienda). La tienda empieza en borrador.'],
    ['¿Cómo activo mi tienda?', 'Abre tu tienda y, en Resumen, pulsa "Activar tienda". Una tienda activa puede publicar productos que cumplan los requisitos del catálogo.'],
    ['¿Dónde veo mi tienda como la ven mis clientes?', 'Usa el botón "Ver tienda" o el enlace /tienda/tu-tienda. Ahí aparecen los productos publicados con la plantilla que elegiste.'],
  ] },
  { id: 'catalogo', titulo: 'Catálogo', icono: Package, preguntas: [
    ['¿Cómo agrego un producto?', 'En Catálogo completa el nombre, la descripción, el precio base y la categoría. Luego abre el producto para agregar imágenes, presentaciones y colecciones.'],
    ['¿Qué son las variantes y las presentaciones?', 'Las variantes son características como talla o color, con sus valores (S, M, L…). Una presentación combina esos valores, tiene su propio SKU y puede tener un precio distinto al precio base.'],
    ['¿Por qué un producto no aparece en mi tienda?', 'Revisa que la tienda esté activa y que el producto esté publicado y tenga al menos una presentación activa.'],
  ] },
  { id: 'inventario', titulo: 'Inventario', icono: Boxes, preguntas: [
    ['¿Cómo controlo el stock?', 'En Inventario verás cada presentación con su stock actual y su stock mínimo. Puedes buscar por nombre o SKU y filtrar por estado.'],
    ['¿Qué pasa cuando el stock baja?', 'Usa el filtro de alertas de stock para encontrar rápido las presentaciones que llegaron a su stock mínimo y reponerlas a tiempo.'],
  ] },
  { id: 'pedidos', titulo: 'Pedidos', icono: ShoppingCart, preguntas: [
    ['¿Dónde veo los pedidos de mis clientes?', 'En Pedidos aparece la lista con fecha, estado y total. Abre uno para ver los productos, el receptor y la dirección de entrega en el mapa.'],
    ['¿Puedo devolver un pago?', 'Desde el detalle del pedido puedes consultar su pago y, si corresponde, registrar un reembolso y decidir si se repone el stock.'],
  ] },
  { id: 'apariencia', titulo: 'Apariencia', icono: Palette, preguntas: [
    ['¿Cómo cambio el diseño de mi tienda?', 'En Apariencia elige una plantilla y edita el título y la descripción de la portada. También puedes ver todas las opciones en Plantillas.'],
    ['¿Cómo agrego banners a la portada?', 'En Apariencia sube tus banners, ordénalos y activa el carrusel para que se muestren en la portada de tu tienda.'],
  ] },
  { id: 'pagos', titulo: 'Cobros', icono: CreditCard, preguntas: [
    ['¿Cómo recibo pagos?', 'En Pagos conecta tu cuenta de Mercado Pago. Serás redirigido para autorizar la conexión y, al volver, verás el estado de tu cuenta.'],
    ['¿Puedo desconectar mi cuenta?', 'Sí, desde la misma sección Pagos puedes gestionar la cuenta conectada.'],
  ] },
]

export function AyudaVendedor() {
  const [busqueda, establecerBusqueda] = useState('')
  const termino = busqueda.trim().toLowerCase()
  const temas = temasAyuda.map(t => ({ ...t, preguntas: t.preguntas.filter(([p, r]) => !termino || p.toLowerCase().includes(termino) || r.toLowerCase().includes(termino)) })).filter(t => t.preguntas.length > 0)

  return <>
    <section className="relative overflow-hidden rounded-3xl border bg-card px-6 py-10 text-center sm:px-10">
      <div className="pointer-events-none absolute -top-28 left-1/2 size-80 -translate-x-1/2 rounded-full bg-primary/10 blur-3xl dark:bg-[#4d7cff]/20" />
      <div className="relative mx-auto max-w-2xl">
        <span className={`mx-auto grid size-12 place-items-center rounded-2xl ${iconoAzul}`}><BookOpen className="size-6" /></span>
        <h1 className="mt-5 text-3xl font-semibold tracking-tight">¿En qué te ayudamos?</h1>
        <p className="mt-2 text-muted-foreground">Guías rápidas para sacar adelante tu tienda en Nubea.</p>
        <label className="relative mt-6 block"><span className="sr-only">Buscar en la ayuda</span>
          <Search className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-muted-foreground" />
          <input value={busqueda} onChange={e => establecerBusqueda(e.target.value)} placeholder="Busca una pregunta, por ejemplo: stock, banners, pagos..." className="h-12 w-full rounded-xl border border-input bg-background pr-4 pl-12 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20" />
        </label>
      </div>
    </section>

    {!termino && <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{temasAyuda.map(({ id, titulo, icono: Icono, preguntas }) => <a key={id} href={`#${id}`} className={`${tarjeta} group flex items-center gap-4 p-5 transition hover:border-primary/40`}>
      <span className={`grid size-11 shrink-0 place-items-center rounded-xl ${iconoAzul}`}><Icono className="size-5" /></span>
      <span className="flex-1"><span className="block font-semibold">{titulo}</span><span className="text-sm text-muted-foreground">{preguntas.length} artículos</span></span>
      <ArrowRight className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
    </a>)}</div>}

    {temas.length === 0 ? <p className={`${tarjeta} px-6 py-10 text-center text-sm text-muted-foreground`}>No encontramos resultados para "{busqueda}".</p>
      : <div className="space-y-8">{temas.map(({ id, titulo, icono: Icono, preguntas }) => <section key={id} id={id} className="scroll-mt-24">
        <h2 className="mb-3 flex items-center gap-2 text-lg font-semibold"><Icono className="size-5 text-primary dark:text-[#8fb3ff]" />{titulo}</h2>
        <div className={`${tarjeta} divide-y overflow-hidden`}>{preguntas.map(([pregunta, respuesta]) => <details key={pregunta} className="group" open={!!termino}>
          <summary className="flex cursor-pointer list-none items-center justify-between gap-4 px-5 py-4 font-medium transition hover:bg-muted/50 [&::-webkit-details-marker]:hidden">{pregunta}<ChevronDown className="size-5 shrink-0 text-muted-foreground transition-transform group-open:rotate-180" /></summary>
          <p className="px-5 pb-5 text-sm leading-relaxed text-muted-foreground">{respuesta}</p>
        </details>)}</div>
      </section>)}</div>}
  </>
}
