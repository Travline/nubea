import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { PortadaInicio } from "@/components/inicio/portada";
import { PanelInicio } from "@/components/inicio/panel";
import { FuncionesInicio } from "@/components/inicio/funciones";
import { ComoFuncionaInicio } from "@/components/inicio/como-funciona";
import { PlantillasInicio } from "@/components/inicio/plantillas";

export const Inicio = () => {
  const { hash, state } = useLocation();
  const navegar = useNavigate();
  const destino = (state as { seccion?: string } | null)?.seccion ?? (hash ? decodeURIComponent(hash.slice(1)) : "");

  // Si se llega desde otra página (o con un enlace antiguo como /#funciones),
  // se baja a esa sección y se limpia la dirección para que no quede ningún "#".
  useEffect(() => {
    if (!destino) return;
    document.getElementById(destino)?.scrollIntoView({ behavior: "smooth" });
    navegar("/", { replace: true, state: null });
  }, [destino, navegar]);

  return (
    <div>
      <PortadaInicio />
      <PanelInicio />
      <FuncionesInicio />
      <PlantillasInicio />
      <ComoFuncionaInicio />
    </div>
  );
};
