import { Link } from "react-router-dom";
import { EnlaceSeccion } from "@/components/inicio/enlace-seccion";

const base = import.meta.env.BASE_URL;

type Enlace = { etiqueta: string; href: string } | { etiqueta: string; to: string } | { etiqueta: string; seccion: string };

const columnas: { titulo: string; enlaces: Enlace[] }[] = [
  {
    titulo: "Producto",
    enlaces: [
      { etiqueta: "Tu panel", seccion: "panel" },
      { etiqueta: "Funciones", seccion: "funciones" },
      { etiqueta: "Cómo funciona", seccion: "como-funciona" },
    ],
  },
  {
    titulo: "Cuenta",
    enlaces: [
      { etiqueta: "Crear tienda", to: "/registro" },
      { etiqueta: "Iniciar sesión", to: "/iniciar-sesion" },
    ],
  },
  {
    titulo: "Legal",
    enlaces: [
      { etiqueta: "Términos y Condiciones", href: `${base}terminos` },
      { etiqueta: "Política de Privacidad", href: `${base}privacidad` },
      { etiqueta: "Contacto", href: `${base}contacto` },
    ],
  },
];

const claseEnlace = "text-[15px] text-nubea-texto-suave transition-colors hover:text-nubea-texto";

export const PieInicio = () => {
  return (
    <footer className="bg-nubea-fondo text-nubea-texto">
      {/* Línea superior con brillo */}
      <div aria-hidden="true" className="h-px w-full bg-linear-to-r from-transparent via-nubea-acento/50 to-transparent" />

      <div className="mx-auto max-w-[1200px] px-5 pt-[clamp(56px,7vw,80px)]">
        <div className="grid gap-12 md:grid-cols-[1.4fr_1fr_1fr_1fr]">
          <div className="max-w-[340px]">
            <Link to="/" className="flex items-center gap-3">
              <img src={`${base}assets/logo.webp`} alt="" className="h-[30px] w-auto" />
              <span className="text-[22px] font-bold tracking-tight">Nubea</span>
            </Link>
            <p className="mt-4 text-[15px] leading-[1.6] text-nubea-texto-suave">
              La plataforma para llevar tu tienda a todos lados.
            </p>
            <Link
              to="/registro"
              className="mt-6 inline-flex h-[42px] items-center rounded-full bg-nubea-acento px-5 text-[14px] font-semibold text-nubea-sobre-acento transition hover:brightness-110"
            >
              Crear mi tienda
            </Link>
          </div>

          {columnas.map((columna) => (
            <nav key={columna.titulo} aria-label={columna.titulo}>
              <p className="text-[13px] font-semibold tracking-[0.2em] text-nubea-acento uppercase">{columna.titulo}</p>
              <ul className="mt-5 flex flex-col gap-3.5">
                {columna.enlaces.map((enlace) => (
                  <li key={enlace.etiqueta}>
                    {"seccion" in enlace ? (
                      <EnlaceSeccion seccion={enlace.seccion} className={claseEnlace}>{enlace.etiqueta}</EnlaceSeccion>
                    ) : "to" in enlace ? (
                      <Link to={enlace.to} className={claseEnlace}>{enlace.etiqueta}</Link>
                    ) : (
                      <a href={enlace.href} className={claseEnlace}>{enlace.etiqueta}</a>
                    )}
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>

        <div className="mt-14 flex flex-col gap-3 border-t border-white/8 py-7 text-[13px] text-nubea-texto-suave sm:flex-row sm:items-center">
          <p>© {new Date().getFullYear()} Nubea. Todos los derechos reservados.</p>
        </div>
      </div>

    </footer>
  );
};
