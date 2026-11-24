import type { MouseEvent, ReactNode } from "react";
import { useLocation, useNavigate } from "react-router-dom";

/*
 * Enlace a una sección de la página principal SIN "#" en la dirección.
 * - En la página principal baja suavemente a la sección.
 * - Desde otra página (iniciar sesión, términos…) vuelve a la principal y baja a la sección.
 */
export function EnlaceSeccion({ seccion, className, children }: { seccion: string; className?: string; children: ReactNode }) {
  const navegar = useNavigate();
  const { pathname } = useLocation();

  function irASeccion(evento: MouseEvent<HTMLAnchorElement>) {
    // Ctrl/Cmd + clic o clic central: dejar que el navegador abra otra pestaña.
    if (evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.button !== 0) return;
    evento.preventDefault();
    if (pathname === "/") document.getElementById(seccion)?.scrollIntoView({ behavior: "smooth" });
    else navegar("/", { state: { seccion } });
  }

  return (
    <a href={import.meta.env.BASE_URL} onClick={irASeccion} className={className}>
      {children}
    </a>
  );
}
