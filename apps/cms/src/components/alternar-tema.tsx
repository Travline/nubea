import React from "react";
import { Moon, Sun } from "lucide-react";
import { useTema } from "@/hooks/use-tema";
import { cn } from "@/lib/utils";

interface PropiedadesAlternarTema {
  className?: string;
  mostrarEtiquetas?: boolean;
}

export function AlternarTema({ className, mostrarEtiquetas = false }: PropiedadesAlternarTema) {
  const { esOscuro, alternarTema } = useTema();

  const manejarTecla = (e: React.KeyboardEvent<HTMLButtonElement>) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      alternarTema();
    }
  };

  return (
    <button
      type="button"
      role="switch"
      aria-checked={esOscuro}
      aria-label="Alternar tema oscuro o claro"
      title={esOscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro"}
      onClick={alternarTema}
      onKeyDown={manejarTecla}
      className={cn(
        "relative inline-flex h-10 w-20 shrink-0 cursor-pointer items-center rounded-full border border-border p-1 transition-colors duration-300 ease-in-out select-none",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        esOscuro ? "bg-muted/80" : "bg-muted/80",
        className
      )}
    >
      {/* Iconos del fondo */}
      <span className="flex w-full items-center justify-between px-2 pointer-events-none">
        <Sun
          className={cn(
            "size-4.5 transition-opacity duration-300 text-amber-500",
            esOscuro ? "opacity-40" : "opacity-0"
          )}
        />
        <Moon
          className={cn(
            "size-4.5 transition-opacity duration-300 text-blue-400",
            esOscuro ? "opacity-0" : "opacity-40"
          )}
        />
      </span>

      {/* Control deslizante */}
      <span
        className={cn(
          "absolute left-1 top-1 flex size-8 items-center justify-center rounded-full bg-background shadow-md ring-0 transition-transform duration-300 ease-in-out border border-border/40",
          esOscuro ? "translate-x-10 bg-card text-blue-400" : "translate-x-0 bg-white text-amber-500"
        )}
      >
        <Sun
          className={cn(
            "size-5 absolute transition-all duration-300",
            esOscuro ? "scale-0 -rotate-90 opacity-0" : "scale-100 rotate-0 opacity-100 text-amber-500"
          )}
        />
        <Moon
          className={cn(
            "size-5 absolute transition-all duration-300",
            esOscuro ? "scale-100 rotate-0 opacity-100 text-blue-400" : "scale-0 rotate-90 opacity-0"
          )}
        />
      </span>

      {mostrarEtiquetas && (
        <span className="sr-only">
          {esOscuro ? "Modo oscuro activado" : "Modo claro activado"}
        </span>
      )}
    </button>
  );
}

export { AlternarTema as InterruptorTema };
export default AlternarTema;
