import { Button } from "@/components/ui/button";
import { Link } from "react-router-dom";

export const NotFound = () => {
  return (
    <div className="flex flex-col items-center justify-center gap-3 min-h-screen bg-background text-foreground">
      <h1 className="text-4xl font-bold">404</h1>
      <p className="text-lg">Página no encontrada</p>
      <Button variant="default" size="lg" className="">
        <Link to="/">Volver a la página principal</Link>
      </Button>
    </div>
  );
};