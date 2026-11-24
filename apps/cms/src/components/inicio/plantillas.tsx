import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, ChevronLeft, ChevronRight, Eye } from "lucide-react";
import { MiniaturaEnVivo, ModalVistaPrevia, PLANTILLAS_VISIBLES, infoPlantillas, nombresPlantilla } from "@/components/plantillas/vista-plantillas";

const INTERVALO = 3000; // milisegundos entre cada plantilla

// Sección "Plantillas" de la página de inicio: carrusel con las 5 plantillas y su vista previa real.
export const PlantillasInicio = () => {
  const total = PLANTILLAS_VISIBLES.length;
  const [activa, establecerActiva] = useState(0);
  const [pausado, establecerPausado] = useState(false);
  const [vistaPrevia, establecerVistaPrevia] = useState<{ codigo: string; nombre: string } | null>(null);

  const ir = (indice: number) => establecerActiva(((indice % total) + total) % total);

  // Avance automático (se detiene al pasar el mouse, con la vista previa abierta o si el usuario prefiere menos movimiento).
  useEffect(() => {
    if (pausado || vistaPrevia || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const temporizador = window.setInterval(() => establecerActiva((a) => (a + 1) % total), INTERVALO);
    return () => window.clearInterval(temporizador);
  }, [pausado, vistaPrevia, total]);

  const codigoActivo = PLANTILLAS_VISIBLES[activa];

  return (
    <section id="plantillas" className="scroll-mt-[88px] overflow-hidden bg-nubea-fondo px-5 py-[clamp(88px,10vw,140px)] text-nubea-texto">
      {vistaPrevia && <ModalVistaPrevia {...vistaPrevia} alCerrar={() => establecerVistaPrevia(null)} />}
      <div className="mx-auto max-w-[1200px]">
        <div className="mx-auto max-w-[760px] text-center">
          <p className="text-[13px] font-semibold tracking-[0.2em] text-nubea-acento uppercase">Plantillas</p>
          <h2 className="mt-4 font-display text-[clamp(40px,4.6vw,64px)] leading-[1.05] tracking-[-0.01em]">
            Un diseño para cada <span className="italic text-nubea-acento">negocio</span>.
          </h2>
          <p className="mx-auto mt-5 max-w-[560px] text-[17px] leading-[1.6] text-nubea-texto-suave">
            Elige una de nuestras {total} plantillas y tu tienda queda lista. Mira cómo se ven con productos de ejemplo y cámbiala cuando quieras.
          </p>
        </div>
      </div>

      {/* Carrusel */}
      <div
        className="relative mx-auto mt-[clamp(40px,5vw,64px)] h-[clamp(240px,40vw,520px)] max-w-[1400px]"
        onMouseEnter={() => establecerPausado(true)}
        onMouseLeave={() => establecerPausado(false)}
        aria-roledescription="carrusel"
        aria-label="Plantillas de tienda"
      >
        {/* Brillo detrás de la tarjeta central */}
        <div className="pointer-events-none absolute top-1/2 left-1/2 size-[60%] -translate-x-1/2 -translate-y-1/2 rounded-full bg-nubea-acento/15 blur-[120px]" />

        {PLANTILLAS_VISIBLES.map((codigo, indice) => {
          let desplazamiento = indice - activa;
          if (desplazamiento > total / 2) desplazamiento -= total;
          if (desplazamiento < -total / 2) desplazamiento += total;
          const distancia = Math.abs(desplazamiento);
          const central = desplazamiento === 0;
          const nombre = nombresPlantilla[codigo];
          const info = infoPlantillas[codigo];
          return (
            <article
              key={codigo}
              aria-hidden={!central}
              className="absolute top-1/2 left-1/2 aspect-[16/10] w-[clamp(300px,58vw,780px)] overflow-hidden rounded-[22px] border border-white/10 bg-nubea-superficie shadow-[0_40px_80px_-30px_rgba(0,0,0,0.85)] transition-all duration-700 ease-[cubic-bezier(.2,.8,.2,1)]"
              style={{
                transform: `translate(-50%, -50%) translateX(${desplazamiento * 62}%) scale(${central ? 1 : 0.78})`,
                zIndex: 10 - distancia,
                opacity: distancia > 1 ? 0 : central ? 1 : 0.55,
                pointerEvents: distancia > 1 ? "none" : "auto",
                filter: central ? "none" : "saturate(0.85)",
              }}
            >
              <MiniaturaEnVivo codigo={codigo} />
              {/* Capa de texto como portada de tarjeta */}
              <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(to_top,rgba(5,7,13,0.88)_0%,rgba(5,7,13,0.35)_38%,transparent_65%)]" />
              <button
                type="button"
                tabIndex={central ? 0 : -1}
                onClick={() => (central ? establecerVistaPrevia({ codigo, nombre }) : ir(indice))}
                aria-label={central ? `Ver vista previa de ${nombre}` : `Mostrar ${nombre}`}
                className="group absolute inset-0 flex cursor-pointer flex-col justify-end p-[clamp(18px,2.6vw,32px)] text-left"
              >
                <span className={`absolute top-4 right-4 inline-flex items-center gap-2 rounded-full bg-white/90 px-3.5 py-1.5 text-[13px] font-semibold text-zinc-900 shadow-lg transition ${central ? "opacity-100" : "opacity-0"} group-hover:bg-white`}>
                  <Eye className="size-4" />Vista previa
                </span>
                <span className="text-[clamp(28px,4vw,54px)] leading-none font-semibold tracking-tight text-white drop-shadow">{nombre}</span>
                <span className="mt-4 flex items-center gap-4 text-[11px] font-semibold tracking-[0.18em] text-white/70 uppercase">
                  <span className="shrink-0">{info.ideal}</span>
                  <span className="h-px flex-1 bg-white/25" />
                  <span className="hidden shrink-0 items-center gap-1.5 sm:inline-flex">Ver plantilla <ArrowRight className="size-3.5 transition-transform group-hover:translate-x-1" /></span>
                </span>
              </button>
            </article>
          );
        })}

        {/* Flechas */}
        <button type="button" onClick={() => ir(activa - 1)} aria-label="Plantilla anterior" className="absolute top-1/2 left-2 z-20 grid size-11 -translate-y-1/2 cursor-pointer place-items-center rounded-full border border-white/15 bg-nubea-fondo/70 text-white backdrop-blur transition hover:bg-white hover:text-nubea-fondo md:left-6">
          <ChevronLeft className="size-5" />
        </button>
        <button type="button" onClick={() => ir(activa + 1)} aria-label="Plantilla siguiente" className="absolute top-1/2 right-2 z-20 grid size-11 -translate-y-1/2 cursor-pointer place-items-center rounded-full border border-white/15 bg-nubea-fondo/70 text-white backdrop-blur transition hover:bg-white hover:text-nubea-fondo md:right-6">
          <ChevronRight className="size-5" />
        </button>
      </div>

      {/* Puntos + descripción de la plantilla activa */}
      <div className="mx-auto mt-8 flex max-w-[640px] flex-col items-center gap-5 text-center">
        <div className="flex items-center gap-2.5" role="tablist" aria-label="Elegir plantilla">
          {PLANTILLAS_VISIBLES.map((codigo, indice) => (
            <button
              key={codigo}
              type="button"
              role="tab"
              aria-selected={indice === activa}
              aria-label={nombresPlantilla[codigo]}
              onClick={() => ir(indice)}
              className={`h-2 cursor-pointer rounded-full transition-all duration-500 ${indice === activa ? "w-8 bg-nubea-acento" : "w-2 bg-white/25 hover:bg-white/50"}`}
            />
          ))}
        </div>
        <p key={codigoActivo} className="min-h-[3.2em] animate-[aparecer_.5s_ease] text-[16px] leading-[1.6] text-nubea-texto-suave">
          <span className="font-semibold text-nubea-texto">{nombresPlantilla[codigoActivo]}:</span> {infoPlantillas[codigoActivo].descripcion}
        </p>
        <div className="flex flex-wrap justify-center gap-3">
          <button type="button" onClick={() => establecerVistaPrevia({ codigo: codigoActivo, nombre: nombresPlantilla[codigoActivo] })} className="inline-flex h-[52px] cursor-pointer items-center gap-2 rounded-full border border-white/20 px-6 text-[15px] font-semibold transition hover:bg-white/10">
            <Eye className="size-4" />Ver vista previa
          </button>
          <Link to="/registro" className="group inline-flex h-[52px] items-center gap-2.5 rounded-full bg-nubea-acento px-6 text-[15px] font-semibold text-nubea-sobre-acento transition hover:brightness-110">
            Empezar con una plantilla
            <ArrowRight className="size-4 transition-transform group-hover:translate-x-1" />
          </Link>
        </div>
      </div>
      <style>{"@keyframes aparecer{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:none}}"}</style>
    </section>
  );
};
