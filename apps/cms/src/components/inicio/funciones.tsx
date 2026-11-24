import type { ReactNode } from "react";
import { Check, GripVertical, Link2, LogIn, MapPin, Store } from "lucide-react";
import { cn } from "@/lib/utils";

// Todo lo que se muestra aquí corresponde a lo que permite el modelo de datos
// (módulos 1 a 4). Los datos de los ejemplos son ficticios.

// ---------- Fila reutilizable (texto + visual) ----------
type PropiedadesFila = {
  etiqueta: string;
  titulo: ReactNode;
  descripcion: string;
  puntos: string[];
  visual: ReactNode;
  invertida?: boolean;
};

const Fila = ({ etiqueta, titulo, descripcion, puntos, visual, invertida }: PropiedadesFila) => (
  <div
    className={cn(
      "grid items-center gap-12 lg:gap-24",
      invertida ? "lg:grid-cols-[1fr_460px]" : "lg:grid-cols-[460px_1fr]"
    )}
  >
    <div className={cn(invertida && "lg:order-2")}>
      <p className="text-[13px] font-semibold tracking-[0.2em] text-nubea-acento uppercase">{etiqueta}</p>
      <h3 className="mt-3.5 font-display text-[clamp(34px,3.2vw,44px)] leading-[1.08] tracking-[-0.01em]">{titulo}</h3>
      <p className="mt-4 text-[17px] leading-[1.6] text-nubea-texto-suave">{descripcion}</p>
      <ul className="mt-7 flex flex-col gap-3.5 text-[15px]">
        {puntos.map((punto) => (
          <li key={punto} className="flex items-center gap-3">
            <Check className="size-[18px] shrink-0 text-nubea-petroleo-claro" strokeWidth={2.4} />
            {punto}
          </li>
        ))}
      </ul>
    </div>
    <div
      className={cn(
        "overflow-hidden rounded-[24px] border border-white/8 bg-nubea-superficie p-6 sm:p-8",
        invertida && "lg:order-1"
      )}
    >
      {visual}
    </div>
  </div>
);

const Etiqueta = ({ children, tono }: { children: ReactNode; tono: "ok" | "alerta" | "neutro" }) => (
  <span
    className={cn(
      "inline-block rounded-full px-2.5 py-1 text-[11px] font-semibold whitespace-nowrap",
      tono === "ok" && "bg-nubea-petroleo-claro/15 text-nubea-petroleo-claro",
      tono === "alerta" && "bg-nubea-acento/12 text-nubea-acento",
      tono === "neutro" && "bg-white/8 text-nubea-texto-suave"
    )}
  >
    {children}
  </span>
);

// ---------- Visual 1: catálogo, presentaciones e inventario ----------
// Miniaturas ilustradas de cada prenda (puedes reemplazarlas por fotos reales).
type Prenda = "camisa" | "polo" | "casaca" | "gorra";

const Miniatura = ({ prenda, color }: { prenda: Prenda; color: string }) => (
  <span className="grid size-10 shrink-0 place-items-center rounded-[10px] bg-linear-to-br from-[#F4F6FA] to-[#DCE3EC]">
    <svg viewBox="0 0 48 48" className="size-8" fill={color} stroke="rgba(15,23,42,0.35)" strokeWidth="1.2" strokeLinejoin="round" strokeLinecap="round">
      {prenda === "camisa" && (
        <>
          <path d="M14 10 19 7l5 3 5-3 5 3 8 6-4 6-3-2v21H13V20l-3 2-4-6z" />
          <path d="M19 7l5 6 5-6M24 13v28" fill="none" />
          <circle cx="24" cy="19" r="0.9" fill="rgba(15,23,42,0.45)" stroke="none" />
          <circle cx="24" cy="26" r="0.9" fill="rgba(15,23,42,0.45)" stroke="none" />
          <circle cx="24" cy="33" r="0.9" fill="rgba(15,23,42,0.45)" stroke="none" />
        </>
      )}
      {prenda === "polo" && (
        <>
          <path d="M15 9l5-2q4 3 8 0l5 2 8 6-4 6-3-2v22H14V19l-3 2-4-6z" />
          <path d="M20 7q4 4 8 0M24 10v7" fill="none" />
        </>
      )}
      {prenda === "casaca" && (
        <>
          <path d="M15 9l5-2 4 4 4-4 5 2 7 4 2 25h-5l-2-16v19H13V22l-2 16H6l2-25z" />
          <path d="M20 7l4 4 4-4M24 11v30M16 26h5v5h-5zM27 26h5v5h-5z" fill="none" />
        </>
      )}
      {prenda === "gorra" && (
        <>
          <path d="M10 30q0-16 14-16t14 16z" />
          <path d="M7 30q17-5 35 1-6 4-18 3-11 0-17-4z" />
          <path d="M24 14v16" fill="none" />
          <circle cx="24" cy="14" r="1.4" />
        </>
      )}
    </svg>
  </span>
);

const presentaciones: { nombre: string; variante: string; precio: string; stock: number; estado: string; prenda: Prenda; color: string }[] = [
  { nombre: "Camisa de lino", variante: "M · Blanco", precio: "S/ 89.00", stock: 24, estado: "Publicado", prenda: "camisa", color: "#FAF8F3" },
  { nombre: "Camisa de lino", variante: "L · Azul", precio: "S/ 89.00", stock: 2, estado: "Stock bajo", prenda: "camisa", color: "#7D9BC6" },
  { nombre: "Polo básico", variante: "S · Negro", precio: "S/ 49.00", stock: 31, estado: "Publicado", prenda: "polo", color: "#2B2E35" },
  { nombre: "Casaca denim", variante: "M · Índigo", precio: "S/ 159.00", stock: 8, estado: "Publicado", prenda: "casaca", color: "#3E5A8A" },
  { nombre: "Gorra bordada", variante: "Única · Beige", precio: "S/ 39.00", stock: 0, estado: "Borrador", prenda: "gorra", color: "#D8C3A0" },
];

const tono = (estado: string) => (estado === "Publicado" ? "ok" : estado === "Stock bajo" ? "alerta" : "neutro");

const VisualCatalogo = () => (
  <div aria-hidden="true">
    <div className="mb-4 flex items-center justify-between">
      <p className="text-[18px] font-semibold">Inventario</p>
      <span className="rounded-full bg-nubea-acento px-3.5 py-2 text-[12px] font-semibold text-nubea-sobre-acento">
        + Nuevo producto
      </span>
    </div>
    <div className="grid grid-cols-[1fr_76px_48px_92px] items-center border-b border-white/8 py-2.5 text-[13px] text-nubea-texto-suave sm:grid-cols-[1fr_90px_60px_104px]">
      <span>Presentación</span><span>Precio</span><span>Stock</span><span className="text-right">Estado</span>
    </div>
    {presentaciones.map((p, i) => (
      <div
        key={p.nombre + p.variante}
        className={cn(
          "grid grid-cols-[1fr_76px_48px_92px] items-center py-3 text-[13px] sm:grid-cols-[1fr_90px_60px_104px]",
          i < presentaciones.length - 1 && "border-b border-white/8"
        )}
      >
        <span className="flex min-w-0 items-center gap-3">
          <Miniatura prenda={p.prenda} color={p.color} />
          <span className="min-w-0">
            <span className="block truncate">{p.nombre}</span>
            <span className="block truncate text-[11px] text-nubea-texto-suave">{p.variante}</span>
          </span>
        </span>
        <span>{p.precio}</span>
        <span>{p.stock}</span>
        <span className="text-right"><Etiqueta tono={tono(p.estado)}>{p.estado}</Etiqueta></span>
      </div>
    ))}
  </div>
);

// ---------- Visual 2: apariencia (plantilla, portada y banners) ----------
const Campo = ({ etiqueta, valor }: { etiqueta: string; valor: string }) => (
  <div>
    <p className="mb-1.5 text-[12px] text-nubea-texto-suave">{etiqueta}</p>
    <p className="rounded-[10px] border border-white/8 bg-nubea-fondo/60 px-3 py-2 text-[13px]">{valor}</p>
  </div>
);

const VisualApariencia = () => (
  <div aria-hidden="true" className="flex gap-6">
    <div className="hidden w-[210px] shrink-0 flex-col gap-4 sm:flex">
      <div>
        <p className="mb-1.5 text-[12px] text-nubea-texto-suave">Plantilla</p>
        <div className="flex items-center gap-3 rounded-[12px] border border-nubea-acento bg-nubea-acento/12 p-2.5">
          <span className="h-9 w-12 rounded-md bg-linear-to-br from-[#F6F8FB] to-[#C9D3DE]" />
          <span className="text-[13px] font-semibold">Minimalista</span>
          <Check className="ml-auto size-4 text-nubea-acento" />
        </div>
      </div>
      <Campo etiqueta="Título de inicio" valor="Lino para el verano" />
      <div className="flex items-center justify-between">
        <span className="text-[13px]">Mostrar carrusel</span>
        <span className="flex h-5 w-9 items-center justify-end rounded-full bg-nubea-acento p-0.5">
          <span className="size-4 rounded-full bg-white" />
        </span>
      </div>
      <div>
        <p className="mb-1.5 text-[12px] text-nubea-texto-suave">Banners</p>
        <div className="flex flex-col gap-2">
          {["Banner 1 · /coleccion/verano", "Banner 2 · /coleccion/basicos"].map((b, i) => (
            <span key={b} className="flex items-center gap-2 rounded-[10px] border border-white/8 px-2 py-2 text-[12px]">
              <GripVertical className="size-3.5 text-nubea-texto-suave" />
              <span className={cn("h-5 w-8 shrink-0 rounded bg-linear-to-r", i === 0 ? "from-[#22303F] to-[#A9BFC9]" : "from-[#5A4632] to-[#C9A97E]")} />
              <span className="truncate">{b}</span>
            </span>
          ))}
        </div>
      </div>
    </div>

    <div className="flex flex-1 flex-col gap-3 rounded-2xl bg-[#F6F8FB] p-5 text-[#0F172A]">
      <p className="font-display text-[22px]">Mar &amp; Lino</p>
      <div className="relative h-[120px] overflow-hidden rounded-xl bg-linear-to-r from-[#22303F] via-[#4F6B7D] to-[#A9BFC9]">
        <span className="absolute bottom-2.5 left-1/2 flex -translate-x-1/2 gap-1.5">
          <span className="h-1.5 w-4 rounded-full bg-white" />
          <span className="size-1.5 rounded-full bg-white/50" />
        </span>
      </div>
      <div>
        <p className="font-display text-[20px] leading-tight">Lino para el verano</p>
        <p className="text-[12px] text-[#475569]">Prendas frescas, hechas en Lima.</p>
      </div>
      <div className="grid grid-cols-3 gap-2.5">
        {[0, 1, 2].map((i) => (
          <div key={i} className="h-[78px] rounded-[10px] bg-linear-to-br from-[#E3E8EA] to-[#9DB0BA]" />
        ))}
      </div>
    </div>
  </div>
);

// ---------- Visual 3: pedido con IGV y pago con Mercado Pago ----------
const VisualPedidos = () => (
  <div aria-hidden="true" className="flex flex-col gap-4">
    <div className="rounded-[18px] border border-white/8 bg-nubea-superficie-2 p-5">
      <div className="flex items-center justify-between">
        <p className="text-[15px] font-semibold">Pedido #1042</p>
        <Etiqueta tono="ok">Pagado</Etiqueta>
      </div>
      <div className="mt-4 flex flex-col gap-2.5 text-[13px]">
        <div className="flex justify-between gap-4">
          <span>Camisa de lino · M · Blanco <span className="text-nubea-texto-suave">×2</span></span>
          <span>S/ 178.00</span>
        </div>
        <div className="flex justify-between gap-4">
          <span>Polo básico · S · Negro <span className="text-nubea-texto-suave">×1</span></span>
          <span>S/ 49.00</span>
        </div>
      </div>
      <div className="mt-4 flex flex-col gap-1.5 border-t border-white/8 pt-4 text-[13px] text-nubea-texto-suave">
        <div className="flex justify-between"><span>Subtotal</span><span>S/ 192.37</span></div>
        <div className="flex justify-between"><span>IGV (18%)</span><span>S/ 34.63</span></div>
        <div className="flex justify-between text-[15px] font-semibold text-nubea-texto"><span>Total</span><span>S/ 227.00</span></div>
      </div>
      <p className="mt-4 rounded-[10px] bg-nubea-petroleo/40 px-3 py-2 text-[12px] text-nubea-petroleo-claro">
        Pago aprobado con Mercado Pago
      </p>
    </div>
    {[
      { id: "#1043", detalle: "Stock reservado mientras el cliente paga", estado: "Pendiente de pago", t: "alerta" as const },
      { id: "#1041", detalle: "El stock reservado volvió al inventario", estado: "Cancelado", t: "neutro" as const },
    ].map((p) => (
      <div key={p.id} className="flex items-center justify-between gap-3 rounded-[14px] bg-nubea-superficie-2 px-[18px] py-3">
        <div className="min-w-0">
          <p className="text-[14px]">Pedido {p.id}</p>
          <p className="truncate text-[12px] text-nubea-texto-suave">{p.detalle}</p>
        </div>
        <Etiqueta tono={p.t}>{p.estado}</Etiqueta>
      </div>
    ))}
  </div>
);

// ---------- Extras ----------
const extras = [
  { icono: Store, titulo: "Varias tiendas, una cuenta", texto: "Administra más de una tienda con el mismo usuario." },
  { icono: Link2, titulo: "Tu propio enlace", texto: "Cada tienda tiene su dirección propia, por ejemplo /tienda/mi-tienda, lista para compartir." },
  { icono: LogIn, titulo: "Ingreso con Google", texto: "Tú y tus clientes pueden entrar con correo o con Google." },
  { icono: MapPin, titulo: "Direcciones de todo el Perú", texto: "Tus clientes guardan sus direcciones con departamento, provincia y distrito." },
];

// ---------- Sección ----------
export const FuncionesInicio = () => (
  <section
    id="funciones"
    className="scroll-mt-[88px] bg-nubea-fondo px-5 pt-[clamp(24px,4vw,40px)] pb-[clamp(88px,10vw,140px)] text-nubea-texto"
  >
    <div className="mx-auto max-w-[1200px]">
      <div className="mx-auto max-w-[900px] text-center">
        <p className="text-[13px] font-semibold tracking-[0.2em] text-nubea-acento uppercase">Todo en un solo lugar</p>
        <h2 className="mt-4 font-display text-[clamp(40px,4.6vw,64px)] leading-[1.05] tracking-[-0.01em]">
          Lo que tu negocio necesita, <span className="italic text-nubea-acento">nada que sobre</span>.
        </h2>
        <p className="mx-auto mt-5 max-w-[600px] text-[clamp(16px,1.3vw,18px)] leading-[1.6] text-nubea-texto-suave">
          Herramientas pensadas para emprendedores: simples de usar y listas para crecer contigo.
        </p>
      </div>

      <div className="mt-[clamp(64px,8vw,96px)] flex flex-col gap-[clamp(72px,8vw,96px)]">
        <Fila
          etiqueta="Catálogo e inventario"
          titulo="Tus productos, siempre en orden."
          descripcion="Organiza por categorías y colecciones, sube varias fotos por producto y crea presentaciones con su propio SKU, precio y stock."
          puntos={["Hasta 3 variantes por producto: talla, color o lo que vendas", "Stock mínimo para detectar lo que se está agotando"]}
          visual={<VisualCatalogo />}
        />
        <Fila
          invertida
          etiqueta="Apariencia"
          titulo={<>Una portada que se siente <span className="italic text-nubea-acento">tuya</span>.</>}
          descripcion="Elige una plantilla, escribe el título y la descripción de tu inicio y sube banners para el carrusel de tu tienda."
          puntos={["Banners con enlace y en el orden que elijas", "Muestra u oculta el carrusel cuando quieras"]}
          visual={<VisualApariencia />}
        />
        <Fila
          etiqueta="Pedidos y pagos"
          titulo="Cobra sin enredos con Mercado Pago."
          descripcion="Vincula tu cuenta de Mercado Pago y cada pago llega directo a ella. Tus precios ya incluyen IGV y cada pedido guarda su detalle."
          puntos={["El stock se reserva mientras tu cliente paga", "Pedidos pendientes, pagados o cancelados en tu panel"]}
          visual={<VisualPedidos />}
        />
      </div>

      <div className="mt-[clamp(72px,8vw,112px)]">
        <p className="text-center text-[13px] font-semibold tracking-[0.2em] text-nubea-acento uppercase">Y además</p>
        <ul className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {extras.map(({ icono: Icono, titulo, texto }) => (
            <li key={titulo} className="rounded-[20px] border border-white/8 bg-nubea-superficie p-6">
              <span className="grid size-11 place-items-center rounded-xl bg-nubea-acento/12 text-nubea-acento">
                <Icono className="size-5" />
              </span>
              <h3 className="mt-5 text-[17px] font-semibold">{titulo}</h3>
              <p className="mt-2 text-[15px] leading-[1.55] text-nubea-texto-suave">{texto}</p>
            </li>
          ))}
        </ul>
      </div>
    </div>
  </section>
);
