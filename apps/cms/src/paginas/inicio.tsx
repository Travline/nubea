import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { PortadaInicio } from "@/components/inicio/portada";
import { PanelInicio } from "@/components/inicio/panel";
import { FuncionesInicio } from "@/components/inicio/funciones";
import { ComoFuncionaInicio } from "@/components/inicio/como-funciona";

export const Inicio = () => {
  const { hash } = useLocation();

  // Si se llega desde otra página con un enlace como /#funciones,
  // se baja a esa sección cuando la página ya se dibujó.
  useEffect(() => {
    if (!hash) return;
    const seccion = document.getElementById(decodeURIComponent(hash.slice(1)));
    seccion?.scrollIntoView();
  }, [hash]);

  return (
    <div>
      <PortadaInicio />
      <PanelInicio />
      <FuncionesInicio />
      <ComoFuncionaInicio />
    </div>
  );
};
