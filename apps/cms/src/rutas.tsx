import { createBrowserRouter } from "react-router-dom";
import { Inicio } from "./paginas/inicio";
import { PaginaNoEncontrada } from "@/paginas/no-encontrada";
import { DisenoPublico } from "./disenos/publico";
import { PaginaInicioSesion } from "./paginas/inicio-sesion";
import { PaginaRegistro } from "./paginas/registro";
import { RutaProtegida } from "@/autenticacion/ruta-protegida";
import { Panel } from "@/paginas/panel";

export const enrutador = createBrowserRouter([
  {
    path: "/", element: <DisenoPublico />,
    children: [
      { index: true, element: <Inicio /> },
      { path: "iniciar-sesion", element: <PaginaInicioSesion /> },
      { path: "registro", element: <PaginaRegistro /> }
    ]
  },
  {
    element: <RutaProtegida />,
    children: [{ path: "/panel", element: <Panel /> }]
  },
  {
    path: "*", element: <PaginaNoEncontrada />
  }
])
