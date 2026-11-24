import { Link, NavLink, Outlet, useParams } from 'react-router-dom'
import { ArrowLeft, Boxes, CreditCard, ExternalLink, Eye, Info, Package, Palette } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { api, rutaTienda } from '@/comercio/datos'
import type { Tienda } from '@/comercio/datos'
import { Aviso, EstadoCarga, useCarga, useOperacion } from '@/comercio/comunes'
import { claseTono, etiquetaTienda } from '@/comercio/estados'
import type { ContextoTienda } from '@/paginas/panel'

/** Respuesta de /api/admin/tiendas: la tienda con su dueño legible. */
export interface TiendaAdministrada {
  idTienda: string; nombre: string; slug: string; estado: string; plantilla: string | null; fechaCreacion: string
  idPropietario: string; nombrePropietario: string; correoPropietario: string
}

export function EstadoTiendaAdmin({ estado }: { estado: string }) {
  const e = etiquetaTienda(estado); const t = claseTono[e.tono]
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${t.clase}`}><span className={`size-1.5 rounded-full ${t.punto}`} />{e.texto}</span>
}

/**
 * Suspender (abierta o en preparación → suspendida) o reactivar (suspendida → en preparación) con
 * PATCH /api/admin/tiendas/{id}/estado. Una tienda cerrada no admite ninguno de los dos.
 */
export function AccionesEstadoTienda({ tienda, alCambiar }: { tienda: TiendaAdministrada; alCambiar: (t: TiendaAdministrada) => void }) {
  const op = useOperacion()
  const cambiar = (estado: 'SUSPENDIDA' | 'ACTIVA') => void op.ejecutar(async () => { alCambiar(await api.modificar<TiendaAdministrada>(`/api/admin/tiendas/${tienda.idTienda}/estado`, { estado })) })
  function suspender() {
    if (window.confirm(`¿Suspender «${tienda.nombre}»?\n\nSus clientes dejarán de verla y no podrán comprar. Los pedidos que esperan pago se mantienen. ${tienda.nombrePropietario || 'El dueño'} podrá consultar sus datos, pero no modificarlos.`)) cambiar('SUSPENDIDA')
  }
  function reactivar() {
    if (window.confirm(`¿Reactivar «${tienda.nombre}»?\n\nLa tienda vuelve a "En preparación": no se abre sola. ${tienda.nombrePropietario || 'El dueño'} podrá editarla y tendrá que abrirla de nuevo para que sus clientes la vean.`)) cambiar('ACTIVA')
  }
  if (tienda.estado === 'CERRADA') return <p className="text-sm text-muted-foreground">Una tienda cerrada no se puede suspender ni reactivar.</p>
  return <div className="grid justify-items-start gap-2">
    {tienda.estado === 'SUSPENDIDA'
      ? <Button type="button" variant="outline" disabled={op.ocupado} onClick={reactivar}>{op.ocupado ? 'Reactivando...' : 'Reactivar'}</Button>
      : <Button type="button" variant="outline" className="border-destructive/40 text-destructive hover:bg-destructive/10" disabled={op.ocupado} onClick={suspender}>{op.ocupado ? 'Suspendiendo...' : 'Suspender'}</Button>}
    <Aviso error={op.error} />
  </div>
}

export function FilaTiendaAdmin({ tienda, alCambiar }: { tienda: TiendaAdministrada; alCambiar: (t: TiendaAdministrada) => void }) {
  return <div className="flex flex-wrap items-start justify-between gap-4 rounded-lg border p-4">
    <div className="min-w-0 space-y-1">
      <div className="flex flex-wrap items-center gap-2"><strong>{tienda.nombre}</strong><EstadoTiendaAdmin estado={tienda.estado} /></div>
      <p className="text-sm text-muted-foreground">/tienda/{tienda.slug} · Plantilla: {tienda.plantilla ?? 'Sin configurar'}</p>
      <p className="text-sm">Dueño: <span className="font-medium">{tienda.nombrePropietario || 'Sin nombre'}</span> <span className="text-muted-foreground">· {tienda.correoPropietario}</span></p>
    </div>
    <div className="flex flex-wrap items-start gap-2">
      <Link to={`/panel/administracion/tiendas/${tienda.idTienda}/catalogo`} className="inline-flex h-9 items-center gap-1.5 rounded-md border px-3 text-sm font-medium hover:bg-accent"><Eye className="size-4" />Ver contenido</Link>
      <AccionesEstadoTienda tienda={tienda} alCambiar={alCambiar} />
    </div>
  </div>
}

const secciones = [['catalogo', 'Catálogo', Package], ['inventario', 'Inventario', Boxes], ['apariencia', 'Apariencia', Palette], ['cuenta-pago', 'Cuenta de pago', CreditCard]] as const

/**
 * Contenido de una tienda visto por el administrador, en solo lectura: reutiliza las pantallas del vendedor con
 * soloLectura y esAdmin. No incluye pedidos ni pagos (datos personales de los compradores; el backend responde 403).
 */
export function TiendaAdministradaVista() {
  const { idTienda = '' } = useParams()
  const { datos, error, recargar, actualizarDatos } = useCarga(() => api.leer<TiendaAdministrada>(`/api/admin/tiendas/${encodeURIComponent(idTienda)}`), [idTienda])
  if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
  const base = `/panel/administracion/tiendas/${idTienda}`
  const tienda: Tienda = { idTienda: datos.idTienda, idPropietario: datos.idPropietario, nombre: datos.nombre, slug: datos.slug, estado: datos.estado, plantilla: datos.plantilla ?? '' }
  const contexto: ContextoTienda = { tienda, base, apiBase: rutaTienda(idTienda), actualizarTienda: () => {}, soloLectura: true, esAdmin: true }
  const claseEnlace = ({ isActive }: { isActive: boolean }) => `inline-flex items-center gap-2 rounded-md px-3 py-2 text-sm ${isActive ? 'bg-primary/10 font-medium text-primary' : 'hover:bg-accent'}`
  return <div className="space-y-5">
    <Link to="/panel/administracion/tiendas" className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-primary"><ArrowLeft className="size-4" />Volver a tiendas</Link>
    <section className="flex flex-wrap items-start justify-between gap-4 rounded-xl border bg-card p-5">
      <div className="min-w-0 space-y-1">
        <div className="flex flex-wrap items-center gap-2.5"><h2 className="text-2xl font-semibold tracking-tight">{datos.nombre}</h2><EstadoTiendaAdmin estado={datos.estado} /></div>
        <p className="text-sm">Dueño: <span className="font-medium">{datos.nombrePropietario || 'Sin nombre'}</span> <span className="text-muted-foreground">· {datos.correoPropietario}</span></p>
        <a href={`/tienda/${encodeURIComponent(datos.slug)}`} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-primary">/tienda/{datos.slug}<ExternalLink className="size-3.5" /></a>
      </div>
      <AccionesEstadoTienda tienda={datos} alCambiar={t => actualizarDatos(() => t)} />
    </section>
    <p className="flex items-start gap-2 rounded-xl border bg-muted/40 px-4 py-3 text-sm text-muted-foreground"><Info className="mt-0.5 size-4 shrink-0 text-primary" /><span>Vista de solo lectura. Como administrador puedes consultar el catálogo, el inventario, la apariencia y el estado de la cuenta de pago, pero no modificarlos. Los pedidos y pagos contienen datos de los compradores y solo los ve el vendedor.</span></p>
    <nav aria-label="Contenido de la tienda" className="flex flex-wrap gap-2 border-b pb-3">{secciones.map(([ruta, nombre, Icono]) => <NavLink key={ruta} to={`${base}/${ruta}`} className={claseEnlace}><Icono className="size-4" />{nombre}</NavLink>)}</nav>
    <Outlet context={contexto} />
  </div>
}
