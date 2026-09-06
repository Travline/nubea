// El logo.png luego se cambiará por algo como una captura de un template

import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { ArrowRight } from "lucide-react";
import { Aurora, Shader } from 'shaders/react'

export const HomeBanner = () => {
  return (
    <div className="p-5 pt-35 sm:pt-0 w-full min-h-dvh max-w-250 m-auto flex flex-col gap-10 items-center md:flex-row md:justify-between text-foreground">
      <Shader className="absolute top-0 left-0 w-full h-full z-[-1]">
        <Aurora
          waviness={0}
          height={60}
          speed={15}
          colorA="#6366F1"
          colorB="#8B5CF6"
          colorC="#EC4899"
          curtainCount={3}
        />
      </Shader>
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
        <img src="/assets/logo.webp" alt="Logo" className="w-full h-auto" />
      </div>
    </div>
  );
};