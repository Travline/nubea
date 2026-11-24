import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { enlaceSesion, ErrorSolicitud, mensajeDeError, solicitar } from './api'
import { Pencil, Trash2 } from 'lucide-react'
import { MarcoTienda, Volver } from './diseno'
import { MapaLugar } from './mapa'

export type Direccion = { id: string; codigoUbigeo: string | null; departamento: string | null; provincia: string | null; distrito: string | null; alias: string | null; direccion: string; referencia: string | null; telefono: string | null; principal: boolean; placeId: string | null }
export function textoDireccion(d: Direccion) { return [d.direccion, d.distrito, d.provincia, d.departamento].filter(Boolean).join(', ') }
export function FormularioDireccion({ inicial, alGuardar, alCancelar }: { inicial?: Direccion; alGuardar: (d: Direccion) => void; alCancelar?: () => void }) {
  const [direccion, setDireccion] = useState(inicial?.direccion || '')
  const [departamento, setDepartamento] = useState(inicial?.departamento || '')
  const [provincia, setProvincia] = useState(inicial?.provincia || '')
  const [distrito, setDistrito] = useState(inicial?.distrito || '')
  const [alias, setAlias] = useState(inicial?.alias || '')
  const [referencia, setReferencia] = useState(inicial?.referencia || '')
  const [telefono, setTelefono] = useState(inicial?.telefono || '')
  const [principal, setPrincipal] = useState(inicial?.principal || false)
  const [placeId, setPlaceId] = useState<string | null>(inicial?.placeId || null)
  const cambiar = useCallback((id: string | null) => setPlaceId(id), [])
  const [error, setError] = useState('')
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [ocupado, setOcupado] = useState(false)
  async function guardar() {
    setOcupado(true); setError(''); setErrores({})
    try { alGuardar(await solicitar<Direccion>(`/api/cliente/direcciones${inicial ? `/${inicial.id}` : ''}`, inicial ? 'PUT' : 'POST', { codigoUbigeo: null, departamento, provincia, distrito, direccion, alias, referencia, telefono, principal, placeId })) }
    catch (e) {
      // Errores por campo junto a cada campo; el resto, arriba del formulario.
      const campos = e instanceof ErrorSolicitud ? e.campos : {}
      if (Object.keys(campos).length) setErrores(campos)
      else setError(mensajeDeError(e, 'No se pudo guardar la dirección.'))
    }
    finally { setOcupado(false) }
  }
  const errorDe = (campo: string) => errores[campo] && <span role="alert" className="error-campo">{errores[campo]}</span>
  return <form className="formulario-direccion" onSubmit={e => { e.preventDefault(); void guardar() }}>
    {error && <p role="alert">{error}</p>}
    <label>Dirección<input required maxLength={250} value={direccion} onChange={e => setDireccion(e.target.value)} aria-invalid={!!errores.direccion} />{errorDe('direccion')}</label>
    <label>Departamento<input maxLength={100} value={departamento} onChange={e => setDepartamento(e.target.value)} aria-invalid={!!errores.departamento} />{errorDe('departamento')}</label>
    <label>Provincia<input maxLength={100} value={provincia} onChange={e => setProvincia(e.target.value)} aria-invalid={!!errores.provincia} />{errorDe('provincia')}</label>
    <label>Distrito<input maxLength={100} value={distrito} onChange={e => setDistrito(e.target.value)} aria-invalid={!!errores.distrito} />{errorDe('distrito')}</label>
    <label>Alias<input maxLength={60} value={alias} onChange={e => setAlias(e.target.value)} aria-invalid={!!errores.alias} />{errorDe('alias')}</label>
    <label>Referencia<input maxLength={250} value={referencia} onChange={e => setReferencia(e.target.value)} aria-invalid={!!errores.referencia} />{errorDe('referencia')}</label>
    <label>Teléfono<input maxLength={20} value={telefono} onChange={e => setTelefono(e.target.value)} aria-invalid={!!errores.telefono} />{errorDe('telefono')}</label>
    <label><input type="checkbox" checked={principal} onChange={e => setPrincipal(e.target.checked)} /> Dirección principal</label>
    <p>Ubicación en Google (opcional)</p><MapaLugar placeId={placeId} cambiar={cambiar} />{errorDe('placeId')}
    <button disabled={ocupado}>{inicial ? 'Guardar cambios' : 'Guardar dirección'}</button>{alCancelar && <button type="button" onClick={alCancelar}>Cancelar</button>}
  </form>
}
export function Direcciones() {
  const { slug = '' } = useParams()
  const [direcciones, setDirecciones] = useState<Direccion[]>([])
  const [editando, setEditando] = useState<Direccion | null>(null)
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const [sesion, setSesion] = useState(true)
  async function cargar() { try { setDirecciones(await solicitar<Direccion[]>('/api/cliente/direcciones')) } catch (e) { if (e instanceof ErrorSolicitud && e.estado === 401) setSesion(false); else setError(mensajeDeError(e, 'Error de conexión')) } }
  useEffect(() => { void Promise.resolve().then(cargar) }, [])
  async function eliminar(id: string) { if (!window.confirm('¿Eliminar esta dirección?')) return; try { await solicitar(`/api/cliente/direcciones/${id}`, 'DELETE'); await cargar() } catch (e) { setError(mensajeDeError(e, 'No se pudo eliminar la dirección.')) } }
  return <MarcoTienda slug={slug}><div className="pagina-interior"><Volver to={`/${slug}/carrito`}>Volver al carrito</Volver><h1>Mis direcciones</h1>{!sesion ? <p>Para administrar direcciones, <Link to={enlaceSesion(slug)}>inicia sesión</Link>.</p> : <>{error && <p role="alert">{error}</p>}{direcciones.map(d => <div className="linea-carrito" key={d.id}><div><strong>{d.alias || 'Dirección'} {d.principal && '· Principal'}</strong><p>{textoDireccion(d)}</p>{d.referencia && <p>Referencia: {d.referencia}</p>}{d.telefono && <p>Teléfono: {d.telefono}</p>}</div><div><button onClick={() => { setEditando(d); setVersion(v => v + 1) }}><Pencil size={16} /> Editar</button><button onClick={() => void eliminar(d.id)}><Trash2 size={16} /> Eliminar</button></div></div>)}<h2>{editando ? 'Editar dirección' : 'Agregar dirección'}</h2><FormularioDireccion key={version} inicial={editando || undefined} alGuardar={() => { setEditando(null); setVersion(v => v + 1); void cargar() }} alCancelar={editando ? () => { setEditando(null); setVersion(v => v + 1) } : undefined} /></>}</div></MarcoTienda>
}
