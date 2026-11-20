import { createBrowserRouter } from 'react-router-dom'
import { Vitrina } from './vitrina'
import { CarritoTienda } from './carrito'
import { Direcciones } from './direcciones'
import { Pedidos } from './pedidos'

export const enrutador = createBrowserRouter([
  { path: '/:slug/pedidos/:idPedido', element: <Pedidos /> },
  { path: '/:slug/pedidos', element: <Pedidos /> },
  { path: '/:slug/direcciones', element: <Direcciones /> },
  { path: '/:slug/carrito', element: <CarritoTienda /> },
  { path: '/:slug', element: <Vitrina /> },
  { path: '/:slug/productos/:idProducto', element: <Vitrina /> },
  { path: '*', element: <main className="contenedor"><h1>Tienda no encontrada</h1></main> },
], { basename: '/tienda' })
