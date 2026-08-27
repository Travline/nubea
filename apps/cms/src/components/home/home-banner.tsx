// El logo.png luego se cambiará por algo como una captura de un template

import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";

export const HomeBanner = () => {
  const navigate = useNavigate();

  return (
    <div className="p-5 pt-35 sm:pt-0 w-full min-h-dvh max-w-250 m-auto flex flex-col gap-10 items-center md:flex-row md:justify-between bg-background text-foreground">
      <div className="flex flex-col gap-5 items-center md:items-start">
        <h1 className="text-6xl font-bold text-center md:text-left">Tu tienda online, sin vueltas</h1>
        <p className="text-lg text-center md:text-left text-muted-foreground">
          Crea y gestiona tu tienda online de forma rápida y sencilla. Sin complicaciones técnicas, para que puedas enfocarte en lo que realmente importa.
        </p>
        <div>
          <Button onClick={() => navigate("/register")} variant="default" size="lg" className="w-full md:w-auto text-lg">
            Crear tienda
          </Button>
          <Button onClick={() => navigate("/dashboard")} variant="outline" size="lg" className="w-full md:w-auto text-lg ml-0 md:ml-4 mt-4 md:mt-0">
            Gestionar tienda
          </Button>
        </div>
      </div>
      <div className="max-w-100">
        <img src="/assets/logo.png" alt="Logo" className="w-full h-auto" />
      </div>
    </div>
  );
};