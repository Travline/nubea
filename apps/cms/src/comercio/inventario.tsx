import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import { Boxes, ChevronDown, ImageOff, Minus, Package, PackageCheck, PackageX, Plus, Search, TriangleAlert, Undo2, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { ContextoTienda } from '@/paginas/panel'
import { api } from './datos'
import type { Imagen, Inventario, PaginaInventario, Presentacion, Producto, Valor, Variante } from './datos'
import { Aviso, EstadoCarga, useCarga, useOperacion } from './comunes'

const claseEntrada = 'h-11 w-full rounded-xl border border-input bg-card px-3.5 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20'
const capitalizar = (t: string) => t.charAt(0) + t.slice(1).toLowerCase()

type Nivel = 'disponible' | 'bajo' | 'agotado' | 'sin-registro'
const niveles: Record<Nivel, { texto: string; clase: string; barra: string; icono: ReactNode }> = {
  disponible: { texto: 'Disponible', clase: 'bg-emerald-500/12 text-emerald-700 dark:text-emerald-400', barra: 'bg-emerald-500', icono: <PackageCheck className="size-3.5" /> },
  bajo: { texto: 'Stock bajo', clase: 'bg-amber-500/15 text-amber-700 dark:text-amber-400', barra: 'bg-amber-500', icono: <TriangleAlert className="size-3.5" /> },
  agotado: { texto: 'Agotado', clase: 'bg-red-500/12 text-red-600 dark:text-red-400', barra: 'bg-red-500', icono: <PackageX className="size-3.5" /> },
  'sin-registro': { texto: 'Sin registro', clase: 'bg-zinc-500/15 text-zinc-600 dark:text-zinc-400', barra: 'bg-zinc-400', icono: <PackageX className="size-3.5" /> },
}
const nivelDe = (actual: number, minimo: number, sinRegistro = false): Nivel => sinRegistro ? 'sin-registro' : actual === 0 ? 'agotado' : actual <= minimo ? 'bajo' : 'disponible'
function Etiqueta({ nivel }: { nivel: Nivel }) { const n = niveles[nivel]; return <span className={`inline-flex w-fit items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${n.clase}`}>{n.icono}{n.texto}</span> }

type Linea = { presentacion: Presentacion; inventario?: Inventario; etiquetas: string[] }
type Grupo = { producto: Producto; lineas: Linea[] }

// Trae todas las páginas del inventario (máximo 100 por página).
async function leerInventario(apiBase: string) {
  const todos: Inventario[] = []
  for (let page = 0; ; page++) {
    const pagina = await api.leer<PaginaInventario>(`${apiBase}/inventario?page=${page}&size=100`)
    todos.push(...pagina.contenido)
    if (page + 1 >= pagina.totalPaginas) break
  }
  return todos
}

export function InventarioTienda() {
  const { apiBase, base } = useOutletContext<ContextoTienda>()
  const [texto, setTexto] = useState(''); const [estadoProducto, setProducto] = useState(''); const [alerta, setAlerta] = useState(''); const [abierto, setAbierto] = useState('')
  const { datos, error, recargar } = useCarga(async () => {
    const [productos, variantes, inventario] = await Promise.all([api.leer<Producto[]>(`${apiBase}/productos`), api.leer<Variante[]>(`${apiBase}/variantes`), leerInventario(apiBase)])
    const [valores, presentaciones] = await Promise.all([
      Promise.all(variantes.map(v => api.leer<Valor[]>(`${apiBase}/variantes/${v.idVariante}/valores`).then(l => l.map(x => ({ ...x, variante: v.nombre }))).catch(() => []))).then(l => l.flat()),
      Promise.all(productos.map(p => api.leer<Presentacion[]>(`${apiBase}/productos/${p.idProducto}/presentaciones`).catch(() => [] as Presentacion[]))),
    ])
    const porId = new Map(inventario.map(i => [i.idPresentacion, i]))
    const nombreValor = new Map(valores.map(v => [v.idValor, `${capitalizar(v.variante)}: ${v.valor}`]))
    const grupos: Grupo[] = productos.map((producto, i) => ({ producto, lineas: presentaciones[i].map(presentacion => ({ presentacion, inventario: porId.get(presentacion.idPresentacion), etiquetas: presentacion.idsValores.map(id => nombreValor.get(id)).filter((x): x is string => !!x) })) }))
    return grupos.filter(g => g.lineas.length > 0)
  }, [apiBase])

  if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />

  const resumen = (g: Grupo) => {
    const activas = g.lineas.filter(l => l.presentacion.estado === 'ACTIVA' && l.inventario && !l.inventario.sinRegistroInventario)
    const total = activas.reduce((s, l) => s + (l.inventario!.stockActual ?? 0), 0)
    const minimo = activas.reduce((s, l) => s + (l.inventario!.stockMinimo ?? 0), 0)
    const bajas = activas.filter(l => nivelDe(l.inventario!.stockActual ?? 0, l.inventario!.stockMinimo ?? 0) !== 'disponible').length
    const nivel: Nivel = activas.length === 0 ? 'sin-registro' : total === 0 ? 'agotado' : bajas > 0 ? 'bajo' : 'disponible'
    return { total, minimo, bajas, nivel, activas: activas.length }
  }
  const termino = texto.trim().toLowerCase()
  const visibles = datos.filter(g => {
    const r = resumen(g)
    if (estadoProducto && g.producto.estado !== estadoProducto) return false
    if (alerta === 'bajo' && r.nivel === 'disponible') return false
    if (alerta === 'ok' && r.nivel !== 'disponible') return false
    return !termino || g.producto.nombre.toLowerCase().includes(termino) || g.lineas.some(l => l.presentacion.sku.toLowerCase().includes(termino))
  })
  const todas = datos.flatMap(g => g.lineas.filter(l => l.presentacion.estado === 'ACTIVA' && l.inventario && !l.inventario.sinRegistroInventario))
  const unidades = todas.reduce((s, l) => s + (l.inventario!.stockActual ?? 0), 0)
  const bajas = todas.filter(l => nivelDe(l.inventario!.stockActual ?? 0, l.inventario!.stockMinimo ?? 0) === 'bajo').length
  const agotadas = todas.filter(l => (l.inventario!.stockActual ?? 0) === 0).length
  const filtros = [texto, estadoProducto, alerta].filter(Boolean).length

  return <div className="space-y-6">
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div><h2 className="text-xl font-semibold tracking-tight">Inventario</h2><p className="text-sm text-muted-foreground">Stock por producto. Haz clic en un producto para administrar el stock de sus variantes.</p></div>
      <div className="flex rounded-xl border bg-card p-1" role="tablist" aria-label="Filtrar por stock">
        {([['', 'Todo', Boxes], ['bajo', 'Requiere atención', TriangleAlert], ['ok', 'Suficiente', PackageCheck]] as const).map(([valor, nombre, Icono]) => <button key={nombre} type="button" role="tab" aria-selected={alerta === valor} onClick={() => setAlerta(valor)} className={`inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg px-3.5 text-sm font-medium transition ${alerta === valor ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`}><Icono className="size-4" />{nombre}</button>)}
      </div>
    </div>

    <div className="grid gap-3 sm:grid-cols-4">
      {([['Productos', datos.length, 'text-foreground', Package], ['Unidades en stock', unidades, 'text-emerald-600 dark:text-emerald-400', Boxes], ['Variantes con stock bajo', bajas, 'text-amber-600 dark:text-amber-400', TriangleAlert], ['Variantes agotadas', agotadas, 'text-red-600 dark:text-red-400', PackageX]] as const).map(([titulo, valor, color, Icono]) => <div key={titulo} className="flex items-center gap-3 rounded-2xl border bg-card p-4"><span className={`grid size-10 place-items-center rounded-xl bg-muted ${color}`}><Icono className="size-5" /></span><div><p className="text-sm text-muted-foreground">{titulo}</p><p className="text-2xl font-semibold tracking-tight">{valor}</p></div></div>)}
    </div>

    <div className="flex flex-wrap items-center gap-3">
      <label className="relative min-w-60 flex-1"><span className="sr-only">Buscar producto o SKU</span><Search className="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-muted-foreground" /><input className={`${claseEntrada} pl-11`} placeholder="Buscar producto o SKU..." value={texto} onChange={e => setTexto(e.target.value)} /></label>
      <label className="flex h-11 items-center gap-2 rounded-xl border border-input bg-card pr-2 pl-4 text-sm"><span className="text-muted-foreground">Producto:</span><select className="h-full cursor-pointer bg-transparent font-medium outline-none" value={estadoProducto} onChange={e => setProducto(e.target.value)}><option value="">Todos</option>{['BORRADOR', 'PUBLICADO', 'INACTIVO'].map(x => <option key={x} value={x}>{capitalizar(x)}</option>)}</select></label>
      {filtros > 0 && <Button variant="ghost" className="h-11 rounded-xl" onClick={() => { setTexto(''); setProducto(''); setAlerta('') }}><X className="size-4" />Limpiar filtros</Button>}
    </div>

    {visibles.length === 0 ? <div className="rounded-2xl border border-dashed bg-card px-6 py-14 text-center"><span className="mx-auto grid size-12 place-items-center rounded-2xl bg-primary/10 text-primary"><Package className="size-6" /></span><p className="mt-3 font-medium">{datos.length ? 'Ningún producto coincide' : 'Aún no hay stock que administrar'}</p><p className="mt-1 text-sm text-muted-foreground">{datos.length ? 'Prueba con otros filtros.' : 'Crea productos con presentaciones en Catálogo para verlos aquí.'}</p></div>
    : <div className="overflow-hidden rounded-2xl border bg-card">
      <div className="hidden grid-cols-[minmax(0,2.4fr)_1fr_1.6fr_1fr_28px] gap-4 border-b bg-muted/40 px-5 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase md:grid"><span>Producto</span><span>Variantes</span><span>Stock total</span><span>Alerta</span><span /></div>
      <ul className="divide-y">{visibles.map(g => {
        const r = resumen(g); const abiertoAhora = abierto === g.producto.idProducto
        const porcentaje = r.activas === 0 ? 0 : Math.min(100, (r.total / Math.max(r.minimo * 3, 1)) * 100)
        return <li key={g.producto.idProducto} className={abiertoAhora ? 'bg-muted/20' : ''}>
          <button type="button" aria-expanded={abiertoAhora} onClick={() => setAbierto(abiertoAhora ? '' : g.producto.idProducto)} className="grid w-full cursor-pointer items-center gap-4 px-5 py-4 text-left transition hover:bg-muted/30 md:grid-cols-[minmax(0,2.4fr)_1fr_1.6fr_1fr_28px]">
            <span className="flex min-w-0 items-center gap-3">
              <FotoProducto apiBase={apiBase} idProducto={g.producto.idProducto} />
              <span className="min-w-0"><span className="block truncate font-semibold">{g.producto.nombre}</span><span className="text-xs text-muted-foreground">{capitalizar(g.producto.estado)}</span></span>
            </span>
            <span className="text-sm"><span className="font-semibold">{g.lineas.length}</span> <span className="text-muted-foreground">{g.lineas.length === 1 ? 'presentación' : 'variantes'}</span>{r.bajas > 0 && <span className="block text-xs text-amber-600 dark:text-amber-400">{r.bajas} por reponer</span>}</span>
            <span><span className="text-sm"><span className="text-lg font-semibold">{r.total}</span> <span className="text-muted-foreground">unid.</span></span><span className="mt-1.5 block h-1.5 w-full max-w-[180px] overflow-hidden rounded-full bg-muted"><span className={`block h-full rounded-full ${niveles[r.nivel].barra}`} style={{ width: `${porcentaje}%` }} /></span></span>
            <Etiqueta nivel={r.nivel} />
            <ChevronDown className={`size-5 text-muted-foreground transition-transform ${abiertoAhora ? 'rotate-180' : ''}`} />
          </button>
          {abiertoAhora && <AdministradorVariantes key={g.lineas.map(l => `${l.presentacion.idPresentacion}${l.inventario?.stockActual}${l.inventario?.stockMinimo}`).join('|')} grupo={g} apiBase={apiBase} base={base} alGuardar={recargar} />}
        </li>
      })}</ul>
    </div>}
  </div>
}

function FotoProducto({ apiBase, idProducto }: { apiBase: string; idProducto: string }) {
  const { datos } = useCarga(() => api.leer<Imagen[]>(`${apiBase}/productos/${idProducto}/imagenes`), [apiBase, idProducto])
  const imagen = datos?.find(i => i.esPrincipal) ?? datos?.[0]
  return <span className="relative grid size-12 shrink-0 place-items-center overflow-hidden rounded-xl bg-muted text-muted-foreground">{imagen ? <img src={imagen.url} alt="" className="size-full object-cover" /> : datos ? <ImageOff className="size-5" /> : <span className="size-full animate-pulse bg-muted" />}</span>
}

// Edición en lote del stock de todas las variantes de un producto.
function AdministradorVariantes({ grupo, apiBase, base, alGuardar }: { grupo: Grupo; apiBase: string; base: string; alGuardar: () => void }) {
  const editables = grupo.lineas.filter(l => l.inventario && !l.inventario.sinRegistroInventario)
  const inicial = Object.fromEntries(editables.map(l => [l.presentacion.idPresentacion, { actual: l.inventario!.stockActual ?? 0, minimo: l.inventario!.stockMinimo ?? 0 }]))
  const [valores, setValores] = useState<Record<string, { actual: number; minimo: number }>>(inicial)
  const [paraTodas, setParaTodas] = useState(''); const op = useOperacion(); const [guardado, setGuardado] = useState(false)
  const cambiadas = editables.filter(l => { const a = valores[l.presentacion.idPresentacion], b = inicial[l.presentacion.idPresentacion]; return a.actual !== b.actual || a.minimo !== b.minimo })
  const totalNuevo = editables.filter(l => l.presentacion.estado === 'ACTIVA').reduce((s, l) => s + valores[l.presentacion.idPresentacion].actual, 0)
  const totalAntes = editables.filter(l => l.presentacion.estado === 'ACTIVA').reduce((s, l) => s + inicial[l.presentacion.idPresentacion].actual, 0)
  const cambiar = (id: string, campo: 'actual' | 'minimo', valor: number) => { setGuardado(false); setValores(v => ({ ...v, [id]: { ...v[id], [campo]: Math.max(0, valor) } })) }
  useEffect(() => { if (!cambiadas.length) return; const avisar = (e: BeforeUnloadEvent) => e.preventDefault(); window.addEventListener('beforeunload', avisar); return () => window.removeEventListener('beforeunload', avisar) }, [cambiadas.length])
  const paso = 'grid size-9 shrink-0 cursor-pointer place-items-center rounded-lg border bg-background transition hover:bg-muted disabled:opacity-40'

  return <div className="border-t bg-background/60 px-5 py-5">
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm font-semibold">Stock por variante</p>
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs text-muted-foreground">Poner a todas:</span>
        <input className="h-9 w-24 rounded-lg border border-input bg-background px-2.5 text-sm outline-none focus:border-primary" type="number" min="0" value={paraTodas} onChange={e => setParaTodas(e.target.value)} placeholder="Ej. 10" />
        <Button variant="outline" size="sm" disabled={paraTodas === ''} onClick={() => { setGuardado(false); setValores(v => Object.fromEntries(Object.entries(v).map(([id, x]) => [id, { ...x, actual: Math.max(0, Number(paraTodas)) }]))) }}>Aplicar</Button>
        <Link to={`${base}/catalogo/productos/${grupo.producto.idProducto}`} className="text-sm font-medium text-primary hover:underline">Ver producto</Link>
      </div>
    </div>

    <div className="overflow-hidden rounded-xl border">
      <div className="hidden grid-cols-[minmax(0,2fr)_1.4fr_1fr_1fr] gap-3 bg-muted/40 px-4 py-2.5 text-[11px] font-semibold tracking-wide text-muted-foreground uppercase sm:grid"><span>Variante</span><span>Stock</span><span>Mínimo</span><span>Alerta</span></div>
      <ul className="divide-y">{grupo.lineas.map(l => {
        const id = l.presentacion.idPresentacion; const v = valores[id]; const inactiva = l.presentacion.estado !== 'ACTIVA'
        const cambiada = v && (v.actual !== inicial[id].actual || v.minimo !== inicial[id].minimo)
        return <li key={id} className={`grid items-center gap-3 px-4 py-3 sm:grid-cols-[minmax(0,2fr)_1.4fr_1fr_1fr] ${inactiva ? 'opacity-60' : ''} ${cambiada ? 'bg-primary/5' : ''}`}>
          <div className="min-w-0">
            <div className="flex flex-wrap gap-1">{l.etiquetas.length ? l.etiquetas.map(t => <span key={t} className="rounded-md bg-muted px-2 py-0.5 text-xs font-medium">{t}</span>) : <span className="text-sm font-medium">Presentación única</span>}{inactiva && <span className="rounded-md bg-zinc-500/15 px-2 py-0.5 text-xs text-muted-foreground">Inactiva</span>}</div>
            <p className="mt-1 truncate font-mono text-[11px] text-muted-foreground">SKU {l.presentacion.sku}</p>
          </div>
          {v ? <>
            <div className="flex items-center gap-1.5">
              <button type="button" className={paso} aria-label="Restar 1" disabled={v.actual <= 0} onClick={() => cambiar(id, 'actual', v.actual - 1)}><Minus className="size-3.5" /></button>
              <input aria-label={`Stock de ${l.presentacion.sku}`} className="h-9 w-16 rounded-lg border border-input bg-background text-center text-sm font-semibold outline-none focus:border-primary" type="number" min="0" value={v.actual} onChange={e => cambiar(id, 'actual', Number(e.target.value))} />
              <button type="button" className={paso} aria-label="Sumar 1" onClick={() => cambiar(id, 'actual', v.actual + 1)}><Plus className="size-3.5" /></button>
            </div>
            <input aria-label={`Stock mínimo de ${l.presentacion.sku}`} className="h-9 w-20 rounded-lg border border-input bg-background px-2.5 text-sm outline-none focus:border-primary" type="number" min="0" value={v.minimo} onChange={e => cambiar(id, 'minimo', Number(e.target.value))} />
            <Etiqueta nivel={nivelDe(v.actual, v.minimo)} />
          </> : <p className="text-sm text-muted-foreground sm:col-span-3">Sin registro de inventario</p>}
        </li>
      })}</ul>
    </div>

    <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm">Stock total: <strong>{totalNuevo} unid.</strong>{totalNuevo !== totalAntes && <span className={`ml-2 font-medium ${totalNuevo > totalAntes ? 'text-emerald-600 dark:text-emerald-400' : 'text-red-600 dark:text-red-400'}`}>({totalNuevo > totalAntes ? '+' : ''}{totalNuevo - totalAntes})</span>}<span className="ml-1 text-xs text-muted-foreground">· suma de las variantes activas</span></p>
      <div className="flex items-center gap-2">
        {guardado && !cambiadas.length && <span className="text-sm font-medium text-emerald-600 dark:text-emerald-400">Stock guardado</span>}
        {cambiadas.length > 0 && <Button variant="ghost" size="sm" onClick={() => setValores(inicial)}><Undo2 className="size-4" />Deshacer</Button>}
        <Button className="rounded-xl" disabled={op.ocupado || cambiadas.length === 0} onClick={() => void op.ejecutar(async () => { for (const l of cambiadas) { const x = valores[l.presentacion.idPresentacion]; await api.actualizar(`${apiBase}/inventario/${l.presentacion.idPresentacion}`, { stockActual: x.actual, stockMinimo: x.minimo }) } setGuardado(true); alGuardar() })}>{op.ocupado ? 'Guardando...' : cambiadas.length ? `Guardar ${cambiadas.length} ${cambiadas.length === 1 ? 'cambio' : 'cambios'}` : 'Sin cambios'}</Button>
      </div>
    </div>
    <div className="mt-2"><Aviso error={op.error} /></div>
  </div>
}
