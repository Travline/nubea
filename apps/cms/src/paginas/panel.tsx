import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Outlet, useNavigate, useOutletContext, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { AlternarTema } from '@/components/alternar-tema'
import { apiAutenticacion } from '@/autenticacion/api'
import { useSesion } from '@/autenticacion/sesion'
import { api, rutaTienda } from '@/comercio/datos'
import type { Tienda } from '@/comercio/datos'
import { Aviso, Bloque, EstadoCarga, campo, useCarga, useOperacion } from '@/comercio/comunes'

export function Panel() {
  const { usuario, limpiar } = useSesion()
  const navegar = useNavigate()
  const { error, ocupado, ejecutar } = useOperacion()
  async function salir() { await ejecutar(async () => { await apiAutenticacion.cerrarSesion(); limpiar(); navegar('/iniciar-sesion', { replace: true }) }) }
  return <div className="min-h-screen bg-muted/30"><header className="border-b bg-background"><div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-5 py-4"><Link to="/panel" className="flex items-center gap-2 text-xl font-bold"><img className="w-10" src="/assets/logo.webp" alt="" />Nubea</Link><div className="flex items-center gap-2"><span className="text-sm">{usuario?.nombres}</span><AlternarTema /><Button variant="outline" disabled={ocupado} onClick={() => void salir()}>Cerrar sesión</Button></div></div></header><main className="mx-auto max-w-6xl space-y-6 px-5 py-8"><Aviso error={error} /><Outlet /></main></div>
}

export function MisTiendas() {
  const { usuario, actualizar } = useSesion()
  const navegar = useNavigate()
  const [nombre, establecerNombre] = useState('')
  const { datos, error, recargar } = useCarga(() => api.leer<Tienda[]>(`/api/tiendas/propietario/${usuario!.idUsuario}`), [usuario!.idUsuario])
  const operacion = useOperacion()
  async function crear(e: FormEvent) { e.preventDefault(); await operacion.ejecutar(async () => { const tienda = await api.crear<Tienda>('/api/tiendas', { nombre }); try { await actualizar() } catch { navegar(`/panel/tiendas/${tienda.idTienda}`); return }; navegar(`/panel/tiendas/${tienda.idTienda}`) }) }
  return <><div><h1 className="text-3xl font-semibold">Tus tiendas</h1><p className="mt-2 text-muted-foreground">Selecciona una tienda o crea una para empezar.</p></div><Aviso error={operacion.error} /><Bloque titulo="Crear tienda"><form onSubmit={crear} className="flex flex-wrap gap-3"><label className="min-w-48 flex-1"><span className="sr-only">Nombre de la tienda</span><input className={campo} placeholder="Nombre de la tienda" value={nombre} onChange={e => establecerNombre(e.target.value)} maxLength={150} required /></label><Button type="submit" disabled={operacion.ocupado}>Crear tienda</Button></form></Bloque>{error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : datos.length === 0 ? <p>Aún no tienes tiendas.</p> : <div className="grid gap-4 sm:grid-cols-2">{datos.map(t => <Link key={t.idTienda} className="rounded-xl border bg-card p-5 hover:border-primary" to={`/panel/tiendas/${t.idTienda}`}><h2 className="font-semibold">{t.nombre}</h2><p className="text-sm text-muted-foreground">{t.slug} · {t.estado}</p></Link>)}</div>}</>
}

export function EspacioTienda() {
  const { idTienda = '' } = useParams()
  const { usuario } = useSesion()
  const { datos, error, recargar, actualizarDatos } = useCarga(() => api.leer<Tienda[]>(`/api/tiendas/propietario/${usuario!.idUsuario}`), [usuario!.idUsuario])
  if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
  const tienda = datos.find(t => t.idTienda === idTienda)
  if (!tienda) return <p role="alert">No tienes acceso a esta tienda. <Link className="underline" to="/panel">Volver a tus tiendas</Link></p>
  const base = `/panel/tiendas/${idTienda}`
  return <><div><Link className="text-sm text-primary underline" to="/panel">← Tus tiendas</Link><h1 className="mt-2 text-3xl font-semibold">{tienda.nombre}</h1><p className="text-sm text-muted-foreground">{tienda.slug} · {tienda.estado}</p></div><nav aria-label="Gestión de tienda" className="flex flex-wrap gap-2 border-b pb-4">{[['', 'Resumen'], ['/catalogo', 'Catálogo'], ['/inventario', 'Inventario'], ['/apariencia', 'Apariencia'], ['/pagos', 'Pagos']].map(([ruta, nombre]) => <Link className="rounded-md px-3 py-2 text-sm hover:bg-accent" key={ruta} to={base + ruta}>{nombre}</Link>)}</nav><Outlet context={{ tienda, base, apiBase: rutaTienda(idTienda), actualizarTienda: (actualizada: Tienda) => actualizarDatos(tiendas => tiendas.map(t => t.idTienda === actualizada.idTienda ? actualizada : t)) }} /></>
}
export function ResumenTienda() {
  const { tienda, apiBase, actualizarTienda } = useOutletContext<ContextoTienda>()
  const operacion = useOperacion()
  return <Bloque titulo="Tu espacio de trabajo"><p>Administra tus productos, consulta el inventario y configura la apariencia y los pagos desde el menú.</p>
    {tienda.estado === 'BORRADOR' && <><p>Activa tu tienda cuando quieras para poder publicar productos que cumplan los requisitos del catálogo.</p><Aviso error={operacion.error} /><Button disabled={operacion.ocupado} onClick={() => void operacion.ejecutar(async () => { const actualizada = await api.crear<Tienda>(`${apiBase}/activacion`); actualizarTienda(actualizada) })}>{operacion.ocupado ? 'Activando...' : 'Activar tienda'}</Button></>}
    <p className="text-sm text-muted-foreground">La tienda pública estará disponible en una etapa posterior.</p></Bloque>
}
export interface ContextoTienda { tienda: Tienda; base: string; apiBase: string; actualizarTienda: (tienda: Tienda) => void }
