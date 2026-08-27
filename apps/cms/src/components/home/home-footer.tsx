export const HomeFooter = () => {
  return (
    <div className="flex flex-col bg-secondary items-center justify-center gap-4 px-4 py-8">
      <p className="text-muted-foreground text-center">
        © {new Date().getFullYear()} Nubea.
      </p>
      <ul className="flex flex-wrap justify-center gap-4 text-sm">
        <li><a href="/terms" className="text-muted-foreground hover:text-primary">
          Términos y Condiciones
        </a></li>
        <li><a href="/privacy" className="text-muted-foreground hover:text-primary">
          Política de Privacidad
        </a></li>
        <li><a href="/contact" className="text-muted-foreground hover:text-primary">
          Contacto
        </a></li>
      </ul>
    </div>
  )
}