import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowRight, Lock, Mail } from 'lucide-react'
import { AccesoConGoogle, SiGoogle } from '@/autenticacion/boton-google'
import { BotonVerContrasena, CampoAutenticacion, MarcoAutenticacion, SeparadorAlterno, claseBotonPrincipal, claseEntrada } from '@/autenticacion/marco-autenticacion'
import { ErrorApi, apiAutenticacion } from '@/autenticacion/api'
import { useSesion } from '@/autenticacion/sesion'

export function PaginaInicioSesion() {
  const [correo, establecerCorreo] = useState('')
  const [contrasena, establecerContrasena] = useState('')
  const [recordarme, establecerRecordarme] = useState(false)
  const [verContrasena, establecerVerContrasena] = useState(false)
  const [error, establecerError] = useState('')
  const [ocupado, establecerOcupado] = useState(false)
  const [bienvenida, establecerBienvenida] = useState(false)
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const { actualizar } = useSesion()
  const retorno = new URLSearchParams(ubicacion.search).get('retorno')
  const retornoSeguro = retorno && /^\/tienda\/[a-z0-9]+(?:-[a-z0-9]+)*(?:\/carrito)?\/?$/.test(retorno) ? retorno : null
  function irAlDestino() {
    if (retornoSeguro) window.location.assign(retornoSeguro)
    else navegar(typeof ubicacion.state?.from === 'string' && ubicacion.state.from.startsWith('/panel') ? ubicacion.state.from : '/panel', { replace: true })
  }
  // Google: 204 (cuenta existente) entra directo; 201 (cuenta nueva) muestra la bienvenida un momento.
  async function entrarConGoogle(cuentaNueva: boolean) {
    await actualizar()
    if (cuentaNueva) { establecerBienvenida(true); await new Promise(r => setTimeout(r, 1500)) }
    irAlDestino()
  }
  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault(); establecerError(''); establecerOcupado(true)
    try {
      await apiAutenticacion.iniciarSesion(correo, contrasena, recordarme)
      await actualizar()
      irAlDestino()
    } catch (err) {
      establecerError(err instanceof ErrorApi && err.codigoEstado === 401 ? 'El correo o la contraseña son incorrectos.' : err instanceof Error ? err.message : 'No se pudo iniciar sesión.')
    } finally { establecerOcupado(false) }
  }
  return <MarcoAutenticacion
    etiqueta="Bienvenido de nuevo"
    titular="Tu tienda te está"
    destacado="esperando."
    descripcion="Ingresa para gestionar tus productos, pedidos y clientes desde un solo lugar."
    puntos={['Revisa tus pedidos y clientes', 'Actualiza productos, precios y stock', 'Cambia la apariencia de tu vitrina']}
  >
    <h1 className="text-[28px] leading-[1.15] font-bold tracking-tight text-white">Ingresa a tu cuenta</h1>
    <p className="mt-[8px] mb-[24px] text-[15px] text-white/65">Accede a tu espacio en Nubea.</p>
    {ubicacion.state?.sesionVencida && <p role="status" className="mb-[18px] rounded-[12px] border border-nubea-acento/30 bg-nubea-acento/10 px-[14px] py-[10px] text-[13px] text-white/90">Inicia sesión para continuar. Si tu sesión venció, vuelve a ingresar.</p>}
    {ubicacion.state?.registroCompletado && <p role="status" className="mb-[18px] rounded-[12px] border border-nubea-petroleo-claro/30 bg-nubea-petroleo-claro/10 px-[14px] py-[10px] text-[13px] text-white/90">Tu cuenta fue creada. Inicia sesión para continuar.</p>}
{bienvenida && <p role="status" className="mb-[18px] rounded-[12px] border border-nubea-petroleo-claro/30 bg-nubea-petroleo-claro/10 px-[14px] py-[10px] text-[13px] text-white/90">¡Bienvenido/a a Nubea! Creamos tu cuenta con Google.</p>}
    <form onSubmit={enviar} className="space-y-[14px]">
      <CampoAutenticacion id="email" etiqueta="Correo" icono={<Mail />}>
        <input id="email" type="email" autoComplete="email" placeholder="tucorreo@ejemplo.com" className={claseEntrada} value={correo} onChange={e => establecerCorreo(e.target.value)} required />
      </CampoAutenticacion>
      <CampoAutenticacion id="password" etiqueta="Contraseña" icono={<Lock />} accion={<BotonVerContrasena visible={verContrasena} alCambiar={() => establecerVerContrasena(v => !v)} />}>
        <input id="password" type={verContrasena ? 'text' : 'password'} autoComplete="current-password" placeholder="Tu contraseña" className={claseEntrada} value={contrasena} onChange={e => establecerContrasena(e.target.value)} required />
      </CampoAutenticacion>
      <label className="flex cursor-pointer items-center gap-[10px] pt-[2px] text-[14px] text-white/70 select-none">
        <input type="checkbox" className="size-[16px] cursor-pointer accent-[#5b8cff]" checked={recordarme} onChange={e => establecerRecordarme(e.target.checked)} /> Recordarme durante 24 horas
      </label>
      {error && <p role="alert" className="rounded-[12px] border border-red-400/30 bg-red-500/10 px-[14px] py-[10px] text-[13px] text-red-300">{error}</p>}
      <div className="pt-[6px]">
        <button type="submit" className={claseBotonPrincipal} disabled={ocupado}>
          {ocupado ? 'Ingresando...' : <>Ingresar <ArrowRight className="size-[18px] transition-transform group-hover:translate-x-[3px]" /></>}
        </button>
      </div>
    </form>
    <SiGoogle><SeparadorAlterno texto="o ingresa con" /></SiGoogle>
    <AccesoConGoogle recordarme={recordarme} retorno={retornoSeguro} alEntrar={entrarConGoogle} alUsarContrasena={() => document.getElementById('email')?.focus()} />
    <p className="mt-[20px] text-center text-[14px] text-white/70">¿Aún no tienes cuenta? <Link className="font-semibold text-[#5b8cff] hover:underline" to={`/registro${retornoSeguro ? `?retorno=${encodeURIComponent(retornoSeguro)}` : ""}`}>Crea una cuenta</Link></p>
  </MarcoAutenticacion>
}
