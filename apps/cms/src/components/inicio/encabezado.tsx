import { Link, useLocation } from "react-router-dom";
import { AlternarTema } from "@/components/alternar-tema";

const enlaces = [
  { etiqueta: "Funciones", destino: "#funciones" },
  { etiqueta: "Cómo funciona", destino: "#como-funciona" },
  { etiqueta: "Plantillas", destino: "#plantillas" },
];

const logo = `${import.meta.env.BASE_URL}assets/logo.webp`;

export const EncabezadoInicio = () => {
  const { pathname } = useLocation();

  // En la portada el encabezado va transparente, encima del video de fondo.
  if (pathname === "/") {
    return (
      <header className="fixed inset-x-0 top-0 z-50 border-b border-white/10 bg-nubea-fondo/80 shadow-[0_8px_30px_rgba(0,0,0,0.25)] backdrop-blur-md">
        <nav
          aria-label="Principal"
          className="mx-auto flex h-[88px] max-w-[1440px] items-center justify-between gap-6 px-5 md:px-[120px]"
        >
          <Link to="/" className="flex items-center gap-3 text-white">
            <img src={logo} alt="" className="h-[30px] w-auto" />
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
  }

  // Resto de páginas públicas (iniciar sesión, registro): se mantiene el diseño anterior.
  return (
    <header className="fixed z-50 max-w-250 m-auto top-4 left-4 right-4 md:left-0 md:right-0 flex flex-row items-center justify-between gap-4 p-4 shadow rounded-full bg-background/75 backdrop-blur-sm border border-border">
      <Link to="/" className="flex flex-row items-center justify-center gap-3">
        <div>
          <img src={logo} alt="Logo" className="w-15" />
        </div>
        <p className="text-2xl font-bold text-center">
          Nubea
        </p>
      </Link>
      <div className="flex items-center gap-2">
        <AlternarTema />
      </div>
    </header>
  );
};
