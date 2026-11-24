import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ArrowRight, Lock, Mail, User } from 'lucide-react'
import { BotonGoogle, googleConfigurado } from '@/autenticacion/boton-google'
import { BotonVerContrasena, CampoAutenticacion, MarcoAutenticacion, SeparadorAlterno, claseBotonPrincipal, claseEntrada } from '@/autenticacion/marco-autenticacion'
import { useSesion } from '@/autenticacion/sesion'
import { ErrorApi, apiAutenticacion } from '@/autenticacion/api'

export function PaginaRegistro() {
  const [nombres, setNombres] = useState('')
  const [correo, establecerCorreo] = useState('')
  const [contrasena, establecerContrasena] = useState('')
  const [verContrasena, establecerVerContrasena] = useState(false)
  const [error, establecerError] = useState('')
  const [ocupado, establecerOcupado] = useState(false)
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const retorno = new URLSearchParams(ubicacion.search).get('retorno')
  const retornoSeguro = retorno && /^\/tienda\/[a-z0-9]+(?:-[a-z0-9]+)*(?:\/carrito)?\/?$/.test(retorno) ? retorno : null
  const { actualizar } = useSesion()
  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault(); establecerError(''); establecerOcupado(true)
    try {
      await apiAutenticacion.registrar(nombres, correo, contrasena)
      try {
        await apiAutenticacion.iniciarSesion(correo, contrasena)
        await actualizar()
        if (retornoSeguro) window.location.assign(retornoSeguro)
        else navegar('/panel', { replace: true })
      } catch {
        navegar(`/iniciar-sesion${retornoSeguro ? `?retorno=${encodeURIComponent(retornoSeguro)}` : ''}`, { replace: true, state: { registroCompletado: true } })
      }
    } catch (err) {
      if (err instanceof ErrorApi) establecerError(Object.values(err.campos || {}).join(' ') || err.message)
      else establecerError('No se pudo crear la cuenta.')
    } finally { establecerOcupado(false) }
  }
  return <MarcoAutenticacion
    etiqueta="Plataforma e-commerce"
    titular="Crea tu tienda online en"
    destacado="minutos."
    descripcion="Todo lo que necesitas para vender en línea: personaliza, administra y haz crecer tu negocio desde un solo lugar."
    puntos={['Sube tus productos con fotos y presentaciones', 'Elige una plantilla profesional', 'Recibe pedidos desde tu propio enlace']}
  >
    <h1 className="text-[28px] leading-[1.15] font-bold tracking-tight text-white">Crea tu cuenta</h1>
    <p className="mt-[8px] mb-[24px] text-[15px] text-white/65">Empieza a formar parte de Nubea.</p>
    <form onSubmit={enviar} className="space-y-[14px]">
      <CampoAutenticacion id="nombres" etiqueta="Nombres" icono={<User />}>
        <input id="nombres" autoComplete="given-name" maxLength={100} placeholder="Tu nombre" className={claseEntrada} value={nombres} onChange={e => setNombres(e.target.value)} required />
      </CampoAutenticacion>
      <CampoAutenticacion id="email" etiqueta="Correo" icono={<Mail />}>
        <input id="email" type="email" autoComplete="email" maxLength={100} placeholder="tucorreo@ejemplo.com" className={claseEntrada} value={correo} onChange={e => establecerCorreo(e.target.value)} required />
      </CampoAutenticacion>
      <CampoAutenticacion id="password" etiqueta="Contraseña" icono={<Lock />} accion={<BotonVerContrasena visible={verContrasena} alCambiar={() => establecerVerContrasena(v => !v)} />}>
        <input id="password" type={verContrasena ? 'text' : 'password'} autoComplete="new-password" minLength={8} maxLength={128} placeholder="Mínimo 8 caracteres" className={claseEntrada} value={contrasena} onChange={e => establecerContrasena(e.target.value)} required />
      </CampoAutenticacion>
      {error && <p role="alert" className="rounded-[12px] border border-red-400/30 bg-red-500/10 px-[14px] py-[10px] text-[13px] text-red-300">{error}</p>}
      <div className="pt-[6px]">
        <button type="submit" className={claseBotonPrincipal} disabled={ocupado}>
          {ocupado ? 'Creando cuenta...' : <>Crear cuenta <ArrowRight className="size-[18px] transition-transform group-hover:translate-x-[3px]" /></>}
        </button>
      </div>
    </form>
    {googleConfigurado && <>
      <SeparadorAlterno texto="o regístrate con" />
      <BotonGoogle modo="registrarse" alIniciarSesion={async () => { await actualizar(); if (retornoSeguro) window.location.assign(retornoSeguro); else navegar('/panel', { replace: true }) }} />
    </>}
    <p className="mt-[20px] text-center text-[14px] text-white/70">¿Ya tienes cuenta? <Link className="font-semibold text-[#5b8cff] hover:underline" to={`/iniciar-sesion${retornoSeguro ? `?retorno=${encodeURIComponent(retornoSeguro)}` : ""}`}>Inicia sesión</Link></p>
  </MarcoAutenticacion>
}
