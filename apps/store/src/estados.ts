// Etiquetas de estados para el comprador, en un solo lugar (glosario de docs/revision/guia-rediseno.md).
// La API devuelve los códigos de la BD; la pantalla nunca los muestra tal cual.

const PEDIDO: Record<string, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  PAGADO: 'Pago confirmado',
  CANCELADO: 'Cancelado',
}

const PAGO: Record<string, string> = {
  PENDIENTE: 'Esperando confirmación del pago',
  APROBADO: 'Pagado',
  RECHAZADO: 'Pago rechazado',
  CANCELADO: 'Pago cancelado',
  REEMBOLSO_PENDIENTE: 'Te estamos devolviendo el dinero',
  REEMBOLSADO: 'Dinero devuelto',
}

/** Clase visual de la etiqueta del pedido (ver .etiqueta-estado en global.css). */
const CLASE_PEDIDO: Record<string, string> = { PENDIENTE_PAGO: 'pendiente', PAGADO: 'confirmado', CANCELADO: 'cancelado' }

export const textoEstadoPedido = (estado: string) => PEDIDO[estado] ?? 'En proceso'
export const claseEstadoPedido = (estado: string) => CLASE_PEDIDO[estado] ?? 'pendiente'
export const textoEstadoPago = (estado: string | null) => (estado && PAGO[estado]) || 'Preparando el pago'

/** Disponibilidad de una opción del producto (Bloque 2): nunca se muestra el stock exacto salvo en las últimas unidades. */
export type Disponibilidad = 'DISPONIBLE' | 'ULTIMAS_UNIDADES' | 'AGOTADO'

export function textoDisponibilidad(disponibilidad: Disponibilidad | undefined, unidadesRestantes?: number | null, disponible = true) {
  if (disponibilidad === 'AGOTADO' || (!disponibilidad && !disponible)) return 'Agotado'
  if (disponibilidad === 'ULTIMAS_UNIDADES') {
    if (unidadesRestantes === 1) return '¡Última unidad!'
    return unidadesRestantes ? `¡Últimas ${unidadesRestantes} unidades!` : '¡Últimas unidades!'
  }
  return 'Disponible'
}

export const claseDisponibilidad = (disponibilidad: Disponibilidad | undefined, disponible = true) =>
  disponibilidad === 'AGOTADO' || (!disponibilidad && !disponible) ? 'agotado' : disponibilidad === 'ULTIMAS_UNIDADES' ? 'ultimas' : ''

/**
 * Motivo de una línea del carrito que no se puede comprar.
 * {@code unidadesDisponibles} solo llega cuando la versión sigue a la venta pero el stock no alcanza.
 */
export function motivoLineaNoDisponible(linea: { valida: boolean; disponible: boolean; unidadesDisponibles?: number }) {
  if (!linea.valida) return 'Ya no está a la venta'
  if (!linea.disponible) {
    const n = linea.unidadesDisponibles
    if (n === 0) return 'Agotado'
    if (n === 1) return 'Solo queda 1 unidad'
    if (n !== undefined) return `Solo quedan ${n} unidades`
    return 'No hay suficientes unidades para la cantidad elegida'
  }
  return ''
}

export const TIENDA_NO_DISPONIBLE = 'Esta tienda no está disponible en este momento'

/** "hasta las 18:40" o "hasta el 24 nov., 18:40" si no es hoy. */
export function plazoParaPagar(vencimiento: string) {
  const fecha = new Date(vencimiento)
  const hoy = new Date()
  const hora = new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit' }).format(fecha)
  return fecha.toDateString() === hoy.toDateString()
    ? `hasta las ${hora}`
    : `hasta el ${new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short' }).format(fecha)}, ${hora}`
}

/**
 * Aviso de productos que no se compran y quedan en el carrito (checkout parcial).
 * {@code soloPorStock}: todos quedaron por falta de stock (si alguno ya no está a la venta, el motivo es genérico).
 */
export function avisoProductosApartados(cantidad: number, soloPorStock: boolean, yaOcurrio: boolean) {
  const sujeto = cantidad === 1 ? '1 producto' : `${cantidad} productos`
  const verbo = yaOcurrio ? (cantidad === 1 ? 'quedó' : 'quedaron') : (cantidad === 1 ? 'quedará' : 'quedarán')
  const motivo = soloPorStock ? 'no hay stock suficiente' : 'no están disponibles en este momento'
  return `${sujeto} ${verbo} en tu carrito porque ${motivo}.`
}
