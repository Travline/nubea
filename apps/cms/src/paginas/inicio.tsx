import { PortadaInicio } from "@/components/inicio/portada";
import { PanelInicio } from "@/components/inicio/panel";
import { FuncionesInicio } from "@/components/inicio/funciones";
import { ComoFuncionaInicio } from "@/components/inicio/como-funciona";

export const Inicio = () => {
  return (
    <div>
      <PortadaInicio />
      <PanelInicio />
      <FuncionesInicio />
      <ComoFuncionaInicio />
    </div>
  );
};
