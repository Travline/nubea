import { useState } from 'react'
import type { ReactNode } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import { Boxes, ChevronDown, ClipboardCheck, ImageOff, Info, Minus, Package, PackageCheck, PackageX, Plus, Search, TriangleAlert, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { ContextoTienda } from '@/paginas/panel'
import { api } from './datos'
import type { Imagen, Inventario, PaginaInventario, Presentacion, Producto, StockActual, Valor, Variante } from './datos'
import { Aviso, ErrorCampo, EstadoCarga, conError, useCarga, useOperacion, useSoloLectura } from './comunes'
import { claseTono, etiquetaProducto, etiquetaVersion, opcionesProducto } from './estados'

const claseEntrada = 'h-11 w-full rounded-xl border border-input bg-card px-3.5 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20'
const capitalizar = (t: string) => t.charAt(0) + t.slice(1).toLowerCase()

type Nivel = 'disponible' | 'bajo' | 'agotado' | 'sin-registro'
const niveles: Record<Nivel, { texto: string; clase: string; barra: string; icono: ReactNode }> = {
  disponible: { texto: 'Disponible', clase: 'bg-emerald-500/12 text-emerald-700 dark:text-emerald-400', barra: 'bg-emerald-500', icono: <PackageCheck className="size-3.5" /> },
  bajo: { texto: 'Pocas unidades', clase: 'bg-amber-500/15 text-amber-700 dark:text-amber-400', barra: 'bg-amber-500', icono: <TriangleAlert className="size-3.5" /> },
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
  const { datos, error, recargar, actualizarDatos } = useCarga(async () => {
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
  // Aplica la respuesta de un ajuste o conteo sin volver a cargar todo el inventario.
  const aplicar = (n: StockActual) => actualizarDatos(gs => gs.map(g => ({ ...g, lineas: g.lineas.map(l => l.presentacion.idPresentacion === n.idPresentacion && l.inventario ? { ...l, inventario: { ...l.inventario, stockActual: n.stockActual, stockMinimo: n.stockMinimo, stockReservado: n.stockReservado } } : l) })))

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
  const apartadas = todas.reduce((s, l) => s + (l.inventario!.stockReservado ?? 0), 0)
  const filtros = [texto, estadoProducto, alerta].filter(Boolean).length

  return <div className="space-y-6">
    <div className="flex flex-wrap items-end justify-between gap-3">
      <div><h2 className="text-xl font-semibold tracking-tight">Inventario</h2><p className="text-sm text-muted-foreground">Unidades por producto. Haz clic en un producto para ajustar las unidades de cada versión.</p></div>
      <div className="flex rounded-xl border bg-card p-1" role="tablist" aria-label="Filtrar por stock">
        {([['', 'Todo', Boxes], ['bajo', 'Requiere atención', TriangleAlert], ['ok', 'Suficiente', PackageCheck]] as const).map(([valor, nombre, Icono]) => <button key={nombre} type="button" role="tab" aria-selected={alerta === valor} onClick={() => setAlerta(valor)} className={`inline-flex h-9 cursor-pointer items-center gap-2 rounded-lg px-3.5 text-sm font-medium transition ${alerta === valor ? 'bg-primary text-primary-foreground shadow-sm' : 'text-muted-foreground hover:bg-muted hover:text-foreground'}`}><Icono className="size-4" />{nombre}</button>)}
      </div>
    </div>

    <div className="grid gap-3 sm:grid-cols-4">
      {([['Productos', datos.length, 'text-foreground', Package], ['Unidades disponibles', unidades, 'text-emerald-600 dark:text-emerald-400', Boxes], ['Versiones con pocas unidades', bajas, 'text-amber-600 dark:text-amber-400', TriangleAlert], ['Versiones agotadas', agotadas, 'text-red-600 dark:text-red-400', PackageX]] as const).map(([titulo, valor, color, Icono]) => <div key={titulo} className="flex items-center gap-3 rounded-2xl border bg-card p-4"><span className={`grid size-10 place-items-center rounded-xl bg-muted ${color}`}><Icono className="size-5" /></span><div><p className="text-sm text-muted-foreground">{titulo}</p><p className="text-2xl font-semibold tracking-tight">{valor}</p></div></div>)}
    </div>

    {apartadas > 0 && <p className="flex items-start gap-2 rounded-xl border bg-card px-4 py-3 text-sm text-muted-foreground"><Info className="mt-0.5 size-4 shrink-0 text-primary" /><span><strong className="text-foreground">{apartadas} {apartadas === 1 ? 'unidad apartada' : 'unidades apartadas'}</strong> en pedidos pendientes de pago. No se pueden vender a otro cliente; si el pago no llega a tiempo, vuelven a estar disponibles solas.</span></p>}

    <div className="flex flex-wrap items-center gap-3">
      <label className="relative min-w-60 flex-1"><span className="sr-only">Buscar producto o código interno</span><Search className="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-muted-foreground" /><input className={`${claseEntrada} pl-11`} placeholder="Buscar producto o código interno..." value={texto} onChange={e => setTexto(e.target.value)} /></label>
      <label className="flex h-11 items-center gap-2 rounded-xl border border-input bg-card pr-2 pl-4 text-sm"><span className="text-muted-foreground">Producto:</span><select className="h-full cursor-pointer bg-transparent font-medium outline-none" value={estadoProducto} onChange={e => setProducto(e.target.value)}><option value="">Todos</option>{opcionesProducto.map(o => <option key={o.valor} value={o.valor}>{o.texto}</option>)}</select></label>
      {filtros > 0 && <Button variant="ghost" className="h-11 rounded-xl" onClick={() => { setTexto(''); setProducto(''); setAlerta('') }}><X className="size-4" />Limpiar filtros</Button>}
    </div>

    {visibles.length === 0 ? <div className="rounded-2xl border border-dashed bg-card px-6 py-14 text-center"><span className="mx-auto grid size-12 place-items-center rounded-2xl bg-primary/10 text-primary"><Package className="size-6" /></span><p className="mt-3 font-medium">{datos.length ? 'Ningún producto coincide' : 'Aún no hay stock que administrar'}</p><p className="mt-1 text-sm text-muted-foreground">{datos.length ? 'Prueba con otros filtros.' : 'Crea productos con sus versiones en Catálogo para verlos aquí.'}</p></div>
    : <div className="overflow-hidden rounded-2xl border bg-card">
      <div className="hidden grid-cols-[minmax(0,2.4fr)_1fr_1.6fr_1fr_28px] gap-4 border-b bg-muted/40 px-5 py-3 text-xs font-semibold tracking-wide text-muted-foreground uppercase md:grid"><span>Producto</span><span>Versiones</span><span>Disponibles</span><span>Alerta</span><span /></div>
      <ul className="divide-y">{visibles.map(g => {
        const r = resumen(g); const abiertoAhora = abierto === g.producto.idProducto
        const porcentaje = r.activas === 0 ? 0 : Math.min(100, (r.total / Math.max(r.minimo * 3, 1)) * 100)
        return <li key={g.producto.idProducto} className={abiertoAhora ? 'bg-muted/20' : ''}>
          <button type="button" aria-expanded={abiertoAhora} onClick={() => setAbierto(abiertoAhora ? '' : g.producto.idProducto)} className="grid w-full cursor-pointer items-center gap-4 px-5 py-4 text-left transition hover:bg-muted/30 md:grid-cols-[minmax(0,2.4fr)_1fr_1.6fr_1fr_28px]">
            <span className="flex min-w-0 items-center gap-3">
              <FotoProducto apiBase={apiBase} idProducto={g.producto.idProducto} />
              <span className="min-w-0"><span className="block truncate font-semibold">{g.producto.nombre}</span><span className="text-xs text-muted-foreground">{etiquetaProducto(g.producto.estado).texto}</span></span>
            </span>
            <span className="text-sm"><span className="font-semibold">{g.lineas.length}</span> <span className="text-muted-foreground">{g.lineas.length === 1 ? 'versión' : 'versiones'}</span>{r.bajas > 0 && <span className="block text-xs text-amber-600 dark:text-amber-400">{r.bajas} por reponer</span>}</span>
            <span><span className="text-sm"><span className="text-lg font-semibold">{r.total}</span> <span className="text-muted-foreground">unid.</span></span><span className="mt-1.5 block h-1.5 w-full max-w-[180px] overflow-hidden rounded-full bg-muted"><span className={`block h-full rounded-full ${niveles[r.nivel].barra}`} style={{ width: `${porcentaje}%` }} /></span></span>
            <Etiqueta nivel={r.nivel} />
            <ChevronDown className={`size-5 text-muted-foreground transition-transform ${abiertoAhora ? 'rotate-180' : ''}`} />
          </button>
          {abiertoAhora && <VersionesProducto grupo={g} apiBase={apiBase} base={base} alCambiar={aplicar} alRecargar={recargar} />}
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

// Unidades de cada versión de un producto: cada fila se ajusta por separado.
function VersionesProducto({ grupo, apiBase, base, alCambiar, alRecargar }: { grupo: Grupo; apiBase: string; base: string; alCambiar: (n: StockActual) => void; alRecargar: () => void }) {
  return <div className="border-t bg-background/60 px-5 py-5">
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <p className="text-sm font-semibold">Unidades por versión</p>
      <Link to={`${base}/catalogo/productos/${grupo.producto.idProducto}`} className="text-sm font-medium text-primary hover:underline">Ver producto</Link>
    </div>
    <ul className="divide-y overflow-hidden rounded-xl border">{grupo.lineas.map(l => {
      const id = l.presentacion.idPresentacion; const inactiva = l.presentacion.estado !== 'ACTIVA'; const inv = l.inventario
      return <li key={id} className={`grid items-start gap-3 px-4 py-3 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,2fr)_auto] ${inactiva ? 'bg-muted/30' : ''}`}>
        <div className="min-w-0">
          <div className="flex flex-wrap gap-1">{l.etiquetas.length ? l.etiquetas.map(t => <span key={t} className="rounded-md bg-muted px-2 py-0.5 text-xs font-medium">{t}</span>) : <span className="text-sm font-medium">Versión única</span>}{inactiva && <span className={`rounded-md px-2 py-0.5 text-xs ${claseTono.neutro.clase}`}>{etiquetaVersion(l.presentacion.estado).texto}</span>}</div>
          <p className="mt-1 truncate font-mono text-[11px] text-muted-foreground">Código interno {l.presentacion.sku}</p>
        </div>
        {inv && !inv.sinRegistroInventario
          ? <><ControlStock apiBase={apiBase} idPresentacion={id} inventario={inv} nombre={l.etiquetas.join(', ') || grupo.producto.nombre} alCambiar={alCambiar} alRecargar={alRecargar} /><Etiqueta nivel={nivelDe(inv.stockActual ?? 0, inv.stockMinimo ?? 0)} /></>
          : <p className="text-sm text-muted-foreground lg:col-span-2">Esta versión aún no tiene registro de unidades.</p>}
      </li>
    })}</ul>
  </div>
}

type ValoresStock = Pick<Inventario, 'stockActual' | 'stockMinimo' | 'stockReservado'>

/**
 * Unidades de una versión: "N disponibles · M apartadas", +/− para el día a día (POST …/ajustes) y "Fijar por conteo"
 * (PUT …/inventario/{id}). En el conteo se indican las unidades EN MANO: el backend resta las apartadas y rechaza
 * con 409 si el stock cambió desde que se abrió el formulario (stockEsperado).
 */
export function ControlStock({ apiBase, idPresentacion, inventario, nombre, alCambiar, alRecargar }: { apiBase: string; idPresentacion: string; inventario: ValoresStock; nombre: string; alCambiar: (n: StockActual) => void; alRecargar?: () => void }) {
  const soloLectura = useSoloLectura()
  const disponibles = inventario.stockActual ?? 0, minimo = inventario.stockMinimo ?? 0, apartadas = inventario.stockReservado ?? 0
  const ajuste = useOperacion(['cantidad'])
  const conteo = useOperacion(['stockActual', 'stockMinimo', 'stockEsperado'])
  const [abierto, setAbierto] = useState(false); const [confirmar, setConfirmar] = useState(false)
  const [enMano, setEnMano] = useState(''); const [nuevoMinimo, setNuevoMinimo] = useState(''); const [esperado, setEsperado] = useState(0)
  const ruta = `${apiBase}/inventario/${idPresentacion}`
  const ajustar = (cantidad: number) => void ajuste.ejecutar(async () => { alCambiar(await api.crear<StockActual>(`${ruta}/ajustes`, { cantidad })) })
  function abrir() { setEnMano(String(disponibles + apartadas)); setNuevoMinimo(String(minimo)); setEsperado(disponibles); setConfirmar(false); conteo.limpiar(); setAbierto(true) }
  const cantidadEnMano = Number(enMano), quedan = cantidadEnMano - apartadas
  const conteoValido = enMano !== '' && nuevoMinimo !== '' && Number.isInteger(cantidadEnMano) && cantidadEnMano >= 0 && Number(nuevoMinimo) >= 0
  const guardar = () => void conteo.ejecutar(async () => {
    await api.actualizar(ruta, { stockActual: cantidadEnMano, stockMinimo: Number(nuevoMinimo), stockEsperado: esperado })
    alCambiar({ idPresentacion, stockActual: quedan, stockMinimo: Number(nuevoMinimo), stockReservado: apartadas }); setAbierto(false)
  })
  const paso = 'grid size-9 shrink-0 cursor-pointer place-items-center rounded-lg border bg-background transition hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40'
  const errorCambio = conteo.errorDe('stockEsperado')

  return <div className="min-w-0 space-y-2">
    <div className="flex flex-wrap items-center gap-2">
      <button type="button" className={paso} aria-label={`Quitar 1 unidad de ${nombre}`} disabled={soloLectura || ajuste.ocupado || disponibles <= 0} onClick={() => ajustar(-1)}><Minus className="size-3.5" /></button>
      <p className="min-w-[8.5rem] text-sm" aria-live="polite"><strong className="text-base">{disponibles}</strong> {disponibles === 1 ? 'disponible' : 'disponibles'}{apartadas > 0 && <span className="text-muted-foreground" title="Unidades de pedidos pendientes de pago. Si el pago no llega, vuelven a estar disponibles."> · {apartadas} {apartadas === 1 ? 'apartada' : 'apartadas'}</span>}</p>
      <button type="button" className={paso} aria-label={`Sumar 1 unidad a ${nombre}`} disabled={soloLectura || ajuste.ocupado} onClick={() => ajustar(1)}><Plus className="size-3.5" /></button>
      <Button type="button" variant="ghost" size="sm" disabled={soloLectura} onClick={() => abierto ? setAbierto(false) : abrir()}><ClipboardCheck className="size-4" />Fijar por conteo</Button>
    </div>
    <p className="text-xs text-muted-foreground">Avisarme cuando queden {minimo} o menos</p>
    {apartadas > 0 && <p className="text-xs text-muted-foreground">Las apartadas son de pedidos pendientes de pago: no se pueden vender a otro cliente.</p>}
    <ErrorCampo mensaje={ajuste.errorDe('cantidad')} /><Aviso error={ajuste.error} />

    {abierto && <div className="space-y-3 rounded-xl border bg-card p-3">
      {!confirmar ? <>
        <p className="text-xs text-muted-foreground">Cuenta las unidades que tienes físicamente, <strong className="text-foreground">incluidas las {apartadas} apartadas</strong> para pedidos pendientes de pago.</p>
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="grid gap-1.5 text-sm font-medium">Unidades en mano<input className={`h-10 rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-primary${conError(conteo.errorDe('stockActual'))}`} type="number" min="0" step="1" value={enMano} onChange={e => setEnMano(e.target.value)} /><ErrorCampo mensaje={conteo.errorDe('stockActual')} /></label>
          <label className="grid gap-1.5 text-sm font-medium">Avisarme cuando queden<input className={`h-10 rounded-lg border border-input bg-background px-3 text-sm outline-none focus:border-primary${conError(conteo.errorDe('stockMinimo'))}`} type="number" min="0" step="1" value={nuevoMinimo} onChange={e => setNuevoMinimo(e.target.value)} /><ErrorCampo mensaje={conteo.errorDe('stockMinimo')} /></label>
        </div>
        {conteoValido && cantidadEnMano < apartadas && <p className="text-xs text-destructive">No puedes contar menos de {apartadas}: esas unidades están apartadas para pedidos.</p>}
        <div className="flex justify-end gap-2"><Button type="button" variant="ghost" size="sm" onClick={() => setAbierto(false)}>Cancelar</Button><Button type="button" size="sm" disabled={!conteoValido || cantidadEnMano < apartadas} onClick={() => setConfirmar(true)}>Revisar</Button></div>
      </> : <>
        <p className="text-sm">Vas a dejar <strong>{cantidadEnMano} {cantidadEnMano === 1 ? 'unidad' : 'unidades'} en mano</strong>{apartadas > 0 ? <>: {apartadas} siguen apartadas y quedarán <strong>{quedan} disponibles</strong> para vender</> : <> disponibles para vender</>} (ahora hay {disponibles}). Te avisaremos cuando queden {nuevoMinimo} o menos.</p>
        {errorCambio && <div className="flex flex-wrap items-center gap-2 rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive"><span>{errorCambio}</span>{alRecargar && <Button type="button" variant="outline" size="sm" onClick={() => { setAbierto(false); alRecargar() }}>Ver el valor actual</Button>}</div>}
        <ErrorCampo mensaje={conteo.errorDe('stockActual')} /><ErrorCampo mensaje={conteo.errorDe('stockMinimo')} /><Aviso error={conteo.error === 'Revisa los campos marcados.' ? '' : conteo.error} />
        <div className="flex justify-end gap-2"><Button type="button" variant="ghost" size="sm" disabled={conteo.ocupado} onClick={() => setConfirmar(false)}>Volver</Button><Button type="button" size="sm" disabled={conteo.ocupado || !!errorCambio} onClick={guardar}>{conteo.ocupado ? 'Guardando...' : 'Confirmar conteo'}</Button></div>
      </>}
    </div>}
  </div>
}
