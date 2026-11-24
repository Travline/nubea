import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ErrorApi, apiAutenticacion } from "@/autenticacion/api";
import { cn } from "@/lib/utils";

/*
 * Inicio de sesión y registro con Google (Google Identity Services).
 * Contrato: docs/revision/google-login-frontend.md
 * - Google entrega un ID token; se envía tal cual a POST /api/auth/google (no se decodifica ni se guarda).
 * - El backend inicia la misma sesión por cookie que el login con contraseña.
 */

type RespuestaCredencial = { credential: string };
type GoogleId = {
  initialize: (opciones: { client_id: string; callback: (r: RespuestaCredencial) => void; ux_mode?: "popup"; auto_select?: boolean; cancel_on_tap_outside?: boolean }) => void;
  renderButton: (elemento: HTMLElement, opciones: Record<string, unknown>) => void;
};
declare global { interface Window { google?: { accounts: { id: GoogleId } } } }

const idCliente = (import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined)?.trim() ?? "";
/** true si hay VITE_GOOGLE_CLIENT_ID configurado (si no, no se muestra nada de Google). */
export const googleConfigurado = idCliente !== "";
let cargaScript: Promise<void> | null = null;
function cargarGoogle(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (!cargaScript) {
    cargaScript = new Promise((resolver, rechazar) => {
      const script = document.createElement("script");
      script.src = "https://accounts.google.com/gsi/client";
      script.async = true;
      script.defer = true;
      script.onload = () => resolver();
      script.onerror = () => { cargaScript = null; rechazar(new Error("No se pudo cargar Google")); };
      document.head.appendChild(script);
    });
  }
  return cargaScript;
}

type PropiedadesBotonGoogle = {
  /** "continuar" en el inicio de sesión, "registrarse" en el registro. */
  modo?: "continuar" | "registrarse";
  /** Igual que en el login local: sesión de 24 h. */
  recordarme?: boolean;
  /** Se llama cuando la sesión ya quedó iniciada (cuenta existente o nueva). */
  alIniciarSesion: (cuentaNueva: boolean) => void | Promise<void>;
  className?: string;
};

export function BotonGoogle({ modo = "continuar", recordarme = false, alIniciarSesion, className }: PropiedadesBotonGoogle) {
  const contenedor = useRef<HTMLDivElement>(null);
  const capa = useRef<HTMLDivElement>(null);
  const [disponible, establecerDisponible] = useState(idCliente !== "");
  const [ocupado, establecerOcupado] = useState(false);
  const [error, establecerError] = useState("");
  const [cuentaConContrasena, establecerCuentaConContrasena] = useState(false);
  // Refs para que el callback de Google siempre use los valores actuales.
  const recordarmeRef = useRef(recordarme);
  const alIniciarRef = useRef(alIniciarSesion);
  recordarmeRef.current = recordarme;
  alIniciarRef.current = alIniciarSesion;

  useEffect(() => {
    if (!idCliente) return;
    let activo = true;
    cargarGoogle()
      .then(() => {
        if (!activo || !contenedor.current || !window.google) return;
        window.google.accounts.id.initialize({
          client_id: idCliente,
          ux_mode: "popup",
          auto_select: false,
          cancel_on_tap_outside: true,
          callback: (respuesta) => void continuar(respuesta.credential),
        });
        contenedor.current.innerHTML = "";
        window.google.accounts.id.renderButton(contenedor.current, {
          type: "standard",
          theme: "filled_black",
          size: "large",
          shape: "rectangular",
          text: modo === "registrarse" ? "signup_with" : "continue_with",
          logo_alignment: "center",
          locale: "es",
          width: ANCHO_GOOGLE,
        });
      })
      .catch(() => { if (activo) establecerError("No se pudo cargar el botón de Google. Revisa tu conexión."); });
    return () => { activo = false; };
    // Se vuelve a dibujar solo si cambia el modo.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [modo]);

  async function continuar(idToken: string) {
    establecerError(""); establecerCuentaConContrasena(false); establecerOcupado(true);
    try {
      const usuario = await apiAutenticacion.continuarConGoogle(idToken, recordarmeRef.current);
      await alIniciarRef.current(!!usuario); // 201 trae el usuario nuevo; 204 no trae cuerpo
    } catch (err) {
      if (err instanceof ErrorApi) {
        if (err.codigoEstado === 409) { establecerCuentaConContrasena(true); establecerError(err.campos?.correoElectronico ?? err.message); }
        else if (err.codigoEstado === 503) { establecerDisponible(false); establecerError(err.message || "El inicio de sesión con Google no está disponible en este momento."); }
        else if (err.codigoEstado === 422) establecerError(err.campos?.idToken ?? err.message);
        else if (err.codigoEstado === 401) establecerError(err.message && err.message !== "No se pudo completar la solicitud." ? err.message : "No se pudo iniciar sesión.");
        else establecerError(err.message);
      } else establecerError("No se pudo iniciar sesión con Google. Inténtalo de nuevo.");
    } finally { establecerOcupado(false); }
  }

  // El botón oficial de Google (iframe) va invisible encima de nuestro botón y se estira
  // para cubrirlo entero: el clic lo recibe Google, pero se ve con el estilo de Nubea.
  useEffect(() => {
    const caja = capa.current;
    if (!caja || !contenedor.current) return;
    const ajustar = () => {
      if (!contenedor.current) return;
      const { width, height } = caja.getBoundingClientRect();
      contenedor.current.style.transform = `scale(${width / ANCHO_GOOGLE}, ${height / ALTO_GOOGLE})`;
    };
    ajustar();
    const observador = new ResizeObserver(ajustar);
    observador.observe(caja);
    return () => observador.disconnect();
  }, [disponible]);

  // Sin VITE_GOOGLE_CLIENT_ID no se muestra el botón (docs: "Si está vacío, no mostrar el botón").
  if (!idCliente) return null;

  return (
    <div className={cn("grid gap-3", className)}>
      {disponible && (
        <div
          ref={capa}
          aria-busy={ocupado}
          className={cn(
            "group relative h-[52px] overflow-hidden rounded-[12px] border border-white/12 bg-white/[0.03] transition",
            "hover:border-white/25 hover:bg-white/[0.07] focus-within:border-nubea-acento focus-within:ring-[3px] focus-within:ring-nubea-acento/15",
            ocupado && "pointer-events-none opacity-70",
          )}
        >
          {/* Lo que se ve */}
          <span aria-hidden className="pointer-events-none absolute inset-0 flex items-center justify-center gap-[12px] text-[15px] font-medium text-white">
            <span className="grid size-[28px] place-items-center rounded-full bg-white shadow-[0_2px_8px_-2px_rgba(0,0,0,0.5)] transition-transform group-hover:scale-105"><LogoGoogle /></span>
            {ocupado ? "Conectando con Google…" : modo === "registrarse" ? "Registrarse con Google" : "Continuar con Google"}
          </span>
          {/* Botón real de Google: invisible y estirado sobre toda la caja */}
          <div ref={contenedor} className="absolute top-0 left-0 origin-top-left opacity-[0.01]" style={{ width: ANCHO_GOOGLE, height: ALTO_GOOGLE }} />
        </div>
      )}
      {error && (
        <div role="alert" className="rounded-[12px] border border-amber-400/30 bg-amber-500/10 px-[14px] py-[10px] text-[13px] text-amber-200">
          <p>{error}</p>
          {cuentaConContrasena && modo === "registrarse" && (
            <Link to="/iniciar-sesion" className="mt-2 inline-block font-semibold text-[#8fb3ff] hover:underline">Iniciar sesión con contraseña</Link>
          )}
        </div>
      )}
    </div>
  );
}

const ANCHO_GOOGLE = 400; // ancho máximo que acepta renderButton
const ALTO_GOOGLE = 40; // alto del botón "large"

function LogoGoogle() {
  return (
    <svg viewBox="0 0 48 48" className="size-[18px]" aria-hidden>
      <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3C33.7 32.7 29.2 36 24 36c-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.4-.4-3.5z" />
      <path fill="#FF3D00" d="m6.3 14.7 6.6 4.8C14.7 15.1 19 12 24 12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 16.3 4 9.7 8.3 6.3 14.7z" />
      <path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.2l-6.2-5.2C29.2 35.1 26.7 36 24 36c-5.2 0-9.6-3.3-11.3-8l-6.5 5C9.5 39.6 16.2 44 24 44z" />
      <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3c-.8 2.2-2.2 4.2-4.1 5.6l6.2 5.2C37 39.2 44 34 44 24c0-1.3-.1-2.4-.4-3.5z" />
    </svg>
  );
}

export function SeparadorCorreo() {
  return (
    <div className="my-5 flex items-center gap-3 text-xs text-muted-foreground uppercase">
      <span className="h-px flex-1 bg-border" />
      o con tu correo
      <span className="h-px flex-1 bg-border" />
    </div>
  );
}
