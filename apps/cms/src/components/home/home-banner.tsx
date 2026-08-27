// El logo.png luego se cambiará por algo como una captura de un template

import { Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { ArrowRight } from "lucide-react";

export const HomeBanner = () => {
  const navigate = useNavigate();

  return (
    <div className="p-5 pt-35 sm:pt-0 w-full min-h-dvh max-w-250 m-auto flex flex-col gap-10 items-center md:flex-row md:justify-between bg-background text-foreground">
      <div className="flex flex-col gap-5 items-center md:items-start">
        <h1 className="text-6xl font-bold text-center md:text-left">Tu tienda online, sin vueltas</h1>
        <p className="text-lg text-center md:text-left text-muted-foreground">
          Crea y gestiona tu tienda online de forma rápida y sencilla. Sin complicaciones técnicas, para que puedas enfocarte en lo que realmente importa.
        </p>
        <div className="flex flex-col sm:flex-row items-center gap-4 w-full md:w-auto">
          <Button
            size="lg"
            className="text-base font-semibold px-8 py-6 rounded-2xl group shadow-lg w-full sm:w-auto cursor-pointer"
          >
            <Link to="/register">Crear tienda</Link>
            <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
          </Button>
          <Button
            variant="outline"
            size="lg"
            className="text-base font-semibold px-8 py-6 rounded-2xl group border-border shadow-sm hover:shadow-md transition-all w-full sm:w-auto cursor-pointer"
          >
            <Link to="/dashboard">Gestionar tienda</Link>
          </Button>
        </div>
      </div>
      <div className="max-w-100">
        <img src="/assets/logo.png" alt="Logo" className="w-full h-auto" />
      </div>
    </div>
  );
};