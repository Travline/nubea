import { useEffect, useState, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, ShoppingBag, Store, UserRound } from 'lucide-react'
import { actualizarSesion, enlaceSesion, solicitar } from './api'
import { PLANTILLA_POR_DEFECTO, plantillaDisponible } from './plantillas'

const api = import.meta.env.VITE_API_URL?.trim() || (import.meta.env.DEV ? 'http://localhost:8080' : '')
type Usuario = { nombres: string }
type Carrito = { items: { cantidad: number }[] }
export function MarcoTienda({ slug, nombre, plantilla, children }: { slug: string; nombre?: string; plantilla?: string; children: ReactNode }) {
  const [nombreCargado, setNombreCargado] = useState('')
  const [plantillaCargada, setPlantillaCargada] = useState('')
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [cantidad, setCantidad] = useState(0)
  const [menu, setMenu] = useState(false)
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const ubicacion = useLocation()
  const navegar = useNavigate()
  useEffect(() => {
    if (nombre) return
    const controller = new AbortController()
    fetch(`${api}/api/publico/tiendas/${encodeURIComponent(slug)}`, { signal: controller.signal })
      .then(res => res.ok ? res.json() as Promise<{ nombre: string; plantilla?: string }> : null)
      .then(datos => { setNombreCargado(datos?.nombre || ''); setPlantillaCargada(datos?.plantilla || '') })
      .catch(() => {})
    return () => controller.abort()
  }, [slug, nombre])
  useEffect(() => {
    const actualizar = () => setVersion(v => v + 1)
    window.addEventListener('tienda-sesion', actualizar); window.addEventListener('tienda-carrito', actualizar)
    return () => { window.removeEventListener('tienda-sesion', actualizar); window.removeEventListener('tienda-carrito', actualizar) }
  }, [])
  useEffect(() => {
    let activo = true
    solicitar<Usuario>('/api/auth/me').then(async u => {
      if (!activo) return
      setUsuario(u)
      try { const carrito = await solicitar<Carrito>(`/api/compra/tiendas/${encodeURIComponent(slug)}/carrito`); if (activo) setCantidad(carrito.items.reduce((total, item) => total + item.cantidad, 0)) }
      catch { if (activo) setCantidad(0) }
    }).catch(() => { if (activo) { setUsuario(null); setCantidad(0) } })
    return () => { activo = false }
  }, [slug, version, ubicacion.pathname])
  useEffect(() => {
    if (!usuario) return
    const vencer = () => {
      setUsuario(null); setCantidad(0)
      const retorno = window.location.pathname + window.location.search
      navegar(`/${slug}/iniciar-sesion?retorno=${encodeURIComponent(retorno)}&vencida=1`, { replace: true })
    }
    window.addEventListener('tienda-sesion-vencida', vencer)
    return () => window.removeEventListener('tienda-sesion-vencida', vencer)
  }, [usuario, navegar, slug])
  async function salir() {
    try { setError(''); await solicitar('/logout', 'POST'); setMenu(false); actualizarSesion(); navegar(`/${slug}`, { replace: true }) }
    catch (e) { setError(e instanceof Error ? e.message : 'No se pudo cerrar sesión.') }
  }
  const nombreVisible = nombre || nombreCargado || 'Tienda'
  const codigoPlantilla = plantilla || plantillaCargada
  const clasePlantilla = plantillaDisponible(codigoPlantilla) ? codigoPlantilla : PLANTILLA_POR_DEFECTO
  return <div className={`sitio plantilla-${clasePlantilla}`}>
    <div className="barra-anuncio"><span>{nombreVisible}</span><span aria-hidden="true">·</span><span>Tienda en línea</span></div>
    <header className="cabecera-sitio"><div className="contenedor cabecera">
      <nav className="navegacion-secundaria" aria-label="Secciones de la tienda"><Link to={`/${slug}`}>Inicio</Link><a href={`${import.meta.env.BASE_URL}${slug}#productos`}>Productos</a></nav>
      <Link className="marca" to={`/${slug}`}><span className="marca-icono"><Store size={18} /></span><span>{nombreVisible}</span></Link>
      <nav className="navegacion" aria-label="Navegación principal"><div className="cuenta-menu"><button className="navegacion-icono" aria-label="Cuenta" aria-expanded={menu} onClick={() => setMenu(!menu)}><UserRound size={21} /></button>{menu && <div className="menu-desplegable">{usuario ? <><p>Hola, {usuario.nombres}</p><Link to={`/${slug}/pedidos`} onClick={() => setMenu(false)}>Mis pedidos</Link><Link to={`/${slug}/direcciones`} onClick={() => setMenu(false)}>Mis direcciones</Link><button onClick={() => void salir()}>Cerrar sesión</button>{error && <p role="alert">{error}</p>}</> : <><Link to={enlaceSesion(slug)} onClick={() => setMenu(false)}>Iniciar sesión</Link><Link to={`/${slug}/registro?retorno=${encodeURIComponent(window.location.pathname + window.location.search)}`} onClick={() => setMenu(false)}>Crear cuenta</Link></>}</div>}</div><Link className="navegacion-icono" to={`/${slug}/carrito`} aria-label={cantidad ? `Carrito, ${cantidad} unidades` : 'Carrito'}><ShoppingBag size={21} />{cantidad > 0 && <span className="cantidad-carrito">{cantidad}</span>}</Link></nav>
    </div></header>
    <main className="contenedor contenido">{children}</main>
    <footer className="pie"><div className="contenedor pie-contenido">
      <div className="pie-marca"><span className="pie-nombre">{nombreVisible}</span><p>Tienda en línea · Vitrina en Nubea</p></div>
      <nav className="pie-enlaces" aria-label="Enlaces de la tienda"><Link to={`/${slug}`}>Productos</Link><Link to={`/${slug}/carrito`}>Carrito</Link><Link to={`/${slug}/pedidos`}>Mis pedidos</Link><Link to={`/${slug}/direcciones`}>Mis direcciones</Link></nav>
    </div><div className="contenedor pie-legal"><span>© {new Date().getFullYear()} {nombreVisible}</span></div></footer>
  </div>
}
export function Volver({ to, children }: { to: string; children: ReactNode }) { return <Link className="volver" to={to}><ArrowLeft size={16} />{children}</Link> }
