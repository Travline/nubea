import { Navigate, createBrowserRouter } from "react-router-dom";
import { Inicio } from "./paginas/inicio";
import { PaginaNoEncontrada } from "@/paginas/no-encontrada";
import { DisenoPublico } from "./disenos/publico";
import { PaginaInicioSesion } from "./paginas/inicio-sesion";
import { PaginaRegistro } from "./paginas/registro";
import { RutaProtegida } from "@/autenticacion/ruta-protegida";
import { Panel, InicioPanel, MisTiendas, EspacioTienda, ResumenTienda } from "@/paginas/panel";
import { PlantillasVendedor, AyudaVendedor } from "@/paginas/panel-secciones";
import { PerfilUsuario } from "@/paginas/perfil";

import { InventarioTienda } from '@/comercio/inventario'
import { PedidosTienda } from '@/comercio/pedidos'
import { Apariencia } from '@/comercio/apariencia'
import { Pagos, RetornoPago } from '@/comercio/pagos'
import { Catalogo, DetalleProducto } from '@/comercio/catalogo'

import { TiendaAdministradaVista } from '@/administracion/tienda'
import { Administracion, ResumenAdministracion, UsuariosAdministracion, TiendasAdministracion, PlantillasAdministracion, ImpuestosAdministracion } from '@/administracion/panel'
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
      { path: "tiendas", element: <MisTiendas /> },
      { path: "plantillas", element: <PlantillasVendedor /> },
      { path: "ayuda", element: <AyudaVendedor /> },
      { path: "perfil", element: <PerfilUsuario /> },
      { path: "administracion", element: <Administracion />, children: [
        { index: true, element: <ResumenAdministracion /> },
        { path: "usuarios", element: <UsuariosAdministracion /> },
        { path: "tiendas", element: <TiendasAdministracion /> },
        { path: "plantillas", element: <PlantillasAdministracion /> },
        { path: "impuestos", element: <ImpuestosAdministracion /> },
        // Contenido de una tienda en solo lectura; sin pedidos ni pagos.
        { path: "tiendas/:idTienda", element: <TiendaAdministradaVista />, children: [
          { index: true, element: <Navigate to="catalogo" replace /> },
          { path: "catalogo", element: <Catalogo /> },
          { path: "catalogo/productos/:idProducto", element: <DetalleProducto /> },
          { path: "inventario", element: <InventarioTienda /> },
          { path: "apariencia", element: <Apariencia /> },
          { path: "cuenta-pago", element: <Pagos /> },
        ] }
      ] },
      { path: "retorno-pago", element: <RetornoPago /> },
      { path: "tiendas/:idTienda", element: <EspacioTienda />, children: [
        { index: true, element: <ResumenTienda /> },
        { path: "catalogo", element: <Catalogo /> },
        { path: "catalogo/productos/:idProducto", element: <DetalleProducto /> },
        { path: "inventario", element: <InventarioTienda /> },
        { path: "pedidos", element: <PedidosTienda /> },
        { path: "apariencia", element: <Apariencia /> },
        { path: "pagos", element: <Pagos /> },
      ] }
    ] }]
  },
  {
    path: "*", element: <PaginaNoEncontrada />
  }
], { basename: "/plataforma" })
