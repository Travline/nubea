# NUBEA · Guía de rediseño de interfaz (Stitch → Figma → código)

Nov 24, 2026 · @CarlosMiguel

## Objetivo y alcance

Vamos a rediseñar las dos interfaces de NUBEA para que cualquier vendedor y cualquier comprador las entiendan sin conocer la base de datos. El backend ya funciona: el trabajo es de diseño visual, lenguaje y orden de los flujos.

|  | Plataforma | Vitrina |
| --- | --- | --- |
| Carpeta | `apps/cms` | `apps/store` |
| Ruta | `/plataforma/...` | `/tienda/<slug-de-la-tienda>/...` |
| Quién la usa | Administrador general y dueños de tienda | Compradores de cada tienda |
| Qué hace | Crear y gestionar tiendas, catálogo, inventario, pedidos, apariencia, pagos | Ver productos, carrito, direcciones, pagar, ver pedidos |
| Estilo actual | Oscuro, azul | Claro, verde |

**Por qué se rediseña:** hoy las pantallas usan los nombres de las tablas (slug, SKU, presentación, `PENDIENTE_PAGO`), obligan a recorrer varias secciones para publicar un producto y se ven poco cuidadas. El detalle está en `docs/revision/analisis-frontend.md` y `docs/revision/hallazgos-flujo.md` del repositorio.

**Qué NO se toca:**

- Las rutas (URL) de las dos apps, porque el despliegue en Vercel depende de ellas.
- Los datos que se piden y se envían a la API: se pueden reorganizar en pasos o pestañas, pero no inventar campos que el backend no tiene. Si un diseño necesita un dato nuevo, se anota como pendiente de backend.
- La lógica de sesión, roles y permisos.
- El stack: React + Vite + Tailwind. La plataforma ya usa componentes estilo shadcn/ui (`components/ui/button`).

## Contexto del sistema

NUBEA es multitienda: cualquier persona se registra en la plataforma, crea su tienda y la publica en `/tienda/<slug>`. El comprador no sabe que detrás está NUBEA; solo ve la marca de la tienda.

**Roles** (una sola cuenta por persona; los roles se suman):

| Rol interno | Cómo llamarlo en pantalla | Qué ve |
| --- | --- | --- |
| `ADMIN_GENERAL` | Administrador de NUBEA | Plataforma › Administración: usuarios, tiendas, plantillas, impuestos |
| `DUENO_TIENDA` | Vendedor | Plataforma › Sus tiendas: catálogo, inventario, pedidos, apariencia, pagos |
| `CLIENTE` | Comprador | Vitrina de cada tienda: carrito, direcciones, pedidos |

**Conceptos del catálogo**, de lo general a lo concreto:

1. **Categoría** ("Televisores"): agrupa productos. Cada producto pertenece a una.
2. **Producto** ("Televisor HD 4K"): nombre, descripción, precio y fotos.
3. **Opciones** (en la BD, "variantes"): Color, Talla, Tamaño. Se definen una vez por tienda y se reutilizan.
4. **Versión del producto** (en la BD, "presentación"): la combinación concreta que se vende, por ejemplo "55 pulgadas / Negro". Tiene su propio código (SKU), puede tener un precio distinto y tiene su propio stock. Un producto sin opciones tiene una sola versión, que el usuario no debería ver.
5. **Colección** ("Ofertas de verano"): selección libre de productos para destacar.

**Estados que el diseño debe contemplar:**

- Tienda: en preparación → abierta → suspendida o cerrada. Suspendida = el vendedor puede mirar pero no editar.
- Producto: borrador → visible en la tienda → oculto. Para publicar hace falta tienda abierta, categoría activa y al menos una versión activa con stock.
- Pedido: pendiente de pago (con un plazo para pagar) → pagado o cancelado.
- Pago: pagado, rechazado, devolución en curso, dinero devuelto.

**Flujos principales:**

- Vendedor: registrarse → crear tienda → abrirla → crear categoría → crear producto con sus versiones, stock y fotos → publicar → conectar Mercado Pago → atender pedidos.
- Comprador: entrar a la tienda → ver producto y elegir opciones → carrito → dirección → resumen → pagar → seguir su pedido.
- Administrador: ver usuarios y tiendas, dar o quitar roles, suspender, gestionar plantillas e impuestos.

## Flujo de trabajo

El diseño pasa por cuatro herramientas, y cada una tiene un rol distinto. Stitch sirve para explorar rápido, Figma es la fuente de verdad, y Claude Code convierte Figma en código.

&#91;embedded content: flujo de trabajo · 5 pasos, 1 revisión\]

Ninguna pantalla pasa a código sin que Miguel la apruebe en Figma, y nada llega a `main` sin probarse en local.

1. **Stitch: explorar.** Generar 2 o 3 propuestas por pantalla con los prompts de la sección correspondiente. Elegir la mejor y exportarla a Figma (Stitch tiene la opción "Copy to Figma").
2. **Figma: ordenar.** Rehacer la propuesta elegida con componentes, variables y Auto Layout, siguiendo las reglas para Figma. Aquí se corrigen los textos con el glosario.
3. **Revisión.** Compartir el enlace del frame con Miguel. Los comentarios se dejan en Figma, sobre el elemento.
4. **Claude Code: implementar.** Con el diseño aprobado, una pantalla por pedido, en la rama de trabajo.
5. **Probar y pasar a main.** Revisar con la checklist en local y, cuando el bloque esté completo, pasar a `main`.

Orden sugerido: primero los fundamentos y componentes (una vez), después las 8 pantallas imprescindibles, y por último las importantes.

## Lineamientos visuales y de lenguaje

Las dos apps tienen públicos distintos y pueden verse distintas, pero cada una debe ser coherente consigo misma.

**Plataforma (vendedor y admin):** herramienta de trabajo. Prioriza claridad y densidad moderada: barra lateral de navegación por tienda, tablas con filtros, formularios por pasos, tema claro y oscuro (ya existe el selector). Referencia de estilo: el panel de Shopify o Stripe.

**Vitrina (comprador):** escaparate. Prioriza fotos grandes, precio y botón de compra visibles, pocos textos, móvil primero. Es una plantilla única y configurable: el vendedor cambia logo, colores, título, descripción e imágenes destacadas, no el diseño.

**Reglas para las dos:**

- Diseñar primero en móvil (390 px) y después en escritorio (1440 px).
- Cada pantalla con sus estados: cargando, vacío, con datos, error, sin permiso.
- Los errores se muestran junto al campo que falló, con un texto que diga qué hacer.
- Las acciones que no se pueden hacer se ven deshabilitadas con el motivo ("Falta cargar stock"), en vez de fallar al guardar.
- Moneda siempre con formato: S/ 1,200.00. Fechas en español: 24 nov 2026, 18:40.
- Ningún identificador interno a la vista (UUID). Los pedidos se muestran como "Pedido #1042" (pendiente de backend: número corto).

**Glosario: lo que dice la BD → lo que dice la pantalla**

| En la BD / hoy | Vendedor | Comprador |
| --- | --- | --- |
| Slug | Dirección web (se genera sola) | — |
| SKU | Código interno (opcional, se sugiere uno) | — |
| Presentación | Versión del producto | Opción elegida (Color, Talla) |
| Variante / valor | Opción / valor (Color / Rojo) | Color, Talla… |
| Precio base / precio especial | Precio / precio distinto para esta versión | Precio |
| Stock actual / stock mínimo | Unidades disponibles / avisarme cuando queden | Disponible, Últimas N unidades, Agotado |
| Tienda BORRADOR / ACTIVA / SUSPENDIDA / CERRADA | En preparación / Abierta / Suspendida / Cerrada | Esta tienda no está disponible |
| Producto BORRADOR / PUBLICADO / INACTIVO | Borrador / Visible en tu tienda / Oculto | — |
| Pedido PENDIENTE\_PAGO / PAGADO / CANCELADO | Esperando pago / Pagado / Cancelado | Pendiente de pago / Pago confirmado / Cancelado |
| Pago APROBADO / RECHAZADO / REEMBOLSADO | Cobrado / Rechazado / Dinero devuelto | Pagado / Pago rechazado / Dinero devuelto |
| Cuenta de pago PENDIENTE / ACTIVA / ERROR | Sin conectar / Lista para cobrar / Hay un problema | — |
| Banner / "Enlace HTTPS" | Imagen destacada / Al hacer clic, ir a… | — |
| Mostrar carrusel | Mostrar imágenes destacadas en la portada | — |
| Plantilla | Diseño de la tienda | — |
| ADMIN\_GENERAL / DUENO\_TIENDA / CLIENTE | Administrador / Vendedor / Comprador | — |

La tabla completa está en la sección 4 de `docs/revision/analisis-frontend.md`.

## Pantallas a diseñar

Empieza por las imprescindibles, en este orden: son las que hoy bloquean o confunden. Cada pantalla se entrega en móvil y escritorio, con los estados indicados.

### Imprescindibles

| # | Pantalla | App | Qué debe tener | Estados |
| --- | --- | --- | --- | --- |
| 1 | Estructura del panel de tienda | Plataforma | Barra lateral (Inicio, Productos, Categorías, Colecciones, Inventario, Pedidos, Apariencia, Pagos), nombre y estado de la tienda arriba, botón "Ver mi tienda" | Tienda en preparación, abierta, **suspendida** (aviso fijo arriba y todo en solo lectura) |
| 2 | Crear y editar producto | Plataforma | Una sola página por pasos o secciones: datos básicos → opciones → versiones con precio y stock en la misma tabla → fotos → publicar | Sin opciones (una versión oculta), con opciones (tabla de combinaciones), errores por campo |
| 3 | Lista de requisitos para publicar | Plataforma | Recuadro dentro del producto: ✔ tienda abierta, ✔ categoría activa, ✖ falta stock; botón Publicar deshabilitado hasta cumplirlos | Todo listo, faltan requisitos |
| 4 | Detalle de producto | Vitrina | Galería, nombre, precio, un selector por opción (Color: círculos, Talla: botones), cantidad, disponibilidad, Agregar al carrito | Disponible, últimas unidades, agotado, sin opciones |
| 5 | Checkout por pasos | Vitrina | Carrito → dirección (buscar en Google primero, completar después) → resumen con subtotal, IGV y total → Pagar con Mercado Pago | Sin sesión ("Continúa como… / usar otra cuenta"), sin direcciones, pagos no disponibles |
| 6 | Detalle de pedido del comprador | Vitrina | Número de pedido, línea de tiempo (creado → pago confirmado), plazo para pagar, productos, dirección, total | Pendiente (con cuenta regresiva), pagado, cancelado, en devolución |
| 7 | Pagos de la tienda | Plataforma | Estado de cobro en lenguaje claro, qué falta para cobrar, cuenta enmascarada, modo prueba o real, qué hacer si hay error | Sin conectar, lista para cobrar, desconectada, con error |
| 8 | Componentes de estado | Las dos | Etiqueta (badge) de estado con color y texto del glosario, avisos, mensajes vacíos, errores | Uno por cada estado del glosario |

### Importantes

| # | Pantalla | App | Qué debe tener |
| --- | --- | --- | --- |
| 9 | Inicio de la tienda | Plataforma | Tareas pendientes (abrir tienda, conectar pagos, productos sin stock, pedidos nuevos) y números del día |
| 10 | Pedidos del vendedor | Plataforma | Lista con filtros por estado y fecha, detalle en su propia página, botón de devolución |
| 11 | Lista de productos | Plataforma | Miniatura, nombre, precio, estado, stock total, búsqueda y filtros |
| 12 | Apariencia | Plataforma | Formulario a la izquierda, vista previa de la portada a la derecha; logo, colores, título, imágenes destacadas |
| 13 | Galería de fotos del producto | Plataforma | Arrastrar para ordenar, marcar principal, asignar fotos a versiones |
| 14 | Portada de la vitrina | Vitrina | Cabecera con logo, imágenes destacadas, categorías, grilla de productos; estado "Próximamente" si no hay productos |
| 15 | Inicio de sesión y registro | Las dos | Formularios simples; en la vitrina, "Continúa como correo@…" si ya hay sesión |
| 16 | Administración | Plataforma | Usuarios y tiendas con nombre y correo del dueño (no UUID), detalle de usuario, acciones de rol y suspensión |

Las deseables (mi perfil, cerrar tienda, login con Google, envíos) necesitan backend nuevo: no se diseñan todavía.

## Prompts para Google Stitch

Stitch funciona mejor con una pantalla por prompt y el contexto repetido en cada uno. Pega primero el prompt base y después el de la pantalla. Genera siempre las dos versiones (móvil y escritorio), y si un resultado sale bien, pide variaciones sobre ese en vez de empezar de cero.

**Prompt base de la plataforma** (antes de cada pantalla de la plataforma):

```markdown
Diseña una pantalla para NUBEA, una plataforma multitienda tipo Shopify en español (Perú, moneda S/).
Usuario: un vendedor pequeño sin conocimientos técnicos que administra su tienda online.
Estilo: panel de administración limpio y profesional, tipografía sans serif, tema claro con versión oscura,
color principal azul #2563EB, bordes redondeados de 8 px, mucho espacio en blanco.
Estructura: barra lateral con Inicio, Productos, Categorías, Colecciones, Inventario, Pedidos, Apariencia, Pagos;
arriba el nombre de la tienda, su estado y el botón "Ver mi tienda".
Usa lenguaje simple: nunca "slug", "SKU" ni "presentación"; usa "dirección web", "código interno" y "versión".
```

**Prompt base de la vitrina** (antes de cada pantalla de la vitrina):

```markdown
Diseña una pantalla de tienda online en español (Perú, moneda S/) para una tienda de ejemplo llamada "MiguelTech"
que vende tecnología. Diseño móvil primero, moderno y minimalista, fotos grandes, fondo claro,
un color de acento configurable (usa verde oscuro #2F4F3A como ejemplo), botón de compra muy visible.
El comprador no debe ver la marca de la plataforma, solo la de la tienda.
```

**Prompts por pantalla:**

1. **Panel de tienda suspendida:** "Pantalla de inicio del panel de una tienda en estado Suspendida. Aviso fijo arriba en color ámbar: 'Tu tienda está suspendida por la plataforma. Puedes consultar tus datos, pero no modificarlos.' Todas las acciones de edición se ven deshabilitadas. Muestra también la misma pantalla con la tienda Abierta, con tareas pendientes: conectar pagos, 2 productos sin stock, 3 pedidos nuevos."
2. **Crear producto:** "Página para crear un producto en secciones verticales: 1) Datos básicos (nombre, descripción larga, categoría, precio). 2) Opciones: interruptor '¿Este producto tiene opciones como color o talla?'; si está activo, permite elegir hasta 3 opciones y sus valores. 3) Versiones: tabla con una fila por combinación (ej. 55 pulgadas / Negro) y columnas precio, código interno sugerido automáticamente y unidades disponibles. 4) Fotos con arrastrar y soltar. A la derecha, un recuadro 'Listo para publicar' con una lista de requisitos marcados con check o cruz y el botón Publicar."
3. **Lista de productos:** "Tabla de productos con miniatura, nombre, categoría, precio, estado (Borrador, Visible, Oculto) como etiquetas de color y unidades disponibles. Buscador y filtros por estado y categoría arriba, botón 'Nuevo producto'. Incluye el estado vacío: 'Aún no tienes productos'."
4. **Pagos de la tienda:** "Pantalla de cobros: tarjeta con el estado de Mercado Pago ('Lista para cobrar' en verde o 'Sin conectar' en gris), cuenta enmascarada (\*\*\*\*6789), fecha de conexión, etiqueta 'Modo prueba'. Lista de pasos que faltan para empezar a cobrar. Variante con estado de error y qué hacer."
5. **Detalle de producto (vitrina):** "Página de producto: galería de fotos con miniaturas, nombre, precio grande, selector de Color con círculos de color y selector de Tamaño con botones, selector de cantidad, texto de disponibilidad ('¡Últimas 3 unidades!'), botón 'Agregar al carrito'. Variante agotada con el botón deshabilitado."
6. **Checkout (vitrina):** "Checkout en 3 pasos con indicador de progreso: 1) Carrito con productos, cantidades y quitar. 2) Dirección de entrega: buscador de dirección con autocompletado y debajo los campos referencia, teléfono y alias; o elegir una dirección guardada. 3) Resumen: productos, dirección, subtotal, IGV 18 %, total y botón 'Pagar con Mercado Pago'. Arriba, aviso 'Continúas como juan@correo.com · ¿No eres tú?'."
7. **Detalle de pedido (vitrina):** "Detalle del Pedido #1042: línea de tiempo vertical (Pedido creado, Pago confirmado), aviso 'Tienes hasta las 18:40 para pagar' con botón 'Pagar ahora' cuando está pendiente, lista de productos, dirección de entrega y total. Variantes: pagado, cancelado, devolución en curso."
8. **Portada de la vitrina:** "Portada de tienda: cabecera con logo, buscador y carrito; carrusel de imágenes destacadas; fila de categorías; grilla de productos con foto, nombre y precio. Variante sin productos: 'Próximamente'."

Para las pantallas importantes que no tienen prompt aquí, sigue el mismo formato: prompt base, qué contiene la pantalla según la tabla de pantallas, y sus estados.

## Reglas para Figma

Lo que sale de Stitch es una referencia visual, no el diseño final. En Figma se ordena en componentes reutilizables, para que Claude Code los convierta en componentes de React con una correspondencia uno a uno.

**Organización del archivo** (un archivo "NUBEA · Diseño" con estas páginas):

| Página | Contenido |
| --- | --- |
| 00 Fundamentos | Colores, tipografía, espaciado, radios, sombras, iconos, como variables de Figma |
| 01 Componentes | Botón, campo de texto, selector, etiqueta de estado, tarjeta, tabla, aviso, modal, barra lateral, cabecera |
| 02 Plataforma | Pantallas del panel, una sección por pantalla, móvil y escritorio |
| 03 Vitrina | Pantallas de la tienda, igual |
| 99 Stitch | Lo exportado de Stitch, como referencia; no se edita |

**Variables (tokens):** usa variables de Figma con modos Claro y Oscuro para la plataforma. Nombres que coincidan con los de Tailwind y shadcn/ui, porque el código ya los usa: `primary`, `primary-foreground`, `background`, `foreground`, `muted`, `muted-foreground`, `border`, `destructive`, `card`. Espaciado en múltiplos de 4. Para la vitrina, un token `accent` que el vendedor podrá cambiar.

**Componentes:**

- Cada componente con sus variantes como propiedades de Figma, por ejemplo `Button`: `variant = primary | secondary | ghost | destructive`, `size = sm | md | lg`, `state = default | hover | disabled | loading`.
- `StatusBadge` con una variante por estado del glosario (tienda, producto, pedido, pago, cuenta de pago), con el texto ya traducido.
- Nombres en inglés y en PascalCase, iguales a como se llamarán en el código: `ProductCard`, `StatusBadge`, `StoreSidebar`, `CheckoutSteps`, `PublishChecklist`.
- Si un componente ya existe en `apps/cms/src/components/ui` (por ejemplo `button`), usa el mismo nombre y las mismas variantes.

**Pantallas:**

- Construidas solo con instancias de componentes; nada suelto que no sea componente.
- Auto Layout en todo: así el código sale con flex y grid, no con posiciones fijas.
- Frames de 390 px (móvil) y 1440 px (escritorio), con el nombre `<App> / <Pantalla> / <Estado> / <Móvil|Escritorio>`, por ejemplo `Plataforma / Crear producto / Con opciones / Escritorio`.
- Textos reales del glosario, nunca "Lorem ipsum".

**Entregables por pantalla:** el frame en todos sus estados, una nota corta con qué hace cada acción, y qué dato de la API usa cada campo si no es obvio.

## Entrega e integración con Claude Code

La integración se hace por lotes pequeños: primero los componentes, después una pantalla a la vez, cada una en su commit y en la rama `feature/miguel-local` (nunca directo en `main`, que se despliega solo).

**Cómo pasarle el diseño a Claude Code:**

1. Conectar Figma a Claude Code (servidor MCP de Figma) para que lea los frames y las variables directamente. Si no se puede, exportar cada frame como PNG y pasarlo junto con las medidas.
2. Primer lote, los fundamentos: pedir que pase las variables de Figma a los tokens de Tailwind (`global.css` de cada app) y que cree o actualice los componentes de `01 Componentes`.
3. Después, una pantalla por pedido, con este prompt:

```markdown
Implementa la pantalla "<App> / <Pantalla>" del Figma <enlace al frame> en <apps/cms | apps/store>.
- Usa solo los componentes existentes de components/ui; si falta uno, créalo primero según Figma.
- Conecta los datos con los endpoints que ya usa la pantalla actual; no cambies la API.
  Si el diseño pide un dato que la API no devuelve, no lo inventes: avísame.
- Implementa todos los estados del Figma (cargando, vacío, error, sin permiso).
- Textos según el glosario de docs/revision/guia-rediseno.md.
- Mantén las rutas actuales.
Muéstrame el plan antes de cambiar archivos. No hagas commit.
```

**Checklist de revisión antes de cada commit:**

- [ ] Se ve igual que el Figma en móvil (390 px) y en escritorio (1440 px).
- [ ] Ningún término técnico a la vista (slug, SKU, presentación, estados en mayúsculas, UUID).
- [ ] Probado con datos reales en local: crear, editar, error y estado vacío.
- [ ] Probado con una tienda suspendida: se puede mirar y no se puede editar.
- [ ] Tema claro y oscuro (plataforma).
- [ ] Las rutas siguen funcionando, incluida la recarga de la página.

**Lo que no debe cambiar:** las rutas, las llamadas a la API, el manejo de sesión y CSRF (`autenticacion/api.ts`), las variables `VITE_*` y la configuración de despliegue (`vercel.json`, `server.js`, `scripts/compilar.mjs`).

Guarda una copia de este documento como `docs/revision/guia-rediseno.md` para que Claude Code la lea.
