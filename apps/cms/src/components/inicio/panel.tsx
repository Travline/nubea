import { Lock } from "lucide-react";

// Captura real del panel del vendedor. Para cambiarla, reemplaza este archivo:
// public/assets/panel/panel-tienda.webp
const CAPTURA = `${import.meta.env.BASE_URL}assets/panel/panel-tienda.webp`;

export const PanelInicio = () => {
  return (
    <section
      id="panel"
      className="relative overflow-hidden bg-nubea-fondo px-5 py-[clamp(88px,10vw,140px)] text-nubea-texto"
    >
      {/* Brillo azul detrás del panel */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute top-1/2 left-1/2 -z-0 h-[520px] w-[min(1100px,90vw)] -translate-x-1/2 rounded-full bg-nubea-acento/15 blur-[140px]"
      />

      <div className="relative mx-auto flex max-w-[1200px] flex-col items-center">
        <p className="text-[13px] font-semibold tracking-[0.2em] text-nubea-acento uppercase">
          Tu panel
        </p>
        <h2 className="mt-4 text-center font-display text-[clamp(40px,4.6vw,64px)] leading-[1.05] tracking-[-0.01em]">
          Todo tu negocio, <span className="italic text-nubea-acento">en un solo panel</span>.
        </h2>
        <p className="mt-5 max-w-[640px] text-center text-[clamp(16px,1.3vw,18px)] leading-[1.6] text-nubea-texto-suave">
          Catálogo, inventario, pedidos, apariencia y pagos en un mismo lugar. Así se ve tu tienda por dentro
          apenas la creas.
        </p>

        {/* Ventana del navegador con la captura */}
        <figure className="mt-14 w-full overflow-hidden rounded-[18px] border border-white/12 bg-nubea-superficie shadow-[0_-10px_120px_rgba(108,155,255,0.22),0_40px_80px_rgba(0,0,0,0.6)]">
          <div className="flex h-[46px] items-center gap-2 border-b border-white/8 bg-nubea-superficie-2 px-[18px]">
            <span className="size-2.5 rounded-full bg-white/20" />
            <span className="size-2.5 rounded-full bg-white/20" />
            <span className="size-2.5 rounded-full bg-white/20" />
            <span className="ml-3.5 hidden h-[26px] w-[360px] items-center gap-2 rounded-lg bg-nubea-fondo/60 px-3 text-[12px] text-nubea-texto-suave sm:flex">
              <Lock className="size-3" />
              nubea.app/plataforma/panel
            </span>
          </div>
          <img
            src={CAPTURA}
            alt="Panel de una tienda en Nubea con las secciones Resumen, Catálogo, Inventario, Pedidos, Apariencia y Pagos"
            loading="lazy"
            className="block h-auto w-full"
          />
        </figure>
      </div>
    </section>
  );
};
