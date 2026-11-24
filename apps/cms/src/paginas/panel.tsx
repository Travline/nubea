import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, NavLink, Navigate, Outlet, useLocation, useNavigate, useOutletContext, useParams } from 'react-router-dom'
import { ArrowRight, Boxes, House, LayoutTemplate, LifeBuoy, UserRound, CreditCard, ExternalLink, LayoutDashboard, LayoutGrid, List, LogOut, Package, Palette, Plus, Search, ShoppingCart, Store, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { AlternarTema } from '@/components/alternar-tema'
import { apiAutenticacion } from '@/autenticacion/api'
import { useSesion } from '@/autenticacion/sesion'
import { api, rutaTienda } from '@/comercio/datos'
import type { Tienda } from '@/comercio/datos'
import { Aviso, Bloque, EstadoCarga, useCarga, useOperacion } from '@/comercio/comunes'
import { InicioVendedor } from '@/paginas/panel-secciones'

const seccionesTienda = [
  ['', 'Resumen', LayoutDashboard],
  ['/catalogo', 'Catálogo', Package],
  ['/inventario', 'Inventario', Boxes],
  ['/pedidos', 'Pedidos', ShoppingCart],
  ['/apariencia', 'Apariencia', Palette],
  ['/pagos', 'Pagos', CreditCard],
] as const

const nombresPlantilla: Record<string, string> = { minimal: 'Minimalista', boutique: 'Boutique', audaz: 'Tech', gourmet: 'Gourmet', natural: 'Natural' }
const nombrePlantilla = (codigo: string | null | undefined) => (codigo && nombresPlantilla[codigo]) || codigo || 'Sin plantilla'

const estilosEstado: Record<string, { texto: string; clase: string; punto: string }> = {
  ACTIVA: { texto: 'Activa', clase: 'bg-emerald-500/12 text-emerald-600 dark:text-emerald-400', punto: 'bg-emerald-500' },
  BORRADOR: { texto: 'Borrador', clase: 'bg-amber-500/12 text-amber-600 dark:text-amber-400', punto: 'bg-amber-500' },
  INACTIVA: { texto: 'Inactiva', clase: 'bg-zinc-500/15 text-zinc-600 dark:text-zinc-400', punto: 'bg-zinc-400' },
}
function EstadoTienda({ estado }: { estado: string }) {
  const e = estilosEstado[estado] ?? { texto: estado, clase: 'bg-muted text-muted-foreground', punto: 'bg-muted-foreground' }
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${e.clase}`}><span className={`size-1.5 rounded-full ${e.punto}`} />{e.texto}</span>
}

const claseEnlaceMenu = ({ isActive }: { isActive: boolean }) =>
  `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${isActive ? 'bg-primary/12 text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`

export function Panel() {
  const { usuario, limpiar } = useSesion()
  const navegar = useNavigate()
  const { pathname } = useLocation()
  const { error, ocupado, ejecutar } = useOperacion()
  async function salir() { await ejecutar(async () => { await apiAutenticacion.cerrarSesion(); limpiar(); navegar('/iniciar-sesion', { replace: true }) }) }
  const esAdmin = !!usuario?.roles.includes('ADMIN_GENERAL')
  const idTiendaActual = pathname.match(/^\/panel\/tiendas\/([^/]+)/)?.[1] ?? ''
  const { datos: tiendas } = useCarga(() => idTiendaActual ? api.leer<Tienda[]>(`/api/tiendas/propietario/${usuario!.idUsuario}`) : Promise.resolve([] as Tienda[]), [usuario!.idUsuario, idTiendaActual])
  const tiendaActual = tiendas?.find(t => t.idTienda === idTiendaActual)
  const inicial = (usuario?.nombres ?? '?').trim().charAt(0).toUpperCase()

  const menu = <nav aria-label="Panel" className="space-y-6">
    <div className="space-y-1">
      {esAdmin
        ? <><NavLink to="/panel/administracion" className={claseEnlaceMenu}><LayoutDashboard className="size-[18px]" />Administración</NavLink><NavLink to="/panel/perfil" className={claseEnlaceMenu}><UserRound className="size-[18px]" />Mi perfil</NavLink></>
        : <>
          <NavLink to="/panel" end className={claseEnlaceMenu}><House className="size-[18px]" />Inicio</NavLink>
          <NavLink to="/panel/tiendas" end className={claseEnlaceMenu}><Store className="size-[18px]" />Mis tiendas</NavLink>
          <NavLink to="/panel/plantillas" className={claseEnlaceMenu}><LayoutTemplate className="size-[18px]" />Plantillas</NavLink>
          <NavLink to="/panel/ayuda" className={claseEnlaceMenu}><LifeBuoy className="size-[18px]" />Ayuda</NavLink>
          <NavLink to="/panel/perfil" className={claseEnlaceMenu}><UserRound className="size-[18px]" />Mi perfil</NavLink>
        </>}
    </div>
    {idTiendaActual && <div className="space-y-1">
      <p className="truncate px-3 pb-1 text-[11px] font-semibold tracking-[0.14em] text-muted-foreground/80 uppercase">{tiendaActual?.nombre ?? 'Tienda'}</p>
      {seccionesTienda.map(([ruta, nombre, Icono]) => <NavLink key={ruta} end={ruta === ''} to={`/panel/tiendas/${idTiendaActual}${ruta}`} className={claseEnlaceMenu}><Icono className="size-[18px]" />{nombre}</NavLink>)}
      {tiendaActual && <a href={`/tienda/${encodeURIComponent(tiendaActual.slug)}`} target="_blank" rel="noreferrer" className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"><ExternalLink className="size-[18px]" />Ver tienda pública</a>}
    </div>}
  </nav>

  return <div className="min-h-screen bg-muted/40 dark:bg-[#0a0d14]">
    {/* Barra lateral */}
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-[248px] flex-col border-r bg-background dark:bg-[#0d111a] lg:flex">
      <Link to="/panel" className="flex h-[68px] items-center gap-2.5 px-6 text-lg font-bold"><img className="h-7 w-auto" src={`${import.meta.env.BASE_URL}assets/logo.webp`} alt="" />Nubea</Link>
      <div className="flex-1 overflow-y-auto px-4 py-4">{menu}</div>
      <div className="border-t px-4 py-4"><AlternarTema /></div>
    </aside>

    <div className="lg:pl-[248px]">
      {/* Barra superior */}
      <header className="sticky top-0 z-20 border-b bg-background/85 backdrop-blur dark:bg-[#0a0d14]/85">
        <div className="flex h-[68px] items-center justify-between gap-3 px-5 lg:px-8">
          <Link to="/panel" className="flex items-center gap-2 text-lg font-bold lg:hidden"><img className="h-7 w-auto" src={`${import.meta.env.BASE_URL}assets/logo.webp`} alt="" />Nubea</Link>
          <span className="hidden lg:block" />
          <div className="flex items-center gap-3">
            <div className="lg:hidden"><AlternarTema /></div>
            <Link to="/panel/perfil" title="Mi perfil" className="flex items-center gap-2.5 rounded-xl border-l py-1 pr-2 pl-3 transition hover:bg-muted">
              <span className="grid size-9 place-items-center rounded-full bg-primary/12 text-sm font-semibold text-primary dark:bg-[#4d7cff]/20 dark:text-[#8fb3ff]">{inicial}</span>
              <span className="hidden text-sm font-medium sm:block">{usuario?.nombres}</span>
            </Link>
            <Button variant="ghost" size="sm" disabled={ocupado} onClick={() => void salir()} className="text-muted-foreground"><LogOut className="size-4" /><span className="hidden sm:inline">Cerrar sesión</span></Button>
          </div>
        </div>
        {/* Menú en móvil */}
        <div className="flex gap-1 overflow-x-auto border-t px-3 py-2 lg:hidden">
          {esAdmin ? <NavLink to="/panel/administracion" className={claseEnlaceMenu}>Administración</NavLink> : ([['/panel', 'Inicio'], ['/panel/tiendas', 'Mis tiendas'], ['/panel/plantillas', 'Plantillas'], ['/panel/ayuda', 'Ayuda'], ['/panel/perfil', 'Mi perfil']] as const).map(([ruta, nombre]) => <NavLink key={ruta} to={ruta} end className={claseEnlaceMenu}><span className="whitespace-nowrap">{nombre}</span></NavLink>)}
          {idTiendaActual && seccionesTienda.map(([ruta, nombre]) => <NavLink key={ruta} end={ruta === ''} to={`/panel/tiendas/${idTiendaActual}${ruta}`} className={claseEnlaceMenu}><span className="whitespace-nowrap">{nombre}</span></NavLink>)}
        </div>
      </header>
      <main className="mx-auto max-w-[1280px] space-y-6 px-5 py-8 lg:px-8"><Aviso error={error} /><Outlet /></main>
    </div>
  </div>
}

export function InicioPanel() {
  const { usuario } = useSesion()
  return usuario?.roles.includes('ADMIN_GENERAL') ? <Navigate to="/panel/administracion" replace /> : <InicioVendedor />
}

function Indicador({ titulo, valor, icono, clase }: { titulo: string; valor: number; icono: ReactNode; clase: string }) {
  return <div className="flex items-center gap-4 rounded-2xl border bg-card p-5">
    <span className={`grid size-12 shrink-0 place-items-center rounded-xl ${clase}`}>{icono}</span>
    <div><p className="text-sm text-muted-foreground">{titulo}</p><p className="text-2xl font-semibold tracking-tight">{valor}</p></div>
  </div>
}

export function MisTiendas() {
  const { usuario, actualizar } = useSesion()
  const navegar = useNavigate()
  const [nombre, establecerNombre] = useState('')
  const [mostrarCrear, establecerMostrarCrear] = useState(false)
  const [busqueda, establecerBusqueda] = useState('')
  const [filtroEstado, establecerFiltroEstado] = useState('TODAS')
  const [vista, establecerVista] = useState<'cuadricula' | 'lista'>('cuadricula')
  const { datos, error, recargar } = useCarga(() => api.leer<Tienda[]>(`/api/tiendas/propietario/${usuario!.idUsuario}`), [usuario!.idUsuario])
  const operacion = useOperacion()
  async function crear(e: FormEvent) { e.preventDefault(); await operacion.ejecutar(async () => { const tienda = await api.crear<Tienda>('/api/tiendas', { nombre }); try { await actualizar() } catch { navegar(`/panel/tiendas/${tienda.idTienda}`); return }; navegar(`/panel/tiendas/${tienda.idTienda}`) }) }

  const lista = datos ?? []
  const filtradas = lista.filter(t => (filtroEstado === 'TODAS' || t.estado === filtroEstado) && t.nombre.toLowerCase().includes(busqueda.trim().toLowerCase()))
  const formularioVisible = mostrarCrear || (datos !== null && lista.length === 0)

  return <>
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div><h1 className="text-3xl font-semibold tracking-tight">Mis tiendas</h1><p className="mt-1.5 text-muted-foreground">Administra, crea y personaliza todas tus tiendas en un solo lugar.</p></div>
      {!formularioVisible && <Button size="lg" onClick={() => establecerMostrarCrear(true)} className="h-11 gap-2 rounded-xl px-5"><Plus className="size-4" />Crear tienda</Button>}
    </div>

    <Aviso error={operacion.error} />

    {formularioVisible && <section className="relative overflow-hidden rounded-2xl border border-primary/30 bg-card p-6 dark:border-[#4d7cff]/35">
      <div className="pointer-events-none absolute -top-20 -right-20 size-60 rounded-full bg-primary/10 blur-3xl dark:bg-[#4d7cff]/15" />
      <div className="relative flex items-start justify-between gap-3">
        <div><h2 className="text-lg font-semibold">Crear una tienda nueva</h2><p className="mt-1 text-sm text-muted-foreground">Ponle un nombre; luego podrás configurar su catálogo, apariencia y pagos.</p></div>
        {lista.length > 0 && <button type="button" onClick={() => establecerMostrarCrear(false)} aria-label="Cerrar" className="grid size-8 cursor-pointer place-items-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground"><X className="size-4" /></button>}
      </div>
      <form onSubmit={crear} className="relative mt-5 flex flex-wrap gap-3">
        <label className="relative min-w-56 flex-1"><span className="sr-only">Nombre de la tienda</span>
          <Store className="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-muted-foreground" />
          <input className="h-11 w-full rounded-xl border border-input bg-background pr-3 pl-11 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20" placeholder="Nombre de la tienda" value={nombre} onChange={e => establecerNombre(e.target.value)} maxLength={150} required autoFocus={mostrarCrear} />
        </label>
        <Button type="submit" disabled={operacion.ocupado} className="h-11 gap-2 rounded-xl px-5">{operacion.ocupado ? 'Creando...' : <>Crear tienda <ArrowRight className="size-4" /></>}</Button>
      </form>
    </section>}

    {error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Indicador titulo="Total de tiendas" valor={lista.length} icono={<Store className="size-5" />} clase="bg-primary/10 text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]" />
        <Indicador titulo="Activas" valor={lista.filter(t => t.estado === 'ACTIVA').length} icono={<span className="size-2.5 rounded-full bg-emerald-500 ring-4 ring-emerald-500/25" />} clase="bg-emerald-500/12" />
        <Indicador titulo="En borrador" valor={lista.filter(t => t.estado === 'BORRADOR').length} icono={<span className="size-2.5 rounded-full bg-amber-500 ring-4 ring-amber-500/25" />} clase="bg-amber-500/12" />
        <Indicador titulo="Inactivas" valor={lista.filter(t => t.estado !== 'ACTIVA' && t.estado !== 'BORRADOR').length} icono={<span className="size-2.5 rounded-full bg-zinc-400 ring-4 ring-zinc-400/25" />} clase="bg-zinc-500/12" />
      </div>

      {lista.length > 0 && <div className="flex flex-wrap items-center gap-3">
        <label className="relative min-w-60 flex-1"><span className="sr-only">Buscar tienda</span>
          <Search className="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-muted-foreground" />
          <input className="h-11 w-full rounded-xl border border-input bg-card pr-3 pl-11 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20" placeholder="Buscar tienda por nombre..." value={busqueda} onChange={e => establecerBusqueda(e.target.value)} />
        </label>
        <label className="flex h-11 items-center gap-2 rounded-xl border border-input bg-card pr-2 pl-4 text-sm"><span className="text-muted-foreground">Estado:</span>
          <select className="h-full cursor-pointer bg-transparent pr-1 font-medium outline-none" value={filtroEstado} onChange={e => establecerFiltroEstado(e.target.value)}>
            <option value="TODAS">Todas</option><option value="ACTIVA">Activas</option><option value="BORRADOR">En borrador</option><option value="INACTIVA">Inactivas</option>
          </select>
        </label>
        <div className="flex rounded-xl border border-input bg-card p-1">
          {([['cuadricula', LayoutGrid, 'Ver en cuadrícula'], ['lista', List, 'Ver en lista']] as const).map(([valor, Icono, etiqueta]) => <button key={valor} type="button" aria-label={etiqueta} aria-pressed={vista === valor} onClick={() => establecerVista(valor)} className={`grid size-9 cursor-pointer place-items-center rounded-lg transition ${vista === valor ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:text-foreground'}`}><Icono className="size-[18px]" /></button>)}
        </div>
      </div>}

      {lista.length === 0 ? <div className="rounded-2xl border border-dashed bg-card px-6 py-14 text-center"><Store className="mx-auto size-10 text-muted-foreground/60" /><p className="mt-3 font-medium">Aún no tienes tiendas</p><p className="mt-1 text-sm text-muted-foreground">Crea la primera con el formulario de arriba.</p></div>
        : filtradas.length === 0 ? <p className="rounded-2xl border bg-card px-6 py-10 text-center text-sm text-muted-foreground">Ninguna tienda coincide con tu búsqueda.</p>
        : vista === 'cuadricula'
          ? <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">{filtradas.map(t => <TarjetaTienda key={t.idTienda} tienda={t} />)}</div>
          : <div className="overflow-hidden rounded-2xl border bg-card">{filtradas.map(t => <FilaTienda key={t.idTienda} tienda={t} />)}</div>}

      {lista.length > 0 && <p className="text-sm text-muted-foreground">Mostrando {filtradas.length} de {lista.length} {lista.length === 1 ? 'tienda' : 'tiendas'}</p>}
    </>}
  </>
}

function TarjetaTienda({ tienda: t }: { tienda: Tienda }) {
  const base = `/panel/tiendas/${t.idTienda}`
  return <article className="group flex flex-col rounded-2xl border bg-card p-5 transition hover:border-primary/40 hover:shadow-[0_18px_40px_-24px_rgba(77,124,255,0.45)]">
    <div className="flex items-start gap-4">
      <span className="grid size-12 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]"><Store className="size-[22px]" /></span>
      <div className="min-w-0 flex-1">
        <h2 className="truncate text-lg font-semibold">{t.nombre}</h2>
        <a href={`/tienda/${encodeURIComponent(t.slug)}`} target="_blank" rel="noreferrer" className="mt-0.5 inline-flex max-w-full items-center gap-1.5 text-sm text-muted-foreground hover:text-primary"><span className="truncate">/tienda/{t.slug}</span><ExternalLink className="size-3.5 shrink-0" /></a>
        <div className="mt-2.5"><EstadoTienda estado={t.estado} /></div>
      </div>
    </div>
    <div className="my-5 grid grid-cols-3 gap-2 border-t pt-4 text-center">
      {([['/catalogo', 'Catálogo', Package], ['/pedidos', 'Pedidos', ShoppingCart], ['/apariencia', nombrePlantilla(t.plantilla), Palette]] as const).map(([ruta, texto, Icono]) => <Link key={ruta} to={base + ruta} className="flex flex-col items-center gap-1.5 rounded-lg px-1 py-2 text-xs text-muted-foreground transition hover:bg-muted hover:text-foreground"><Icono className="size-[18px]" /><span className="max-w-full truncate">{texto}</span></Link>)}
    </div>
    <Link to={base} className="mt-auto inline-flex h-11 items-center justify-center gap-2 rounded-xl border border-primary/40 text-sm font-semibold text-primary transition hover:bg-primary hover:text-primary-foreground dark:border-[#4d7cff]/50 dark:text-[#8fb3ff] dark:hover:bg-[#4d7cff] dark:hover:text-[#06101f]"><Store className="size-4" />Administrar tienda<ArrowRight className="size-4 transition-transform group-hover:translate-x-0.5" /></Link>
  </article>
}

function FilaTienda({ tienda: t }: { tienda: Tienda }) {
  return <div className="flex flex-wrap items-center gap-4 border-b px-5 py-4 last:border-b-0">
    <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]"><Store className="size-5" /></span>
    <div className="min-w-0 flex-1"><p className="truncate font-semibold">{t.nombre}</p><a href={`/tienda/${encodeURIComponent(t.slug)}`} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-primary">/tienda/{t.slug}<ExternalLink className="size-3" /></a></div>
    <span className="hidden text-sm text-muted-foreground md:block">Plantilla: <span className="text-foreground">{nombrePlantilla(t.plantilla)}</span></span>
    <EstadoTienda estado={t.estado} />
    <Link to={`/panel/tiendas/${t.idTienda}`} className="inline-flex h-9 items-center gap-1.5 rounded-lg px-3 text-sm font-semibold text-primary hover:bg-primary/10 dark:text-[#8fb3ff]">Administrar<ArrowRight className="size-4" /></Link>
  </div>
}

export function EspacioTienda() {
  const { idTienda = '' } = useParams()
  const { usuario } = useSesion()
  const { datos, error, recargar, actualizarDatos } = useCarga(() => api.leer<Tienda[]>(`/api/tiendas/propietario/${usuario!.idUsuario}`), [usuario!.idUsuario])
  if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
  const tienda = datos.find(t => t.idTienda === idTienda)
  if (!tienda) return <p role="alert">No tienes acceso a esta tienda. <Link className="underline" to="/panel/tiendas">Volver a tus tiendas</Link></p>
  const base = `/panel/tiendas/${idTienda}`
  return <><div className="flex flex-wrap items-end justify-between gap-4"><div><Link className="text-sm text-muted-foreground hover:text-primary" to="/panel/tiendas">← Mis tiendas</Link><div className="mt-2 flex flex-wrap items-center gap-3"><h1 className="text-3xl font-semibold tracking-tight">{tienda.nombre}</h1><EstadoTienda estado={tienda.estado} /></div><p className="mt-1 text-sm text-muted-foreground">/tienda/{tienda.slug} · Plantilla {nombrePlantilla(tienda.plantilla)}</p></div><a href={`/tienda/${encodeURIComponent(tienda.slug)}`} target="_blank" rel="noreferrer" className="inline-flex h-10 items-center gap-2 rounded-xl border bg-card px-4 text-sm font-medium hover:border-primary/40"><ExternalLink className="size-4" />Ver tienda</a></div><Outlet context={{ tienda, base, apiBase: rutaTienda(idTienda), actualizarTienda: (actualizada: Tienda) => actualizarDatos(tiendas => tiendas.map(t => t.idTienda === actualizada.idTienda ? actualizada : t)) }} /></>
}
export function ResumenTienda() {
  const { tienda, apiBase, actualizarTienda } = useOutletContext<ContextoTienda>()
  const operacion = useOperacion()
  return <Bloque titulo="Tu espacio de trabajo"><p>Administra tus productos, consulta el inventario y configura la apariencia y los pagos desde el menú.</p>
    {tienda.estado === 'BORRADOR' && <><p>Activa tu tienda cuando quieras para poder publicar productos que cumplan los requisitos del catálogo.</p><Aviso error={operacion.error} /><Button disabled={operacion.ocupado} onClick={() => void operacion.ejecutar(async () => { const actualizada = await api.crear<Tienda>(`${apiBase}/activacion`); actualizarTienda(actualizada) })}>{operacion.ocupado ? 'Activando...' : 'Activar tienda'}</Button></>}
    <a className="text-sm text-primary underline" href={`/tienda/${encodeURIComponent(tienda.slug)}`}>Ver tienda pública</a></Bloque>
}
export interface ContextoTienda { tienda: Tienda; base: string; apiBase: string; actualizarTienda: (tienda: Tienda) => void }
