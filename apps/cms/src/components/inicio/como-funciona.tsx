import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";

const pasos = [
  {
    numero: "01",
    titulo: "Crea tu cuenta",
    descripcion: "Regístrate y crea tu tienda: elige su nombre, su enlace y una plantilla.",
  },
  {
    numero: "02",
    titulo: "Publica tus productos",
    descripcion: "Agrega categorías, productos con fotos y presentaciones con su precio y stock.",
  },
  {
    numero: "03",
    titulo: "Comienza a vender",
    descripcion: "Vincula Mercado Pago, comparte el enlace de tu tienda y recibe tus pedidos.",
  },
];

export const ComoFuncionaInicio = () => (
  <section
    id="como-funciona"
    className="scroll-mt-[88px] bg-nubea-fondo px-5 pb-[clamp(88px,10vw,140px)] text-nubea-texto"
  >
    <div className="mx-auto max-w-[1200px]">
      <div className="mx-auto max-w-[900px] text-center">
        <p className="text-[13px] font-semibold tracking-[0.2em] text-nubea-acento uppercase">Cómo funciona</p>
        <h2 className="mt-4 font-display text-[clamp(40px,4.6vw,64px)] leading-[1.05] tracking-[-0.01em]">
          De la idea a tu primera venta, en <span className="italic text-nubea-acento">tres pasos</span>.
        </h2>
      </div>

      <ol className="mt-[clamp(48px,6vw,72px)] grid gap-10 md:grid-cols-3 md:gap-6">
        {pasos.map((paso) => (
          <li key={paso.numero} className="border-t border-nubea-acento/40 pt-8">
            <p className="font-display text-[72px] leading-none text-nubea-acento">{paso.numero}</p>
            <h3 className="mt-6 text-[22px] font-semibold">{paso.titulo}</h3>
            <p className="mt-3 text-[17px] leading-[1.6] text-nubea-texto-suave">{paso.descripcion}</p>
          </li>
        ))}
      </ol>

      {/* Llamado final */}
      <div className="mt-[clamp(88px,10vw,140px)] flex flex-col items-start justify-between gap-8 rounded-[32px] bg-linear-to-r from-nubea-petroleo via-[#173E8A] to-[#3D6FE0] p-[clamp(32px,6vw,80px)] md:flex-row md:items-center">
        <div className="max-w-[620px]">
          <h2 className="font-display text-[clamp(38px,4.2vw,60px)] leading-[1.05]">Tu tienda te está esperando.</h2>
          <p className="mt-4 text-[18px] text-nubea-texto/75">
            Crea tu cuenta y publica tus primeros productos hoy mismo.
          </p>
        </div>
        <Link
          to="/registro"
          className="group inline-flex h-[58px] shrink-0 items-center gap-2.5 rounded-full bg-nubea-fondo px-8 text-[16px] font-semibold text-nubea-texto transition hover:bg-black"
        >
          Crear mi tienda
          <ArrowRight className="size-4 transition-transform group-hover:translate-x-1" />
        </Link>
      </div>
    </div>
  </section>
);
