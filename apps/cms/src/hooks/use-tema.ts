import { useEffect, useState } from "react";

export type Tema = "light" | "dark" | "system";

const CLAVE_ALMACENAMIENTO = "nubea-ui-theme";

export function useTema(temaPredeterminado: Tema = "system") {
  const [tema, establecerEstadoTema] = useState<Tema>(() => {
    if (typeof window === "undefined") return temaPredeterminado;
    const guardado = localStorage.getItem(CLAVE_ALMACENAMIENTO) as Tema | null;
    return guardado || temaPredeterminado;
  });

  const [esOscuro, establecerEsOscuro] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    if (tema === "dark") return true;
    if (tema === "light") return false;
    return window.matchMedia("(prefers-color-scheme: dark)").matches;
  });

  useEffect(() => {
    const raiz = document.documentElement;
    const consultaMedios = window.matchMedia("(prefers-color-scheme: dark)");

    const aplicarTema = () => {
      const oscuroActivo =
        tema === "dark" || (tema === "system" && consultaMedios.matches);

      if (oscuroActivo) {
        raiz.classList.add("dark");
      } else {
        raiz.classList.remove("dark");
      }
      establecerEsOscuro(oscuroActivo);
    };

    aplicarTema();

    const manejarCambioMedios = () => {
      if (tema === "system") {
        aplicarTema();
      }
    };

    consultaMedios.addEventListener("change", manejarCambioMedios);
    return () => consultaMedios.removeEventListener("change", manejarCambioMedios);
  }, [tema]);

  const establecerTema = (nuevoTema: Tema) => {
    localStorage.setItem(CLAVE_ALMACENAMIENTO, nuevoTema);
    establecerEstadoTema(nuevoTema);
    window.dispatchEvent(new Event("nubea-theme-change"));
  };

  const alternarTema = () => {
    const siguienteTema = esOscuro ? "light" : "dark";
    establecerTema(siguienteTema);
  };

  useEffect(() => {
    const manejarAlmacenamiento = (e: StorageEvent) => {
      if (e.key === CLAVE_ALMACENAMIENTO && e.newValue) {
        establecerEstadoTema(e.newValue as Tema);
      }
    };

    const manejarCambioPersonalizado = () => {
      const guardado = (localStorage.getItem(CLAVE_ALMACENAMIENTO) as Tema | null) || temaPredeterminado;
      establecerEstadoTema(guardado);
    };

    window.addEventListener("storage", manejarAlmacenamiento);
    window.addEventListener("nubea-theme-change", manejarCambioPersonalizado);
    return () => {
      window.removeEventListener("storage", manejarAlmacenamiento);
      window.removeEventListener("nubea-theme-change", manejarCambioPersonalizado);
    };
  }, [temaPredeterminado]);

  return {
    tema,
    esOscuro,
    establecerTema,
    alternarTema,
  };
}
