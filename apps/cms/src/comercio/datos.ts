import { solicitar } from '@/autenticacion/api'

export interface Tienda { idTienda: string; idPropietario: string; nombre: string; slug: string; estado: string; plantilla: string }
export interface Categoria { idCategoria: string; nombre: string; slug: string; activa: boolean }
export interface Coleccion { idColeccion: string; nombre: string; slug: string; activa: boolean }
export interface Producto { idProducto: string; idCategoria: string; nombre: string; descripcion: string; precioBase: number; estado: string }
export interface Variante { idVariante: string; nombre: string }
export interface Valor { idValor: string; valor: string }
export interface Presentacion { idPresentacion: string; sku: string; precioOverride: number | null; precioEfectivo: number; estado: string; idsValores: string[] }
export interface Imagen { idImagen: string; url: string; orden: number; esPrincipal: boolean }
export interface Plantilla { idPlantilla: number; codigo: string; nombre: string; previewUrl: string | null }
export interface Configuracion { plantilla: Plantilla; tituloInicio: string; descripcionInicio: string | null; mostrarCarrusel: boolean }
export interface Banner { idBanner: string; url: string; enlaceDestino: string | null; orden: number; activo: boolean }
export interface Inventario { idPresentacion: string; nombreProducto: string; sku: string; estadoProducto: string; estadoPresentacion: string; stockActual: number | null; stockMinimo: number | null; sinRegistroInventario: boolean }
export interface PaginaInventario { contenido: Inventario[]; page: number; totalPaginas: number; totalElementos: number }
export interface CuentaPago { estado: string; proveedor: string; idCuentaExterna: string | null }
export const rutaTienda = (id: string) => `/api/tiendas/${encodeURIComponent(id)}`
export const api = {
  leer: <T>(ruta: string) => solicitar<T>('GET', ruta),
  crear: <T>(ruta: string, cuerpo?: object) => solicitar<T>('POST', ruta, cuerpo),
  actualizar: <T>(ruta: string, cuerpo?: object) => solicitar<T>('PUT', ruta, cuerpo),
  modificar: <T>(ruta: string, cuerpo?: object) => solicitar<T>('PATCH', ruta, cuerpo),
  eliminar: <T>(ruta: string) => solicitar<T>('DELETE', ruta),
}
