import { Link, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import {
  ArrowRight,
  ChartNoAxesCombined,
  Gauge,
  PackagePlus,
  Rocket,
  ShieldCheck,
  Store,
} from "lucide-react"

export const HomeInfo = () => {
  const navigate = useNavigate()

  const features = [
    {
      icon: ChartNoAxesCombined,
      title: "Fácil Gestión",
      description: "Crea tu tienda en cuestión de minutos y gestiona tus productos sin esfuerzo ni conocimientos técnicos.",
    },
    {
      icon: ShieldCheck,
      title: "Pagos Seguros",
      description: "Protege las transacciones de tus clientes y recibe pagos de forma segura y rápida a través de Mercado Pago.",
    },
    {
      icon: Gauge,
      title: "Velocidad Extrema",
      description: "Tus clientes no deben esperar gracias al rendimiento optimizado para una experiencia fluida y agradable.",
    },
  ]

  const steps = [
    {
      step: "01",
      icon: Store,
      title: "Crea tu cuenta",
      description: "Regístrate en menos de dos minutos y personaliza el nombre, logo y estilo de tu tienda.",
    },
    {
      step: "02",
      icon: PackagePlus,
      title: "Publica tus productos",
      description: "Carga fotos, añade variantes, gestiona tu inventario y establece precios sin complicaciones.",
    },
    {
      step: "03",
      icon: Rocket,
      title: "Comienza a vender",
      description: "Comparte el enlace de tu tienda, recibe pedidos al instante y procesa cobros seguros.",
    },
  ]

  return (
    <div className="flex flex-col items-center justify-center gap-16 px-4 py-16">
      {/* Sección de Confianza / Tarjetas principales */}
      <section className="w-full max-w-250 m-auto flex flex-col items-center">
        <h2 className="text-3xl font-bold text-center mb-8">Confía en Nuestra Plataforma</h2>

        <div className="w-full grid grid-cols-1 md:grid-cols-3 gap-6">
          {features.map((item) => {
            const Icon = item.icon
            return (
              <div
                key={item.title}
                className="relative flex flex-col gap-4 p-6 rounded-2xl border border-border bg-card shadow-sm hover:shadow-md transition-all group"
              >
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-primary-foreground transition-colors shrink-0">
                    <Icon className="w-6 h-6" />
                  </div>
                  <h3 className="text-xl font-bold text-foreground">{item.title}</h3>
                </div>
                <p className="text-muted-foreground text-sm leading-relaxed">{item.description}</p>
              </div>
            )
          })}
        </div>
      </section>

      {/* Sección: Cómo funciona en 3 sencillos pasos */}
      <section className="w-full max-w-250 m-auto flex flex-col items-center gap-8">
        <div className="text-center max-w-xl flex flex-col gap-2">
          <span className="text-sm font-semibold tracking-wider text-primary uppercase">Paso a paso</span>
          <h3 className="text-3xl font-bold">¿Cómo funciona Nubea?</h3>
          <p className="text-muted-foreground text-base">
            Diseñamos el camino más directo entre tus productos y tus primeros compradores.
          </p>
        </div>

        <div className="w-full grid grid-cols-1 md:grid-cols-3 gap-6">
          {steps.map((item) => {
            const Icon = item.icon
            return (
              <div
                key={item.step}
                className="relative flex flex-col gap-4 p-6 rounded-2xl border border-border bg-card shadow-sm hover:shadow-md transition-all group"
              >
                <div className="flex items-center justify-between">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-primary-foreground transition-colors">
                    <Icon className="w-6 h-6" />
                  </div>
                  <span className="text-3xl font-extrabold text-muted-foreground/30 group-hover:text-primary/40 transition-colors">
                    {item.step}
                  </span>
                </div>
                <h4 className="text-xl font-bold text-foreground">{item.title}</h4>
                <p className="text-muted-foreground text-sm leading-relaxed">{item.description}</p>
              </div>
            )
          })}
        </div>
      </section>

      {/* Sección: Call to Action Final */}
      <section className="w-full max-w-250 m-auto">
        <div className="relative overflow-hidden rounded-3xl bg-linear-to-br from-primary/15 via-secondary to-background border border-primary/20 p-8 sm:p-12 flex flex-col md:flex-row items-center justify-between gap-8 text-center md:text-left">
          <div className="flex flex-col gap-3 max-w-lg">
            <h3 className="text-3xl font-bold text-foreground">¿Listo para hacer crecer tu negocio?</h3>
            <p className="text-muted-foreground text-base">
              Crea tu tienda online ahora mismo y empieza a recibir pedidos sin intermediarios.
            </p>
          </div>
          <div className="flex flex-col sm:flex-row gap-4 shrink-0">
            <Button
              size="lg"
              className="text-base font-semibold px-8 py-6 rounded-2xl group shadow-lg"
            >
              <Link to="/register">Comenzar gratis</Link>
              <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
            </Button>
          </div>
        </div>
      </section>
    </div>
  )
}