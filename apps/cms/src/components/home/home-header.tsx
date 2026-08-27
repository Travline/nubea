import { Link } from "react-router-dom";
import { ThemeToggle } from "@/components/theme-toggle";

export const HomeHeader = () => {
  return (
    <header className="fixed max-w-250 m-auto top-4 left-4 right-4 md:left-0 md:right-0 flex flex-row items-center justify-between gap-4 p-4 shadow rounded-full bg-background/80 backdrop-blur-sm border border-border">
      <Link to="/" className="flex flex-row items-center justify-center gap-3">
        <div>
          <img src="/assets/logo.png" alt="Logo" className="w-15" />
        </div>
        <p className="text-2xl font-bold text-center">
          Nubea
        </p>
      </Link>
      <div className="flex items-center gap-2">
        <ThemeToggle />
      </div>
    </header>
  );
};
