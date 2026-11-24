import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, Outlet, useOutletContext } from 'react-router-dom'
import { LayoutDashboard, LayoutTemplate, Store, UsersRound, Percent } from 'lucide-react'
import { useSesion } from '@/autenticacion/sesion'
import { Button } from '@/components/ui/button'
import { api } from '@/comercio/datos'
import { FilaTiendaAdmin } from './tienda'
import type { TiendaAdministrada } from './tienda'
import { Aviso, Bloque, EstadoCarga, campo, useCarga, useOperacion } from '@/comercio/comunes'
import { textoCuentaUsuario, textoRol } from '@/comercio/estados'

type UsuarioAdministrado = { idUsuario: string; nombres: string; apellidoPaterno: string | null; apellidoMaterno: string | null; correoElectronico: string; estado: string; roles: string[] }
type Plantilla = { idPlantilla: number; codigo: string; nombre: string; previewUrl: string | null; activa: boolean }
type Pagina<T> = { contenido: T[]; pagina: number; totalPaginas: number; totalElementos: number }

export function Administracion() {
  const { usuario } = useSesion()
  if (!usuario?.roles.includes('ADMIN_GENERAL')) return <Navigate to="/panel" replace />
  return <div className="space-y-6"><div><p className="text-sm font-medium text-primary">ADMINISTRACIÓN DE NUBEA</p><h1 className="mt-1 text-3xl font-semibold">Panel general</h1><p className="mt-2 text-muted-foreground">Gestiona las cuentas, consulta las tiendas y administra las plantillas.</p></div><nav aria-label="Administración general" className="flex flex-wrap gap-2 border-b pb-4">{[["/panel/administracion", "Resumen", LayoutDashboard], ["/panel/administracion/usuarios", "Usuarios", UsersRound], ["/panel/administracion/tiendas", "Tiendas", Store], ["/panel/administracion/plantillas", "Plantillas", LayoutTemplate], ["/panel/administracion/impuestos", "Impuestos", Percent]].map(([ruta, nombre, Icono]) => { const IconoMenu = Icono as typeof Store; return <Link key={ruta as string} to={ruta as string} className="flex items-center gap-2 rounded-md px-3 py-2 text-sm hover:bg-accent"><IconoMenu className="size-4" aria-hidden="true" />{nombre as string}</Link> })}</nav><Outlet context={{ idAdministrador: usuario.idUsuario }} /></div>
}
export function ResumenAdministracion() {
  return <div className="grid gap-4 md:grid-cols-3">{[["usuarios", "Usuarios", "Consulta cuentas y administra sus roles."], ["tiendas", "Tiendas", "Revisa las tiendas y sus dueños, suspéndelas o reactívalas."], ["plantillas", "Plantillas", "Controla las plantillas disponibles para los vendedores."], ["impuestos", "Impuestos", "Consulta y programa vigencias de IGV."]].map(([ruta, titulo, detalle]) => <Link key={ruta} to={`/panel/administracion/${ruta}`} className="rounded-xl border bg-card p-5 hover:border-primary"><h2 className="font-semibold">{titulo}</h2><p className="mt-2 text-sm text-muted-foreground">{detalle}</p></Link>)}</div>
}
function NavegacionPagina<T>({ pagina, cambiar }: { pagina: Pagina<T>; cambiar: (pagina: number) => void }) { return <div className="flex flex-wrap items-center gap-3"><Button type="button" variant="outline" disabled={pagina.pagina === 0} onClick={() => cambiar(pagina.pagina - 1)}>Anterior</Button><span className="text-sm">Página {pagina.pagina + 1} de {Math.max(pagina.totalPaginas, 1)} · {pagina.totalElementos} resultados</span><Button type="button" variant="outline" disabled={pagina.pagina + 1 >= pagina.totalPaginas} onClick={() => cambiar(pagina.pagina + 1)}>Siguiente</Button></div> }
function useListado<T>(recurso: 'usuarios' | 'tiendas') {
  const [texto, setTexto] = useState(''); const [busqueda, setBusqueda] = useState(''); const [page, setPage] = useState(0)
  const query = new URLSearchParams({ texto: busqueda, page: String(page), size: '20' }).toString()
  const carga = useCarga(() => api.leer<Pagina<T>>(`/api/admin/${recurso}?${query}`), [recurso, query])
  function buscar(e: FormEvent) { e.preventDefault(); setPage(0); setBusqueda(texto.trim()) }
  return { ...carga, texto, setTexto, buscar, setPage }
}
export function UsuariosAdministracion() {
  const { idAdministrador } = useOutletContext<{ idAdministrador: string }>()
  const listado = useListado<UsuarioAdministrado>('usuarios')
  return <Bloque titulo="Usuarios"><form className="flex flex-wrap gap-2" onSubmit={listado.buscar}><label className="min-w-48 flex-1"><span className="sr-only">Buscar por nombre o correo</span><input className={campo} placeholder="Buscar por nombre o correo" value={listado.texto} onChange={e => listado.setTexto(e.target.value)} /></label><Button type="submit">Buscar</Button></form>{listado.error || !listado.datos ? <EstadoCarga error={listado.error} recargar={listado.recargar} /> : <>{listado.datos.contenido.length === 0 && <p>No se encontraron usuarios.</p>}{listado.datos.contenido.map(usuario => <UsuarioFila key={`${usuario.idUsuario}-${usuario.roles.join(',')}`} usuario={usuario} esActual={usuario.idUsuario === idAdministrador} recargar={listado.recargar} />)}<NavegacionPagina pagina={listado.datos} cambiar={listado.setPage} /></>}</Bloque>
}
function UsuarioFila({ usuario, esActual, recargar }: { usuario: UsuarioAdministrado; esActual: boolean; recargar: () => void }) {
  const op = useOperacion(); const esDueno = usuario.roles.includes('DUENO_TIENDA'); const esAdmin = usuario.roles.includes('ADMIN_GENERAL')
  function cambiarRol(rol: 'DUENO_TIENDA' | 'ADMIN_GENERAL') {
    const retirar = rol === 'DUENO_TIENDA' && esDueno
    const mensaje = retirar ? `¿Retirar el rol de vendedor a ${usuario.correoElectronico}? Sus tiendas abiertas o en preparación serán suspendidas.` : `¿Asignar el rol de ${textoRol(rol)} a ${usuario.correoElectronico}?`
    if (!window.confirm(mensaje)) return
    void op.ejecutar(async () => { const ruta = `/api/usuarios/${usuario.idUsuario}/roles/${rol}`; if (retirar) await api.eliminar(ruta); else await api.actualizar(ruta); recargar() })
  }
  return <div className="space-y-3 rounded-lg border p-4"><div className="flex flex-wrap justify-between gap-2"><div><strong>{[usuario.nombres, usuario.apellidoPaterno, usuario.apellidoMaterno].filter(Boolean).join(' ')}{esActual && ' (tu cuenta)'}</strong><p className="text-sm text-muted-foreground">{usuario.correoElectronico} · Cuenta {textoCuentaUsuario(usuario.estado).toLowerCase()}</p></div><p className="text-sm">{usuario.roles.map(textoRol).join(', ') || 'Sin roles'}</p></div><Aviso error={op.error} />{!esActual && <div className="flex flex-wrap gap-2"><Button type="button" variant="outline" disabled={op.ocupado} onClick={() => cambiarRol('DUENO_TIENDA')}>{esDueno ? 'Retirar rol de vendedor' : 'Asignar rol de vendedor'}</Button>{!esAdmin && <Button type="button" variant="outline" disabled={op.ocupado} onClick={() => cambiarRol('ADMIN_GENERAL')}>Asignar administrador de Nubea</Button>}</div>}</div>
}
export function TiendasAdministracion() {
  const listado = useListado<TiendaAdministrada>('tiendas')
  const reemplazar = (t: TiendaAdministrada) => listado.actualizarDatos(p => ({ ...p, contenido: p.contenido.map(x => x.idTienda === t.idTienda ? t : x) }))
  return <Bloque titulo="Tiendas"><form className="flex flex-wrap gap-2" onSubmit={listado.buscar}><label className="min-w-48 flex-1"><span className="sr-only">Buscar por nombre, dirección o correo del dueño</span><input className={campo} placeholder="Buscar por nombre, dirección o correo del dueño" value={listado.texto} onChange={e => listado.setTexto(e.target.value)} /></label><Button type="submit">Buscar</Button></form><p className="text-sm text-muted-foreground">Suspender oculta la tienda a sus clientes y deja al dueño en solo lectura. Reactivar la devuelve a "En preparación": el dueño decide cuándo abrirla de nuevo.</p>{listado.error || !listado.datos ? <EstadoCarga error={listado.error} recargar={listado.recargar} /> : <>{listado.datos.contenido.length === 0 && <p>No se encontraron tiendas.</p>}{listado.datos.contenido.map(t => <FilaTiendaAdmin key={t.idTienda} tienda={t} alCambiar={reemplazar} />)}<NavegacionPagina pagina={listado.datos} cambiar={listado.setPage} /></>}</Bloque>
}
export function PlantillasAdministracion() {
  const { datos, error, recargar } = useCarga(() => api.leer<Plantilla[]>('/api/plantillas?incluirInactivas=true'), [])
  const [editando, setEditando] = useState<Plantilla | null>(null)
  return <div className="space-y-5"><FormularioPlantilla key={editando?.idPlantilla ?? 'nueva'} plantilla={editando} cancelar={() => setEditando(null)} alGuardar={() => { setEditando(null); recargar() }} /><Bloque titulo="Plantillas disponibles"><p className="text-sm text-muted-foreground">El código identifica una plantilla implementada en la futura vitrina; crear metadatos no crea su diseño.</p>{error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : datos.length === 0 ? <p>No hay plantillas registradas.</p> : datos.map(p => <PlantillaFila key={`${p.idPlantilla}-${p.activa}`} plantilla={p} editar={() => setEditando(p)} recargar={recargar} />)}</Bloque></div>
}
function FormularioPlantilla({ plantilla, cancelar, alGuardar }: { plantilla: Plantilla | null; cancelar: () => void; alGuardar: () => void }) {
  const [codigo, setCodigo] = useState(plantilla?.codigo ?? ''); const [nombre, setNombre] = useState(plantilla?.nombre ?? ''); const [previewUrl, setPreviewUrl] = useState(plantilla?.previewUrl ?? ''); const [activa, setActiva] = useState(plantilla?.activa ?? true); const op = useOperacion()
  return <Bloque titulo={plantilla ? 'Editar plantilla' : 'Crear plantilla'}><form className="grid gap-3 sm:grid-cols-2" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { if (plantilla) await api.modificar(`/api/plantillas/${plantilla.idPlantilla}`, { nombre, previewUrl, activa }); else { await api.crear('/api/plantillas', { codigo, nombre, previewUrl, activa }); setCodigo(''); setNombre(''); setPreviewUrl(''); setActiva(true) }; alGuardar() }) }}><label>Código<input className={campo} required maxLength={60} value={codigo} disabled={!!plantilla} onChange={e => setCodigo(e.target.value)} /></label><label>Nombre<input className={campo} required maxLength={100} value={nombre} onChange={e => setNombre(e.target.value)} /></label><label className="sm:col-span-2">URL de vista previa opcional<input className={campo} type="url" maxLength={2048} value={previewUrl} onChange={e => setPreviewUrl(e.target.value)} /></label><label className="flex items-center gap-2"><input type="checkbox" checked={activa} onChange={e => setActiva(e.target.checked)} />Activa</label><div className="sm:col-span-2 space-y-3"><Aviso error={op.error} /><div className="flex gap-2"><Button type="submit" disabled={op.ocupado}>{op.ocupado ? 'Guardando...' : plantilla ? 'Guardar cambios' : 'Crear plantilla'}</Button>{plantilla && <Button type="button" variant="outline" onClick={cancelar}>Cancelar</Button>}</div></div></form></Bloque>
}
function PlantillaFila({ plantilla, editar, recargar }: { plantilla: Plantilla; editar: () => void; recargar: () => void }) {
 const op = useOperacion(); return <div className="space-y-2 rounded-lg border p-4"><div className="flex flex-wrap items-center justify-between gap-3"><div><strong>{plantilla.nombre}</strong><p className="text-sm text-muted-foreground">{plantilla.codigo} · {plantilla.activa ? 'Activa' : 'Inactiva'}</p>{plantilla.previewUrl && <a className="break-all text-sm text-primary underline" href={plantilla.previewUrl} target="_blank" rel="noreferrer">Ver vista previa</a>}</div><div className="flex gap-2"><Button type="button" variant="outline" onClick={editar}>Editar</Button><Button type="button" variant="outline" disabled={op.ocupado} onClick={() => { if (window.confirm(plantilla.idPlantilla === 1 && plantilla.activa ? `¿Desactivar ${plantilla.nombre}? Es la plantilla predeterminada y no se podrán crear tiendas mientras esté inactiva.` : `¿${plantilla.activa ? 'Desactivar' : 'Activar'} la plantilla ${plantilla.nombre}?`)) void op.ejecutar(async () => { await api.modificar(`/api/plantillas/${plantilla.idPlantilla}/estado`, { activa: !plantilla.activa }); recargar() }) }}>{plantilla.activa ? 'Desactivar' : 'Activar'}</Button></div></div><Aviso error={op.error} /></div>
}


type Impuesto = { id: number; tasa: number; inicio: string; fin: string | null }
export function ImpuestosAdministracion() {
  const { datos, error, recargar } = useCarga(() => api.leer<Impuesto[]>('/api/admin/impuestos'), [])
  const [tasa, setTasa] = useState('')
  const [inicio, setInicio] = useState('')
  const op = useOperacion()
  return <Bloque titulo="Vigencias de IGV"><p className="text-sm text-muted-foreground">Los precios incluyen IGV. Las nuevas vigencias comienzan en una fecha futura.</p><form className="flex flex-wrap items-end gap-3" onSubmit={e => { e.preventDefault(); void op.ejecutar(async () => { await api.crear('/api/admin/impuestos', { tasa: Number(tasa), inicio }); setTasa(''); setInicio(''); recargar() }) }}><label>Tasa (%)<input className={campo} type="number" min="0" max="100" step="0.01" required value={tasa} onChange={e => setTasa(e.target.value)} /></label><label>Inicio<input className={campo} type="date" required value={inicio} onChange={e => setInicio(e.target.value)} /></label><Button disabled={op.ocupado}>Programar vigencia</Button></form><Aviso error={op.error} />{error || !datos ? <EstadoCarga error={error} recargar={recargar} /> : datos.map(i => <p key={i.id}>{i.tasa}% · Desde {i.inicio} {i.fin ? `hasta ${i.fin} (exclusiva)` : 'sin fecha de fin'}</p>)}</Bloque>
}
