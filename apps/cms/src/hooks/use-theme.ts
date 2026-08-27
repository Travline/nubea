import { useEffect, useState } from "react";

export type Theme = "light" | "dark" | "system";

const STORAGE_KEY = "nubea-ui-theme";

export function useTheme(defaultTheme: Theme = "system") {
  const [theme, setThemeState] = useState<Theme>(() => {
    if (typeof window === "undefined") return defaultTheme;
    const stored = localStorage.getItem(STORAGE_KEY) as Theme | null;
    return stored || defaultTheme;
  });

  const [isDark, setIsDark] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    if (theme === "dark") return true;
    if (theme === "light") return false;
    return window.matchMedia("(prefers-color-scheme: dark)").matches;
  });

  useEffect(() => {
    const root = document.documentElement;
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    const applyTheme = () => {
      const darkActive =
        theme === "dark" || (theme === "system" && mediaQuery.matches);

      if (darkActive) {
        root.classList.add("dark");
      } else {
        root.classList.remove("dark");
      }
      setIsDark(darkActive);
    };

    applyTheme();

    const handleMediaChange = () => {
      if (theme === "system") {
        applyTheme();
      }
    };

    mediaQuery.addEventListener("change", handleMediaChange);
    return () => mediaQuery.removeEventListener("change", handleMediaChange);
  }, [theme]);

  const setTheme = (newTheme: Theme) => {
    localStorage.setItem(STORAGE_KEY, newTheme);
    setThemeState(newTheme);
    window.dispatchEvent(new Event("nubea-theme-change"));
  };

  const toggleTheme = () => {
    const nextTheme = isDark ? "light" : "dark";
    setTheme(nextTheme);
  };

  useEffect(() => {
    const handleStorage = (e: StorageEvent) => {
      if (e.key === STORAGE_KEY && e.newValue) {
        setThemeState(e.newValue as Theme);
      }
    };

    const handleCustomChange = () => {
      const stored = (localStorage.getItem(STORAGE_KEY) as Theme | null) || defaultTheme;
      setThemeState(stored);
    };

    window.addEventListener("storage", handleStorage);
    window.addEventListener("nubea-theme-change", handleCustomChange);
    return () => {
      window.removeEventListener("storage", handleStorage);
      window.removeEventListener("nubea-theme-change", handleCustomChange);
    };
  }, [defaultTheme]);

  return {
    theme,
    isDark,
    setTheme,
    toggleTheme,
  };
}
