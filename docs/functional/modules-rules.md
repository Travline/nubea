# NUBEA · Reglas de Negocio por Módulo v1.2

> **Referencia de diseño:** las reglas siguientes incluyen comportamiento objetivo y simulaciones del modelo; no implican que todos los flujos estén implementados o disponibles. En particular, checkout, pagos, renovación OAuth y vencimiento de reservas siguen pendientes. El [estado del módulo 4](../technical/modulo-4.md) y el código prevalecen sobre este documento.

El modelo v1.2 (30 tablas, 4 módulos, PostgreSQL; hoy **39 tablas** tras las migraciones V2–V14, ver "Cambios respecto a v1.1 y trabajo futuro") está bien definido: cada regla de negocio se garantiza en la base de datos, en el backend Spring Boot, o en ambos. Este documento indica para cada regla dónde se controla y con qué elemento concreto.

## Alcance y convenciones

El modelo fue ejecutado y verificado en PostgreSQL con una simulación de flujos completos (registro, tienda, catálogo, carrito, checkout, pago, vencimiento y concurrencia).

| Control | Significado | Ejemplos de mecanismo |
| --- | --- | --- |
| **BD** | La base de datos lo impide siempre, sin importar quién escriba | PK, FK, UNIQUE, CHECK, EXCLUDE, trigger |
| **Backend** | Lo valida Spring Boot antes de escribir | Servicio `@Transactional`, Spring Security, validación de DTO |
| **BD + Backend** | El backend valida y muestra un mensaje claro; la BD es la última barrera | `UNIQUE` + validación previa en el servicio |

* **Criterio de reparto:** La BD garantiza la integridad de los datos (lo que nunca puede quedar inconsistente); el backend aplica permisos, flujos y reglas que dependen de quién hace la acción o de servicios externos (Mercado Pago, Cloudflare R2).
* **Permisos en dos niveles:** El rol global (`usuario_rol`) indica qué tipo de usuario es; el permiso sobre una tienda concreta se valida con `tienda.id_propietario` o `tienda_cliente`.
* **Errores:** Cada restricción tiene nombre (por ejemplo `uq_sku_tienda`); el backend la traduce a un mensaje entendible para el usuario.

---

## Módulo 1 · Usuarios, identidades, roles y tiendas

Una sola cuenta global por persona; los roles indican el tipo de usuario y la tienda define el contexto de cada permiso. Cualquier usuario registrado puede crear su tienda.

| Código | Regla | Control | Implementación |
| --- | --- | --- | --- |
| **RN-M1-01** | El correo identifica una cuenta global y es único sin distinguir mayúsculas. | BD + Backend | `UNIQUE(email)` + `chk_usuario_email_formato` (exige minúsculas); el backend normaliza con `trim().toLowerCase()` |
| **RN-M1-02** | Un usuario puede tener varios roles; la misma asignación no se repite. | BD | PK compuesta de `usuario_rol` |
| **RN-M1-03** | El documento es opcional; si se registra, tipo, número y país emisor van juntos y no se repiten entre usuarios. | BD | `chk_documento_conjunto` + `uq_usuario_documento` |
| **RN-M1-04** | El formato del número depende del tipo (DNI: 8 dígitos; CE y pasaporte según su norma). | Backend | Validador del DTO de registro |
| **RN-M1-05** | Una identidad externa (Google) se vincula solo tras acreditar titularidad, nunca por coincidencia de correo; una cuenta de Google pertenece a un solo usuario. El inicio de sesión con Google verifica en el backend el ID token de Google Identity Services (firma RS256 con las claves públicas de Google, emisor, audiencia = GOOGLE_CLIENT_ID, vigencia y correo verificado). Si la identidad no existe y el correo ya tiene cuenta, **no** se vincula: el usuario inicia sesión con su contraseña y vincula Google desde su cuenta, con la sesión iniciada. | BD + Backend | `uq_usuario_identidad_oidc (issuer, subject)` + `uq_usuario_identidad_proveedor`; `GoogleIdTokenVerifier`, `AuthService.autenticarConGoogle` / `vincularGoogle`; `POST /api/auth/google` y `POST /api/auth/google/vinculacion` |
| **RN-M1-06** | Una identidad LOCAL exige contraseña cifrada; una GOOGLE exige issuer y subject y no guarda contraseña. | BD | `chk_identidad_modalidad`; el backend cifra contraseñas locales con Argon2 |
| **RN-M1-07** | Al registrarse, todo usuario recibe el rol CLIENTE. | Backend | Servicio de registro, misma transacción |
| **RN-M1-08** | Al crear su primera tienda, el usuario recibe el rol DUENO_TIENDA en la misma transacción. | Backend | Servicio de creación de tienda |
| **RN-M1-09** | El rol ADMIN_GENERAL solo lo asigna otro ADMIN_GENERAL o la carga inicial del sistema. | Backend | Spring Security: `hasRole('ADMIN_GENERAL')` |
| **RN-M1-10** | Si un ADMIN_GENERAL retira el rol DUENO_TIENDA, el usuario no puede crear tiendas nuevas y sus tiendas pasan a SUSPENDIDA. | Backend | Servicio de administración, misma transacción |
| **RN-M1-11** | Una tienda tiene un único propietario; un propietario puede tener varias tiendas. | BD | FK `tienda.id_propietario`, sin `UNIQUE` |
| **RN-M1-12** | El slug de la tienda es único y solo usa minúsculas, números y guiones. | BD | `UNIQUE(slug)` + `chk_tienda_slug` |
| **RN-M1-13** | Solo el propietario de una tienda la administra (edita su contenido), siempre que conserve el rol DUENO_TIENDA y la tienda esté ACTIVA o en BORRADOR; el rol DUENO_TIENDA no da acceso a tiendas ajenas. Un ADMIN_GENERAL no edita el contenido de tiendas ajenas: solo consulta catálogo, apariencia, banners, inventario y el estado de la cuenta de pago (no pedidos ni pagos, que contienen datos del comprador), y puede suspender o reactivar la tienda; al reactivarla queda en BORRADOR y el dueño decide cuándo activarla. | Backend | `@PreAuthorize`: escrituras `@tiendaAuthorizationService.puedeEditar`; lecturas `hasRole('ADMIN_GENERAL') or puedeVer` (pedidos y pagos solo `puedeVer`); `PATCH /api/admin/tiendas/{id}/estado` (`TiendaService.cambiarEstadoAdministracion`) |
| **RN-M1-14** | Un cliente puede comprar en varias tiendas con una sola cuenta; la relación con cada tienda no se duplica. | BD | PK compuesta de `tienda_cliente` |
| **RN-M1-15** | La relación cliente-tienda se crea al primer "agregar al carrito" o al asociarse expresamente. | BD + Backend | FK de carrito a `tienda_cliente`; el servicio de carrito la crea si no existe |
| **RN-M1-16** | El cliente ve sus pedidos solo dentro de la tienda que visita; el id_usuario sale del token, nunca del request. | Backend | Filtro por usuario autenticado + tienda del contexto |
| **RN-M1-17** | Un usuario o tienda CERRADO/CERRADA registra su fecha de cierre; las transacciones históricas nunca se eliminan. | BD | `chk_cierre_usuario`, `chk_tienda_fecha_cierre`, FK con `ON DELETE RESTRICT` |
| **RN-M1-18** | Antes de cerrar una tienda o cuenta se verifica que no tenga pedidos pendientes de pago. | Backend | Servicio de cierre con bloqueo de fila |

---

## Módulo 2 · Catálogo, variantes, presentaciones, imágenes y colecciones

Todo el catálogo pertenece a una tienda y la BD impide mezclar datos entre tiendas, incluso con escrituras simultáneas.

| Código | Regla | Control | Implementación |
| --- | --- | --- | --- |
| **RN-M2-01** | Cada categoría, variante y colección pertenece a una tienda y no puede cambiar de tienda. | BD | FK a tienda + triggers `trg_categoria_no_mover`, `trg_variante_no_mover`, `trg_coleccion_no_mover` |
| **RN-M2-02** | Cada producto pertenece a una categoría y a la misma tienda de esa categoría; no puede moverse a otra tienda. | BD | FK compuesta `fk_producto_categoria_tienda` + `trg_producto_no_mover` |
| **RN-M2-03** | El slug de categoría y de colección es único dentro de cada tienda. | BD | `UNIQUE(id_tienda, slug)` |
| **RN-M2-04** | Las variantes (Color, Talla) se definen una vez por tienda y se reutilizan; su nombre y sus valores no se repiten sin distinguir mayúsculas. | BD | `uq_variante_nombre_ci`, `uq_valor_variante_ci` |
| **RN-M2-05** | Un valor no puede pasar a otra variante (Negro no puede volverse una Talla). | BD | `trg_valor_no_mover` |
| **RN-M2-06** | La presentación es la unidad vendible: pertenece a un solo producto y a su tienda, y no puede cambiar de producto. | BD | FK compuesta `fk_presentacion_producto_tienda` + `trg_presentacion_no_mover` |
| **RN-M2-07** | El SKU es único en toda la tienda, no solo dentro del producto. | BD + Backend | `uq_sku_tienda (id_tienda, sku)` |
| **RN-M2-08** | El precio de venta es `precio_override` si existe; si no, `precio_base` del producto. Ninguno puede ser negativo. | BD + Backend | CHECK de precios; el servicio calcula el precio efectivo |
| **RN-M2-09** | Una presentación usa como máximo un valor por variante y solo valores de su tienda. | BD | `uq_pv_una_por_variante`, FK compuesta `fk_pv_valor_variante`, `trg_presentacion_valor_validar` |
| **RN-M2-10** | No hay dos presentaciones con la misma combinación de valores en un producto. | BD + Backend | `uq_presentacion_combinacion (id_producto, firma_valores)`; el backend calcula la firma (ids de valor ordenados) |
| **RN-M2-11** | Un producto sin opciones tiene una sola presentación, sin valores. | BD | Firma vacía `''` + `uq_presentacion_combinacion` |
| **RN-M2-12** | Todas las presentaciones de un producto usan el mismo conjunto de variantes (máximo 3). | Backend | Servicio de catálogo |
| **RN-M2-13** | Para publicar un producto: tienda ACTIVA, categoría activa y al menos una versión (presentación) ACTIVA con unidades disponibles (`stock_actual > 0`) en esa misma versión; no basta con que exista el registro de inventario, y una versión inactiva con stock no cuenta. Si falla, se informa el primer requisito incumplido en lenguaje de usuario (por ejemplo: "Para publicar, al menos una versión necesita unidades disponibles"). | Backend | `ProductoService.validarPublicable` (422 `REGLA_NEGOCIO` con `campos.estado`) |
| **RN-M2-14** | Las imágenes se guardan fuera de PostgreSQL (Cloudflare R2); la BD guarda solo la clave de almacenamiento, única. | BD + Backend | `UNIQUE(storage_key)`; el backend sube a R2 y limpia archivos huérfanos |
| **RN-M2-15** | Un producto tiene como máximo una imagen principal. | BD | `uq_imagen_principal_producto` (índice parcial) |
| **RN-M2-16** | Una imagen solo se asigna a presentaciones de su mismo producto. | BD | `trg_presentacion_imagen_producto` |
| **RN-M2-17** | Un producto puede estar en varias colecciones, pero solo de su misma tienda. | BD | PK de `coleccion_producto` + `trg_coleccion_producto_tienda` |
| **RN-M2-18** | Solo el propietario de la tienda modifica su catálogo; el ADMIN_GENERAL solo lo consulta (ver RN-M1-13). | Backend | Spring Security (`puedeEditar`) + verificación de `id_propietario` en `ImagenProductoService` |

---

## Módulo 3 · Apariencia, banners, cuenta de pago e inventario

Cada tienda tiene una sola configuración visual y una sola cuenta de Mercado Pago, y cada presentación vendible tiene un único registro de stock que nunca baja de cero.

| Código | Regla | Control | Implementación |
| --- | --- | --- | --- |
| **RN-M3-01** | Cada tienda tiene como máximo una configuración visual y una cuenta de pago. | BD | `id_tienda` como PK y FK en `configuracion_tienda` y `cuenta_pago_tienda` |
| **RN-M3-02** | La configuración de la tienda se crea junto con la tienda, con la plantilla por defecto. | Backend | Servicio de creación de tienda, misma transacción |
| **RN-M3-03** | Una plantilla puede ser usada por muchas tiendas; solo se pueden elegir plantillas activas. | BD + Backend | FK `id_plantilla`; el servicio valida `plantilla.activa` |
| **RN-M3-04** | Solo un ADMIN_GENERAL crea, edita o desactiva plantillas. | Backend | Spring Security |
| **RN-M3-05** | Los banners tienen orden positivo y sin repetir dentro de la tienda; pueden reordenarse en una sola operación. | BD | `chk_banner_orden` + `uq_banner_tienda_orden` (diferible, se verifica al confirmar) |
| **RN-M3-06** | Las imágenes de banners se guardan en Cloudflare R2; el enlace de destino debe ser una URL segura. | Backend | Servicio de banners |
| **RN-M3-07** | En el MVP solo se integra Mercado Pago; los tokens nunca se guardan en texto plano ni se exponen en React. | BD + Backend | `chk_cuenta_pago_proveedor`; la BD guarda solo una referencia a la credencial (gestor de secretos) |
| **RN-M3-08** | Una cuenta de pago ACTIVA exige cuenta externa, referencia de credencial y fecha de vinculación. | BD | `chk_cuenta_pago_activa` |
| **RN-M3-09** | Solo una vinculación OAuth validada con Mercado Pago habilita los cobros de la tienda. | Backend | Flujo OAuth de vinculación |
| **RN-M3-10** | Cada presentación vendible tiene un único registro de inventario, creado junto con ella. | BD + Backend | PK/FK `inventario.id_presentacion`; el servicio lo crea en la misma transacción |
| **RN-M3-11** | El stock actual y el stock mínimo nunca son negativos. | BD | `chk_inventario_stock`, `chk_inventario_minimo` |
| **RN-M3-12** | El stock se reserva (descuenta) solo al confirmar el checkout, nunca al agregar al carrito. | Backend | `UPDATE inventario … WHERE stock_actual >= :cantidad`, verificando 1 fila afectada |
| **RN-M3-14** | `stock_actual` guarda las unidades **disponibles para vender**. Las unidades **apartadas** son las de pedidos pendientes de pago (reserva ACTIVA): se descuentan en el checkout y, si el pedido se cancela, `trg_pedido_transicion` las devuelve una sola vez. **Mientras no exista el módulo de envíos, un pedido PAGADO se considera entregado; las unidades apartadas son solo las de pedidos pendientes de pago.** El vendedor ajusta el stock de dos formas: (a) ajuste relativo (+/−) del día a día, que nunca deja el stock en negativo; (b) "fijar stock" tras un conteo, donde indica las unidades en mano y se guardan como disponibles `unidades en mano − apartadas` (si el conteo es menor que las apartadas, se rechaza). Ambas bloquean la fila de inventario y "fijar" acepta un `stockEsperado` opcional para detectar cambios simultáneos (409). | Backend | `AjusteInventarioService` (`POST …/inventario/{id}/ajustes`, `PUT …/inventario/{id}`) con `SELECT … FOR UPDATE` |
| **RN-M3-13** | Si dos clientes compran la última unidad a la vez, solo uno la obtiene. | BD | Bloqueo de fila del `UPDATE` condicional (verificado en simulación) |

---

## Módulo 4 · Direcciones, carrito, pedidos, impuestos y pagos

El carrito es libre y no reserva stock; el pedido congela precios, impuestos y dirección en el checkout, y su estado solo avanza en un sentido.

| Código | Regla | Control | Implementación |
| --- | --- | --- | --- |
| **RN-M4-01** | La dirección se registra con Google Places (`place_id`) o como texto libre; `codigo_ubigeo` es opcional y, si se envía, debe existir. Cada usuario tiene como máximo una dirección principal. | BD + Backend | `uq_direccion_principal_usuario` + FK opcional a ubigeo; `DireccionesController` valida el UBIGEO cuando viene y desmarca la principal anterior |
| **RN-M4-02** | Un carrito pertenece a un cliente de una tienda; máximo un carrito ACTIVO por pareja usuario-tienda. | BD | FK compuesta a `tienda_cliente` + `uq_carrito_activo_usuario_tienda` |
| **RN-M4-03** | El carrito solo admite presentaciones de su tienda y cantidades mayores que cero. | BD | `trg_carrito_item_tienda` + CHECK `cantidad > 0` |
| **RN-M4-04** | Solo se agregan al carrito presentaciones ACTIVAS de productos PUBLICADOS; el precio mostrado se recalcula hasta el checkout. | Backend | Servicio de carrito |
| **RN-M4-05** | El checkout es una sola transacción: valida precios y stock, descuenta inventario, crea pedido e ítems y cierra el carrito. **Checkout parcial** (decisión del equipo): se compran solo las líneas disponibles (versión ACTIVA, producto PUBLICADO, categoría activa y stock suficiente); una línea con cantidad mayor que el stock se aparta **entera** (no se compran cantidades parciales). Las líneas no disponibles pasan a un carrito ACTIVO nuevo del mismo comprador. Si ninguna línea está disponible, el checkout se rechaza con un mensaje claro. El total esperado se compara con el total de las líneas disponibles. | Backend | `CheckoutService.reservar` (`@Transactional`); inventario bloqueado en orden fijo por id (`FOR UPDATE`) para que la disponibilidad no cambie hasta confirmar |
| **RN-M4-06** | El carrito de origen del pedido es opcional y, si existe, pertenece al mismo usuario y tienda. | BD | `fk_pedido_carrito_origen_contexto` |
| **RN-M4-07** | Desde el checkout, precios, tasa e importes del pedido quedan congelados. | BD | `trg_pedido_transicion` (rechaza cambios de importes) |
| **RN-M4-08** | El pedido guarda una copia de la dirección, nombres y precios; los cambios posteriores del catálogo o de la dirección no lo afectan. | BD | Columnas snapshot en `pedido` y `pedido_item` |
| **RN-M4-09** | Totales: `total = subtotal + IGV` en cabecera y línea; `total_linea = precio_unitario × cantidad`; la cabecera es la suma de sus líneas. | BD + Backend | `chk_pedido_total`, `chk_linea_total`, `chk_linea_precio`; la suma la calcula el servicio con `BigDecimal HALF_UP` |
| **RN-M4-10** | Los precios del catálogo incluyen IGV; la tasa vigente se toma al momento del checkout. | Backend | Consulta de vigencia en `configuracion_impuesto` |
| **RN-M4-11** | La tasa del pedido coincide exactamente con la vigencia tributaria que referencia. | BD | `fk_pedido_impuesto_tasa (id_impuesto, tasa_impuesto)` |
| **RN-M4-12** | La tasa fiscal es global, la administra solo ADMIN_GENERAL y sus vigencias no se superponen; cambiar la tasa es crear una vigencia nueva. | BD + Backend | `excl_impuesto_vigencia`; Spring Security; `creado_por` registra al administrador |
| **RN-M4-13** | Estados del pedido: PENDIENTE_PAGO pasa a PAGADO o a CANCELADO; ambos son finales. | BD | `chk_pedido_estado_reserva` + `trg_pedido_transicion` |
| **RN-M4-14** | Un pedido solo pasa a PAGADO si existe un pago APROBADO por su total. | BD | `trg_pedido_transicion` |
| **RN-M4-15** | El monto de cada pago es igual al total del pedido; no se crean intentos de pago para pedidos ya pagados o cancelados. | BD | `trg_pago_validar` |
| **RN-M4-16** | Cada intento de pago tiene una clave de idempotencia única y una referencia de Mercado Pago única; un pedido tiene como máximo un pago APROBADO. | BD | `UNIQUE(clave_idempotencia)`, `uq_pago_referencia_proveedor`, `uq_pago_aprobado_pedido` |
| **RN-M4-17** | El webhook de Mercado Pago se verifica, se consulta en el proveedor y se procesa una sola vez. | Backend | Firma + consulta a la API + `UPDATE … WHERE estado = 'PENDIENTE'` |
| **RN-M4-18** | Mercado Pago no se invoca dentro de una transacción abierta de PostgreSQL. | Backend | `@TransactionalEventListener(AFTER_COMMIT)` |
| **RN-M4-19** | Al vencer la reserva, el pedido se cancela **siempre**, haya o no pagos de Mercado Pago habilitados. Sin orden en Mercado Pago, se cancela al vencer. Con una orden creada, si el procesador de pagos está activo, la cancela en Mercado Pago y confirma antes de cancelar el pedido; si está apagado, se cancela pasada una gracia. **Cambio (Bloque 3):** si la creación de la orden quedó incierta, el pedido se cancela localmente pasada la gracia (40 min por defecto), sin confirmar con Mercado Pago: las órdenes se crean con expiración de 30 minutos y solo antes del vencimiento, así que pasada la gracia ya no se pueden pagar. Si aun así llega un pago tardío, se reembolsa automáticamente (RN-M4-21). Un pedido con un trabajo de pago en curso no se interrumpe. | Backend | `VencimientoReservasService` (`@Scheduled`, siempre activo; `reservas.vencimiento.gracia-minutos`), bloqueo intento → pedido con `FOR UPDATE SKIP LOCKED`; `PagosProcesador` para las órdenes creadas |
| **RN-M4-20** | Al cancelar un pedido, su stock vuelve al inventario una sola vez. Si la cancelación es **por vencimiento**, sus productos (versión y cantidad) se agregan además al carrito ACTIVO del comprador en esa tienda (creándolo si no existe y sumando cantidades, con un máximo de 999 por línea), aunque la tienda ya no esté disponible. Un pago rechazado u otras cancelaciones no devuelven los productos al carrito. | BD + Backend | `trg_pedido_transicion` (reserva ACTIVA → LIBERADA) + `ReservasPedido.cancelar` (orden de bloqueo carrito → inventario → pedido) |
| **RN-M4-21** | Si Mercado Pago aprueba un pago después de cancelar el pedido, el pago se registra como REEMBOLSO_PENDIENTE y se devuelve el dinero. | BD + Backend | Estados `REEMBOLSO_PENDIENTE`/`REEMBOLSADO`; el servicio solicita el reembolso |
| **RN-M4-22** | Una tienda solo cobra con su propia cuenta de Mercado Pago ACTIVA. | Backend | El servicio de pagos usa la cuenta de la tienda del pedido |
| **RN-M4-23** | Ningún carrito ni pedido puede reasignarse a otro cliente u otra tienda. | BD | `trg_carrito_no_reasignar`, `trg_pedido_no_reasignar` |
| **RN-M4-24** | Cada pedido tiene un número corto único dentro de su tienda (#1001, #1002…), asignado en el checkout sin huecos ni colisiones y que no cambia después. | BD | V14: `trg_pedido_numero` (contador `tienda.ultimo_numero_pedido`, con la tienda bloqueada por el checkout) + `uq_pedido_numero_tienda` + `trg_pedido_numero_fijo` |

---

## Cambios respecto a v1.1 y trabajo futuro

La v1.2 no agrega tablas respecto a v1.1: convierte en garantías de la BD las reglas que en v1.1 estaban marcadas como "Pendiente" y corrige tres fallas detectadas en la simulación.

> **Estado actual del esquema (V1–V14): 39 tablas** en `public`, sin contar `flyway_schema_history`:
> - **30** del modelo v1.2 (V1);
> - **7** añadidas para OAuth, pagos y almacenamiento: `oauth_mercado_pago_intento` (V2), `cuenta_pago_historial` (V3), `storage_tarea` (V5), `checkout_idempotencia` (V6), `intento_order_mercadopago` (V7), `notificacion_order_mercadopago` (V8) y `reembolso_order` (V9);
> - **2** de sesión: `SPRING_SESSION` y `SPRING_SESSION_ATTRIBUTES` (V13).
>
> V4, V8, V9, V12 y V14 además añaden columnas a tablas del modelo (por ejemplo, `pedido.numero` y `tienda.ultimo_numero_pedido` en V14).

| Cambio | Reglas afectadas | Motivo |
| --- | --- | --- |
| `id_tienda` en producto y presentacion_producto con FK compuestas | RN-M2-02, RN-M2-06, RN-M2-07 | SKU único por tienda garantizado aun con escrituras simultáneas |
| `firma_valores` en presentacion_producto | RN-M2-10, RN-M2-11 | Combinación de valores única por producto |
| `id_variante` en `presentacion_valor` + UNIQUE | RN-M2-09 | Corrige el trigger que impedía editar un valor y resiste concurrencia |
| `id_banner` con DEFAULT y orden diferible | RN-M3-05 | Corrige la falta de id automático y permite reordenar banners |
| FK compuesta de la tasa del pedido | RN-M4-11 | La tasa copiada no puede diferir de su vigencia |
| Triggers `trg_pedido_transicion` y `trg_pago_validar` | RN-M4-07, RN-M4-13 a RN-M4-15, RN-M4-20 | Estados finales, pago obligatorio y stock devuelto una sola vez |
| Estados `REEMBOLSO_PENDIENTE` y `REEMBOLSADO` en pago | RN-M4-21 | Registrar pagos aprobados tarde |
| Reglas de asignación de roles | RN-M1-07 a RN-M1-10, RN-M1-13 | Definir cuándo se asigna cada rol y separar rol global de permiso por tienda |

### Trabajo futuro (se agrega sin modificar las tablas actuales):

* **Envío:** Columna `estado_envio` en pedido o tabla `envio` 1:1; si se cobra, `costo_envio` y ajuste de `chk_pedido_total`.
* **UBIGEO obligatorio para envíos (RN-M4-01):** Hoy `codigo_ubigeo` es opcional porque la dirección se toma de Google Places. Cuando se calculen y gestionen envíos, el UBIGEO pasará a ser obligatorio para las direcciones de entrega.
* **Empleados por tienda:** Tabla `tienda_usuario` con rol por tienda.
* **Cupones y descuentos:** Devoluciones e historial de movimientos de inventario.
