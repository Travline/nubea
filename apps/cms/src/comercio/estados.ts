// Único lugar con los textos que ve el vendedor para cada estado de la base de datos
// (glosario de docs/revision/guia-rediseno.md). Ningún estado se muestra en mayúsculas.

type Etiqueta = { texto: string; tono: Tono }
export type Tono = 'ok' | 'aviso' | 'neutro' | 'error' | 'info'

const desconocido = (valor: string | null | undefined): Etiqueta => ({ texto: valor ? valor.charAt(0) + valor.slice(1).toLowerCase().replace(/_/g, ' ') : '—', tono: 'neutro' })

const tienda: Record<string, Etiqueta> = {
  BORRADOR: { texto: 'En preparación', tono: 'aviso' },
  ACTIVA: { texto: 'Abierta', tono: 'ok' },
  SUSPENDIDA: { texto: 'Suspendida', tono: 'error' },
  CERRADA: { texto: 'Cerrada', tono: 'neutro' },
}
const producto: Record<string, Etiqueta> = {
  BORRADOR: { texto: 'Borrador', tono: 'aviso' },
  PUBLICADO: { texto: 'Visible en tu tienda', tono: 'ok' },
  INACTIVO: { texto: 'Oculto', tono: 'neutro' },
}
const version: Record<string, Etiqueta> = {
  ACTIVA: { texto: 'A la venta', tono: 'ok' },
  INACTIVA: { texto: 'Pausada', tono: 'neutro' },
}
const pedido: Record<string, Etiqueta> = {
  PENDIENTE_PAGO: { texto: 'Esperando pago', tono: 'aviso' },
  PAGADO: { texto: 'Pagado', tono: 'ok' },
  CANCELADO: { texto: 'Cancelado', tono: 'neutro' },
}
const pago: Record<string, Etiqueta> = {
  PENDIENTE: { texto: 'Esperando pago', tono: 'aviso' },
  APROBADO: { texto: 'Cobrado', tono: 'ok' },
  RECHAZADO: { texto: 'Rechazado', tono: 'error' },
  REEMBOLSADO: { texto: 'Dinero devuelto', tono: 'neutro' },
}
const cuentaPago: Record<string, Etiqueta> = {
  PENDIENTE: { texto: 'Sin conectar', tono: 'neutro' },
  ACTIVA: { texto: 'Lista para cobrar', tono: 'ok' },
  ERROR: { texto: 'Hay un problema', tono: 'error' },
  DESCONECTADA: { texto: 'Desconectada', tono: 'neutro' },
}
const rol: Record<string, string> = {
  DUENO_TIENDA: 'Vendedor',
  ADMIN_GENERAL: 'Administrador de Nubea',
  CLIENTE: 'Comprador',
}
const cuentaUsuario: Record<string, string> = { ACTIVO: 'Activa', SUSPENDIDO: 'Suspendida', CIERRE_PENDIENTE: 'Cierre en curso', CERRADO: 'Cerrada' }

export const etiquetaTienda = (estado: string) => tienda[estado] ?? desconocido(estado)
export const etiquetaProducto = (estado: string) => producto[estado] ?? desconocido(estado)
export const etiquetaVersion = (estado: string) => version[estado] ?? desconocido(estado)
export const etiquetaPedido = (estado: string) => pedido[estado] ?? desconocido(estado)
export const etiquetaPago = (estado: string | null | undefined) => (estado && pago[estado]) || { texto: 'Aún sin pago', tono: 'neutro' as Tono }
export const etiquetaCuentaPago = (estado: string) => cuentaPago[estado] ?? desconocido(estado)
export const textoRol = (codigo: string) => rol[codigo] ?? desconocido(codigo).texto
export const textoCuentaUsuario = (estado: string) => cuentaUsuario[estado] ?? desconocido(estado).texto

/** Ambiente con el que cobra la tienda (CuentaPagoResponse.ambientePago y el pago de un pedido). */
export const textoAmbientePago = (ambiente: string | null | undefined) =>
  ambiente === 'PRUEBA' ? 'Modo prueba' : ambiente === 'PRODUCCION' ? 'Cobros reales' : null
export const explicacionAmbientePago = (ambiente: string | null | undefined) =>
  ambiente === 'PRUEBA' ? 'Los pagos son de prueba: no se mueve dinero real.' : ambiente === 'PRODUCCION' ? 'Los pagos de tus clientes son reales.' : 'Aún no se ha configurado cómo cobra tu tienda.'

/** Filtros de listas: valores de la BD con su texto. */
export const opcionesTienda = Object.entries(tienda).map(([valor, e]) => ({ valor, texto: e.texto }))
export const opcionesProducto = Object.entries(producto).map(([valor, e]) => ({ valor, texto: e.texto }))

/** Una tienda suspendida o cerrada solo se puede consultar. */
export const tiendaEditable = (estado: string) => estado === 'ACTIVA' || estado === 'BORRADOR'
export function avisoTiendaNoEditable(estado: string) {
  return estado === 'SUSPENDIDA'
    ? { titulo: 'Tu tienda está suspendida', texto: 'Tus clientes no pueden verla ni comprar, y por ahora no puedes hacer cambios. Puedes consultar tu catálogo, inventario y pedidos. Si crees que es un error, escribe a soporte de Nubea.' }
    : { titulo: 'Tu tienda está cerrada', texto: 'Ya no está disponible para tus clientes y no admite cambios. Puedes consultar su información.' }
}

/** Clases de color por tono, con las mismas tonalidades que ya usa el panel. */
export const claseTono: Record<Tono, { clase: string; punto: string }> = {
  ok: { clase: 'bg-emerald-500/12 text-emerald-700 dark:text-emerald-400', punto: 'bg-emerald-500' },
  aviso: { clase: 'bg-amber-500/15 text-amber-700 dark:text-amber-400', punto: 'bg-amber-500' },
  neutro: { clase: 'bg-zinc-500/15 text-zinc-600 dark:text-zinc-400', punto: 'bg-zinc-400' },
  error: { clase: 'bg-red-500/12 text-red-600 dark:text-red-400', punto: 'bg-red-500' },
  info: { clase: 'bg-primary/10 text-primary dark:text-[#8fb3ff]', punto: 'bg-primary' },
}
