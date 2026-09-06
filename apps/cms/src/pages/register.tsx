import { Button } from "@/components/ui/button"
import {
  Card,
  CardAction,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { PhoneInput } from "@/components/ui/phone-input"
import { Link } from "react-router-dom"

export const RegisterPage = () => {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background py-8 px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Crea tu cuenta</CardTitle>
          <CardAction>
            <Button variant="link">
              <Link to="/login">Inicia sesión</Link>
            </Button>
          </CardAction>
        </CardHeader>
        <CardContent>
          <form onSubmit={(e) => e.preventDefault()}>
            <div className="flex flex-col gap-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="first_name">Nombres</Label>
                  <Input
                    id="first_name"
                    name="first_name"
                    placeholder="Ej. Juan"
                    required
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="last_name">Apellidos</Label>
                  <Input
                    id="last_name"
                    name="last_name"
                    placeholder="Ej. Pérez"
                    required
                  />
                </div>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="email">Correo</Label>
                <Input
                  id="email"
                  name="email"
                  type="email"
                  placeholder="tienda@shop.com"
                  required
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="phone">Teléfono</Label>
                <PhoneInput
                  id="phone"
                  name="phone"
                  defaultCountry="PE"
                  placeholder="912 345 678"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="password">Contraseña</Label>
                  <Input
                    id="password"
                    name="password"
                    type="password"
                    placeholder="••••••••"
                    required
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="confirm_password">Confirmar contraseña</Label>
                  <Input
                    id="confirm_password"
                    name="confirm_password"
                    type="password"
                    placeholder="••••••••"
                    required
                  />
                </div>
              </div>
            </div>
          </form>
        </CardContent>
        <CardFooter className="flex-col gap-2">
          <Button type="submit" className="w-full">
            Crear cuenta
          </Button>
        </CardFooter>
      </Card>
    </div>
  )
}
