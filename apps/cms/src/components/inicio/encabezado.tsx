import { Link } from "react-router-dom";

const base = import.meta.env.BASE_URL;

// Enlaces a secciones de la página principal. Llevan la ruta base para que
// funcionen también desde Iniciar sesión o Registro.
const enlaces = [
  { etiqueta: "Funciones", destino: `${base}#funciones` },
  { etiqueta: "Cómo funciona", destino: `${base}#como-funciona` },
  { etiqueta: "Plantillas", destino: `${base}#plantillas` },
];

// Mismo encabezado para todas las páginas públicas (inicio, iniciar sesión y registro).
export const EncabezadoInicio = () => {
  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b border-white/10 bg-nubea-fondo/80 shadow-[0_8px_30px_rgba(0,0,0,0.25)] backdrop-blur-md">
      <nav
        aria-label="Principal"
        className="mx-auto flex h-[88px] max-w-[1440px] items-center justify-between gap-6 px-5 md:px-[120px]"
      >
        <Link to="/" className="flex items-center gap-3 text-white">
          <img src={`${base}assets/logo.webp`} alt="" className="h-[30px] w-auto" />
          <span className="text-[22px] font-bold tracking-tight">Nubea</span>
        </Link>

        <ul className="hidden items-center gap-10 text-[15px] text-white/70 lg:flex">
          {enlaces.map((enlace) => (
            <li key={enlace.destino}>
              <a href={enlace.destino} className="transition-colors hover:text-white">
                {enlace.etiqueta}
              </a>
            </li>
          ))}
        </ul>

        <div className="flex items-center gap-6 text-[15px]">
          <Link
            to="/iniciar-sesion"
            className="hidden text-white/90 transition-colors hover:text-white sm:inline"
          >
            Iniciar sesión
          </Link>
          <Link
            to="/registro"
            className="inline-flex h-[42px] items-center rounded-full bg-nubea-acento px-5 font-semibold text-nubea-sobre-acento transition hover:brightness-110"
          >
            Crear tienda
          </Link>
        </div>
      </nav>
    </header>
  );
};
