import { createBrowserRouter } from "react-router-dom";
import { Inicio } from "./paginas/inicio";
import { PaginaNoEncontrada } from "@/paginas/no-encontrada";
import { DisenoPublico } from "./disenos/publico";
import { PaginaInicioSesion } from "./paginas/inicio-sesion";
import { PaginaRegistro } from "./paginas/registro";
import { RutaProtegida } from "@/autenticacion/ruta-protegida";
import { Panel, InicioPanel, EspacioTienda, ResumenTienda } from "@/paginas/panel";

import { InventarioTienda } from '@/comercio/inventario'
import { Apariencia } from '@/comercio/apariencia'
import { Pagos, RetornoPago } from '@/comercio/pagos'
import { Catalogo, DetalleProducto } from '@/comercio/catalogo'

import { Administracion, ResumenAdministracion, UsuariosAdministracion, TiendasAdministracion, PlantillasAdministracion } from '@/administracion/panel'
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
    children: [{ path: "/panel", element: <Panel />, children: [
      { index: true, element: <InicioPanel /> },
      { path: "administracion", element: <Administracion />, children: [
        { index: true, element: <ResumenAdministracion /> },
        { path: "usuarios", element: <UsuariosAdministracion /> },
        { path: "tiendas", element: <TiendasAdministracion /> },
        { path: "plantillas", element: <PlantillasAdministracion /> }
      ] },
      { path: "retorno-pago", element: <RetornoPago /> },
      { path: "tiendas/:idTienda", element: <EspacioTienda />, children: [
        { index: true, element: <ResumenTienda /> },
        { path: "catalogo", element: <Catalogo /> },
        { path: "catalogo/productos/:idProducto", element: <DetalleProducto /> },
        { path: "inventario", element: <InventarioTienda /> },
        { path: "apariencia", element: <Apariencia /> },
        { path: "pagos", element: <Pagos /> },
      ] }
    ] }]
  },
  {
    path: "*", element: <PaginaNoEncontrada />
  }
])
