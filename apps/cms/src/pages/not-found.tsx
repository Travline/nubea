import { Button } from "@/components/ui/button";

export const NotFound = () => {
  return (
    <div className="flex flex-col items-center justify-center gap-3 min-h-screen bg-background text-foreground">
      <h1 className="text-4xl font-bold">404</h1>
      <p className="text-lg">Página no encontrada</p>
      <Button onClick={() => window.location.href = "/"} variant="default" size="lg" className="">
        Volver a la página principal
      </Button>
    </div>
  );
};