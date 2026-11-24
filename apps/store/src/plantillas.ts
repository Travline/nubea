// Códigos de plantilla que la vitrina sabe dibujar.
// Deben coincidir con el campo `codigo` de la tabla `plantilla`.
export const PLANTILLAS_DISPONIBLES = ['minimal', 'boutique', 'audaz', 'gourmet', 'natural'] as const

export const plantillaDisponible = (codigo: string | null | undefined) =>
  !!codigo && (PLANTILLAS_DISPONIBLES as readonly string[]).includes(codigo)
