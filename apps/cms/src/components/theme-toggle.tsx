import React from "react";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "@/hooks/use-theme";
import { cn } from "@/lib/utils";

interface ThemeToggleProps {
  className?: string;
  showLabels?: boolean;
}

export function ThemeToggle({ className, showLabels = false }: ThemeToggleProps) {
  const { isDark, toggleTheme } = useTheme();

  const handleKeyDown = (e: React.KeyboardEvent<HTMLButtonElement>) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      toggleTheme();
    }
  };

  return (
    <button
      type="button"
      role="switch"
      aria-checked={isDark}
      aria-label="Alternar tema oscuro o claro"
      title={isDark ? "Cambiar a modo claro" : "Cambiar a modo oscuro"}
      onClick={toggleTheme}
      onKeyDown={handleKeyDown}
      className={cn(
        "relative inline-flex h-10 w-20 shrink-0 cursor-pointer items-center rounded-full border border-border p-1 transition-colors duration-300 ease-in-out select-none",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        isDark ? "bg-muted/80" : "bg-muted/80",
        className
      )}
    >
      {/* Background track icons */}
      <span className="flex w-full items-center justify-between px-2 pointer-events-none">
        <Sun
          className={cn(
            "size-4.5 transition-opacity duration-300 text-amber-500",
            isDark ? "opacity-40" : "opacity-0"
          )}
        />
        <Moon
          className={cn(
            "size-4.5 transition-opacity duration-300 text-blue-400",
            isDark ? "opacity-0" : "opacity-40"
          )}
        />
      </span>

      {/* Sliding Thumb */}
      <span
        className={cn(
          "absolute left-1 top-1 flex size-8 items-center justify-center rounded-full bg-background shadow-md ring-0 transition-transform duration-300 ease-in-out border border-border/40",
          isDark ? "translate-x-10 bg-card text-blue-400" : "translate-x-0 bg-white text-amber-500"
        )}
      >
        <Sun
          className={cn(
            "size-5 absolute transition-all duration-300",
            isDark ? "scale-0 -rotate-90 opacity-0" : "scale-100 rotate-0 opacity-100 text-amber-500"
          )}
        />
        <Moon
          className={cn(
            "size-5 absolute transition-all duration-300",
            isDark ? "scale-100 rotate-0 opacity-100 text-blue-400" : "scale-0 rotate-90 opacity-0"
          )}
        />
      </span>

      {showLabels && (
        <span className="sr-only">
          {isDark ? "Modo oscuro activado" : "Modo claro activado"}
        </span>
      )}
    </button>
  );
}

export { ThemeToggle as ThemeSwitch };
export default ThemeToggle;
