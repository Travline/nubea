import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ChartNoAxesCombined, Gauge, ShieldCheck } from "lucide-react"

export const HomeInfo = () => {
  return (
    <div className="p-5 sm:pt-0 w-full max-w-250 m-auto grid grid-cols-1 gap-8 items-stretch md:grid-cols-3 bg-background text-foreground">
      <Card className="h-full bg-secondary">
        <CardHeader className="flex flex-row items-center gap-4">
          <ChartNoAxesCombined className="w-10 h-10 text-primary" />
          <CardTitle>Fácil Gestión</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          <p>Crea tu tienda en cuestión de minutos y gestiona tus productos sin esfuerzo ni conocimientos técnicos</p>
        </CardContent>
      </Card>
      <Card className="h-full bg-secondary">
        <CardHeader className="flex flex-row items-center gap-4">
          <ShieldCheck className="w-10 h-10 text-primary" />
          <CardTitle>Pagos Seguros</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          <p>Protege las transacciones de tus clientes y recibe pagos de forma segura y rápida a través de Mercado Pago</p>
        </CardContent>
      </Card>
      <Card className="h-full bg-secondary">
        <CardHeader className="flex flex-row items-center gap-4">
          <Gauge className="w-10 h-10 text-primary" />
          <CardTitle>Velocidad Extrema</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          <p>Tus clientes no deben esperar gracias al rendimiento optimizado para una experiencia fluida y agradable</p>
        </CardContent>
      </Card>
    </div>
  )
}