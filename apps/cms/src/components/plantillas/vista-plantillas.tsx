import { useEffect, useRef, useState } from 'react'
import { ExternalLink, Eye, Monitor, Smartphone, X } from 'lucide-react'

// Piezas compartidas para mostrar las plantillas de tienda (página de inicio y panel del vendedor).

// Dirección de la app de tiendas. En desarrollo corre en otro puerto (por defecto 5174); en producción comparte dominio.
export const URL_TIENDA = (import.meta.env.VITE_TIENDA_URL as string | undefined)?.trim().replace(/\/$/, '') || (import.meta.env.DEV ? 'http://localhost:5174/tienda' : '/tienda')

const iconoAzul = 'bg-primary/10 text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]'

/**
 * Única lista de plantillas del cms: las que la vitrina (apps/store, PLANTILLAS_DISPONIBLES) sabe dibujar.
 * Deben existir en la tabla `plantilla` (V1 + V15). El código 'audaz' se muestra como "Tech".
 */
export const PLANTILLAS_VISIBLES = ['minimal', 'boutique', 'audaz', 'gourmet', 'natural']
export const infoPlantillas: Record<string, { descripcion: string; ideal: string }> = {
  minimal: { descripcion: 'Editorial y cálida, con portada a pantalla completa y catálogo en tonos arena.', ideal: 'Ropa y accesorios' },
  boutique: { descripcion: 'Negro y dorado, con portada dividida y un catálogo centrado y elegante.', ideal: 'Moda y joyería' },
  audaz: { descripcion: 'Oscura y tecnológica, con brillo neón y catálogo con barra lateral.', ideal: 'Electrónica y gadgets' },
  gourmet: { descripcion: 'Estilo restaurante: foto a pantalla completa y productos como una carta.', ideal: 'Cafeterías y comida' },
  natural: { descripcion: 'Neutra y limpia en tonos piedra, con portada dividida y rejilla fina.', ideal: 'Cosmética y productos naturales' },
}

/** Nombres de las plantillas (los mismos que en Administración → Plantillas). */
export const nombresPlantilla: Record<string, string> = { minimal: 'Minimalista', boutique: 'Boutique', audaz: 'Tech', gourmet: 'Gourmet', natural: 'Natural' }

export function Miniatura({ codigo }: { codigo: string }) {
  const productos = (clase: string, n = 3) => <div className={`grid gap-1.5 ${n === 4 ? 'grid-cols-4' : 'grid-cols-3'}`}>{Array.from({ length: n }, (_, i) => <div key={i} className={`aspect-[4/5] rounded-[3px] ${clase}`} />)}</div>
  switch (codigo) {
    case 'boutique': return <div className="flex h-full flex-col gap-2 bg-[#0e0d0c] p-3">
      <div className="flex items-center justify-center border-b border-[#c9a96e]/40 pb-1.5"><span className="h-1.5 w-14 rounded bg-[#c9a96e]" /></div>
      <div className="grid flex-1 grid-cols-2 gap-2"><div className="flex flex-col justify-center gap-1"><span className="h-2.5 w-4/5 rounded bg-[#f3ece0]" /><span className="h-2.5 w-3/5 rounded bg-[#f3ece0]" /><span className="mt-1 h-2 w-10 rounded-sm bg-[#c9a96e]" /></div><div className="rounded-[3px] bg-gradient-to-br from-[#3a332b] to-[#1c1915]" /></div>
      {productos('bg-[#1f1c19]')}</div>
    case 'audaz': return <div className="flex h-full flex-col gap-2 bg-[#07080b] bg-[linear-gradient(rgba(0,216,255,0.06)_1px,transparent_1px),linear-gradient(90deg,rgba(0,216,255,0.06)_1px,transparent_1px)] bg-[size:12px_12px] p-3">
      <div className="flex items-center justify-between"><span className="h-1.5 w-10 rounded bg-white/80" /><span className="h-1.5 w-6 rounded bg-[#00d8ff]" /></div>
      <div className="flex flex-1 flex-col items-center justify-center gap-1"><span className="h-2.5 w-3/5 rounded bg-gradient-to-r from-[#00d8ff] to-[#8b5cff]" /><span className="h-1.5 w-2/5 rounded bg-white/30" /><span className="mt-1 h-6 w-3/5 rounded-[3px] border border-[#00d8ff]/50 bg-[#0d1520] shadow-[0_0_14px_rgba(0,216,255,0.35)]" /></div>
      <div className="grid grid-cols-[1fr_3fr] gap-1.5"><div className="space-y-1"><span className="block h-1.5 rounded bg-white/20" /><span className="block h-1.5 rounded bg-white/20" /><span className="block h-1.5 rounded bg-[#00d8ff]/60" /></div>{productos('border border-white/10 bg-[#11141b]')}</div></div>
    case 'gourmet': return <div className="flex h-full flex-col bg-[#f7f4ef]">
      <div className="flex items-center justify-center bg-[#1c1a17] py-1.5"><span className="h-1.5 w-14 rounded bg-[#f7f4ef]/80" /></div>
      <div className="flex flex-1 flex-col items-center justify-center gap-1 bg-gradient-to-br from-[#5a4636] to-[#2a211a]"><span className="h-2.5 w-1/2 rounded bg-white/90" /><span className="mt-1 h-2 w-12 rounded-sm border border-white/70" /></div>
      <div className="grid grid-cols-2 gap-1.5 p-2.5">{[0, 1, 2, 3].map(i => <div key={i} className="flex items-center gap-1.5 rounded-[3px] border border-[#e6e0d6] bg-white p-1"><div className="flex-1 space-y-1"><span className="block h-1.5 w-4/5 rounded bg-[#1c1a17]/70" /><span className="block h-1 w-1/2 rounded bg-[#a88b5f]" /></div><span className="size-5 rounded-[2px] bg-[#d8cbb8]" /></div>)}</div></div>
    case 'natural': return <div className="flex h-full flex-col bg-[#f2efe9]">
      <div className="flex items-center justify-between border-b border-[#ddd7cc] px-3 py-1.5"><span className="h-1.5 w-10 rounded bg-[#22211e]" /><span className="h-1 w-8 rounded bg-[#22211e]/40" /></div>
      <div className="grid flex-1 grid-cols-2"><div className="flex flex-col justify-center gap-1 bg-[#e6e1d8] px-3"><span className="h-2.5 w-4/5 rounded bg-[#22211e]" /><span className="h-1.5 w-3/5 rounded bg-[#22211e]/40" /><span className="mt-1 h-2 w-10 rounded-[2px] bg-[#22211e]" /></div><div className="bg-gradient-to-br from-[#b9b0a1] to-[#8c8273]" /></div>
      <div className="grid grid-cols-4 border-t border-[#d6d0c5]">{[0, 1, 2, 3].map(i => <div key={i} className="border-r border-[#d6d0c5] p-1.5 last:border-r-0"><div className="aspect-square bg-[#e9e5dd]" /></div>)}</div></div>
    default: return <div className="flex h-full flex-col bg-[#f4f0e9]">
      <div className="flex items-center justify-center bg-[#1b1b19] py-1.5"><span className="h-1.5 w-12 rounded bg-[#f7f3ec]/80" /></div>
      <div className="flex flex-1 flex-col justify-end gap-1 bg-gradient-to-t from-[#2a2925] to-[#7b7466] p-3"><span className="h-2.5 w-1/2 rounded bg-[#f7f3ec]" /><span className="h-1.5 w-1/3 rounded bg-[#f7f3ec]/60" /></div>
      <div className="bg-[#e3dcd0] p-2.5">{productos('bg-[#efe9df]')}</div></div>
  }
}

/**
 * Miniatura "en vivo": carga la vista previa real de la tienda en un iframe reducido.
 * Mientras carga (o si la app de tiendas no responde) se ve el dibujo de la plantilla.
 */
export function MiniaturaEnVivo({ codigo, anchoPagina = 1440 }: { codigo: string; anchoPagina?: number }) {
  const contenedor = useRef<HTMLDivElement>(null)
  const [medida, establecerMedida] = useState({ ancho: 0, alto: 0 })
  const [visible, establecerVisible] = useState(false)
  const [cargada, establecerCargada] = useState(false)

  useEffect(() => {
    const elemento = contenedor.current
    if (!elemento) return
    const observador = new ResizeObserver(([entrada]) => establecerMedida({ ancho: entrada.contentRect.width, alto: entrada.contentRect.height }))
    observador.observe(elemento)
    // Solo se carga cuando la tarjeta se acerca a la pantalla.
    const alVer = new IntersectionObserver(([entrada]) => { if (entrada.isIntersecting) { establecerVisible(true); alVer.disconnect() } }, { rootMargin: '300px' })
    alVer.observe(elemento)
    return () => { observador.disconnect(); alVer.disconnect() }
  }, [])

  const escala = medida.ancho ? medida.ancho / anchoPagina : 0
  return <div ref={contenedor} className="relative h-full w-full overflow-hidden">
    <div className={`absolute inset-0 transition-opacity duration-500 ${cargada ? 'opacity-0' : 'opacity-100'}`}><Miniatura codigo={codigo} /></div>
    {visible && escala > 0 && <iframe
      src={`${URL_TIENDA}/vista-previa/${encodeURIComponent(codigo)}?miniatura=1`}
      title=""
      aria-hidden="true"
      tabIndex={-1}
      scrolling="no"
      onLoad={() => establecerCargada(true)}
      className={`pointer-events-none absolute top-0 left-0 origin-top-left border-0 bg-white transition-opacity duration-500 ${cargada ? 'opacity-100' : 'opacity-0'}`}
      style={{ width: anchoPagina, height: medida.alto / escala, transform: `scale(${escala})` }}
    />}
  </div>
}

export function ModalVistaPrevia({ codigo, nombre, alCerrar }: { codigo: string; nombre: string; alCerrar: () => void }) {
  const [dispositivo, establecerDispositivo] = useState<'escritorio' | 'movil'>('escritorio')
  const url = `${URL_TIENDA}/vista-previa/${encodeURIComponent(codigo)}`
  useEffect(() => {
    const alPresionar = (e: KeyboardEvent) => { if (e.key === 'Escape') alCerrar() }
    const desborde = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', alPresionar)
    return () => { document.body.style.overflow = desborde; window.removeEventListener('keydown', alPresionar) }
  }, [alCerrar])

  return <div role="dialog" aria-modal="true" aria-label={`Vista previa de ${nombre}`} className="fixed inset-0 z-50 flex bg-black/70 p-2 backdrop-blur-sm sm:p-5" onClick={alCerrar}>
    <div className="mx-auto flex h-full w-full max-w-[1440px] flex-col overflow-hidden rounded-2xl border bg-background text-foreground shadow-2xl" onClick={e => e.stopPropagation()}>
      <div className="flex flex-wrap items-center justify-between gap-3 border-b px-4 py-3">
        <div className="flex min-w-0 items-center gap-3">
          <span className={`grid size-9 shrink-0 place-items-center rounded-xl ${iconoAzul}`}><Eye className="size-[18px]" /></span>
          <div className="min-w-0"><p className="truncate font-semibold">Vista previa · {nombre}</p><p className="text-xs text-muted-foreground">Con productos de ejemplo. En tu tienda se verán tus propios productos, fotos y textos.</p></div>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex rounded-xl border bg-muted/50 p-1">
            {([['escritorio', Monitor, 'Escritorio'], ['movil', Smartphone, 'Móvil']] as const).map(([valor, Icono, etiqueta]) => <button key={valor} type="button" aria-pressed={dispositivo === valor} onClick={() => establecerDispositivo(valor)} className={`inline-flex h-8 cursor-pointer items-center gap-1.5 rounded-lg px-3 text-sm transition ${dispositivo === valor ? 'bg-background font-medium shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}><Icono className="size-4" /><span className="hidden sm:inline">{etiqueta}</span></button>)}
          </div>
          <a href={url} target="_blank" rel="noreferrer" className="hidden h-10 items-center gap-2 rounded-xl border px-3 text-sm font-medium transition hover:bg-muted sm:inline-flex"><ExternalLink className="size-4" />Abrir en otra pestaña</a>
          <button type="button" onClick={alCerrar} aria-label="Cerrar vista previa" className="grid size-10 cursor-pointer place-items-center rounded-xl text-muted-foreground transition hover:bg-muted hover:text-foreground"><X className="size-5" /></button>
        </div>
      </div>
      <div className="flex flex-1 justify-center overflow-hidden bg-muted/60 p-0 data-[movil=true]:py-6" data-movil={dispositivo === 'movil'}>
        <iframe key={codigo} src={url} title={`Vista previa de ${nombre}`} className={`h-full bg-white transition-[width] duration-300 ${dispositivo === 'movil' ? 'w-[390px] max-w-full rounded-[28px] border-[10px] border-zinc-900 shadow-2xl' : 'w-full'}`} />
      </div>
    </div>
  </div>
}
