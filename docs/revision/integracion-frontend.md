# Integración del frontend con el backend de `feature/miguel-local`

> **Fecha:** 2026-11-24 · **Rama:** `feature/frontend-diseno`, con `origin/feature/miguel-local` fusionado **sin terminar** (hay conflictos abiertos, ver §0).
> **Tipo:** análisis de solo lectura. No se modificó código.
>
> **Fuentes:**
> - `docs/revision/estado-backend.md`, `analisis-frontend.md`, `hallazgos-flujo.md`, `guia-rediseno.md` y `google-login-frontend.md`.
> - `docs/functional/modules-rules.md`.
> - El código del backend tal como queda tras el merge: controladores, DTOs, `GlobalExceptionHandler` y `MensajesUsuario`.
> - `apps/cms` y `apps/store` de esta rama.
>
> **Nota:** `analisis-frontend.md` describe el frontend **anterior** (selector de plantilla fijo en `minimal`, `<select>` "Presentación", etc.). Varias de sus observaciones ya están resueltas en esta rama. Aquí solo se lista lo que sigue pendiente.

---

## 0. Estado del merge (resolver antes que nada)

`git merge origin/feature/miguel-local` quedó a medias: existe `.git/MERGE_HEAD` y hay tres archivos con conflicto.

| Archivo | Conflicto | Resolución propuesta |
| --- | --- | --- |
| `apps/cms/.env.example` | HEAD añade `VITE_TIENDA_URL`; Miguel añade `VITE_GOOGLE_CLIENT_ID` | **Conservar las dos** variables |
| `backend/.../identidad/repository/UsuarioIdentidadRepository.java` | HEAD añade `findByUsuario_IdUsuarioAndProveedor` (lo usa `PerfilService`); Miguel añade `findByIssuerAndSubject` (lo usa el login con Google) | **Conservar los dos métodos.** Comprobar que `existsByUsuario_IdUsuarioAndProveedor` (HEAD, fuera del bloque en conflicto) sigue presente |
| `backend/.../vitrina/VitrinaController.java` | HEAD añade `precioAnterior` (precio tachado); Miguel añade `disponibilidad` y `unidadesRestantes` (últimas unidades) | **Unir los dos:** `Presentacion(id, precio, precioAnterior, disponible, opciones, disponibilidad, unidadesRestantes)`. La consulta SQL debe traer `stock_actual`, `stock_minimo` y el `CASE … precio_base` de HEAD |

Después de resolver, compilar y ejecutar los tests del backend (`VitrinaIntegracionTest` y los de identidad), y comprobar que `PerfilController` (de HEAD) sigue compilando con el `AuthService` nuevo. Recién entonces cerrar el merge con `git commit`.

---

## 1. Llamadas a la API que ya no coinciden con el backend

### 1.1 Cambios de comportamiento que rompen o falsean datos (prioridad alta)

| # | Dónde (frontend) | Qué hace hoy | Qué espera el backend nuevo | Efecto |
| --- | --- | --- | --- | --- |
| C1 | `store/src/carrito.tsx`: checkout | Envía `totalEsperado: carrito.total` | El checkout es **parcial** (RN-M4-05): compara `totalEsperado` con el total **de las líneas disponibles**. Ese valor llega en `carrito.totalAPagar` | Con alguna línea no disponible, el checkout falla siempre ("el total cambió"). **Usar `totalAPagar`** |
| C2 | `cms/src/comercio/inventario.tsx` y `catalogo.tsx`: `PUT …/inventario/{p}` | Envía `stockActual` como **unidades disponibles** | El PUT ahora es **"fijar stock tras un conteo"** (RN-M3-14): `stockActual` = unidades **en mano**. Se guarda `en mano − apartadas`. Acepta `stockEsperado` opcional (409 `CONFLICTO` si cambió) | Si hay pedidos pendientes, el stock guardado queda **menor** de lo que el vendedor escribió. Para "sumar/restar" hay que usar `POST …/inventario/{p}/ajustes {cantidad}`, y en el PUT enviar `stockEsperado` |
| C3 | `cms/src/comercio/catalogo.tsx`: crear producto o versión con stock | 1) `POST …/presentaciones`, 2) `PUT …/inventario/{id}`. SKU generado en el frontend (`skuAutomatico`) | `POST …/presentaciones` acepta **`stockInicial` y `stockMinimo`** y el **SKU es opcional** (lo genera el backend, único por tienda) | Dos llamadas no atómicas (si falla la 2.ª, queda una versión sin stock). El SKU del frontend puede chocar (409). Pasar a **una sola llamada** y no enviar `sku` si el usuario no lo escribió |
| C4 | `cms`: categorías y colecciones | El slug se genera en el frontend y se envía **siempre** | `slug` es **opcional**: si llega vacío, el backend lo genera (al crear) o lo conserva (al editar) | Funciona, pero duplica lógica y puede generar slugs distintos de los del backend. Enviar `slug` solo si el usuario lo editó en "avanzado" |
| C5 | `cms/src/comercio/inventario.tsx` | Carga todos los productos y luego **una llamada de presentaciones por producto** para agrupar | `InventarioResponse` ahora trae **`idProducto`** y `stockReservado` | Se puede agrupar con **solo** `GET …/inventario` (paginado). Quitar las N llamadas |
| C6 | `apps/store/src/vitrina.tsx` (tras resolver §0) | Usa `disponible` y el nuevo `precioAnterior` | Además existen `disponibilidad` (`DISPONIBLE`, `ULTIMAS_UNIDADES`, `AGOTADO`) y `unidadesRestantes` | No rompe (`disponible` se mantiene), pero falta "¡Últimas N unidades!" |

### 1.2 Códigos y formato de error

Formato común de error del backend:

```json
{ "status": 422, "error": "REGLA_NEGOCIO", "mensaje": "...", "campos": { "estado": "..." }, "timeStap": "..." }
```

| Código | `error` | Origen | Cómo lo trata hoy el frontend |
| --- | --- | --- | --- |
| 400 | `VALIDACION` | Bean Validation: `campos` = campo → mensaje (uno por campo) | cms: `mensajeError` **une todos** los textos en una línea; store: **descarta `campos`** |
| 400 | `DATOS_INVALIDOS` **(nuevo)** | `ValidacionCampoException` y restricciones de BD traducidas (`campos` con el campo) | Igual que arriba |
| 400 | `JSON_INVALIDO` | Cuerpo mal formado | Mensaje genérico (correcto) |
| 404 | `NO_ENCONTRADO` | Recurso inexistente | Mensaje (correcto) |
| 404 | `TIENDA_NO_DISPONIBLE` **(nuevo)** | Vitrina, carrito o checkout de una tienda suspendida o cerrada | store: `vitrina.tsx` muestra "no existe o no está disponible" (aceptable). El **carrito y el checkout** muestran el mensaje en crudo, sin estado dedicado |
| 409 | `DUPLICADO` | Correo, documento, slug, SKU o combinación repetidos; `campos` con el campo | Mensaje en línea; no se marca el campo |
| 409 | `CONFLICTO` **(nuevo)** | `ConflictoCampoException`: `stockEsperado` distinto ("El stock cambió mientras lo editabas…") | **No se usa** (`stockEsperado` no se envía) |
| 409 | `CONFLICTO` | `TiendaNoModificableException` (tienda SUSPENDIDA o CERRADA). El mensaje es técnico: *"La tienda se encuentra en estado 'SUSPENDIDA' y no permite modificaciones"* | Se muestra ese texto tal cual. **Debe anticiparse** (§2.1) y, si llega, traducirse |
| 409 | `INTEGRIDAD` | Restricción de BD sin traducir | "Los datos no cumplen las reglas…" (genérico) |
| 422 | `REGLA_NEGOCIO` | Reglas como publicar sin requisitos (`campos.estado`) | Mensaje en línea |
| 401 | `GOOGLE_TOKEN_INVALIDO` **(nuevo)** | Login con Google | No existe la integración |
| 503 | `GOOGLE_NO_DISPONIBLE` **(nuevo)** | Login con Google sin configurar | No existe la integración |
| 401 | *(sin cuerpo)* | Sesión vencida, credenciales malas, cuenta suspendida | cms y store emiten `sesion-vencida` (correcto) |
| 403 | *(sin cuerpo)* | Sin permiso (por ejemplo, dueño sin rol DUENO_TIENDA) o sin CSRF | Mensaje genérico "No se pudo completar la solicitud." Falta el texto de `hallazgos-flujo.md` §2 |

**Conclusión:** ninguna app muestra los errores **junto al campo**, como pide la guía de rediseño. El store además pierde `campos` y el código `error`.

### 1.3 Campos nuevos que la API devuelve y el frontend ignora

| Respuesta | Campos nuevos | Dónde usarlos |
| --- | --- | --- |
| `GET …/carrito` | `subtotal`, `igv`, `tasaIgv`, `totalAPagar` | Desglose del carrito y `totalEsperado` (C1) |
| `POST …/checkout` (`PagosStore.Estado`) | `numero`, `productosApartados`, `vencimiento` | "Pedido #1042", aviso "N productos quedaron en tu carrito", plazo para pagar |
| `GET …/pedidos` y detalle (`Pedido`) | `numero` | Cambiar "Pedido 6f1c…" por "#1042" (cms y store) |
| `GET …/pedidos/{p}/pago` | `numero`, `vencimiento` | Detalle de pedido |
| `GET …/inventario` | `idProducto`, `stockReservado`, `fechaActualizacion` | Agrupar por producto (C5), mostrar "N apartadas", "actualizado el…" |
| `POST …/inventario/{p}/ajustes` | `InventarioActualResponse` (`stockActual`, `stockMinimo`, `stockReservado`) | Actualizar la fila sin recargar |
| `GET …/productos/{p}/requisitos` | `publicable`, `requisitos[]` (`codigo`, `cumplido`, `mensaje`) | Checklist para publicar |
| `GET …/cuenta-pago` | `fechaVinculacion`, `fechaActualizacion`, `ambientePago`; `idCuentaExterna` ya llega **enmascarada** | Pantalla de Pagos |
| `TiendaResponse` | `fechaCreacion` | "Creada el…" en Mis tiendas |
| Vitrina `Presentacion` | `disponibilidad`, `unidadesRestantes` | "¡Últimas N unidades!" |

---

## 2. Funcionalidades nuevas del backend que el frontend todavía no usa

| # | Funcionalidad | Backend | Estado en esta rama | Qué falta |
| --- | --- | --- | --- | --- |
| F1 | **Tienda suspendida o cerrada** | Escrituras → 409 `CONFLICTO`; vitrina, carrito y checkout → 404 `TIENDA_NO_DISPONIBLE`; el admin suspende y reactiva con `PATCH /api/admin/tiendas/{id}/estado {estado: SUSPENDIDA\|ACTIVA}` (al reactivar queda en BORRADOR) | `EstadoTienda` del panel **no tiene** etiquetas para `SUSPENDIDA`/`CERRADA` (se ven en crudo); los formularios siguen editables; el admin **no tiene** botones Suspender/Reactivar | Aviso fijo, modo solo lectura en todas las secciones de la tienda, acciones del admin, estado "no disponible" en la vitrina |
| F2 | **Requisitos para publicar** | `GET …/productos/{p}/requisitos` (TIENDA_ACTIVA, CATEGORIA_ACTIVA, VERSION_ACTIVA, VERSION_CON_UNIDADES) | El botón Publicar solo se bloquea si la tienda no está activa | Checklist en el detalle de producto y Publicar deshabilitado con el motivo |
| F3 | **Slug automático** (categoría, colección) | `slug` opcional | Se genera en el frontend (C4) | Dejar de enviarlo; mostrarlo solo como "Dirección web" en un campo avanzado |
| F4 | **SKU automático** | `sku` opcional al crear | Se genera en el frontend (C3) | No enviarlo por defecto; mostrar el devuelto como "Código interno" |
| F5 | **Stock inicial al crear la versión** | `stockInicial`, `stockMinimo` en `POST …/presentaciones` | Se hace con dos llamadas (C3), también en el generador de combinaciones | Una sola llamada en crear producto, nueva versión y combinaciones |
| F6 | **Ajustes de inventario** | `POST …/ajustes {cantidad: ±n}`; PUT = conteo con `stockEsperado`; `stockReservado` | Los botones +/− y "Poner a todas" hacen un PUT con valor absoluto (C2) | +/− → `ajustes`; "Fijar tras un conteo" → PUT con `stockEsperado`; mostrar apartadas y el 409 de cambio simultáneo |
| F7 | **Checkout parcial** | Compra las líneas disponibles; las no disponibles pasan a un carrito nuevo; `productosApartados` | El carrito no distingue las líneas que no se cobrarán; usa `total` (C1) | Líneas no disponibles en gris "No se incluirá en esta compra", total a pagar y aviso tras pagar |
| F8 | **Desglose del carrito con IGV** | `subtotal`, `igv`, `tasaIgv`, `totalAPagar` | Solo "Total (incluye impuestos)" | Resumen con subtotal, IGV (18 %) y total a pagar |
| F9 | **Número de pedido** | `numero` (#1001…) en pedidos, detalle, checkout y pago | Se muestra el UUID | "Pedido #1042" en las dos apps |
| F10 | **Disponibilidad en la vitrina** | `disponibilidad`, `unidadesRestantes` | Solo "Disponible/Agotado"; los botones de talla y color marcan agotado con `disponible` | "¡Últimas N unidades!" en el detalle y en los botones |
| F11 | **Login con Google** | `POST /api/auth/google`, `POST /api/auth/google/vinculacion`; `VITE_GOOGLE_CLIENT_ID` | `BotonGoogle` es un marcador ("estará disponible pronto"); el store no tiene botón | Integrar Google Identity Services en login y registro (cms y store) y "Vincular Google" en Mi perfil, con los mensajes de `google-login-frontend.md` |
| F12 | **Vencimiento de reservas** (siempre activo) | `vencimiento` en el estado del pago; al vencer, los productos vuelven al carrito | No se muestra el plazo | "Tienes hasta las 18:40 para pagar" y, si se cancela, "Tus productos volvieron al carrito" |
| F13 | **Cuenta de pago detallada** | `ambientePago`, fechas, cuenta enmascarada | `pagos.tsx` muestra `Estado: ACTIVA` y la cuenta en crudo | Pantalla de cobros según la guía (#7) |

**Al revés:** esta rama aporta al backend `PerfilController` (`GET/PUT /api/perfil`, `PUT /api/perfil/contrasena`), que `estado-backend.md` todavía lista como backlog ("Mi perfil"). Hay que **avisar a Miguel** para que lo revise e incluya en su documento.

---

## 3. Términos técnicos que todavía se ven (según el glosario)

| Término | Dónde (esta rama) | Debería decir |
| --- | --- | --- |
| **SKU** | cms: Catálogo › Detalle (Presentaciones, generador), Inventario ("SKU …", buscador) | "Código interno" |
| **Presentación / Presentaciones** | cms: título de la sección del detalle, "Presentación única", "presentaciones" en Inventario y en el resumen; store: label del `<select>` de respaldo | "Versiones" / "Versión única" |
| **Variante / Variantes** | cms: pestaña del catálogo, "Nueva variante", "Productos que usan…" | "Opciones" (Color, Talla) |
| **Precio base / Precio especial** | cms: formulario de producto y de versión | "Precio" / "Precio distinto para esta versión" |
| **Stock / Stock mínimo** | cms: catálogo e inventario | "Unidades disponibles" / "Avisarme cuando queden" |
| **Enlace (slug)** | cms: categorías y colecciones ("Enlace (se genera solo)") | "Dirección web" (solo en avanzado) |
| **Banners / "Mostrar carrusel de banners"** | cms: Apariencia; store: alt "Banner N" | "Imágenes destacadas" / "Mostrar imágenes destacadas en la portada" |
| **Plantilla** | cms: Apariencia, Plantillas, cabecera de la tienda | "Diseño de la tienda" |
| **Estados en crudo** | cms: Pagos (`Estado: ACTIVA`), Pedidos (`PENDIENTE_PAGO`, `APROBADO`, incidencia), Admin (usuarios, tiendas, roles); store: `pago.tsx` ("Pedido: PENDIENTE_PAGO · Pago: APROBADO") | Etiquetas del glosario |
| **Tienda SUSPENDIDA/CERRADA** | cms: `EstadoTienda` no las traduce | "Suspendida" / "Cerrada" |
| **UUID de pedido** | cms y store: Pedidos | "Pedido #1042" |
| **UUID de propietario y roles internos** | cms: Admin › Tiendas y Usuarios | Nombre y correo; "Administrador / Vendedor / Comprador" |
| **Mensaje de tienda no modificable** | Llega del backend con `'SUSPENDIDA'` | "Tu tienda está suspendida: puedes consultar, pero no modificar" |
| **"Única"** | store: opción del `<select>` de respaldo | Ocultar el selector |

---

## 4. Plan de adaptación por partes

Cada parte es un commit pequeño en `feature/frontend-diseno`, compilado con `pnpm build` en las dos apps y probado en local.

### Parte 0 · Cerrar el merge (backend)
1. Resolver los tres conflictos como en §0.
2. Compilar y probar el backend, con especial atención a la vitrina, el perfil y Google.
3. Cerrar el merge con `git commit`.

### Parte 1 · Capa común (sin cambios visibles)

**1a. Cliente de API**
- **store:** `ErrorSolicitud` pasa a guardar `error` y `campos`, igual que `ErrorApi` del cms.
- **Las dos apps:** helper `errorDeCampo(error, 'nombre')` y `mensajeGeneral(error)` para mostrar cada error junto a su campo.
- **Las dos apps:** traducción de los códigos sin texto útil:
  - 403 → "Ya no tienes permiso para administrar esta tienda";
  - 409 `CONFLICTO` de tienda → "Tu tienda está suspendida: puedes consultar, pero no modificar";
  - 404 `TIENDA_NO_DISPONIBLE` → "Esta tienda no está disponible en este momento".

**1b. Tipos**
- Un archivo `tipos.ts` por app con los DTOs vigentes:
  - `Carrito` con el desglose;
  - `Pedido` con `numero`;
  - `EstadoPago` con `numero`, `productosApartados` y `vencimiento`;
  - `Inventario` con `idProducto` y `stockReservado`;
  - `InventarioActual`, `RequisitosPublicacion`;
  - `CuentaPago` con `ambientePago` y fechas;
  - `Presentacion` de la vitrina con `disponibilidad`, `unidadesRestantes` y `precioAnterior`;
  - `Tienda` con `fechaCreacion`.
- Quitar los tipos duplicados de `carrito.tsx`, `pedidos.tsx`, `pago.tsx` y `pagos.tsx`.

**1c. Etiquetas de estado**
- `etiquetas.ts` por app con el glosario. Un mapa por dominio:
  - tienda, producto, versión, pedido (vendedor y comprador);
  - pago (vendedor y comprador), cuenta de pago, usuario y rol.
- Componente `StatusBadge` (cms) y clase `insignia-estado` (store) que lo usan. Reemplazar `EstadoTienda`, `EstadoProducto` y los estados en crudo.

**1d. Formato**
- `soles()` y `fecha()` comunes: "S/ 1,200.00" y "24 nov 2026, 18:40".
- `numeroPedido(p)` → "#1042", con respaldo seguro si `numero` es null.

**1e. Contexto de tienda editable**
- `ContextoTienda` expone `soloLectura` (estado ≠ BORRADOR/ACTIVA).
- Componente `AvisoTiendaSuspendida` en la cabecera de `EspacioTienda`.

### Parte 2 · Panel de la tienda (cms)

| Orden | Pantalla | Cambios |
| --- | --- | --- |
| 2.1 | **Estructura** (`panel.tsx`) | Aviso de suspendida y `soloLectura` en las secciones; badges de estado traducidos; "Creada el…" en Mis tiendas |
| 2.2 | **Catálogo › Productos y detalle** | `GET …/requisitos` → checklist "Listo para publicar" y Publicar deshabilitado con el motivo; crear producto y versión con `stockInicial`/`stockMinimo` en una llamada y SKU opcional (C3, F4, F5); errores por campo; renombrar Presentaciones → Versiones, SKU → Código interno, Variantes → Opciones |
| 2.3 | **Generador de combinaciones** | Una llamada por combinación con `stockInicial`; no enviar `sku` salvo prefijo elegido; mostrar los errores 409 por combinación |
| 2.4 | **Categorías y colecciones** | No enviar `slug` salvo en el campo avanzado "Dirección web" (C4) |
| 2.5 | **Inventario** | Agrupar solo con `GET …/inventario` (C5); +/− con `POST …/ajustes`; "Fijar tras un conteo" con PUT y `stockEsperado`; mostrar "N apartadas"; manejar el 409 `CONFLICTO` ("El stock cambió…", con botón Recargar) (C2, F6) |
| 2.6 | **Pedidos** | `#numero`, estados traducidos, incidencia explicada; detalle con URL propia (`/pedidos/:id`, si la ruta se aprueba) |
| 2.7 | **Pagos** | Estado traducido, cuenta enmascarada, fecha de conexión, modo prueba o real (`ambientePago`), pasos que faltan y qué hacer con `ERROR` (F13) |
| 2.8 | **Apariencia** | Textos del glosario (imágenes destacadas, diseño de la tienda); `soloLectura` |
| 2.9 | **Inicio del vendedor** | Usar el desglose y los estados traducidos; tarea "productos que no se pueden publicar" con los requisitos |
| 2.10 | **Administración** | Botones Suspender/Reactivar tienda (`PATCH /api/admin/tiendas/{id}/estado`), con el mensaje de reactivación a BORRADOR; dueño con nombre y correo; roles y estados traducidos |

### Parte 3 · Vitrina (store)

| Orden | Pantalla | Cambios |
| --- | --- | --- |
| 3.1 | **Detalle de producto** | `disponibilidad` / `unidadesRestantes` → "¡Últimas N unidades!" en el texto y en los botones de opción (F10); quitar el `<select>` de respaldo "Presentación"/"Única" |
| 3.2 | **Carrito y checkout** | Desglose subtotal, IGV y total a pagar (F8); líneas no disponibles en gris y fuera del total; `totalEsperado = totalAPagar` (C1); tras pagar, aviso con `productosApartados` (F7); tienda no disponible → pantalla dedicada |
| 3.3 | **Pedidos y detalle** | `#numero`, estados del comprador, plazo con `vencimiento`, "Tus productos volvieron al carrito" si venció (F9, F12); reescribir `pago.tsx` sin estados en crudo |
| 3.4 | **Tienda no disponible** | Vitrina, carrito y pedidos con un estado "Esta tienda no está disponible"; los pedidos siguen visibles (el backend lo permite) |

### Parte 4 · Login con Google (cms y store)
1. Variable `VITE_GOOGLE_CLIENT_ID` en las dos apps (resolver `.env.example`, §0). Si está vacía, no mostrar el botón.
2. Cargar el script de Google Identity Services y crear un componente `BotonGoogle` real que llame a `POST /api/auth/google { idToken, recordarme }` con CSRF.
3. Manejar 201/204 (sesión iniciada, retorno validado), 409 `DUPLICADO` (botón "Iniciar sesión con contraseña" con el correo precargado), 422, 401 `GOOGLE_TOKEN_INVALIDO` y 503 (ocultar el botón).
4. En **Mi perfil**, "Vincular Google" con `POST /api/auth/google/vinculacion` y sus 409.
5. Si se vuelve a crear la cuenta con Google, Mi perfil debe tener en cuenta que puede no tener contraseña (`tieneContrasena` ya lo devuelve `/api/perfil`).

### Parte 5 · Limpieza y verificación
- Repasar el glosario completo (checklist de `guia-rediseno.md`) en las dos apps.
- Probar con una tienda **suspendida**, con una tienda **sin cobros**, con un carrito con líneas **agotadas** y con una reserva **vencida**.
- Actualizar `analisis-frontend.md` o enlazar este documento como el estado vigente del frontend.

---

## Resumen

- **Primero hay que cerrar el merge.** Quedan tres conflictos. El más delicado es `VitrinaController`, donde hay que unir el precio tachado (rama de diseño) con la disponibilidad (rama de Miguel).
- **Hay tres llamadas que hoy dan resultados incorrectos con el backend nuevo:**
  - el checkout envía `total` en vez de `totalAPagar`;
  - el inventario hace PUT con unidades disponibles, cuando ahora el PUT es un conteo;
  - el stock y el SKU se crean en dos pasos desde el frontend.
- **Hay diez funcionalidades del backend sin pantalla:**
  - suspensión de tiendas, requisitos para publicar, ajustes de inventario y checkout parcial;
  - desglose con IGV, número de pedido y disponibilidad;
  - login con Google, plazo de pago y cuenta de pago detallada.
- **La base común va primero:** errores por campo, tipos, etiquetas y formato. Casi todas las pantallas dependen de ella.
