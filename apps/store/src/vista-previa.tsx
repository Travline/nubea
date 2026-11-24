import { useState } from 'react'
import type { MouseEvent, ReactNode } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { ArrowRight, CakeSlice, ChevronLeft, ChevronRight, Coffee, Croissant, Droplet, Eye, Flame, Flower2, Footprints, Gem, Headphones, Keyboard, Leaf, Mouse, Plug, Sandwich, Shirt, ShoppingBag, Sparkles, Speaker, Sprout, Watch } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { MarcoTienda } from './diseno'
import { precio } from './api'
import { PLANTILLA_POR_DEFECTO, plantillaDisponible } from './plantillas'

/*
 * Vista previa de una plantilla con datos de ejemplo (no consulta la API).
 * Ruta: /tienda/vista-previa/:codigo  · La usa el panel en "Plantillas → Vista previa".
 * Reproduce el mismo marcado que la vitrina real para que los estilos de cada plantilla se vean igual.
 */

type ProductoDemo = { antes?: number; img: string; nombre: string; categoria: string; precios: number[]; agotado?: boolean; emoji: string; foto: string; icono?: LucideIcon }
type Demo = { nombre: string; titulo: string; descripcion: string; colores: [string, string]; tinta: string; portada: [string, string]; fotos: [string, string]; banners: { foto: string; emoji: string }[]; categorias: string[]; productos: ProductoDemo[] }

const demos: Record<string, Demo> = {
  minimal: {
    nombre: 'Casa Lino', titulo: 'Lino para el verano', descripcion: 'Prendas frescas de corte simple, hechas para durar.', colores: ['#efe8dc', '#d9cdb8'], fotos: ['1441984904996-e0b6ba687e04', '1558769132-cb1aea458c5e'], tinta: '#8a7a63', portada: ['#8b8172', '#2f2b25'],
    banners: [{ foto: 'linen,fashion', emoji: '👗' }, { foto: 'clothing,store', emoji: '👕' }],
    categorias: ['Ropa', 'Accesorios'],
    productos: [
      { img: '1740711152088-88a009e877bb', nombre: 'Camisa de lino', categoria: 'Ropa', precios: [89, 99], emoji: '👕', icono: Shirt, foto: 'linen,shirt' },
      { img: '1747396206869-75ea57b325ce', nombre: 'Vestido midi', antes: 179, categoria: 'Ropa', precios: [139], emoji: '👗', icono: Shirt, foto: 'dress' },
      { img: '1780396378407-07411c56389c', nombre: 'Pantalón wide leg', categoria: 'Ropa', precios: [119], emoji: '👖', icono: Shirt, foto: 'trousers' },
      { img: '1524679813234-66a389fe1a42', nombre: 'Bolso de yute', categoria: 'Accesorios', precios: [75], emoji: '👜', icono: ShoppingBag, foto: 'handbag' },
      { img: '1583238619747-27950824e04d', nombre: 'Sombrero de paja', categoria: 'Accesorios', precios: [59], agotado: true, emoji: '👒', icono: Sparkles, foto: 'straw,hat' },
      { img: '1585120824848-8a5cd41493d2', nombre: 'Sandalias de cuero', categoria: 'Accesorios', precios: [129], emoji: '👡', icono: Footprints, foto: 'sandals' },
    ],
  },
  boutique: {
    nombre: 'Maison Aurea', titulo: 'Colección Noche Dorada', descripcion: 'Piezas únicas para ocasiones especiales.', colores: ['#2a241d', '#0e0d0c'], fotos: ['1764512680324-048f158cab2b', '1775021723698-b9afeaa084d1'], tinta: '#c9a96e', portada: ['#3a332b', '#14110e'],
    banners: [{ foto: 'jewelry,gold', emoji: '💍' }, { foto: 'fashion,boutique', emoji: '👠' }],
    categorias: ['Joyería', 'Moda'],
    productos: [
      { img: '1783541507490-edb026e2a961', nombre: 'Collar Aurora', categoria: 'Joyería', precios: [249], emoji: '📿', icono: Gem, foto: 'necklace' },
      { img: '1790467351603-34e6d95739b5', nombre: 'Anillo de perla', antes: 249, categoria: 'Joyería', precios: [189, 219], emoji: '💍', icono: Gem, foto: 'ring,jewelry' },
      { img: '1722410180687-b05b50922362', nombre: 'Set collar y aretes', categoria: 'Joyería', precios: [129], emoji: '✨', icono: Sparkles, foto: 'earrings' },
      { img: '1592343516109-362f7bd871aa', nombre: 'Blazer negro', categoria: 'Moda', precios: [329], emoji: '🧥', icono: Shirt, foto: 'blazer' },
      { img: '1761646238176-b480909e6ba6', nombre: 'Bolso plisado', categoria: 'Moda', precios: [279], agotado: true, emoji: '👛', icono: ShoppingBag, foto: 'clutch,bag' },
      { img: '1535043934128-cf0b28d52f95', nombre: 'Stilettos marfil', categoria: 'Moda', precios: [299], emoji: '👠', icono: Footprints, foto: 'heels,shoes' },
    ],
  },
  audaz: {
    nombre: 'Volt Store', titulo: 'Tecnología que te acompaña', descripcion: 'Audio, accesorios y gadgets con envío a todo el Perú.', colores: ['#141a2b', '#07080b'], fotos: ['1468495244123-6c6c332eeece', '1615655406736-b37c4fabf923'], tinta: '#00d8ff', portada: ['#1b2440', '#0b0f1a'],
    banners: [{ foto: 'headphones', emoji: '🎧' }, { foto: 'gadgets,technology', emoji: '⌚' }],
    categorias: ['Audio', 'Accesorios', 'Wearables'],
    productos: [
      { img: '1505740420928-5e560c06d30e', nombre: 'Audífonos inalámbricos', antes: 349, categoria: 'Audio', precios: [259, 299], emoji: '🎧', icono: Headphones, foto: 'headphones' },
      { img: '1608043152269-423dbba4e7e1', nombre: 'Parlante bluetooth', categoria: 'Audio', precios: [189], emoji: '🔊', icono: Speaker, foto: 'speaker,bluetooth' },
      { img: '1579586337278-3befd40fd17a', nombre: 'Smartwatch Pulse', categoria: 'Wearables', precios: [399], emoji: '⌚', icono: Watch, foto: 'smartwatch' },
      { img: '1618384887929-16ec33fab9ef', nombre: 'Teclado mecánico', categoria: 'Accesorios', precios: [229], emoji: '⌨️', icono: Keyboard, foto: 'keyboard,mechanical' },
      { img: '1615663245857-ac93bb7c39e7', nombre: 'Mouse gamer', categoria: 'Accesorios', precios: [119], agotado: true, emoji: '🖱️', icono: Mouse, foto: 'computer,mouse' },
      { img: '1583863788434-e58a36330cf0', nombre: 'Cargador rápido 30W', categoria: 'Accesorios', precios: [79], emoji: '🔌', icono: Plug, foto: 'charger' },
    ],
  },
  gourmet: {
    nombre: 'Café Alameda', titulo: 'Recién horneado, todos los días', descripcion: 'Café de especialidad, pastelería artesanal y sándwiches para llevar.', colores: ['#efe6d8', '#dccbb2'], fotos: ['1567880905822-56f8e06fe630', '1697890191807-36238e23a0c9'], tinta: '#8a6a4a', portada: ['#6b5443', '#211a14'],
    banners: [{ foto: 'cafe,coffee', emoji: '☕' }, { foto: 'bakery,pastry', emoji: '🥐' }],
    categorias: ['Cafetería', 'Pastelería', 'Salados'],
    productos: [
      { img: '1514432324607-a09d9b4aefdd', nombre: 'Café americano', categoria: 'Cafetería', precios: [9, 12], emoji: '☕', icono: Coffee, foto: 'coffee,cup' },
      { img: '1593443320739-77f74939d0da', nombre: 'Capuchino', categoria: 'Cafetería', precios: [12, 14], emoji: '☕', icono: Coffee, foto: 'cappuccino' },
      { img: '1769138886135-93d1dd8ac06a', nombre: 'Croissant de mantequilla', categoria: 'Pastelería', precios: [8], emoji: '🥐', icono: Croissant, foto: 'croissant' },
      { img: '1714237831514-9b6f07422879', nombre: 'Cheesecake de maracuyá', antes: 20, categoria: 'Pastelería', precios: [16], emoji: '🍰', icono: CakeSlice, foto: 'cheesecake' },
      { img: '1553909489-cd47e0907980', nombre: 'Sándwich de pollo', categoria: 'Salados', precios: [18], emoji: '🥪', icono: Sandwich, foto: 'sandwich' },
      { img: '1769254870299-338bfd99aabd', nombre: 'Empanada de carne', categoria: 'Salados', precios: [9], agotado: true, emoji: '🥟', icono: Croissant, foto: 'empanada' },
    ],
  },
  natural: {
    nombre: 'Raíz Natural', titulo: 'Cuidado natural para tu día a día', descripcion: 'Cosmética artesanal con ingredientes de origen vegetal.', colores: ['#ebe7df', '#d8d1c4'], fotos: ['1765691622563-a57c825c8223', '1552046122-03184de85e08'], tinta: '#6b665d', portada: ['#b9b0a1', '#8c8273'],
    banners: [{ foto: 'natural,cosmetics', emoji: '🌿' }, { foto: 'skincare,botanical', emoji: '🧴' }],
    categorias: ['Rostro', 'Cuerpo', 'Hogar'],
    productos: [
      { img: '1515377905703-c4788e51af15', nombre: 'Aceite facial de rosa mosqueta', categoria: 'Rostro', precios: [65], emoji: '💧', icono: Droplet, foto: 'serum,bottle' },
      { img: '1619451427882-6aaaded0cc61', nombre: 'Crema de karité', antes: 59, categoria: 'Cuerpo', precios: [48, 72], emoji: '🧴', icono: Flower2, foto: 'cream,jar' },
      { img: '1775210603565-37cc9269326f', nombre: 'Jabón de avena y miel', categoria: 'Cuerpo', precios: [18], emoji: '🧼', icono: Leaf, foto: 'handmade,soap' },
      { img: '1768235146463-328c98e7234d', nombre: 'Bálsamo de manteca', categoria: 'Rostro', precios: [15], emoji: '💄', icono: Sparkles, foto: 'lip,balm' },
      { img: '1603905179139-db12ab535ca9', nombre: 'Vela de soya lavanda', categoria: 'Hogar', precios: [39], emoji: '🕯️', icono: Flame, foto: 'candle' },
      { img: '1485955900006-10f4d324d411', nombre: 'Suculenta en maceta', categoria: 'Hogar', precios: [29], agotado: true, emoji: '🪴', icono: Sprout, foto: 'succulent' },
    ],
  },
}

// Fotos de ejemplo locales (opcionales): apps/store/public/demo/<plantilla>/portada-1.jpg, portada-2.jpg, producto-1.jpg … producto-6.jpg.
// Si no existen, se dibuja un fondo neutro con un icono del producto.
const fotoLocal = (codigo: string, archivo: string) => `${import.meta.env.BASE_URL}demo/${codigo}/${archivo}.jpg`
// Fotos de ejemplo de Unsplash (uso libre); se cargan desde su CDN.
const unsplash = (id: string, ancho: number, alto?: number) => `https://images.unsplash.com/photo-${id}?auto=format&fit=crop&w=${ancho}${alto ? `&h=${alto}` : ''}&q=75`

function ImagenDemo({ fuentes, alt, className, respaldo }: { fuentes: string[]; alt: string; className?: string; respaldo: ReactNode }) {
  const [indice, setIndice] = useState(0)
  return indice >= fuentes.length ? <>{respaldo}</> : <img key={fuentes[indice]} className={className} src={fuentes[indice]} alt={alt} loading="lazy" onError={() => setIndice(i => i + 1)} />
}

function FotoSimulada({ demo, icono: Icono = ShoppingBag }: { demo: Demo; icono?: LucideIcon }) {
  return <div className="foto" aria-hidden="true" style={{ display: 'grid', placeItems: 'center', width: '100%', background: `radial-gradient(circle at 50% 38%, rgb(255 255 255 / 0.35), transparent 60%), linear-gradient(160deg, ${demo.colores[0]}, ${demo.colores[1]})`, color: demo.tinta }}>
    <Icono size={56} strokeWidth={1.1} style={{ width: '32%', height: 'auto', opacity: 0.85 }} />
  </div>
}

export function VistaPrevia() {
  const { codigo = PLANTILLA_POR_DEFECTO } = useParams()
  const clave = plantillaDisponible(codigo) && demos[codigo] ? codigo : PLANTILLA_POR_DEFECTO
  const demo = demos[clave]
  const [parametros] = useSearchParams()
  const enMiniatura = parametros.get('miniatura') === '1'
  const [banner, setBanner] = useState(0)
  const [categoria, setCategoria] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [orden, setOrden] = useState('nombre')

  const productos = demo.productos
    .filter(p => (!categoria || p.categoria === categoria) && p.nombre.toLocaleLowerCase('es').includes(busqueda.trim().toLocaleLowerCase('es')))
    .sort((a, b) => orden === 'nombre' ? a.nombre.localeCompare(b.nombre, 'es') : orden === 'precio-asc' ? Math.min(...a.precios) - Math.min(...b.precios) : Math.min(...b.precios) - Math.min(...a.precios))

  // En la vista previa los enlaces no navegan (solo "Productos" baja al catálogo).
  function bloquearEnlaces(e: MouseEvent<HTMLDivElement>) {
    const enlace = (e.target as HTMLElement).closest('a')
    if (!enlace) return
    e.preventDefault()
    if (enlace.getAttribute('href')?.includes('#productos')) document.getElementById('productos')?.scrollIntoView({ behavior: 'smooth' })
  }

  return <div onClickCapture={bloquearEnlaces}>
    <MarcoTienda slug={`vista-previa/${clave}`} nombre={demo.nombre} plantilla={clave}>
      <section className="portada-tienda con-imagen" aria-label="Novedades">
        <div className="portada-fondo"><ImagenDemo key={banner} fuentes={[fotoLocal(clave, `portada-${banner + 1}`), unsplash(demo.fotos[banner], 1800)]} alt={`Banner ${banner + 1} de ${demo.banners.length}`} respaldo={<div style={{ position: 'absolute', inset: 0, background: banner === 0 ? `radial-gradient(ellipse at 70% 30%, rgb(255 255 255 / 0.18), transparent 55%), linear-gradient(135deg, ${demo.portada[0]}, ${demo.portada[1]})` : `radial-gradient(ellipse at 25% 70%, rgb(255 255 255 / 0.14), transparent 55%), linear-gradient(200deg, ${demo.portada[0]}, ${demo.portada[1]})` }} />} /></div>
        <div className="portada-contenido"><p className="etiqueta">Bienvenido a {demo.nombre}</p><h1>{demo.titulo}</h1><p className="introduccion-texto">{demo.descripcion}</p><a className="enlace-flecha" href="#productos">Explorar productos <ArrowRight size={17} /></a></div>
        <div className="controles"><button className="boton-icono" aria-label="Banner anterior" onClick={() => setBanner((banner + demo.banners.length - 1) % demo.banners.length)}><ChevronLeft size={19} /></button><span aria-live="polite">{banner + 1} / {demo.banners.length}</span><button className="boton-icono" aria-label="Banner siguiente" onClick={() => setBanner((banner + 1) % demo.banners.length)}><ChevronRight size={19} /></button></div>
      </section>
      <section id="productos" className="catalogo">
        <div className="seccion-cabecera"><div><p className="etiqueta">Descubre la colección</p><h2>Nuestros productos</h2></div><p>{productos.length} {productos.length === 1 ? 'producto' : 'productos'}</p></div>
        <div className="herramientas-catalogo"><label>Buscar productos<input type="search" value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar por nombre" /></label><label>Ordenar por<select value={orden} onChange={e => setOrden(e.target.value)}><option value="nombre">Nombre</option><option value="precio-asc">Menor precio</option><option value="precio-desc">Mayor precio</option></select></label>{(categoria || busqueda || orden !== 'nombre') && <button onClick={() => { setCategoria(''); setBusqueda(''); setOrden('nombre') }}>Limpiar filtros</button>}</div>
        <div className="filtros" aria-label="Filtrar por categoría"><button className={!categoria ? 'activo' : ''} aria-pressed={!categoria} onClick={() => setCategoria('')}>Todos</button>{demo.categorias.map(c => <button key={c} className={categoria === c ? 'activo' : ''} aria-pressed={categoria === c} onClick={() => setCategoria(c)}>{c}</button>)}</div>
        <div className="productos">{productos.map(p => {
          const indice = demo.productos.indexOf(p) + 1
          return <a className="tarjeta" href="#" key={p.nombre}>
            <div className="tarjeta-media"><ImagenDemo className="foto" fuentes={[fotoLocal(clave, `producto-${indice}`), unsplash(p.img, 700, 875)]} alt={p.nombre} respaldo={<FotoSimulada demo={demo} icono={p.icono} />} />{p.agotado && <span className="insignia">Agotado</span>}{!p.agotado && p.antes && <span className="insignia-oferta">-{Math.round((1 - Math.min(...p.precios) / p.antes) * 100)}%</span>}</div>
            <div className="tarjeta-contenido"><p className="tarjeta-categoria">{p.categoria}</p><h3>{p.nombre}</h3><div className="tarjeta-pie"><span className="precio-grupo">{!p.agotado && p.antes && <s className="precio-anterior">{precio(p.antes)}</s>}<strong>{p.agotado ? 'Agotado' : `${p.precios.length > 1 ? 'Desde ' : ''}${precio(Math.min(...p.precios))}`}</strong></span><ArrowRight size={17} aria-hidden="true" /></div></div>
          </a>
        })}</div>
      </section>
    </MarcoTienda>
    {!enMiniatura && <div role="note" style={{ position: 'fixed', left: '50%', bottom: 16, transform: 'translateX(-50%)', zIndex: 50, display: 'flex', alignItems: 'center', gap: 8, padding: '8px 16px', borderRadius: 999, background: 'rgb(17 17 17 / 0.85)', color: '#fff', font: '500 13px Inter, sans-serif', boxShadow: '0 10px 30px -10px rgb(0 0 0 / 0.5)', backdropFilter: 'blur(8px)', pointerEvents: 'none', whiteSpace: 'nowrap' }}><Eye size={15} />Vista previa · datos de ejemplo</div>}
  </div>
}
