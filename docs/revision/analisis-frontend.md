# Análisis de cobertura del frontend frente al backend y la BD

> **Fecha:** 2026-11-24 · **Rama:** `feature/miguel-local` · **Tipo:** análisis de solo lectura (no se modificó código).
>
> **Fuentes revisadas**
> - Backend: los 28 controladores de `backend/src/main/java` (99 endpoints, más `POST /logout` de Spring Security), DTOs de request/response y los `record` definidos dentro de los controladores de `compra/` y `vitrina/`.
> - BD: migraciones `V1`–`V13`.
> - Reglas: `docs/functional/modules-rules.md` (v1.2) y `docs/functional/uh-m3.md`.
> - Frontends: `apps/cms` (React + axios, ~1.340 líneas) y `apps/store` (React + fetch, ~460 líneas).
>
> **Fuente no disponible:** `docs/revision/hallazgos-flujo.md` **no existe** en el repositorio ni en el historial de git. Este análisis no incorpora hallazgos del flujo manual; conviene cruzarlo con ese documento cuando esté disponible.

## Resumen

- **El backend está más avanzado que el frontend en cobertura de datos, no de endpoints.** Casi todas las escrituras tienen pantalla. Lo que falta son los detalles por id, algunos listados auxiliares y, sobre todo, **campos** que la API ya devuelve y la UI no muestra (fechas, vencimiento de la reserva, apellidos, documento, cuenta enmascarada).
- **Hay 15 endpoints sin pantalla.** 3 lo son por diseño (webhook de Mercado Pago, callback OAuth, health). De los 12 restantes, 8 son consultas por id de bajo impacto y `ubigeo` queda sin uso a propósito (RN-M4-01, opción A). Solo 2 tienen un flujo detrás: productos de una colección y detalle de usuario para el admin.
- **Lo que más falta es de flujo y lenguaje, no de API:**
  - una lista de requisitos antes de publicar (RN-M2-13);
  - un estado visible de "tienda suspendida" que bloquee las acciones (relacionado con el cambio reciente de `puedeEditar`, que ahora responde 409);
  - el resumen del pedido antes de pagar;
  - los estados del pedido en lenguaje de comprador.
- **Hay términos técnicos en casi todas las pantallas:** códigos de estado en mayúsculas (`PENDIENTE_PAGO`, `BORRADOR`), slug, SKU, "presentación", roles internos, UUID de pedidos y de propietarios.
- **Hay huecos que no se resuelven solo con frontend,** porque el backend no tiene endpoint para editar el perfil, editar o cerrar una tienda, eliminar variantes ni cambiar el estado de envío de un pedido. Se marcan como **[requiere backend]**.

Convenciones de las tablas: ✅ lo usa una pantalla · ❌ **sin pantalla** · ➖ sin pantalla por diseño (no es una acción del usuario).

---

## Módulo 1 · Usuarios, roles y tiendas

### 1.1 Endpoints

| Endpoint | cms | store | Pantalla / comentario |
| --- | --- | --- | --- |
| `GET /api/auth/csrf` | ✅ | ✅ | Implícito antes de cada escritura (`solicitar`) |
| `GET /api/auth/me` | ✅ | ✅ | cms: `ProveedorSesion`; store: `MarcoTienda` (cabecera) |
| `POST /api/auth/login` | ✅ | ✅ | cms: `/iniciar-sesion`; store: `/:slug/iniciar-sesion` |
| `POST /api/auth/registro` | ✅ | ✅ | cms: `/registro`; store: `/:slug/registro` |
| `POST /logout` (Spring Security) | ✅ | ✅ | Botón "Cerrar sesión" (panel / menú de cuenta) |
| `GET /api/usuarios/{idUsuario}` | ❌ | — | **Sin pantalla.** El admin no tiene vista de detalle de usuario |
| `PUT /api/usuarios/{idUsuario}/roles/{rol}` | ✅ | — | Admin › Usuarios (asignar dueño / administrador) |
| `DELETE /api/usuarios/{idUsuario}/roles/DUENO_TIENDA` | ✅ | — | Admin › Usuarios (retirar dueño, con confirmación) |
| `POST /api/tiendas` | ✅ | — | Panel › Tus tiendas (crear) |
| `POST /api/tiendas/{idTienda}/activacion` | ✅ | — | Tienda › Resumen (solo si `BORRADOR`) |
| `GET /api/tiendas/propietario/{idUsuario}` | ✅ | — | Panel › Tus tiendas y `EspacioTienda` (se repite la carga en cada sección) |
| `GET /api/admin/usuarios` | ✅ | — | Admin › Usuarios (búsqueda + paginación) |
| `GET /api/admin/tiendas` | ✅ | — | Admin › Tiendas (búsqueda + paginación) |

### 1.2 Campos de BD/DTO no mostrados o con otro nombre

| Entidad | Campo (BD → DTO) | Situación en el frontend |
| --- | --- | --- |
| `usuario_plataforma` | `apellido_paterno`, `apellido_materno` → `apellidoPaterno`, `apellidoMaterno` | `RegistroUsuarioRequest` los acepta, pero **ningún formulario de registro los pide**. El admin sí los muestra; el tipo `Usuario` de cms (sesión) no los incluye |
| `usuario_plataforma` | `telefono` | Lo acepta el registro, pero **no se pide en ninguna app** |
| `usuario_plataforma` | `id_tipo_documento`, `numero_documento`, `pais_emisor_documento` (RN-M1-03/04) | Lo acepta el registro, pero **no hay ningún campo de documento** |
| `usuario_plataforma` | `estado` (`ACTIVO`, `SUSPENDIDO`, `CIERRE_PENDIENTE`, `CERRADO`) | Admin › Usuarios lo muestra **en crudo** |
| `usuario_plataforma` | `fecha_creacion` → `fechaCreacion` | No se muestra |
| `tienda` | `slug` | Se muestra en crudo en Tus tiendas, en la cabecera de la tienda y en Admin; **no se presenta como dirección web** (`/tienda/{slug}`) |
| `tienda` | `estado` | En crudo (`BORRADOR · ACTIVA · SUSPENDIDA`). No hay aviso ni bloqueo de acciones cuando la tienda está `SUSPENDIDA` |
| `tienda` | `fecha_creacion` → `fechaCreacion` | No se muestra |
| `tienda` | `fecha_cierre` | No está en el DTO ni en la UI |
| `tienda` | `id_propietario` → `idPropietario` | Admin › Tiendas muestra el **UUID** ("Propietario: 3f2a…") en lugar del nombre o el correo |
| `tienda` → `plantilla` | `TiendaResponse.plantilla` | Admin › Tiendas la muestra como texto; el vendedor solo la ve en Apariencia |
| `tienda_cliente` | `fecha_registro` | Sin uso en la UI (no hay "clientes de mi tienda") |
| `usuario_identidad` | `proveedor` (LOCAL/GOOGLE) | No hay inicio de sesión con Google (RN-M1-05) **[requiere backend]** |

### 1.3 Pantallas que faltan (M1)

| Pantalla | App | RN/HU | Nota |
| --- | --- | --- | --- |
| **Aviso de tienda suspendida o cerrada** con las acciones de edición deshabilitadas y un texto que explique por qué | cms | RN-M1-10, RN-M1-17, HU-M3-04 §7 | Hoy el vendedor solo ve "SUSPENDIDA" y, al intentar guardar, recibe el error 409 de `puedeEditar`. Las lecturas siguen permitidas |
| **Mi perfil** (nombre, apellidos, teléfono, documento) | cms y store | RN-M1-03/04 | El registro acepta los campos opcionales; **no hay endpoint para editarlos** **[requiere backend]** |
| **Registro completo** con campos opcionales (apellidos, teléfono, documento con tipo/número/país) | cms y store | RN-M1-03/04 | Posible solo con frontend en el registro; la edición posterior requiere backend |
| **Detalle de usuario (admin)**: datos, roles, tiendas que posee | cms | RN-M1-09/10 | `GET /api/usuarios/{id}` existe; las tiendas del usuario se pueden cruzar con `GET /api/admin/tiendas?texto=` |
| **Tiendas (admin) con propietario legible** y enlace a su vitrina | cms | RN-M1-11/13 | El DTO solo trae `idPropietario`; el nombre requiere `GET /api/usuarios/{id}` o un cambio de DTO |
| **Configuración de la tienda** (renombrar, cambiar slug) | cms | RN-M1-12 | **[requiere backend]**: no hay `PUT/PATCH /api/tiendas/{id}` |
| **Cerrar tienda / cerrar cuenta** con verificación de pedidos pendientes | cms | RN-M1-17/18 | **[requiere backend]** |
| **Iniciar sesión con Google** | cms y store | RN-M1-05/06 | **[requiere backend]** |
| **Mis compras en todas las tiendas** | store o cms | RN-M1-14/16 | Por la RN-M1-16 los pedidos se ven por tienda; es una mejora opcional |

---

## Módulo 2 · Catálogo

### 2.1 Endpoints (todos en cms; la vitrina pública está en M4)

| Endpoint | ¿Usado? | Pantalla / comentario |
| --- | --- | --- |
| `POST /categorias` | ✅ | Catálogo › Categorías |
| `GET /categorias` | ✅ | Catálogo › Categorías y selectores de Productos |
| `GET /categorias/{id}` | ❌ | **Sin pantalla** (la lista basta; el impacto es bajo) |
| `PUT /categorias/{id}` | ✅ | Catálogo › Categorías (Editar) |
| `PATCH /categorias/{id}/estado?activa=` | ✅ | Catálogo › Categorías (Activar/Desactivar) |
| `POST /productos` | ✅ | Catálogo › Productos (Crear y continuar) |
| `GET /productos` | ✅ | Catálogo › Productos (lista sin filtros ni paginación) |
| `GET /productos/{id}` | ✅ | Detalle de producto |
| `PUT /productos/{id}` | ✅ | Detalle de producto (Guardar cambios) |
| `PATCH /productos/{id}/estado?estado=` | ✅ | Detalle de producto (Publicar / Desactivar / Pasar a borrador) |
| `POST /productos/{id}/presentaciones` | ✅ | Detalle › Presentaciones |
| `GET /productos/{id}/presentaciones` | ✅ | Detalle › Presentaciones |
| `GET /productos/{id}/presentaciones/{p}` | ❌ | **Sin pantalla** (impacto bajo) |
| `PUT /productos/{id}/presentaciones/{p}` | ✅ | Fila de presentación (SKU y precio especial) |
| `PATCH /productos/{id}/presentaciones/{p}/estado?estado=` | ✅ | Fila de presentación (Activar/Desactivar) |
| `PUT /productos/{id}/presentaciones/{p}/valores` | ✅ | Fila › "Cambiar combinación de variantes" |
| `POST /productos/{id}/imagenes` (multipart) | ✅ | Detalle › Imágenes |
| `GET /productos/{id}/imagenes` | ✅ | Detalle › Imágenes |
| `GET /productos/{id}/imagenes/{i}` | ❌ | **Sin pantalla** (impacto bajo) |
| `PUT /productos/{id}/imagenes/{i}` | ✅ | "Cambiar orden" (con `window.prompt`) |
| `PUT /productos/{id}/imagenes/{i}/archivo` | ✅ | "Reemplazar" |
| `PATCH /productos/{id}/imagenes/{i}/principal` | ✅ | "Hacer principal" |
| `DELETE /productos/{id}/imagenes/{i}` | ✅ | "Eliminar" |
| `POST /presentaciones/{p}/imagenes/{i}` | ✅ | Fila › "Imágenes de esta presentación" (casilla) |
| `GET /presentaciones/{p}/imagenes` | ✅ | Ídem |
| `DELETE /presentaciones/{p}/imagenes/{i}` | ✅ | Ídem |
| `GET /imagenes/{i}/presentaciones` | ❌ | **Sin pantalla** ("¿en qué presentaciones aparece esta imagen?") |
| `POST /colecciones` | ✅ | Catálogo › Colecciones |
| `GET /colecciones` | ✅ | Catálogo › Colecciones y Detalle de producto |
| `GET /colecciones/activas` | ❌ | **Sin pantalla** (se usa la lista completa) |
| `GET /colecciones/{c}` | ❌ | **Sin pantalla** |
| `PUT /colecciones/{c}` | ✅ | Catálogo › Colecciones (Editar) |
| `PATCH /colecciones/{c}/estado?activa=` | ✅ | Catálogo › Colecciones |
| `POST /colecciones/{c}/productos/{p}` | ✅ | Detalle › Colecciones (casilla) |
| `GET /colecciones/{c}/productos` | ❌ | **Sin pantalla.** No se pueden ver ni ordenar los productos **desde la colección** |
| `GET /productos/{p}/colecciones` | ✅ | Detalle › Colecciones |
| `DELETE /colecciones/{c}/productos/{p}` | ✅ | Detalle › Colecciones (casilla) |
| `POST /variantes` | ✅ | Catálogo › Variantes |
| `GET /variantes` | ✅ | Catálogo › Variantes y Detalle de producto |
| `GET /variantes/{v}` | ❌ | **Sin pantalla** |
| `PUT /variantes/{v}` | ✅ | Catálogo › Variantes (Editar) |
| `POST /variantes/{v}/valores` | ✅ | Catálogo › Variantes (Añadir valor) |
| `GET /variantes/{v}/valores` | ✅ | Catálogo › Variantes y Detalle (se piden N listas en cada carga del detalle) |
| `GET /variantes/{v}/valores/{x}` | ❌ | **Sin pantalla** |
| `PUT /variantes/{v}/valores/{x}` | ✅ | Catálogo › Variantes (clic en valor → editar) |

*Prefijo común: `/api/tiendas/{idTienda}`.*

### 2.2 Campos de BD/DTO no mostrados o con otro nombre

| Tabla | Campo BD | DTO | En el formulario / UI |
| --- | --- | --- | --- |
| `categoria` | `slug` | `slug` | Campo **"Slug" obligatorio** escrito a mano (placeholder `ejemplo-tienda`). No se genera desde el nombre ni se explica para qué sirve. La vitrina no lo usa en las URL (filtra por id) |
| `categoria` | `activa` | `activa` | "Activa/Inactiva" ✅ |
| `coleccion` | `slug` | `slug` | Igual que en categoría: "Slug" manual y obligatorio |
| `producto` | `precio_base` | `precioBase` | "Precio base"; en la lista se muestra **como número sin formato** (`15.5`, no `S/ 15.50`) |
| `producto` | `descripcion` | `descripcion` | `<input>` de una línea; no hay área de texto ni formato |
| `producto` | `estado` | `estado` | En crudo (`BORRADOR`, `PUBLICADO`, `INACTIVO`) |
| `producto` | `fecha_creacion`, `fecha_actualizacion` | `fechaCreacion`, `fechaActualizacion` | **No se muestran** |
| `producto` | `id_categoria` | `idCategoria` | Selector "Categoría" ✅. En la edición se listan también las categorías inactivas, sin marcarlas |
| `presentacion_producto` | `sku` | `sku` | **"SKU" obligatorio**, sin explicación ni sugerencia automática |
| `presentacion_producto` | `precio_override` | `precioOverride` | Aparece como **"Precio especial opcional"** (nombre distinto; está bien orientado) |
| `presentacion_producto` | — | `precioEfectivo` | "Precio efectivo: 15.5" sin formato de moneda |
| `presentacion_producto` | `estado` | `estado` | En crudo (`ACTIVA`/`INACTIVA`) |
| `presentacion_producto` | `firma_valores` | — | No se expone (correcto). La UI no avisa antes de crear una combinación repetida (RN-M2-10); solo muestra el error del servidor |
| `presentacion_valor` | `id_valor`, `id_variante` | `idsValores` | Aparece como "Cambiar combinación de variantes" con un selector por variante. No refleja la RN-M2-12 (mismo conjunto de variantes en todas las presentaciones, máximo 3) |
| `inventario` | `stock_actual`, `stock_minimo` | — | **No se piden al crear la presentación.** El texto dice "Ajusta el stock desde Inventario después de crearla": hay que ir a otra sección para poder publicar |
| `imagen_producto` | `orden` | `orden` | "Orden N"; para cambiarlo se usa `window.prompt` (no se puede arrastrar) |
| `imagen_producto` | `es_principal` | `esPrincipal` | "Principal" ✅ |
| `imagen_producto` | `storage_key` | → `url` | Se muestra la imagen (correcto). El texto alternativo es "Imagen N" |
| `variante` / `valor_variante` | `nombre` / `valor` | ídem | ✅. Los valores se muestran como botones "Rojo · editar" |
| `coleccion_producto` | — | — | Solo se gestiona desde el producto |

### 2.3 Pantallas que faltan (M2)

| Pantalla | RN/HU | Nota |
| --- | --- | --- |
| **Asistente de producto** (datos → opciones → combinaciones con precio y stock → fotos → publicar) | RN-M2-06, RN-M2-11, RN-M2-12, RN-M3-10 | Hoy el flujo pasa por tres secciones (Producto, Presentaciones e Inventario) y obliga a volver a Inventario para fijar el stock antes de publicar |
| **Lista de requisitos para publicar** visible en el producto: tienda activa, categoría activa, ≥1 presentación activa con stock | RN-M2-13 | Hoy el botón "Publicar" solo se deshabilita si la tienda no está activa; el resto llega como error del servidor |
| **Generador de combinaciones**: elegir hasta 3 variantes y crear todas las combinaciones de una vez | RN-M2-09, RN-M2-10, RN-M2-12 | Hoy se crea una presentación cada vez, con un selector por variante y sin límite visible |
| **Lista de productos con búsqueda, filtro por estado o categoría, imagen y precio formateado** | — | La lista actual no tiene filtros, miniatura ni paginación |
| **Detalle de colección**: sus productos, añadir o quitar desde la colección | RN-M2-17 | `GET /colecciones/{c}/productos` existe y no se usa |
| **Galería de imágenes** con reordenación por arrastre y uso por presentación | RN-M2-14/15/16 | Reemplaza `window.prompt`; puede usar `GET /imagenes/{i}/presentaciones` |
| **Slug automático** en categorías y colecciones (editable en "avanzado") | RN-M2-03 | Solo frontend |
| Eliminar variante, valor, categoría o producto | — | **[requiere backend]**: no hay `DELETE`. Hoy solo existe desactivar (categoría, colección, producto, presentación) |

---

## Módulo 3 · Apariencia, banners, cuenta de pago e inventario

### 3.1 Endpoints

| Endpoint | cms | Pantalla / comentario |
| --- | --- | --- |
| `GET /api/plantillas` (`?incluirInactivas=true` para admin) | ✅ | Admin › Plantillas; Tienda › Apariencia (selector) |
| `GET /api/plantillas/{id}` | ❌ | **Sin pantalla** (HU-M3-03 §4; impacto bajo) |
| `POST /api/plantillas` | ✅ | Admin › Plantillas (crear) |
| `PATCH /api/plantillas/{id}` | ✅ | Admin › Plantillas (editar) |
| `PATCH /api/plantillas/{id}/estado` | ✅ | Admin › Plantillas (activar/desactivar) |
| `GET /api/tiendas/{id}/configuracion` | ✅ | Tienda › Apariencia |
| `PATCH /api/tiendas/{id}/configuracion` | ✅ | Tienda › Apariencia |
| `GET /api/tiendas/{id}/banners` | ✅ | Tienda › Apariencia › Banners |
| `POST /api/tiendas/{id}/banners` | ✅ | Ídem (añadir) |
| `PATCH /api/tiendas/{id}/banners/{b}` | ✅ | Ídem (enlace y activo) |
| `PUT /api/tiendas/{id}/banners/{b}/archivo` | ✅ | Ídem (reemplazar imagen) |
| `PUT /api/tiendas/{id}/banners/orden` | ✅ | Ídem (Subir/Bajar) |
| `DELETE /api/tiendas/{id}/banners/{b}` | ✅ | Ídem (eliminar) |
| `GET /api/tiendas/{id}/cuenta-pago` | ✅ | Tienda › Pagos |
| `POST /api/tiendas/{id}/cuenta-pago/desconexion` | ✅ | Tienda › Pagos (Desconectar) |
| `POST /api/tiendas/{id}/cuenta-pago/mercado-pago/oauth/inicios` | ✅ | Tienda › Pagos (Conectar; depende de `VITE_MERCADOPAGO_OAUTH_ENABLED`) |
| `GET /api/integraciones/mercado-pago/oauth/callback` | ➖ | Redirección del backend; el cms la recibe en `/panel/retorno-pago` |
| `GET /api/tiendas/{id}/inventario` | ✅ | Tienda › Inventario (filtros + paginación) |
| `PUT /api/tiendas/{id}/inventario/{p}` | ✅ | Tienda › Inventario (guardar fila) |

### 3.2 Campos de BD/DTO no mostrados o con otro nombre

| Tabla | Campo | Situación |
| --- | --- | --- |
| `plantilla` | `codigo` | Admin lo ve ✅. En Apariencia el selector está **filtrado en el código a `codigo === "minimal"`**: una plantilla nueva activa no aparece para el vendedor |
| `plantilla` | `preview_url` → `previewUrl` | Solo lo ve el admin. **El vendedor no ve la vista previa al elegir plantilla** |
| `configuracion_tienda` | `titulo_inicio`, `descripcion_inicio` | "Título de inicio" y "Descripción" ✅ |
| `configuracion_tienda` | `mostrar_carrusel` | "Mostrar carrusel" (término técnico) |
| `configuracion_tienda` | `fecha_actualizacion` → `fechaActualizacion` | No se muestra ("Guardado por última vez…") |
| `banner_tienda` | `enlace_destino` | **"Enlace HTTPS"** (término técnico) |
| `banner_tienda` | `orden` | Se reordena con Subir/Bajar ✅ |
| `banner_tienda` | `fecha_creacion` → `fechaCreacion` | No se muestra |
| `cuenta_pago_tienda` | `estado` | En crudo (`PENDIENTE`, `ACTIVA`, `DESCONECTADA`, `ERROR`); `ERROR` no tiene texto de ayuda |
| `cuenta_pago_tienda` | `id_cuenta_externa` | "Cuenta: 123456789" **sin enmascarar** (HU-M3-07 §2 recomienda enmascarar) |
| `cuenta_pago_tienda` | `fecha_vinculacion`, `fecha_actualizacion` | Vienen en `CuentaPagoResponse`, pero **el tipo `CuentaPago` del cms no los declara** y no se muestran |
| `cuenta_pago_tienda` | `ambiente_pago` (V9: `PRUEBA`/`PRODUCCION`) | No está en `CuentaPagoResponse`. El vendedor solo ve el modo de prueba en el detalle de cada pago |
| `cuenta_pago_historial` | todo | **Sin endpoint ni pantalla** (historial de vinculaciones) |
| `inventario` | `stock_actual`, `stock_minimo` | "Stock actual" y "Stock mínimo" (aceptable; podría ser "Unidades disponibles" y "Avisarme cuando queden") |
| `inventario` | `fecha_actualizacion` → `fechaActualizacion` | No se muestra (HU-M3-06 §3 la incluye) |
| — | `sinRegistroInventario` | "Sin registro de inventario" (diagnóstico interno expuesto al vendedor) |
| — | `estadoProducto`, `estadoPresentacion` | En crudo en cada fila y en los filtros |

### 3.3 Pantallas que faltan (M3)

| Pantalla | RN/HU | Nota |
| --- | --- | --- |
| **Selector de plantilla con vista previa** (tarjetas con `previewUrl`) y sin el filtro fijo a `minimal` | RN-M3-03, HU-M3-03/04 | El filtro debe depender de las plantillas que la vitrina implemente, no estar en el código |
| **Vista previa de la vitrina** al editar título, descripción y banners | HU-M3-04/05 | Hoy solo existe el enlace "Ver tienda pública" en Resumen |
| **Estado de cobro claro**: qué falta para cobrar, cuenta enmascarada, fecha de vinculación, modo de prueba o producción, qué hacer si hay `ERROR` | RN-M3-07 a 09, HU-M3-07 a 10 | El dato de prueba o producción requiere añadir `ambiente_pago` a `CuentaPagoResponse` **[requiere backend]** |
| **Inventario con edición masiva** y alerta de stock bajo en Resumen | RN-M3-10/11, HU-M3-06 | El filtro "Stock bajo" ya existe; falta mostrarlo en el resumen de la tienda |
| **Panel de inicio de la tienda**: tareas pendientes (activar tienda, conectar pagos, productos sin stock, pedidos nuevos) | transversal | `ResumenTienda` hoy es texto fijo y un botón |

---

## Módulo 4 · Direcciones, carrito, pedidos, impuestos y pagos

### 4.1 Endpoints

| Endpoint | cms | store | Pantalla / comentario |
| --- | --- | --- | --- |
| `GET /api/publico/tiendas/{slug}` (vitrina) | — | ✅ | `/:slug`, `/:slug/productos/:id` y nombre de la cabecera |
| `GET /api/compra/tiendas/{slug}/carrito` | — | ✅ | `/:slug/carrito` y contador de la cabecera |
| `POST /api/compra/tiendas/{slug}/carrito/items/{p}` | — | ✅ | Detalle de producto ("Agregar al carrito", siempre con cantidad 1) |
| `PUT /api/compra/tiendas/{slug}/carrito/items/{p}` | — | ✅ | Carrito (cantidad) |
| `DELETE /api/compra/tiendas/{slug}/carrito/items/{p}` | — | ✅ | Carrito (Quitar) |
| `GET /api/cliente/ubigeo` | — | ❌ | **Sin pantalla.** Coherente con RN-M4-01 (opción A, Google Places); el formulario envía `codigoUbigeo: null` |
| `GET /api/cliente/direcciones` | — | ✅ | `/:slug/direcciones` y selector del carrito |
| `POST /api/cliente/direcciones` | — | ✅ | Formulario de dirección (en Direcciones y en el carrito) |
| `PUT /api/cliente/direcciones/{id}` | — | ✅ | Direcciones (Editar) |
| `DELETE /api/cliente/direcciones/{id}` | — | ✅ | Direcciones (Eliminar) |
| `GET /api/compra/tiendas/{slug}/checkout` | — | ✅ | Carrito (si se puede pagar y el modo de prueba) |
| `POST /api/compra/tiendas/{slug}/checkout` | — | ✅ | Carrito ("Pagar con Mercado Pago"; clave idempotente en `sessionStorage`) |
| `GET /api/compra/tiendas/{slug}/pedidos` | — | ✅ | `/:slug/pedidos` |
| `GET /api/compra/tiendas/{slug}/pedidos/{id}` | — | ✅ | `/:slug/pedidos/:id` |
| `GET /api/compra/tiendas/{slug}/pedidos/{p}/pago` | — | ✅ | Detalle de pedido (consulta cada 5 s) |
| `GET /api/tiendas/{idTienda}/pedidos` | ✅ | — | Tienda › Pedidos (sin filtros ni paginación) |
| `GET /api/tiendas/{idTienda}/pedidos/{id}` | ✅ | — | Tienda › Pedidos › Ver detalle (en la misma vista, sin URL propia) |
| `GET /api/tiendas/{idTienda}/pedidos/{p}/pago` | ✅ | — | Detalle de pedido del vendedor (consulta cada 10 s) |
| `POST /api/tiendas/{idTienda}/pedidos/{p}/reembolsos` | ✅ | — | Detalle de pedido del vendedor ("Reembolsar pedido completo") |
| `GET /api/admin/impuestos` | ✅ | — | Admin › Impuestos |
| `POST /api/admin/impuestos` | ✅ | — | Admin › Impuestos (programar vigencia) |
| `POST /api/publico/mercadopago/notificaciones` | ➖ | ➖ | Webhook del proveedor |
| `GET /health` | ➖ | ➖ | Monitorización |

### 4.2 Campos de BD/DTO no mostrados o con otro nombre

| Tabla / DTO | Campo | Situación |
| --- | --- | --- |
| `pedido` | `id_pedido` | Comprador y vendedor ven **"Pedido 6f1c…" (UUID completo)**; no hay número corto legible |
| `pedido` | `estado` | En crudo (`PENDIENTE_PAGO`, `PAGADO`, `CANCELADO`) en las dos apps |
| `pedido` | `estado_reserva`, `fecha_expiracion_reserva` | `EstadoPago.vencimiento` llega al store, pero **no se muestra** ("Tienes hasta las 18:40 para pagar") |
| `pedido` | `fecha_pago` | No está en el DTO ni en la UI |
| `pedido` | `tasa_impuesto` | Se muestra el monto de IGV, no la tasa |
| `pedido` | `place_id_entrega` | El vendedor ve el mapa ✅; el comprador no (su `Detalle` no incluye `placeId`) |
| `pedido` | `ambiente_pago` | Se muestra como "Compra de prueba" (store) / "Prueba sin dinero real" (cms) ✅ |
| `pedido_item` | `descripcion_presentacion` | Aparece pegada al nombre: "Polo Color: Rojo · Talla: M" |
| `pedido_item` | `monto_impuesto` por línea | No se muestra (solo el total) |
| `pago` | `estado` (`PENDIENTE`, `APROBADO`, `RECHAZADO`, `CANCELADO`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADO`) | En crudo: "Pago: APROBADO" |
| `pago` | `referencia_externa` | Vendedor: "Referencia en Mercado Pago" ✅; comprador: no se muestra |
| `PagosStore.Estado` | `incidencia` | Vendedor: se muestra **el código crudo** ("Requiere atención: {incidencia}") |
| `reembolso_order` | `reponer_stock`, `estado`, `fecha_confirmacion` | Casilla "Reponer stock" ✅; no hay historial ni estado del reembolso más allá del estado del pago |
| `direccion_cliente` | `alias`, `referencia`, `telefono_contacto`, `es_principal`, `place_id`, `departamento/provincia/distrito` | Todos se piden ✅. El formulario muestra a la vez **los campos manuales y el buscador de Google**, sin indicar cuál tiene prioridad |
| `direccion_cliente` | `codigo_ubigeo` | Se envía siempre `null` (coherente con RN-M4-01 v1.2) |
| `carrito` | IGV desglosado | El carrito muestra solo "Total" con "incluye impuestos"; no hay desglose de subtotal e IGV antes de pagar |
| `configuracion_impuesto` | `codigo`, `nombre`, `creado_por`, `fecha_creacion` | Admin solo ve tasa y fechas; "(exclusiva)" es jerga |
| Vitrina `Presentacion` | `opciones` | Se muestran como "Color: Rojo · Talla: M — S/ 20" dentro de **un solo `<select>` "Presentación"**, en lugar de un selector por opción |

### 4.3 Pantallas que faltan (M4)

| Pantalla | App | RN/HU | Nota |
| --- | --- | --- | --- |
| **Checkout en pasos**: revisar productos → dirección → resumen con subtotal, IGV y total → pagar | store | RN-M4-05, RN-M4-09/10 | Hoy carrito, dirección y pago están en una sola página; no hay un resumen final antes de redirigir |
| **Detalle de pedido del comprador** con línea de tiempo (pedido creado → pago confirmado o cancelado), plazo para pagar y lenguaje claro | store | RN-M4-13/14/19/21 | `vencimiento` ya llega en `EstadoPago` |
| **Página de retorno desde Mercado Pago** ("Estamos confirmando tu pago") | store | RN-M4-17 | Hoy el retorno cae en el detalle del pedido; conviene un estado dedicado mientras llega la confirmación |
| **Pedidos del vendedor con URL propia**, filtros por estado o fecha, búsqueda y paginación | cms | RN-M1-16 | Hoy el detalle se abre dentro del listado y se pierde al recargar |
| **Estado de envío o entrega del pedido** | cms y store | Trabajo futuro (envío) | **[requiere backend]**: no hay `estado_envio` ni endpoint de transición |
| **Selector de opciones por variante** en el producto (Color: ● ● ●, Talla: S M L) con stock por combinación | store | RN-M2-09/10 | Solo frontend: los datos ya vienen en `opciones` |
| **Cantidad al agregar al carrito** | store | RN-M4-03 | Hoy siempre se agrega 1 |
| **Mis datos / mi cuenta** en la vitrina | store | RN-M1-14 | Ligado al perfil **[requiere backend]** |
| **Impuestos (admin)** con la vigencia actual destacada y las futuras | cms | RN-M4-12 | Mejora de presentación |

---

## 4 · Términos técnicos expuestos al usuario

| Término actual | Dónde aparece | Para el vendedor | Para el comprador |
| --- | --- | --- | --- |
| **Slug** | Categorías, Colecciones (campo obligatorio); Tus tiendas, cabecera de tienda, Admin | "Dirección web" / "Enlace" (ej. `nubea.pe/tienda/mi-tienda`); generarlo automáticamente | — |
| **SKU** | Presentaciones, Inventario (búsqueda) | "Código interno (opcional)" o "Código de producto"; sugerir uno automático | — |
| **Presentación / presentaciones** | Catálogo, Inventario ("N presentaciones"), filtros | "Variante" o "Versión del producto" (ej. "Rojo / M") | "Opción" / "Modelo" |
| **"Sin presentaciones"**, "Esta presentación…" | Vitrina (tarjeta), carrito | — | "No disponible" / "Esta opción ya no está disponible" |
| **Variante / valor** | Catálogo › Variantes | "Opciones" (Color, Talla) y "Valores" (Rojo, M) | "Color", "Talla"… (nombre de la opción) |
| **"Combinación de variantes"** | Fila de presentación | "Opciones de esta versión" | — |
| **Precio base / Precio especial / Precio efectivo** | Producto y presentación | "Precio" / "Precio distinto para esta versión" / "Precio final" | "Precio" |
| **Firma (`firma_valores`)** | No se expone ✅ | Mensaje de error de duplicado: "Ya existe una versión con estas opciones" | — |
| **BORRADOR / PUBLICADO / INACTIVO** (producto) | Catálogo, Inventario | "Borrador" / "Visible en tu tienda" / "Oculto" | — |
| **ACTIVA / INACTIVA** (presentación) | Catálogo, Inventario | "A la venta" / "Pausada" | — |
| **BORRADOR / ACTIVA / SUSPENDIDA / CERRADA** (tienda) | Tus tiendas, cabecera, Admin | "En preparación" / "Abierta" / "Suspendida por la plataforma" / "Cerrada" | "Esta tienda no está disponible" |
| **PENDIENTE_PAGO / PAGADO / CANCELADO** (pedido) | Pedidos (cms y store) | "Esperando pago" / "Pagado" / "Cancelado" | "Pendiente de pago" / "Pago confirmado" / "Cancelado" |
| **APROBADO / RECHAZADO / REEMBOLSO_PENDIENTE / REEMBOLSADO** (pago) | Detalle de pedido | "Cobrado" / "Rechazado" / "Devolución en curso" / "Dinero devuelto" | "Pagado" / "Pago rechazado" / "Te estamos devolviendo el dinero" / "Dinero devuelto" |
| **PENDIENTE / ACTIVA / DESCONECTADA / ERROR** (cuenta de pago) | Tienda › Pagos | "Sin conectar" / "Lista para cobrar" / "Desconectada" / "Hay un problema con la conexión" | — |
| **"Ambiente sin verificar" / "Producción"** | Detalle de pedido (cms) | "Modo de prueba" / "Cobros reales" | — |
| **Incidencia** (código crudo) | Detalle de pedido (cms) | Mensaje explicado por tipo de incidencia | — |
| **"Referencia en Mercado Pago"** | Detalle de pedido (cms) | "N.º de operación en Mercado Pago" | (mostrar también al comprador) |
| **UUID de pedido** ("Pedido 6f1c…") | Pedidos (cms y store) | "Pedido #1042" (número corto) | "Pedido #1042" |
| **UUID de propietario** | Admin › Tiendas | Nombre y correo del dueño | — |
| **DUENO_TIENDA / ADMIN_GENERAL / CLIENTE** | Admin › Usuarios (lista y confirmaciones) | "Vendedor" / "Administrador de Nubea" / "Comprador" | — |
| **ACTIVO / SUSPENDIDO / CIERRE_PENDIENTE / CERRADO** (usuario) | Admin › Usuarios | "Activa" / "Suspendida" / "Cierre en proceso" / "Cerrada" | — |
| **"Sin registro de inventario"** | Inventario | "Falta configurar el stock" (y botón para hacerlo) | — |
| **Stock actual / Stock mínimo** | Inventario | "Unidades disponibles" / "Avisarme cuando queden…" | "Disponible" / "Agotado" ✅ |
| **Plantilla / código / URL de vista previa** | Admin › Plantillas, Apariencia | "Diseño de la tienda" | — |
| **"Mostrar carrusel"** | Apariencia | "Mostrar imágenes destacadas en la portada" | — |
| **Banner / "Enlace HTTPS"** | Apariencia | "Imagen destacada" / "Al hacer clic, ir a… (opcional)" | — |
| **"Orden N"** (imágenes) | Detalle de producto | "Posición" (arrastrar) | — |
| **"Vigencia… hasta X (exclusiva)"** | Admin › Impuestos | "Vigente desde X hasta el día anterior a Y" | — |
| **"Presentación"** (label del `<select>`) | Vitrina, detalle de producto | — | Un selector por opción: "Color", "Talla" |
| **"Única"** | Vitrina (producto sin opciones) | — | Ocultar el selector |
| **IGV** | Carrito, pedidos | Aceptable en Perú | Aceptable ("Incluye IGV") |

---

## 5 · Lista priorizada de pantallas para el rediseño

### Imprescindible (bloquea flujos de negocio o confunde al usuario hoy)

| # | Pantalla | App | Por qué |
| --- | --- | --- | --- |
| 1 | **Estado de tienda visible y acciones bloqueadas** si está suspendida o cerrada | cms | Desde el cambio de `puedeEditar`, las escrituras responden 409; la UI debe anticiparlo y explicarlo (RN-M1-10) |
| 2 | **Creación y edición de producto en un solo flujo** (datos → opciones → versiones con precio y **stock** → fotos) | cms | Hoy hay que pasar por tres secciones para publicar; el stock queda en otra pantalla (RN-M2-13, RN-M3-10) |
| 3 | **Lista de requisitos para publicar** dentro del producto | cms | Evita el ensayo y error con mensajes del servidor (RN-M2-13) |
| 4 | **Selector de opciones por variante** en el detalle de producto de la vitrina | store | El `<select>` "Presentación" con todas las combinaciones es lo primero que ve el comprador |
| 5 | **Checkout con resumen final** (productos, dirección, subtotal, IGV, total) antes de ir a Mercado Pago | store | RN-M4-05/09/10; hoy se redirige sin confirmación previa |
| 6 | **Detalle de pedido del comprador** con estados en lenguaje claro, plazo para pagar (`vencimiento`) y número corto | store | Hoy muestra `PENDIENTE_PAGO` / `APROBADO` y el UUID |
| 7 | **Traducción de todos los estados y roles** (capa de etiquetas común en cms y store) | cms y store | Afecta todas las pantallas; es barato y elimina la mayor parte de la jerga |
| 8 | **Pagos de la tienda**: estado de cobro claro, qué falta, cuenta enmascarada y qué hacer si hay `ERROR` | cms | Sin una cuenta `ACTIVA` la tienda no vende (RN-M4-22) |

### Importante (completa los flujos de las RN/HU)

| # | Pantalla | App | Por qué |
| --- | --- | --- | --- |
| 9 | **Inicio de la tienda** con tareas pendientes (activar, conectar pagos, sin stock, pedidos nuevos) | cms | Hoy `ResumenTienda` es texto fijo |
| 10 | **Pedidos del vendedor** con URL propia, filtros, número corto y reembolso | cms | El detalle se pierde al recargar; no hay filtros |
| 11 | **Lista de productos** con miniatura, precio formateado, filtros y búsqueda | cms | La lista actual crece sin control |
| 12 | **Apariencia con selector visual de plantilla** (`previewUrl`) y vista previa de la portada | cms | HU-M3-03/04; quitar el filtro fijo `minimal` |
| 13 | **Galería de imágenes con arrastre** y asignación por versión | cms | Reemplaza `window.prompt` (RN-M2-15/16) |
| 14 | **Slug automático** en categorías y colecciones | cms | RN-M2-03; quita un campo técnico obligatorio |
| 15 | **Detalle de colección** con sus productos | cms | `GET /colecciones/{c}/productos` sin uso (RN-M2-17) |
| 16 | **Formulario de dirección guiado**: buscar en Google primero y completar los detalles después | store | RN-M4-01 (opción A); hoy se mezclan los campos manuales y el mapa |
| 17 | **Admin: tiendas con dueño legible** y **detalle de usuario** | cms | UUID expuesto; `GET /api/usuarios/{id}` sin uso |
| 18 | **Registro con campos opcionales** (apellidos, teléfono, documento) | cms y store | RN-M1-03/04; el backend ya los acepta |

### Deseable (mejoras o requieren backend nuevo)

| # | Pantalla | App | Nota |
| --- | --- | --- | --- |
| 19 | Mi perfil (editar datos personales) | cms y store | **[requiere backend]** |
| 20 | Configuración de la tienda (nombre, dirección web) | cms | **[requiere backend]** |
| 21 | Cerrar tienda o cuenta | cms | **[requiere backend]** RN-M1-17/18 |
| 22 | Inicio de sesión con Google | cms y store | **[requiere backend]** RN-M1-05 |
| 23 | Estado de envío del pedido | cms y store | **[requiere backend]** trabajo futuro |
| 24 | Historial de la cuenta de pago | cms | **[requiere backend]** (`cuenta_pago_historial` sin endpoint) |
| 25 | Cantidad al agregar al carrito; "Mis compras" en todas las tiendas | store | Solo frontend / opcional |
| 26 | Edición masiva de inventario y alerta de stock bajo en el inicio | cms | Mejora |
| 27 | Impuestos (admin) con la vigencia actual destacada | cms | Mejora de presentación |

---

## Anexo · Endpoints sin pantalla

| Endpoint | Motivo / acción sugerida |
| --- | --- |
| `GET /api/usuarios/{idUsuario}` | Usarlo en el detalle de usuario del admin (#17) |
| `GET /api/plantillas/{id}` | Bajo impacto; la lista basta |
| `GET /categorias/{id}`, `GET /colecciones/{c}`, `GET /variantes/{v}`, `GET /variantes/{v}/valores/{x}` | Bajo impacto; útiles si cada recurso tiene URL de edición propia |
| `GET /productos/{id}/presentaciones/{p}`, `GET /productos/{id}/imagenes/{i}` | Bajo impacto |
| `GET /colecciones/activas` | Usarlo en el selector de colecciones del producto |
| `GET /colecciones/{c}/productos` | Detalle de colección (#15) |
| `GET /imagenes/{i}/presentaciones` | Galería de imágenes (#13) |
| `GET /api/cliente/ubigeo` | Sin uso por RN-M4-01 (opción A); reservado para envíos |
| `GET /api/integraciones/mercado-pago/oauth/callback`, `POST /api/publico/mercadopago/notificaciones`, `GET /health`, `GET /api/auth/csrf` | Por diseño, no son pantallas |
