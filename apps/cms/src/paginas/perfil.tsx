import { useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Check, Eye, EyeOff, IdCard, KeyRound, Mail, ShieldCheck, UserRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useSesion } from '@/autenticacion/sesion'
import { api } from '@/comercio/datos'
import { EstadoCarga, useCarga, useOperacion } from '@/comercio/comunes'

interface Perfil {
  idUsuario: string
  nombres: string
  apellidoPaterno: string | null
  apellidoMaterno: string | null
  correoElectronico: string
  telefono: string | null
  tipoDocumento: string | null
  numeroDocumento: string | null
  paisEmisorDocumento: string | null
  roles: string[]
  fechaCreacion: string
  tieneContrasena: boolean
}

const nombresRol: Record<string, string> = { ADMIN_GENERAL: 'Administrador', DUENO_TIENDA: 'Dueño de tienda', CLIENTE: 'Cliente' }
const claseCampo = 'h-11 w-full rounded-xl border border-input bg-background px-3.5 text-sm outline-none transition focus:border-primary focus:ring-3 focus:ring-primary/20 disabled:cursor-not-allowed disabled:opacity-60'

function Campo({ etiqueta, ayuda, children, className = '' }: { etiqueta: string; ayuda?: string; children: ReactNode; className?: string }) {
  return <label className={`grid gap-1.5 ${className}`}><span className="text-sm font-medium">{etiqueta}</span>{children}{ayuda && <span className="text-xs text-muted-foreground">{ayuda}</span>}</label>
}

function Seccion({ icono, titulo, descripcion, children }: { icono: ReactNode; titulo: string; descripcion: string; children: ReactNode }) {
  return <section className="rounded-2xl border bg-card">
    <div className="flex items-start gap-4 border-b px-6 py-5">
      <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-primary/10 text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]">{icono}</span>
      <div><h2 className="font-semibold">{titulo}</h2><p className="text-sm text-muted-foreground">{descripcion}</p></div>
    </div>
    <div className="p-6">{children}</div>
  </section>
}

function Mensaje({ error, exito }: { error: string; exito: string }) {
  if (error) return <p role="alert" className="rounded-xl bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</p>
  if (exito) return <p role="status" className="flex items-center gap-2 rounded-xl bg-emerald-500/10 px-4 py-3 text-sm text-emerald-700 dark:text-emerald-400"><Check className="size-4" />{exito}</p>
  return null
}

export function PerfilUsuario() {
  const { datos, error, recargar, actualizarDatos } = useCarga(() => api.leer<Perfil>('/api/perfil'), ['perfil'])
  if (error || !datos) return <EstadoCarga error={error} recargar={recargar} />
  const nombreCompleto = [datos.nombres, datos.apellidoPaterno, datos.apellidoMaterno].filter(Boolean).join(' ')
  const desde = new Date(datos.fechaCreacion).toLocaleDateString('es-PE', { month: 'long', year: 'numeric' })

  return <>
    {/* Cabecera del perfil */}
    <section className="relative overflow-hidden rounded-3xl border bg-card">
      <div className="h-28 bg-[linear-gradient(135deg,#1e3a8a_0%,#2f5bff_55%,#4d7cff_100%)]" />
      <div className="flex flex-wrap items-end gap-5 px-6 pb-6">
        <span className="-mt-12 grid size-24 shrink-0 place-items-center rounded-3xl border-4 border-card bg-[#1e3a8a] text-4xl font-semibold text-white shadow-lg">{datos.nombres.trim().charAt(0).toUpperCase()}</span>
        <div className="min-w-0 flex-1 pt-3">
          <h1 className="truncate text-2xl font-semibold tracking-tight">{nombreCompleto}</h1>
          <p className="mt-0.5 flex items-center gap-1.5 text-sm text-muted-foreground"><Mail className="size-4" />{datos.correoElectronico}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {datos.roles.filter(r => r !== 'CLIENTE' || datos.roles.length === 1).map(r => <span key={r} className="rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary dark:bg-[#4d7cff]/15 dark:text-[#8fb3ff]">{nombresRol[r] ?? r}</span>)}
          <span className="text-xs text-muted-foreground">Miembro desde {desde}</span>
        </div>
      </div>
    </section>

    <div className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
      <DatosPersonales perfil={datos} alGuardar={nuevo => actualizarDatos(() => nuevo)} />
      <Seguridad tieneContrasena={datos.tieneContrasena} />
    </div>
  </>
}

function DatosPersonales({ perfil, alGuardar }: { perfil: Perfil; alGuardar: (perfil: Perfil) => void }) {
  const { actualizar } = useSesion()
  const [formulario, establecerFormulario] = useState({
    nombres: perfil.nombres,
    apellidoPaterno: perfil.apellidoPaterno ?? '',
    apellidoMaterno: perfil.apellidoMaterno ?? '',
    telefono: perfil.telefono ?? '',
    tipoDocumento: perfil.tipoDocumento ?? '',
    numeroDocumento: perfil.numeroDocumento ?? '',
    paisEmisorDocumento: perfil.tipoDocumento === 'PASAPORTE' ? perfil.paisEmisorDocumento ?? '' : '',
  })
  const [exito, establecerExito] = useState('')
  const operacion = useOperacion()
  const cambiar = (campo: keyof typeof formulario) => (e: { target: { value: string } }) => { establecerExito(''); establecerFormulario(f => ({ ...f, [campo]: e.target.value })) }

  async function guardar(e: FormEvent) {
    e.preventDefault(); establecerExito('')
    await operacion.ejecutar(async () => {
      const sinDocumento = !formulario.tipoDocumento
      const actualizado = await api.actualizar<Perfil>('/api/perfil', {
        ...formulario,
        numeroDocumento: sinDocumento ? '' : formulario.numeroDocumento,
        paisEmisorDocumento: formulario.tipoDocumento === 'PASAPORTE' ? formulario.paisEmisorDocumento.toUpperCase() : '',
      })
      alGuardar(actualizado)
      establecerExito('Tus datos se guardaron correctamente.')
      try { await actualizar() } catch { /* el nombre del menú se actualizará al recargar */ }
    })
  }

  return <Seccion icono={<UserRound className="size-5" />} titulo="Datos personales" descripcion="Así aparecerás en Nubea y en los pedidos que gestiones.">
    <form onSubmit={guardar} className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <Campo etiqueta="Nombres" className="sm:col-span-2"><input className={claseCampo} value={formulario.nombres} onChange={cambiar('nombres')} maxLength={100} required autoComplete="given-name" /></Campo>
        <Campo etiqueta="Apellido paterno"><input className={claseCampo} value={formulario.apellidoPaterno} onChange={cambiar('apellidoPaterno')} maxLength={80} autoComplete="family-name" /></Campo>
        <Campo etiqueta="Apellido materno"><input className={claseCampo} value={formulario.apellidoMaterno} onChange={cambiar('apellidoMaterno')} maxLength={80} /></Campo>
        <Campo etiqueta="Correo electrónico" ayuda="El correo es tu usuario para ingresar y no se puede cambiar."><input className={claseCampo} value={perfil.correoElectronico} disabled /></Campo>
        <Campo etiqueta="Teléfono"><input className={claseCampo} value={formulario.telefono} onChange={cambiar('telefono')} maxLength={20} inputMode="tel" autoComplete="tel" placeholder="Ej. 987 654 321" /></Campo>
      </div>

      <div>
        <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold"><IdCard className="size-4 text-muted-foreground" />Documento de identidad <span className="font-normal text-muted-foreground">(opcional)</span></h3>
        <div className="grid gap-4 sm:grid-cols-[180px_1fr]">
          <Campo etiqueta="Tipo">
            <select className={`${claseCampo} cursor-pointer`} value={formulario.tipoDocumento} onChange={cambiar('tipoDocumento')}>
              <option value="">Sin documento</option><option value="DNI">DNI</option><option value="CE">Carné de extranjería</option><option value="PASAPORTE">Pasaporte</option>
            </select>
          </Campo>
          <Campo etiqueta="Número" ayuda={formulario.tipoDocumento === 'DNI' ? '8 dígitos.' : formulario.tipoDocumento === 'CE' ? 'Entre 9 y 12 caracteres.' : formulario.tipoDocumento === 'PASAPORTE' ? 'Entre 6 y 12 caracteres.' : undefined}>
            <input className={claseCampo} value={formulario.numeroDocumento} onChange={cambiar('numeroDocumento')} maxLength={formulario.tipoDocumento === 'DNI' ? 8 : 12} disabled={!formulario.tipoDocumento} required={!!formulario.tipoDocumento} inputMode={formulario.tipoDocumento === 'DNI' ? 'numeric' : 'text'} />
          </Campo>
          {formulario.tipoDocumento === 'PASAPORTE' && <Campo etiqueta="País emisor" ayuda="Código de 2 letras, por ejemplo CO, VE, AR." className="sm:col-start-2"><input className={`${claseCampo} uppercase`} value={formulario.paisEmisorDocumento} onChange={cambiar('paisEmisorDocumento')} maxLength={2} required /></Campo>}
        </div>
      </div>

      <Mensaje error={operacion.error} exito={exito} />
      <div className="flex justify-end"><Button type="submit" disabled={operacion.ocupado} className="h-11 rounded-xl px-6">{operacion.ocupado ? 'Guardando...' : 'Guardar cambios'}</Button></div>
    </form>
  </Seccion>
}

function CampoContrasena({ etiqueta, valor, alCambiar, autoComplete, ayuda }: { etiqueta: string; valor: string; alCambiar: (v: string) => void; autoComplete: string; ayuda?: string }) {
  const [visible, establecerVisible] = useState(false)
  return <Campo etiqueta={etiqueta} ayuda={ayuda}>
    <span className="relative block">
      <input type={visible ? 'text' : 'password'} className={`${claseCampo} pr-11`} value={valor} onChange={e => alCambiar(e.target.value)} autoComplete={autoComplete} required minLength={autoComplete === 'new-password' ? 8 : undefined} maxLength={128} />
      <button type="button" onClick={() => establecerVisible(v => !v)} aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'} className="absolute top-1/2 right-1.5 grid size-8 -translate-y-1/2 cursor-pointer place-items-center rounded-lg text-muted-foreground hover:bg-muted hover:text-foreground">{visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}</button>
    </span>
  </Campo>
}

function Seguridad({ tieneContrasena }: { tieneContrasena: boolean }) {
  const [actual, establecerActual] = useState('')
  const [nueva, establecerNueva] = useState('')
  const [confirmacion, establecerConfirmacion] = useState('')
  const [exito, establecerExito] = useState('')
  const [errorLocal, establecerErrorLocal] = useState('')
  const operacion = useOperacion()
  const fuerza = [nueva.length >= 8, /[A-Z]/.test(nueva) && /[a-z]/.test(nueva), /\d/.test(nueva), /[^A-Za-z0-9]/.test(nueva)].filter(Boolean).length
  const textoFuerza = ['Muy débil', 'Débil', 'Aceptable', 'Buena', 'Fuerte'][fuerza]
  const colorFuerza = ['bg-red-500', 'bg-red-500', 'bg-amber-500', 'bg-emerald-500', 'bg-emerald-500'][fuerza]

  async function cambiar(e: FormEvent) {
    e.preventDefault(); establecerExito(''); establecerErrorLocal('')
    if (nueva !== confirmacion) { establecerErrorLocal('Las contraseñas nuevas no coinciden.'); return }
    await operacion.ejecutar(async () => {
      await api.actualizar<void>('/api/perfil/contrasena', { contrasenaActual: actual, contrasenaNueva: nueva })
      establecerActual(''); establecerNueva(''); establecerConfirmacion('')
      establecerExito('Tu contraseña se cambió correctamente.')
    })
  }

  return <Seccion icono={<KeyRound className="size-5" />} titulo="Seguridad" descripcion="Cambia la contraseña con la que ingresas a Nubea.">
    {!tieneContrasena ? <p className="flex items-start gap-3 rounded-xl bg-muted px-4 py-3 text-sm text-muted-foreground"><ShieldCheck className="mt-0.5 size-4 shrink-0" />Tu cuenta ingresa con un proveedor externo, por eso no tiene contraseña en Nubea.</p>
      : <form onSubmit={cambiar} className="space-y-4">
        <CampoContrasena etiqueta="Contraseña actual" valor={actual} alCambiar={v => { establecerExito(''); establecerActual(v) }} autoComplete="current-password" />
        <div>
          <CampoContrasena etiqueta="Nueva contraseña" valor={nueva} alCambiar={v => { establecerExito(''); establecerNueva(v) }} autoComplete="new-password" />
          {nueva && <div className="mt-2 flex items-center gap-3">
            <span className="flex flex-1 gap-1">{[0, 1, 2, 3].map(i => <span key={i} className={`h-1.5 flex-1 rounded-full transition-colors ${i < fuerza ? colorFuerza : 'bg-muted'}`} />)}</span>
            <span className="w-20 text-right text-xs text-muted-foreground">{textoFuerza}</span>
          </div>}
          <p className="mt-1.5 text-xs text-muted-foreground">Mínimo 8 caracteres. Combina mayúsculas, números y símbolos para hacerla más segura.</p>
        </div>
        <CampoContrasena etiqueta="Confirmar nueva contraseña" valor={confirmacion} alCambiar={v => { establecerExito(''); establecerConfirmacion(v) }} autoComplete="new-password" />
        <Mensaje error={errorLocal || operacion.error} exito={exito} />
        <Button type="submit" disabled={operacion.ocupado} className="h-11 w-full rounded-xl">{operacion.ocupado ? 'Cambiando...' : 'Cambiar contraseña'}</Button>
      </form>}
  </Seccion>
}
