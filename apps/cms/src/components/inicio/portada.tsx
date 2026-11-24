import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Pause, Play } from "lucide-react";
import { cn } from "@/lib/utils";

const base = import.meta.env.BASE_URL;
// Coloca aquí tus archivos: public/assets/portada/portada.mp4 y portada.jpg
const VIDEO = `${base}assets/portada/portada.mp4`;
const POSTER = `${base}assets/portada/portada.jpg`;

const tiposDeTienda = [
  { texto: "Moda", clase: "font-display italic text-[30px] md:text-[44px]" },
  { texto: "CAFÉ & TÉ", clase: "font-black tracking-wide text-[24px] md:text-[34px]" },
  { texto: "Artesanía", clase: "font-display text-[30px] md:text-[44px]" },
  { texto: "belleza", clase: "font-light text-[28px] md:text-[42px]" },
  { texto: "HOGAR", clase: "font-bold text-[26px] md:text-[36px]" },
  { texto: "Joyería", clase: "font-display italic text-[30px] md:text-[44px]" },
  { texto: "tecnología", clase: "font-medium text-[26px] md:text-[38px]" },
  { texto: "MASCOTAS", clase: "font-black tracking-wide text-[22px] md:text-[32px]" },
  { texto: "Librería", clase: "font-display text-[30px] md:text-[44px]" },
];

const prefiereMenosMovimiento = () =>
  typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

export const PortadaInicio = () => {
  const video = useRef<HTMLVideoElement>(null);
  const [reproduciendo, establecerReproduciendo] = useState(() => !prefiereMenosMovimiento());

  const alternarVideo = () => {
    const elemento = video.current;
    if (!elemento) return;
    if (elemento.paused) {
      elemento.play().then(() => establecerReproduciendo(true)).catch(() => {});
    } else {
      elemento.pause();
      establecerReproduciendo(false);
    }
  };

  return (
    <section className="relative isolate flex min-h-[100svh] flex-col overflow-hidden bg-nubea-fondo text-nubea-texto">
      {/* Video de fondo */}
      <video
        ref={video}
        autoPlay={reproduciendo}
        muted
        loop
        playsInline
        preload="metadata"
        poster={POSTER}
        aria-hidden="true"
        className="absolute inset-0 -z-20 h-full w-full object-cover"
      >
        <source src={VIDEO} type="video/mp4" />
      </video>

      {/* Capas oscuras para que el texto se lea sobre cualquier video */}
      <div aria-hidden="true" className="absolute inset-0 -z-10 bg-linear-to-r from-nubea-fondo/85 via-nubea-fondo/45 to-nubea-fondo/20" />
      <div aria-hidden="true" className="absolute inset-0 -z-10 bg-linear-to-b from-nubea-fondo/40 via-transparent via-25% to-nubea-fondo/90" />

      {/* Contenido */}
      <div className="mx-auto flex w-full max-w-[1440px] flex-1 items-center px-5 pt-[120px] pb-[170px] md:px-[120px]">
        <div className="max-w-[1000px]">
          <p className="inline-flex items-center gap-2.5 rounded-full border border-white/20 bg-nubea-fondo/45 py-1.5 pr-4 pl-1.5 text-[13px] backdrop-blur-sm">
            <span className="rounded-full bg-nubea-acento px-2.5 py-0.5 text-[12px] font-bold text-nubea-sobre-acento">
              Nuevo
            </span>
            Diseños pensados para marcas con estilo
          </p>

          <h1 className="mt-7 font-display text-[clamp(52px,7.8vw,112px)] leading-[0.98] tracking-[-0.015em]">
            Vende con <span className="italic text-nubea-acento">estilo.</span>
            <br />
            Crece sin límites.
          </h1>

          <p className="mt-7 max-w-[560px] text-[clamp(17px,1.4vw,20px)] leading-[1.55] text-white/85">
            Nubea convierte tu marca en una tienda online preciosa, lista para vender en minutos.
            Sin código y con todo lo que necesitas para crecer.
          </p>

          <div className="mt-10 flex flex-col gap-4 sm:flex-row">
            <Link
              to="/registro"
              className="group inline-flex h-[52px] items-center justify-center gap-2.5 rounded-full bg-nubea-texto px-7 text-[16px] font-semibold text-nubea-fondo transition hover:bg-white"
            >
              Crear mi tienda
              <ArrowRight className="size-[18px] transition-transform group-hover:translate-x-1" />
            </Link>
            <a
              href="#como-funciona"
              className="inline-flex h-[52px] items-center justify-center rounded-full border border-white/70 px-7 text-[16px] font-semibold text-white transition hover:bg-white/10"
            >
              Ver cómo funciona
            </a>
          </div>
        </div>
      </div>

      {/* Marquesina con tipos de tienda */}
      <p className="sr-only">
        Ideal para tiendas de moda, café y té, artesanía, belleza, hogar, joyería, tecnología, mascotas y librerías.
      </p>
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 bottom-10 overflow-hidden [mask-image:linear-gradient(to_right,transparent,black_8%,black_92%,transparent)]"
      >
        <div className="flex w-max animate-marquesina items-center gap-[88px] pr-[88px] motion-reduce:animate-none">
          {[...tiposDeTienda, ...tiposDeTienda].map((tipo, indice) => (
            <span key={indice} className={cn("whitespace-nowrap leading-none text-white/75", tipo.clase)}>
              {tipo.texto}
            </span>
          ))}
        </div>
      </div>

      {/* Control del video */}
      <button
        type="button"
        onClick={alternarVideo}
        aria-label={reproduciendo ? "Pausar video de fondo" : "Reproducir video de fondo"}
        className="absolute right-5 bottom-[130px] grid size-10 cursor-pointer place-items-center rounded-full border border-white/30 bg-nubea-fondo/50 text-white backdrop-blur-sm transition hover:bg-nubea-fondo/70 md:right-[120px]"
      >
        {reproduciendo ? <Pause className="size-3.5 fill-current" /> : <Play className="size-3.5 fill-current" />}
      </button>
    </section>
  );
};
