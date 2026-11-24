import { useState } from 'react'
import type { ReactNode } from 'react'
import { useOutletContext } from 'react-router-dom'
import { ArrowDown, ArrowUp, Check, Eye, ImagePlus, Images, LayoutTemplate, Link2, RefreshCw, Trash2, Type } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { ContextoTienda } from '@/paginas/panel'
import { MiniaturaEnVivo, ModalVistaPrevia, infoPlantillas } from '@/components/plantillas/vista-plantillas'
import { api } from './datos'
import type { Banner, Configuracion, Plantilla } from './datos'
import { Aviso, EstadoCarga, useCarga, useOperacion } from './comunes'

const claseEntrada = 'h-11 w-full rounded-xl border border-input bg-background px-3.5 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20'

function Seccion({ icono, titulo, descripcion, accion, children }: { icono: ReactNode; titulo: string; descripcion: string; accion?: ReactNode; children: ReactNode }) {
  return <section className="rounded-2xl border bg-card">
    <div className="flex flex-wrap items-start justify-between gap-3 border-b px-6 py-5">
      <div className="flex items-start gap-3"><span className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary">{icono}</span><div><h3 className="font-semibold">{titulo}</h3><p className="text-sm text-muted-foreground">{descripcion}</p></div></div>
      {accion}
    </div>
    <div className="p-6">{children}</div>
  </section>
}

function Interruptor({ activo, alCambiar, etiqueta }: { activo: boolean; alCambiar: (v: boolean) => void; etiqueta: string }) {
  return <button type="button" role="switch" aria-checked={activo} aria-label={etiqueta} onClick={() => alCambiar(!activo)} className={`relative h-6 w-11 shrink-0 cursor-pointer rounded-full transition ${activo ? 'bg-primary' : 'bg-muted-foreground/30'}`}><span className={`absolute top-0.5 left-0.5 size-5 rounded-full bg-white shadow transition-transform ${activo ? 'translate-x-5' : ''}`} /></button>
}

export function Apariencia() {
  const { apiBase, tienda, actualizarTienda } = useOutletContext<ContextoTienda>()
  const { datos, error, recargar } = useCarga(async () => { const [configuracion, plantillas, banners] = await Promise.all([api.leer<Configuracion>(`${apiBase}/configuracion`), api.leer<Plantilla[]>('/api/plantillas'), api.leer<Banner[]>(`${apiBase}/banners`)]); return { configuracion, plantillas, banners } }, [apiBase])
  if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
  return <div className="space-y-6">
    <div><h2 className="text-xl font-semibold tracking-tight">Apariencia</h2><p className="text-sm text-muted-foreground">Elige el diseño de tu tienda, el texto de la portada y los banners del carrusel.</p></div>
    <FormularioApariencia key={JSON.stringify(datos.configuracion)} configuracion={datos.configuracion} plantillas={datos.plantillas} apiBase={apiBase} recargar={recargar} alCambiarPlantilla={codigo => actualizarTienda({ ...tienda, plantilla: codigo })} />
    <Banners banners={datos.banners} apiBase={apiBase} recargar={recargar} />
  </div>
}

function FormularioApariencia({ configuracion, plantillas, apiBase, recargar, alCambiarPlantilla }: { configuracion: Configuracion; plantillas: Plantilla[]; apiBase: string; recargar: () => void; alCambiarPlantilla: (codigo: string) => void }) {
  // Solo las plantillas que la vitrina (apps/store) ya sabe dibujar.
  plantillas = plantillas.filter(p => ["minimal", "boutique", "audaz", "gourmet", "natural"].includes(p.codigo))
  const [id, setId] = useState(configuracion.plantilla.idPlantilla); const [titulo, setTitulo] = useState(configuracion.tituloInicio ?? ''); const [descripcion, setDescripcion] = useState(configuracion.descripcionInicio ?? ''); const [carrusel, setCarrusel] = useState(configuracion.mostrarCarrusel); const op = useOperacion(); const [guardado, setGuardado] = useState(false)
  const [vistaPrevia, setVistaPrevia] = useState<{ codigo: string; nombre: string } | null>(null)
  const noDisponible = !plantillas.some(p => p.idPlantilla === configuracion.plantilla.idPlantilla)
  const cambiado = id !== configuracion.plantilla.idPlantilla || titulo !== (configuracion.tituloInicio ?? '') || descripcion !== (configuracion.descripcionInicio ?? '') || carrusel !== configuracion.mostrarCarrusel
  const tocar = () => setGuardado(false)

  return <form className="space-y-6" onSubmit={e => { e.preventDefault(); setGuardado(false); void op.ejecutar(async () => { await api.modificar(`${apiBase}/configuracion`, { idPlantilla: id === configuracion.plantilla.idPlantilla ? null : id, tituloInicio: titulo, descripcionInicio: descripcion, mostrarCarrusel: carrusel }); const elegida = plantillas.find(p => p.idPlantilla === id); if (elegida) alCambiarPlantilla(elegida.codigo); setGuardado(true); recargar() }) }}>
    {vistaPrevia && <ModalVistaPrevia {...vistaPrevia} alCerrar={() => setVistaPrevia(null)} />}

    <Seccion icono={<LayoutTemplate className="size-5" />} titulo="Plantilla" descripcion="Elige cómo se verá tu tienda. Puedes cambiarla cuando quieras sin perder tus productos.">
      {noDisponible && <p className="mb-4 rounded-xl bg-amber-500/10 px-4 py-3 text-sm text-amber-700 dark:text-amber-400">Tu plantilla actual ({configuracion.plantilla.nombre}) ya no está disponible. Elige una nueva.</p>}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" role="radiogroup" aria-label="Plantilla">
        {plantillas.map(p => {
          const elegida = id === p.idPlantilla
          return <div key={p.idPlantilla} className={`group relative overflow-hidden rounded-2xl border-2 bg-background transition ${elegida ? 'border-primary shadow-[0_14px_34px_-20px_rgba(37,99,235,0.6)]' : 'border-transparent ring-1 ring-border hover:ring-primary/40'}`}>
            <button type="button" role="radio" aria-checked={elegida} onClick={() => { tocar(); setId(p.idPlantilla) }} className="block w-full cursor-pointer text-left">
              <div className="relative aspect-[16/10] overflow-hidden border-b bg-muted"><MiniaturaEnVivo codigo={p.codigo} />{elegida && <span className="absolute top-3 left-3 inline-flex items-center gap-1 rounded-full bg-primary px-2.5 py-1 text-xs font-semibold text-primary-foreground shadow"><Check className="size-3.5" strokeWidth={3} />{p.idPlantilla === configuracion.plantilla.idPlantilla ? 'En uso' : 'Seleccionada'}</span>}</div>
              <div className="flex items-center justify-between gap-3 px-4 py-3">
                <div className="min-w-0"><p className="font-semibold">{p.nombre}</p><p className="truncate text-xs text-muted-foreground">{infoPlantillas[p.codigo]?.ideal ?? 'Diseño de tienda'}</p></div>
                <span className={`grid size-5 shrink-0 place-items-center rounded-full border-2 ${elegida ? 'border-primary bg-primary' : 'border-muted-foreground/40'}`}>{elegida && <span className="size-2 rounded-full bg-white" />}</span>
              </div>
            </button>
            <button type="button" onClick={() => setVistaPrevia({ codigo: p.codigo, nombre: p.nombre })} className="absolute top-3 right-3 inline-flex cursor-pointer items-center gap-1.5 rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-zinc-900 opacity-0 shadow transition group-hover:opacity-100 focus-visible:opacity-100"><Eye className="size-3.5" />Vista previa</button>
          </div>
        })}
      </div>
    </Seccion>

    <Seccion icono={<Type className="size-5" />} titulo="Portada" descripcion="El texto principal que ven tus clientes al entrar a tu tienda.">
      <div className="grid gap-5">
        <label className="grid gap-1.5 text-sm font-medium"><span className="flex justify-between">Título de inicio<span className="font-normal text-muted-foreground">{titulo.length}/160</span></span><input className={claseEntrada} maxLength={160} value={titulo} onChange={e => { tocar(); setTitulo(e.target.value) }} placeholder="Ej. Lino para el verano" /></label>
        <label className="grid gap-1.5 text-sm font-medium">Descripción<textarea className={`${claseEntrada} h-24 resize-y py-2.5`} value={descripcion} onChange={e => { tocar(); setDescripcion(e.target.value) }} placeholder="Una frase corta que invite a comprar." /></label>
        <div className="flex items-center justify-between gap-4 rounded-xl border bg-background px-4 py-3">
          <div><p className="text-sm font-medium">Mostrar carrusel de banners</p><p className="text-xs text-muted-foreground">Si está apagado, la portada muestra solo el título y la descripción.</p></div>
          <Interruptor activo={carrusel} alCambiar={v => { tocar(); setCarrusel(v) }} etiqueta="Mostrar carrusel" />
        </div>
      </div>
    </Seccion>

    {/* Barra de guardado */}
    <div className="sticky bottom-4 z-10 flex flex-wrap items-center justify-between gap-3 rounded-2xl border bg-card/95 px-5 py-3 shadow-[0_12px_30px_-16px_rgba(0,0,0,0.35)] backdrop-blur">
      <div className="text-sm">{op.error ? <Aviso error={op.error} /> : guardado && !cambiado ? <span className="inline-flex items-center gap-1.5 font-medium text-emerald-600 dark:text-emerald-400"><Check className="size-4" />Configuración guardada</span> : cambiado ? <span className="text-muted-foreground">Tienes cambios sin guardar.</span> : <span className="text-muted-foreground">Todo está al día.</span>}</div>
      <Button type="submit" className="h-10 rounded-xl px-5" disabled={op.ocupado || !cambiado}>{op.ocupado ? 'Guardando...' : 'Guardar configuración'}</Button>
    </div>
  </form>
}

function Banners({ banners, apiBase, recargar }: { banners: Banner[]; apiBase: string; recargar: () => void }) {
  const op = useOperacion(); const [archivo, setArchivo] = useState<File | null>(null); const [enlace, setEnlace] = useState('')
  const ordenados = [...banners].sort((a,b) => a.orden - b.orden)
  async function mover(indice: number, direccion: number) { const lista = [...ordenados]; const destino = indice + direccion; if (destino < 0 || destino >= lista.length) return; [lista[indice], lista[destino]] = [lista[destino], lista[indice]]; await op.ejecutar(async () => { await api.actualizar(`${apiBase}/banners/orden`, { banners: lista.map((b, i) => ({ idBanner: b.idBanner, orden: i + 1 })) }); recargar() }) }
  return <Seccion icono={<Images className="size-5" />} titulo="Banners" descripcion="Imágenes grandes que rotan en la portada. Recomendado: horizontal, 1600 × 900 px." accion={<span className="rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">{ordenados.filter(b => b.activo).length} activos de {ordenados.length}</span>}>
    <form className="grid gap-4 rounded-2xl border border-dashed bg-muted/30 p-4 sm:grid-cols-[1.2fr_1fr_auto] sm:items-end" onSubmit={e => { e.preventDefault(); const elementoFormulario = e.currentTarget; if (!archivo) return; const formulario = new FormData(); formulario.append('archivo', archivo); formulario.append('orden', String(Math.max(0, ...banners.map(b => b.orden)) + 1)); if (enlace) formulario.append('enlaceDestino', enlace); void op.ejecutar(async () => { await api.crear(`${apiBase}/banners`, formulario); setArchivo(null); setEnlace(''); elementoFormulario.reset(); recargar() }) }}>
      <label className="flex h-11 cursor-pointer items-center gap-3 rounded-xl border bg-background px-3.5 text-sm transition hover:border-primary">
        <ImagePlus className="size-5 shrink-0 text-primary" /><span className={`truncate ${archivo ? 'font-medium' : 'text-muted-foreground'}`}>{archivo ? archivo.name : 'Elegir imagen...'}</span>
        <input className="sr-only" type="file" accept="image/*" required onChange={e => setArchivo(e.target.files?.[0] ?? null)} />
      </label>
      <label className="relative"><span className="sr-only">Enlace HTTPS opcional</span><Link2 className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground" /><input className={`${claseEntrada} pl-10`} type="url" value={enlace} onChange={e => setEnlace(e.target.value)} placeholder="Enlace al hacer clic (opcional)" /></label>
      <Button type="submit" className="h-11 rounded-xl px-5" disabled={op.ocupado || !archivo}><ImagePlus className="size-4" />{op.ocupado ? 'Subiendo...' : 'Añadir banner'}</Button>
    </form>
    <div className="mt-3"><Aviso error={op.error} /></div>
    {ordenados.length === 0 ? <p className="mt-4 rounded-xl border border-dashed px-5 py-10 text-center text-sm text-muted-foreground">Aún no tienes banners. Sube el primero para mostrarlo en la portada.</p>
    : <div className="mt-5 grid gap-4 md:grid-cols-2">{ordenados.map((b, i) => <BannerFila key={`${b.idBanner}-${b.enlaceDestino}-${b.activo}`} banner={b} indice={i} total={ordenados.length} apiBase={apiBase} recargar={recargar} mover={mover} />)}</div>}
  </Seccion>
}

function BannerFila({ banner, indice, total, apiBase, recargar, mover }: { banner: Banner; indice: number; total: number; apiBase: string; recargar: () => void; mover: (i: number, d: number) => Promise<void> }) {
 const [enlace, setEnlace] = useState(banner.enlaceDestino ?? ''); const [activo, setActivo] = useState(banner.activo); const op = useOperacion(); const ruta = `${apiBase}/banners/${banner.idBanner}`
 const cambiado = enlace !== (banner.enlaceDestino ?? '') || activo !== banner.activo
 const icono = 'grid size-8 cursor-pointer place-items-center rounded-lg bg-white/90 text-zinc-800 shadow transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-40'
 return <div className={`overflow-hidden rounded-2xl border bg-background transition ${activo ? '' : 'opacity-75'}`}>
  <div className="group relative aspect-[16/7] bg-muted">
   <img className={`size-full object-cover ${activo ? '' : 'grayscale'}`} src={banner.url} alt={`Banner ${indice + 1}`} />
   <span className="absolute top-3 left-3 rounded-full bg-black/60 px-2.5 py-0.5 text-xs font-semibold text-white">#{indice + 1}</span>
   {!activo && <span className="absolute top-3 left-14 rounded-full bg-zinc-800/80 px-2.5 py-0.5 text-xs font-semibold text-white">Oculto</span>}
   <div className="absolute top-3 right-3 flex gap-1.5">
    <button type="button" className={icono} title="Mover antes" aria-label="Mover antes" disabled={indice === 0 || op.ocupado} onClick={() => void mover(indice, -1)}><ArrowUp className="size-4" /></button>
    <button type="button" className={icono} title="Mover después" aria-label="Mover después" disabled={indice === total - 1 || op.ocupado} onClick={() => void mover(indice, 1)}><ArrowDown className="size-4" /></button>
    <label className={icono} title="Reemplazar imagen"><RefreshCw className="size-4" /><span className="sr-only">Reemplazar imagen</span><input className="sr-only" type="file" accept="image/*" onChange={e => { const archivo = e.target.files?.[0]; if (!archivo) return; const datos = new FormData(); datos.append('archivo', archivo); void op.ejecutar(async () => { await api.actualizar(`${ruta}/archivo`, datos); recargar() }); e.target.value = '' }} /></label>
    <button type="button" className={`${icono} hover:text-red-600`} title="Eliminar" aria-label="Eliminar banner" disabled={op.ocupado} onClick={() => { if (window.confirm('¿Eliminar este banner?')) void op.ejecutar(async () => { await api.eliminar(ruta); recargar() }) }}><Trash2 className="size-4" /></button>
   </div>
  </div>
  <div className="space-y-3 p-4">
   <label className="relative block"><span className="sr-only">Enlace HTTPS</span><Link2 className="pointer-events-none absolute top-1/2 left-3.5 size-4 -translate-y-1/2 text-muted-foreground" /><input className={`${claseEntrada} pl-10`} type="url" value={enlace} onChange={e => setEnlace(e.target.value)} placeholder="Sin enlace" /></label>
   <div className="flex items-center justify-between gap-3">
    <label className="flex items-center gap-2.5 text-sm"><Interruptor activo={activo} alCambiar={setActivo} etiqueta="Banner activo" />{activo ? 'Visible' : 'Oculto'}</label>
    <Button variant={cambiado ? 'default' : 'outline'} size="sm" className="rounded-lg" disabled={op.ocupado || !cambiado} onClick={() => void op.ejecutar(async () => { await api.modificar(ruta, { enlaceDestino: enlace || '', activo }); recargar() })}>Guardar</Button>
   </div>
   <Aviso error={op.error} />
  </div>
 </div>
}
