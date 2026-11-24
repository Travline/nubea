// Códigos de plantilla que la vitrina sabe dibujar.
// Deben coincidir con el campo `codigo` de la tabla `plantilla`.
// Única lista del store; la misma que PLANTILLAS_VISIBLES del cms. Deben existir en la tabla `plantilla` (V1 + V15).
export const PLANTILLAS_DISPONIBLES = ['minimal', 'boutique', 'audaz', 'gourmet', 'natural'] as const
export const PLANTILLA_POR_DEFECTO = 'minimal'

export const plantillaDisponible = (codigo: string | null | undefined) =>
  !!codigo && (PLANTILLAS_DISPONIBLES as readonly string[]).includes(codigo)
