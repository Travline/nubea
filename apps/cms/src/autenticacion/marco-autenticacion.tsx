import type { ReactNode } from "react";
import { Check } from "lucide-react";

type PropiedadesMarco = {
  /** Texto de la pastilla sobre el titular. */
  etiqueta: string;
  /** Titular; la parte en `destacado` va en cursiva azul. */
  titular: string;
  destacado: string;
  descripcion: string;
  puntos: string[];
  children: ReactNode;
};

// Ondas de luz azul del fondo (decorativas).
function Ondas() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="absolute -top-40 -right-40 size-[620px] rounded-full bg-[#2f5bff]/15 blur-[140px]" />
      <div className="absolute -bottom-56 -left-40 size-[680px] rounded-full bg-[#2f5bff]/20 blur-[150px]" />
      <svg viewBox="0 0 1000 420" preserveAspectRatio="none" className="absolute bottom-0 left-0 h-[48%] w-[78%]">
        <defs>
          <linearGradient id="onda-relleno-a" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#2f5bff" stopOpacity="0.35" />
            <stop offset="1" stopColor="#2f5bff" stopOpacity="0" />
          </linearGradient>
          <linearGradient id="onda-linea-a" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" stopColor="#4d7cff" />
            <stop offset="0.7" stopColor="#4d7cff" stopOpacity="0.5" />
            <stop offset="1" stopColor="#4d7cff" stopOpacity="0" />
          </linearGradient>
          <filter id="onda-brillo" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="6" /></filter>
        </defs>
        <path d="M0 150 C 220 110, 380 230, 560 320 S 860 410, 1000 420 L 0 420 Z" fill="url(#onda-relleno-a)" />
        <path d="M0 150 C 220 110, 380 230, 560 320 S 860 410, 1000 420" fill="none" stroke="url(#onda-linea-a)" strokeWidth="10" filter="url(#onda-brillo)" />
        <path d="M0 150 C 220 110, 380 230, 560 320 S 860 410, 1000 420" fill="none" stroke="url(#onda-linea-a)" strokeWidth="2.5" />
      </svg>
      <svg viewBox="0 0 600 420" preserveAspectRatio="none" className="absolute top-0 right-0 h-[46%] w-[36%]">
        <defs>
          <linearGradient id="onda-relleno-b" x1="1" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#2f5bff" stopOpacity="0.3" />
            <stop offset="1" stopColor="#2f5bff" stopOpacity="0" />
          </linearGradient>
          <linearGradient id="onda-linea-b" x1="1" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#4d7cff" />
            <stop offset="1" stopColor="#4d7cff" stopOpacity="0" />
          </linearGradient>
        </defs>
        <path d="M600 60 C 560 220, 420 320, 160 400 L 600 420 Z" fill="url(#onda-relleno-b)" />
        <path d="M600 60 C 560 220, 420 320, 160 400" fill="none" stroke="url(#onda-linea-b)" strokeWidth="10" filter="url(#onda-brillo)" />
        <path d="M600 60 C 560 220, 420 320, 160 400" fill="none" stroke="url(#onda-linea-b)" strokeWidth="2.5" />
      </svg>
    </div>
  );
}

// Diseño compartido de Iniciar sesión y Registro: texto de bienvenida a la
// izquierda y el formulario en una tarjeta de vidrio a la derecha.
export function MarcoAutenticacion({ etiqueta, titular, destacado, descripcion, puntos, children }: PropiedadesMarco) {
  return (
    <main className="relative min-h-screen overflow-hidden bg-[#05070d] pt-[88px] text-nubea-texto">
      <Ondas />
      <div className="relative mx-auto grid min-h-[calc(100vh-88px)] max-w-[1200px] items-center gap-[48px] px-[20px] py-[32px] md:px-[40px] lg:grid-cols-[1fr_minmax(0,480px)] lg:gap-[64px] xl:px-[64px]">
        {/* Bienvenida */}
        <section className="max-w-[540px]">
          <p className="mb-[24px] inline-flex items-center gap-[10px] rounded-full border border-white/15 bg-white/[0.04] px-[16px] py-[7px] text-[14px] text-white/90 backdrop-blur">
            <span className="size-[8px] rounded-full bg-nubea-acento shadow-[0_0_10px_#6C9BFF]" />
            {etiqueta}
          </p>
          <h2 className="text-[clamp(36px,3.8vw,56px)] leading-[1.08] font-bold tracking-tight text-white">
            {titular}{" "}
            <span className="font-display font-normal italic text-[#4d7cff] drop-shadow-[0_0_24px_rgba(77,124,255,0.45)]">{destacado}</span>
          </h2>
          <p className="mt-[20px] max-w-[460px] text-[16px] leading-[1.6] text-white/65">{descripcion}</p>
          <ul className="mt-[28px] space-y-[14px]">
            {puntos.map((punto) => (
              <li key={punto} className="flex items-center gap-[14px] text-[16px] text-white/90">
                <span className="grid size-[28px] shrink-0 place-items-center rounded-full bg-[#1d3a8a] text-[#8fb3ff] ring-1 ring-[#4d7cff]/40">
                  <Check className="size-[14px]" strokeWidth={3} />
                </span>
                {punto}
              </li>
            ))}
          </ul>
        </section>

        {/* Tarjeta del formulario */}
        <section className="relative w-full rounded-[28px] border border-[#4d7cff]/45 bg-[linear-gradient(160deg,rgba(20,30,58,0.78),rgba(8,12,24,0.82))] p-[28px] shadow-[0_0_0_1px_rgba(255,255,255,0.03)_inset,0_0_60px_-10px_rgba(77,124,255,0.35),0_40px_80px_-30px_rgba(0,0,0,0.8)] backdrop-blur-xl sm:p-[36px]">
          {children}
        </section>
      </div>
    </main>
  );
}

type PropiedadesCampo = {
  id: string;
  etiqueta: string;
  icono: ReactNode;
  accion?: ReactNode;
  children: ReactNode;
};

// Campo con icono y etiqueta dentro de la misma caja.
export function CampoAutenticacion({ id, etiqueta, icono, accion, children }: PropiedadesCampo) {
  return (
    <div className="group flex h-[52px] items-center gap-[12px] rounded-[12px] border border-white/12 bg-white/[0.03] px-[16px] transition hover:border-white/20 focus-within:border-nubea-acento focus-within:bg-white/[0.05] focus-within:ring-[3px] focus-within:ring-nubea-acento/15">
      <span className="shrink-0 text-white/70 transition-colors group-focus-within:text-nubea-acento [&_svg]:size-[18px]">{icono}</span>
      <label htmlFor={id} className="w-[92px] shrink-0 cursor-text text-[14px] font-medium text-white max-sm:sr-only">{etiqueta}</label>
      {children}
      {accion}
    </div>
  );
}

export function SeparadorAlterno({ texto }: { texto: string }) {
  return (
    <div className="my-[18px] flex items-center gap-[14px] text-[13px] text-white/60">
      <span className="h-px flex-1 bg-white/15" />
      {texto}
      <span className="h-px flex-1 bg-white/15" />
    </div>
  );
}

export function BotonVerContrasena({ visible, alCambiar }: { visible: boolean; alCambiar: () => void }) {
  return (
    <button type="button" onClick={alCambiar} aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"} className="-mr-[6px] grid size-[34px] shrink-0 cursor-pointer place-items-center rounded-[8px] text-white/70 transition hover:bg-white/5 hover:text-white">
      {visible ? <EyeOffIcono /> : <EyeIcono />}
    </button>
  );
}

function EyeIcono() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="size-[18px]"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></svg>;
}
function EyeOffIcono() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="size-[18px]"><path d="M10.6 5.1A10 10 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.2 3.2M6.6 6.6C3.7 8.4 2 12 2 12s3.5 7 10 7c1.9 0 3.6-.6 5-1.4M9.9 9.9a3 3 0 0 0 4.2 4.2M2 2l20 20" /></svg>;
}

export const claseEntrada =
  "h-full min-w-0 flex-1 bg-transparent text-[14px] text-white outline-none placeholder:text-white/40";

export const claseBotonPrincipal =
  "group inline-flex h-[52px] w-full cursor-pointer items-center justify-center gap-[10px] rounded-[12px] bg-[#5b8cff] text-[15px] font-semibold text-[#06101f] shadow-[0_12px_34px_-10px_rgba(91,140,255,0.8)] transition hover:bg-[#6f9bff] disabled:cursor-not-allowed disabled:opacity-60";

export const claseBotonGoogle =
  "h-[52px] gap-[12px] rounded-[12px] border-white/30 bg-transparent px-[16px] text-[15px] [&_svg]:size-[20px] text-white hover:bg-white/[0.06]";
