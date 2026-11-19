import { PieInicio } from "@/components/inicio/pie"
import { EncabezadoInicio } from "@/components/inicio/encabezado"
import { Outlet } from "react-router-dom"

export const DisenoPublico = () => {
  return (
    <>
      <EncabezadoInicio />
      <Outlet />
      <PieInicio />
    </>
  )
}