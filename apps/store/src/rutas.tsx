import { createBrowserRouter } from 'react-router-dom'
import { Vitrina } from './vitrina'
import { CarritoTienda } from './carrito'
import { Direcciones } from './direcciones'
import { Pedidos } from './pedidos'
import { Autenticacion } from './autenticacion'
import { VistaPrevia } from './vista-previa'

export const enrutador = createBrowserRouter([
  { path: '/:slug/iniciar-sesion', element: <Autenticacion key="login" /> },
  { path: '/:slug/registro', element: <Autenticacion key="registro" registro /> },
  { path: '/:slug/pedidos/:idPedido', element: <Pedidos /> },
  { path: '/:slug/pedidos', element: <Pedidos /> },
  { path: '/:slug/direcciones', element: <Direcciones /> },
  { path: '/:slug/carrito', element: <CarritoTienda /> },
  { path: '/vista-previa/:codigo', element: <VistaPrevia /> },
  { path: '/:slug', element: <Vitrina /> },
  { path: '/:slug/productos/:idProducto', element: <Vitrina /> },
  { path: '*', element: <main className="contenedor"><h1>Tienda no encontrada</h1></main> },
], { basename: '/tienda' })
