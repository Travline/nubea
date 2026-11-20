import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { enlaceSesion, ErrorSolicitud, solicitar } from './api'
import { Pencil, Trash2 } from 'lucide-react'
import { MarcoTienda, Volver } from './diseno'

type Ubigeo = { codigo: string; departamento: string; provincia: string; distrito: string }
type Direccion = { id: string; codigoUbigeo: string; departamento: string; provincia: string; distrito: string; alias: string | null; direccion: string; referencia: string | null; telefono: string | null; principal: boolean }
export function Direcciones() {
  const { slug = '' } = useParams()
  const [direcciones, setDirecciones] = useState<Direccion[]>([])
  const [texto, setTexto] = useState('')
  const [ubigeos, setUbigeos] = useState<Ubigeo[]>([])
  const [codigo, setCodigo] = useState('')
  const [direccion, setDireccion] = useState('')
  const [alias, setAlias] = useState('')
  const [referencia, setReferencia] = useState('')
  const [telefono, setTelefono] = useState('')
  const [principal, setPrincipal] = useState(false)
  const [editando, setEditando] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [sesion, setSesion] = useState(true)
  const [ocupado, setOcupado] = useState(false)
  async function cargar() { try { setDirecciones(await solicitar<Direccion[]>('/api/cliente/direcciones')) } catch (e) { if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false); else setError(e instanceof Error ? e.message : 'Error de conexión') } }
  useEffect(() => { void Promise.resolve().then(cargar) }, [])
  async function buscar() { try { setError(''); setUbigeos(await solicitar<Ubigeo[]>(`/api/cliente/ubigeo?texto=${encodeURIComponent(texto)}`)) } catch (e) { setError(e instanceof Error ? e.message : 'No se pudo buscar UBIGEO') } }
  function limpiar() { setCodigo(''); setDireccion(''); setAlias(''); setReferencia(''); setTelefono(''); setPrincipal(false); setEditando(null) }
  async function guardar() {
    setOcupado(true); setError('')
    try { await solicitar(`/api/cliente/direcciones${editando ? `/${editando}` : ''}`, editando ? 'PUT' : 'POST', { codigoUbigeo: codigo, direccion, alias, referencia, telefono, principal }); limpiar(); await cargar() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo guardar la dirección.') }
    finally { setOcupado(false) }
  }
  async function eliminar(id: string) {
    if (!window.confirm('¿Eliminar esta dirección?')) return
    setOcupado(true); setError('')
    try { await solicitar(`/api/cliente/direcciones/${id}`, 'DELETE'); await cargar() }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo eliminar la dirección.') }
    finally { setOcupado(false) }
  }
  return <MarcoTienda slug={slug}><div className="pagina-interior"><Volver to={`/${slug}/carrito`}>Volver al carrito</Volver><h1>Mis direcciones</h1>{!sesion ? <p>Para administrar direcciones, <Link to={enlaceSesion(slug)}>inicia sesión</Link>.</p> : <>{error && <p role="alert">{error}</p>}{direcciones.map(d => <div className="linea-carrito" key={d.id}><div><strong>{d.alias || 'Dirección'} {d.principal && '· Principal'}</strong><p>{d.direccion}, {d.distrito}, {d.provincia}, {d.departamento}</p>{d.referencia && <p>Referencia: {d.referencia}</p>}{d.telefono && <p>Teléfono: {d.telefono}</p>}</div><div><button disabled={ocupado} onClick={() => { setEditando(d.id); setCodigo(d.codigoUbigeo); setDireccion(d.direccion); setAlias(d.alias || ''); setReferencia(d.referencia || ''); setTelefono(d.telefono || ''); setPrincipal(d.principal) }}><Pencil size={16} /> Editar</button><button disabled={ocupado} onClick={() => void eliminar(d.id)}><Trash2 size={16} /> Eliminar</button></div></div>)}<h2>{editando ? 'Editar dirección' : 'Agregar dirección'}</h2><div className="formulario-direccion"><label>Buscar distrito, provincia o departamento<input value={texto} onChange={e => setTexto(e.target.value)} /></label><button onClick={() => void buscar()}>Buscar UBIGEO</button><form onSubmit={e => { e.preventDefault(); void guardar() }}><label>Distrito<select required value={codigo} onChange={e => setCodigo(e.target.value)}><option value="">Selecciona un distrito</option>{codigo && !ubigeos.some(u => u.codigo === codigo) && <option value={codigo}>{codigo}</option>}{ubigeos.map(u => <option key={u.codigo} value={u.codigo}>{u.distrito}, {u.provincia}, {u.departamento}</option>)}</select></label><label>Dirección<input required maxLength={250} value={direccion} onChange={e => setDireccion(e.target.value)} /></label><label>Alias<input maxLength={60} value={alias} onChange={e => setAlias(e.target.value)} /></label><label>Referencia<input maxLength={250} value={referencia} onChange={e => setReferencia(e.target.value)} /></label><label>Teléfono<input maxLength={20} value={telefono} onChange={e => setTelefono(e.target.value)} /></label><label><input type="checkbox" checked={principal} onChange={e => setPrincipal(e.target.checked)} /> Dirección principal</label><button disabled={ocupado}>{editando ? 'Guardar cambios' : 'Guardar dirección'}</button>{editando && <button type="button" onClick={limpiar}>Cancelar</button>}</form></div></>}</div></MarcoTienda>
}
