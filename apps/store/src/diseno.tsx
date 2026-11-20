import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Package, ShoppingBag, Store } from 'lucide-react'

const api = import.meta.env.VITE_API_URL?.trim() || (import.meta.env.DEV ? 'http://localhost:8080' : '')

export function MarcoTienda({ slug, nombre, children }: { slug: string; nombre?: string; children: ReactNode }) {
  const [nombreCargado, setNombreCargado] = useState('')
  useEffect(() => {
    if (nombre) return
    const controller = new AbortController()
    fetch(`${api}/api/publico/tiendas/${encodeURIComponent(slug)}`, { signal: controller.signal })
      .then(res => res.ok ? res.json() as Promise<{ nombre: string }> : null)
      .then(datos => setNombreCargado(datos?.nombre || ''))
      .catch(() => {})
    return () => controller.abort()
  }, [slug, nombre])
  const nombreVisible = nombre || nombreCargado || 'Tienda'
  return <div className="sitio"><header className="cabecera-sitio"><div className="contenedor cabecera"><Link className="marca" to={`/${slug}`}><span className="marca-icono"><Store size={19} /></span><span>{nombreVisible}</span></Link><nav className="navegacion" aria-label="Navegación principal"><Link to={`/${slug}/pedidos`}><Package size={18} /><span>Mis pedidos</span></Link><Link to={`/${slug}/carrito`}><ShoppingBag size={18} /><span>Carrito</span></Link></nav></div></header><main className="contenedor contenido">{children}</main><footer className="pie"><div className="contenedor pie-contenido"><span>{nombreVisible}</span><span>Vitrina en Nubea</span></div></footer></div>
}
export function Volver({ to, children }: { to: string; children: ReactNode }) { return <Link className="volver" to={to}><ArrowLeft size={16} />{children}</Link> }
