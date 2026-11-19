import { useEffect, useRef } from 'react'
import { useNavigate, useOutletContext, useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import type { ContextoTienda } from '@/paginas/panel'
import { api } from './datos'
import type { CuentaPago } from './datos'
import { Aviso, Bloque, EstadoCarga, useCarga, useOperacion } from './comunes'

const habilitado = import.meta.env.VITE_MERCADOPAGO_OAUTH_ENABLED === 'true'
export function RetornoPago() {
  const navegar = useNavigate(); const [parametros] = useSearchParams(); const procesado = useRef(false)
  useEffect(() => { if (procesado.current) return; procesado.current = true; const id = sessionStorage.getItem('nubea-tienda-oauth'); sessionStorage.removeItem('nubea-tienda-oauth'); navegar(id ? `/panel/tiendas/${encodeURIComponent(id)}/pagos?resultado=${encodeURIComponent(parametros.get('resultado') ?? 'error')}` : '/panel', { replace: true }) }, [navegar, parametros])
  return <p role="status">Volviendo a tu tienda...</p>
}
export function Pagos() {
  const { apiBase, tienda } = useOutletContext<ContextoTienda>(); const [parametros] = useSearchParams(); const op = useOperacion()
  const { datos, error, recargar } = useCarga(() => api.leer<CuentaPago>(`${apiBase}/cuenta-pago`), [apiBase])
  if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
  const resultado = parametros.get('resultado')
  return <Bloque titulo="Mercado Pago"><p>Estado: <strong>{datos.estado}</strong></p>{datos.idCuentaExterna && <p>Cuenta: {datos.idCuentaExterna}</p>}{resultado && <p role="status">{resultado === 'exito' ? 'La autorización ha finalizado. El estado mostrado se consulta al servidor.' : resultado === 'conflicto' ? 'Esta cuenta ya está vinculada a otra tienda.' : 'No se pudo completar la autorización; puedes intentarlo nuevamente.'}</p>}<Aviso error={op.error} />{!habilitado && <p className="text-muted-foreground">La vinculación de Mercado Pago aún no está disponible.</p>}<div className="flex gap-2">{habilitado && datos.estado !== 'ACTIVA' && <Button disabled={op.ocupado} onClick={() => void op.ejecutar(async () => { const respuesta = await api.crear<{ urlAutorizacion: string }>(`${apiBase}/cuenta-pago/mercado-pago/oauth/inicios`); sessionStorage.setItem('nubea-tienda-oauth', tienda.idTienda); window.location.assign(respuesta.urlAutorizacion) })}>Conectar Mercado Pago</Button>}{datos.estado === 'ACTIVA' && <Button variant="outline" disabled={op.ocupado} onClick={() => { if (window.confirm('¿Desconectar esta cuenta de pago?')) void op.ejecutar(async () => { await api.crear(`${apiBase}/cuenta-pago/desconexion`); recargar() }) }}>Desconectar</Button>}</div></Bloque>
}
