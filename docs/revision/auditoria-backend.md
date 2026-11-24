# Auditoría del backend contra reglas de negocio v1.2 y HU del módulo 3

- **Fecha:** 2026-11-23
- **Alcance:** `backend/src/main` (Java y migraciones Flyway V1–V13), contrastado con `docs/functional/modules-rules.md` (v1.2) y `docs/functional/uh-m3.md`.
- **Método:** lectura estática del código. No se ejecutaron pruebas ni la aplicación, y no se modificó código.

**Leyenda de estado**

| Estado | Significado |
| --- | --- |
| CUMPLE | La regla está garantizada donde el documento lo indica, o con un mecanismo equivalente. |
| PARCIAL | Una parte está implementada y otra falta o tiene una brecha explotable. |
| NO IMPLEMENTADA | No existe el flujo en el backend. |
| CONTRADICE | El código o una migración posterior hace lo contrario de lo que pide la regla. |

**Resumen:** 72 reglas → **65 CUMPLE · 5 PARCIAL · 1 NO IMPLEMENTADA · 1 CONTRADICE**.

| Módulo | Reglas | CUMPLE | PARCIAL | NO IMPL. | CONTRADICE |
| --- | --- | --- | --- | --- | --- |
| M1 | 18 | 15 | 2 (05, 13) | 1 (18) | 0 |
| M2 | 18 | 16 | 2 (13, 18) | 0 | 0 |
| M3 | 13 | 13 | 0 | 0 | 0 |
| M4 | 23 | 21 | 1 (19) | 0 | 1 (01) |

> Varias reglas marcadas CUMPLE tienen notas de riesgo. Las más graves se resumen en la sección final "Reglas críticas que faltan".

---

## Módulo 1 · Usuarios, identidades, roles y tiendas

| Regla | Estado | Dónde se cumple | Observaciones |
| --- | --- | --- | --- |
| RN-M1-01 Email único sin distinguir mayúsculas | CUMPLE | V1: `usuario_plataforma.email UNIQUE` + `chk_usuario_email_formato`. `AuthService.registrar` y `AuthService.autenticar` normalizan con `trim().toLowerCase()`. `GlobalExceptionHandler.integridad` traduce `usuario_plataforma_email_key`. | `RegistroUsuarioRequest.correoElectronico` limita a 100 caracteres y la BD admite 254. Es una inconsistencia menor. |
| RN-M1-02 Varios roles sin repetir | CUMPLE | V1: PK `usuario_rol(id_usuario,id_rol)`. `UsuarioPlataforma.roles` es un `Set` (`@ManyToMany`). | — |
| RN-M1-03 Documento opcional, conjunto y único | CUMPLE | V1: `chk_documento_conjunto` + `uq_usuario_documento`. `AuthService.aplicarDocumento` exige tipo y número juntos. | Para DNI y CE el país se fija en `PE`. El mensaje del `@Pattern` de `paisEmisorDocumento` habla de "tipo de documento" y está mal redactado. |
| RN-M1-04 Formato del número según tipo | CUMPLE | `AuthService.aplicarDocumento`: DNI `\d{8}`, CE `[A-Za-z0-9]{9,12}`, pasaporte `[A-Za-z0-9]{6,12}`. | Se valida en el servicio, no en un validador del DTO como dice el documento. El efecto es el mismo. |
| RN-M1-05 Vincular Google solo tras acreditar titularidad | PARCIAL | V1: `uq_usuario_identidad_oidc (issuer,subject)` y `chk_identidad_modalidad`. | No existe flujo OIDC/Google en el backend. Solo está la parte de BD. CLAUDE.md lo marca fuera de alcance. |
| RN-M1-06 LOCAL con contraseña cifrada; GOOGLE sin contraseña | CUMPLE | V1: `chk_identidad_modalidad`. `SecurityConfig.passwordEncoder` usa Argon2 y `AuthService.registrar` cifra. | CLAUDE.md dice "BCrypt" y las reglas dicen Argon2. El código usa Argon2, así que conviene corregir CLAUDE.md. |
| RN-M1-07 Rol CLIENTE al registrarse | CUMPLE | `AuthService.registrar` (`@Transactional`) agrega CLIENTE antes de guardar. | — |
| RN-M1-08 Rol DUENO_TIENDA al crear la primera tienda | CUMPLE | `TiendaService.crearTienda` (`@Transactional`, bloquea al usuario con `FOR UPDATE`). | — |
| RN-M1-09 Solo ADMIN_GENERAL asigna ADMIN_GENERAL | CUMPLE | `UsuarioController.asignar` con `@PreAuthorize("hasRole('ADMIN_GENERAL')")`. La carga inicial está en `InicializacionAdministrador.run` (advisory lock, solo si no hay admins). | — |
| RN-M1-10 Retirar DUENO_TIENDA suspende sus tiendas y bloquea crear nuevas | CUMPLE | `UsuarioService.retirarDueno` quita el rol y hace `UPDATE tienda SET estado='SUSPENDIDA'` en la misma transacción. `TiendaService.crearTienda` rechaza si `!esDueno && yaTieneTiendas`. `ActiveUserFilter` recarga los roles en cada request. | **Brecha:** el propietario de la tienda suspendida conserva acceso de escritura al catálogo, al inventario y a los reembolsos. `TiendaAuthorizationService.esPropietario` no mira estado ni rol (ver RN-M1-13 y RN-M2-18). |
| RN-M1-11 Un propietario, varias tiendas | CUMPLE | V1: FK `tienda.id_propietario` sin UNIQUE. | — |
| RN-M1-12 Slug único `[a-z0-9-]` | CUMPLE | V1: `UNIQUE(slug)` + `chk_tienda_slug`. `SlugUtil.generar` + bucle `existsBySlug` en `TiendaService.crearTienda`. Si dos usuarios crean a la vez, el handler traduce `tienda_slug_key` a 409. | — |
| RN-M1-13 Solo el propietario (o ADMIN_GENERAL) administra la tienda | PARCIAL | `TiendaAuthorizationService.esPropietario` compara `tienda.id_propietario` con el principal. Se usa en `@PreAuthorize` de todos los controladores por tienda. `TiendaService.activarTienda` valida en el servicio. | 1) **Política inconsistente con ADMIN_GENERAL:** M2 e inventario usan `hasRole('ADMIN_GENERAL') or esPropietario`, y M3, pedidos y pagos usan solo `esPropietario` (lo que coincide con la HU-M3). Hay que decidir una sola política. 2) `esPropietario` no considera el estado de la tienda ni que el usuario conserve DUENO_TIENDA. 3) `CrearTiendaRequest.idPropietario` aún llega en el body. Se valida que coincida con el token, pero debería eliminarse. |
| RN-M1-14 Cliente en varias tiendas sin duplicar relación | CUMPLE | V1: PK `tienda_cliente(id_usuario,id_tienda)`. `CarritoController.guardarCantidad` usa `ON CONFLICT DO NOTHING`. | — |
| RN-M1-15 `tienda_cliente` al primer "agregar al carrito" | CUMPLE | `CarritoController.guardarCantidad` inserta `tienda_cliente` antes del carrito. V1: FK `carrito → tienda_cliente`. | No hay endpoint para "asociarse expresamente". Es opcional según la regla. |
| RN-M1-16 Cliente ve sus pedidos solo en la tienda visitada; id desde el token | CUMPLE | `PedidosController.misPedidos/miPedido` filtran por `id_tienda` (desde el slug) y `id_usuario` (de `CurrentUserProvider`). `PagosController.consultar` hace lo mismo. | — |
| RN-M1-17 CERRADO/CERRADA con fecha; histórico nunca se borra | CUMPLE | V1: `chk_cierre_usuario`, `chk_tienda_fecha_cierre`, todas las FK `ON DELETE RESTRICT`. | No hay flujo de cierre, así que la regla solo la garantiza la BD. |
| RN-M1-18 No cerrar tienda o cuenta con pedidos pendientes de pago | NO IMPLEMENTADA | — | No existe servicio de cierre de tienda ni de cuenta. Lo más parecido es `CuentaPagoDesconexionService.desconectar`, que bloquea la desconexión de Mercado Pago si hay pedidos o pagos pendientes. |

---

## Módulo 2 · Catálogo

Todos los controladores de `catalogo/controller` llevan `@PreAuthorize("hasRole('ADMIN_GENERAL') or @tiendaAuthorizationService.esPropietario(#idTienda, authentication)")`. Los servicios lanzan `IllegalArgumentException`, que `Modulo3ExceptionHandler` traduce a 400 (incluidos los casos "no existe", que deberían ser 404).

| Regla | Estado | Dónde se cumple | Observaciones |
| --- | --- | --- | --- |
| RN-M2-01 Categoría, variante y colección no cambian de tienda | CUMPLE | V1: FK a tienda + `trg_categoria_no_mover`, `trg_variante_no_mover`, `trg_coleccion_no_mover`. Ningún servicio modifica `idTienda` después de crear. | — |
| RN-M2-02 Producto en categoría de su misma tienda | CUMPLE | V1: `fk_producto_categoria_tienda` + `trg_producto_no_mover`. `ProductoService.validarCategoria` usa `findByIdCategoriaAndIdTienda`. | — |
| RN-M2-03 Slug de categoría y colección único por tienda | CUMPLE | V1: `UNIQUE(id_tienda, slug)` en ambas tablas. `CategoriaService.validarSlugDisponible` y `ColeccionService.validarSlugDisponible`. | El formato del slug no se valida en el backend. Solo lo frena el CHECK de BD, que devuelve un 409 genérico. |
| RN-M2-04 Variantes y valores sin repetir (case-insensitive) | CUMPLE | V1: `uq_variante_nombre_ci`, `uq_valor_variante_ci`. `VarianteService` y `ValorVarianteService` usan `existsBy…IgnoreCase`. | El servicio permite nombres de variante de hasta 100 caracteres y la BD solo 80 (`variante.nombre VARCHAR(80)`). Entre 81 y 100 caracteres sale un error de BD genérico. |
| RN-M2-05 Un valor no cambia de variante | CUMPLE | V1: `trg_valor_no_mover`. `ValorVarianteService.actualizarValor` solo cambia el texto. | — |
| RN-M2-06 Presentación ligada a un producto y su tienda | CUMPLE | V1: `fk_presentacion_producto_tienda` + `trg_presentacion_no_mover`. `PresentacionProductoService.*` verifica producto y tienda. | — |
| RN-M2-07 SKU único por tienda | CUMPLE | V1: `uq_sku_tienda`. `PresentacionProductoService.crearPresentacion/actualizarPresentacion` validan antes con `existsByIdTiendaAndSku`. | Si dos requests chocan a la vez, el error de BD no se traduce a un mensaje específico. |
| RN-M2-08 Precio efectivo = override o base; nunca negativo | CUMPLE | V1: CHECK de `precio_base` y `precio_override`. DTO con `@DecimalMin("0.00")`. `PresentacionProductoService.respuesta` calcula `precioEfectivo`. `CheckoutService` usa `COALESCE(precio_override, precio_base)`. | — |
| RN-M2-09 Máximo un valor por variante, solo de su tienda | CUMPLE | V1: `uq_pv_una_por_variante`, `fk_pv_valor_variante`, `trg_presentacion_valor_validar`. `PresentacionProductoService.validarValores`. | — |
| RN-M2-10 Combinación única por producto | CUMPLE | V1: `uq_presentacion_combinacion`. `PresentacionProductoService.construirFirmaValores` (ids ordenados, separador `|`) + `existsByIdProductoAndFirmaValores…`. | — |
| RN-M2-11 Producto sin opciones = una presentación sin valores | CUMPLE | Firma `''` + `uq_presentacion_combinacion`. | — |
| RN-M2-12 Mismo conjunto de variantes (máx. 3) | CUMPLE | `PresentacionProductoService.validarConjuntoVariantes` (bloquea el producto con `FOR UPDATE`) + límite 3 en `validarValores`. | — |
| RN-M2-13 Publicar: tienda ACTIVA, categoría activa, presentación ACTIVA con inventario | PARCIAL | `ProductoService.cambiarEstadoProducto` comprueba con `EXISTS(...)` al pasar a `PUBLICADO`. | 1) "Con inventario" se interpreta como que exista la fila de `inventario`, no `stock_actual > 0`. Hay que confirmar la intención. 2) Solo se valida al publicar. Si después se desactiva la categoría o la presentación, o se suspende la tienda, el producto sigue `PUBLICADO`. La vitrina y el carrito filtran por categoría activa, pero no por presentación activa a nivel de producto. |
| RN-M2-14 Imágenes fuera de PostgreSQL; `storage_key` único; limpieza de huérfanos | CUMPLE | V1: `imagen_producto.storage_key UNIQUE`. `AlmacenamientoImagenes.subirYGuardar` (sube fuera de la transacción) + tabla `storage_tarea` (V5) + `procesarPendientes` (`@Scheduled`, `SKIP LOCKED`). `ImagenValidator` valida los bytes mágicos. | Reutiliza `BannerProperties` para los límites de las imágenes de producto. |
| RN-M2-15 Máximo una imagen principal | CUMPLE | V1: `uq_imagen_principal_producto` (índice parcial). `ImagenProductoService.quitarPrincipal` antes de marcar otra. | — |
| RN-M2-16 Imagen solo para presentaciones de su producto | CUMPLE | V1: `trg_presentacion_imagen_producto`. `PresentacionImagenService.asociarImagen` valida el producto. | `PresentacionImagenController` devuelve entidades JPA (`ImagenProducto`, `PresentacionProducto`) en lugar de DTOs. |
| RN-M2-17 Producto en colecciones de su misma tienda | CUMPLE | V1: PK `coleccion_producto` + `trg_coleccion_producto_tienda`. `ColeccionProductoService.agregarProducto` busca ambos por tienda. | — |
| RN-M2-18 Solo el propietario modifica su catálogo | PARCIAL | `@PreAuthorize` en todos los controladores de catálogo. `ImagenProductoService.bloquearProducto` revalida con bloqueo. | 1) Se permite también a ADMIN_GENERAL, y la regla dice "solo el propietario". La HU-M3 dice que el admin no adquiere titularidad. 2) No se aplica `TiendaStatePolicy`: se puede editar el catálogo de una tienda `SUSPENDIDA` o `CERRADA`. |

---

## Módulo 3 · Apariencia, banners, cuenta de pago e inventario

### Reglas RN-M3

| Regla | Estado | Dónde se cumple | Observaciones |
| --- | --- | --- | --- |
| RN-M3-01 Una configuración y una cuenta de pago por tienda | CUMPLE | V1: `id_tienda` PK y FK en `configuracion_tienda` y `cuenta_pago_tienda`. | — |
| RN-M3-02 Configuración creada con la tienda y plantilla por defecto | CUMPLE | `TiendaService.crearTienda` crea `ConfiguracionTienda` con la plantilla 1 y además `cuenta_pago_tienda` en `PENDIENTE`, en la misma transacción. V1 inserta la plantilla `minimal`. | La plantilla por defecto está fija en el código (`PLANTILLA_POR_DEFECTO = 1`). Si se desactiva, no se pueden crear tiendas. |
| RN-M3-03 Plantilla compartida; solo se eligen activas | CUMPLE | V1: FK `fk_config_plantilla`. `ConfiguracionTiendaServiceImpl.actualizarConfiguracion` lanza `PlantillaInactivaException` (409). | — |
| RN-M3-04 Solo ADMIN_GENERAL gestiona plantillas | CUMPLE | `PlantillaController` con `@PreAuthorize("hasRole('ADMIN_GENERAL')")` en POST y PATCH. | La ruta es `/api/plantillas` y la HU propone `/api/admin/plantillas`. |
| RN-M3-05 Orden de banner positivo, único y reordenable | CUMPLE | V1: `chk_banner_orden` + `uq_banner_tienda_orden DEFERRABLE INITIALLY DEFERRED`. `BannerServiceImpl.reordenarBanners` exige el conjunto exacto, sin ids ni órdenes repetidos. | — |
| RN-M3-06 Imagen en R2/S3; enlace HTTPS | CUMPLE | `BannerServiceImpl.normalizarYValidarEnlace` (esquema `https` + host). `AlmacenamientoImagenes` + `S3StorageAdapter`. | — |
| RN-M3-07 Solo Mercado Pago; tokens nunca en claro ni en el frontend | CUMPLE | V1: `chk_cuenta_pago_proveedor`. `AesGcmCredentialVault` (AES-GCM, AAD por finalidad, rotación de claves). `CuentaPagoResponse` no expone la credencial y enmascara la cuenta. `toString()` redactados. | `referencia_credencial` guarda el sobre cifrado, no una referencia a un gestor externo. La HU lo permite. |
| RN-M3-08 ACTIVA exige cuenta, credencial y fecha | CUMPLE | V1: `chk_cuenta_pago_activa`. `OAuthTransactionStore.activar` llena los tres campos. | — |
| RN-M3-09 Solo un OAuth validado habilita cobros | CUMPLE | Solo `OAuthTransactionStore.activar` pone `estado='ACTIVA'`, después de `consumir` (state de un solo uso, hash SHA-256, expiración) + `validar` + canje con PKCE en `OAuthService.callback`. | Todo el flujo depende de `mercadopago.oauth.enabled=true` (`@ConditionalOnProperty`). |
| RN-M3-10 Un inventario por presentación, creado con ella | CUMPLE | V1: PK/FK `inventario.id_presentacion`. `PresentacionProductoService.crearPresentacion` inserta `inventario` en la misma transacción. | — |
| RN-M3-11 Stock y mínimo nunca negativos | CUMPLE | V1: `chk_inventario_stock`, `chk_inventario_minimo`. `ActualizarInventarioRequest` con `@Min(0)`. | — |
| RN-M3-12 Stock se descuenta solo al confirmar el checkout | CUMPLE | `CheckoutService.reservar`: `UPDATE inventario … WHERE stock_actual >= ?` verificando una fila. `CarritoController` no toca el inventario. | Ver riesgo de `InventarioGestionController` en la sección final. |
| RN-M3-13 Última unidad: solo un comprador | CUMPLE | Bloqueo de fila del `UPDATE` condicional anterior, con orden fijo por `id_presentacion`. | — |

### Historias de usuario HU-M3

| HU | Estado | Observaciones |
| --- | --- | --- |
| HU-M3-01 Crear plantilla | CUMPLE | `PlantillaServiceImpl.crearPlantilla`: trim, obligatoriedad, 409 si el código está duplicado, 201. Ruta distinta a la propuesta. |
| HU-M3-02 Ciclo de vida de plantilla | CUMPLE | `actualizarPlantilla` no permite cambiar `codigo`. `actualizarEstadoPlantilla`. 404/400/403. |
| HU-M3-03 Consultar plantillas | PARCIAL | Cumple la visibilidad de inactivas. Pero `@PreAuthorize("hasAnyRole('ADMIN_GENERAL','DUENO_TIENDA')")` impide que un CLIENTE vea el catálogo antes de crear su primera tienda. |
| HU-M3-04 Configuración de la tienda | PARCIAL | Cumple propiedad, plantilla activa, título ≤160 y no vacío, estado de tienda y 404 si falta la configuración. **Falta el criterio 3:** no se rechazan campos desconocidos (Jackson ignora por defecto; no hay `FAIL_ON_UNKNOWN_PROPERTIES` ni `@JsonIgnoreProperties(ignoreUnknown=false)`). |
| HU-M3-05 Banners | CUMPLE | Multipart, validación por bytes mágicos, clave aleatoria, HTTPS, limpieza diferida con `storage_tarea`, reorden completo y control de estado y propiedad en `BannerServiceImpl.validarEstadoTienda`. |
| HU-M3-06 Consultar stock | CONTRADICE (criterio 5) | La consulta cumple: filtro por `presentacion_producto.id_tienda`, filtros, paginación y `sinRegistroInventario`. Pero existe `PUT /api/tiendas/{idTienda}/inventario/{idPresentacion}` en `InventarioGestionController`, y el criterio 5 prohíbe mutar inventario desde M3. Si la intención es que M2 sea dueño del ajuste manual, hay que moverlo y documentarlo. |
| HU-M3-07 Estado de la cuenta de pago | CUMPLE | `CuentaPagoConsultaService.consultar` enmascara `idCuentaExterna`. La fila se crea con la tienda (`PENDIENTE`). |
| HU-M3-08 Iniciar OAuth | CUMPLE | `OAuthService.iniciar` + `OAuthTransactionStore.iniciar`: state aleatorio de 32 bytes guardado como hash, PKCE, invalidación de intentos previos e índice `uq_oauth_mp_pendiente_tienda` (V2). |
| HU-M3-09 Callback OAuth | CUMPLE | El state se consume siempre, en `REQUIRES_NEW`. Se valida propietario y estado. El conflicto por cuenta duplicada (23505) se traduce a `resultado=conflicto`. No se marca `ERROR` ante fallos persistentes, pero la HU lo permite como opcional. |
| HU-M3-10 Desconectar | PARCIAL | Pasa a `DESCONECTADA`, borra la credencial (`referencia_credencial=NULL`) y registra historial (V3). **Falta** intentar la revocación en Mercado Pago después del commit con reintento. Se agregó un bloqueo extra si hay pedidos o pagos pendientes, que no estaba en la HU. |

---

## Módulo 4 · Direcciones, carrito, pedidos, impuestos y pagos

El checkout HTTP y el procesador de pagos solo existen si `mercadopago.pagos.enabled=true` (por defecto `false`). El webhook depende de `mercadopago.notificaciones.enabled`.

| Regla | Estado | Dónde se cumple | Observaciones |
| --- | --- | --- | --- |
| RN-M4-01 UBIGEO válido y una dirección principal | **CONTRADICE** | V1: FK a `ubigeo` + `uq_direccion_principal_usuario`. `DireccionesController.validarUbigeo` cuando viene el código, y `desmarcar` antes de marcar otra como principal. | **V12 hace `codigo_ubigeo` NULLABLE** (direcciones de Google Places con `place_id`). Una dirección puede no tener UBIGEO, lo que contradice "usan un UBIGEO válido". La parte de dirección principal sí cumple. Hay que actualizar la regla o exigir UBIGEO. |
| RN-M4-02 Un carrito ACTIVO por usuario y tienda | CUMPLE | V1: FK a `tienda_cliente` + `uq_carrito_activo_usuario_tienda`. `CarritoController.guardarCantidad` usa `ON CONFLICT … WHERE estado='ACTIVO'`. | — |
| RN-M4-03 Ítems de su tienda y cantidad > 0 | CUMPLE | V1: `trg_carrito_item_tienda` + `CHECK cantidad>0`. DTO `Cantidad` con `@Min(1) @Max(999)`. | — |
| RN-M4-04 Solo presentaciones ACTIVAS de productos PUBLICADOS; precio recalculado | CUMPLE | `CarritoController.guardarCantidad` (filtra estado, publicación y categoría activa). `CarritoController.leer` recalcula el precio en cada lectura. | — |
| RN-M4-05 Checkout en una transacción, con bloqueo en orden fijo | CUMPLE | `CheckoutService.reservar` (dentro de `PagosStore.preparar`, `@Transactional`): bloquea usuario → tienda → cuenta → carrito, ítems `ORDER BY pr.id_presentacion FOR UPDATE`, descuenta stock, crea pedido e ítems y cierra el carrito. | Además valida `totalEsperado` y usa idempotencia (`checkout_idempotencia`, V6). |
| RN-M4-06 Carrito de origen del mismo usuario y tienda | CUMPLE | V1: `fk_pedido_carrito_origen_contexto`. | — |
| RN-M4-07 Importes congelados | CUMPLE | V1: `trg_pedido_transicion`. Ningún servicio actualiza importes. | — |
| RN-M4-08 Snapshot de dirección, nombres y precios | CUMPLE | V1: columnas snapshot en `pedido` y `pedido_item`. `CheckoutService.reservar` las llena. | V12 hace nullable `ubigeo_entrega` y departamento, provincia y distrito. `nombre_receptor` solo copia `nombres` (sin apellidos). |
| RN-M4-09 Totales coherentes | CUMPLE | V1: `chk_pedido_total`, `chk_linea_total`, `chk_linea_precio`. `CheckoutService` calcula por línea con `HALF_UP` y suma la cabecera. | — |
| RN-M4-10 Precios con IGV; tasa vigente al checkout | CUMPLE | `CheckoutService.reservar` consulta la vigencia con `fecha_inicio<=CURRENT_DATE<fecha_fin` y extrae el IGV del total de cada línea. | — |
| RN-M4-11 Tasa del pedido = vigencia referenciada | CUMPLE | V1: `fk_pedido_impuesto_tasa`. | — |
| RN-M4-12 Tasa global, solo ADMIN_GENERAL, vigencias sin solaparse | CUMPLE | V1: `excl_impuesto_vigencia`. `ImpuestosController` (`hasAuthority('ROLE_ADMIN_GENERAL')`): vigencia futura, cierra la anterior y registra `creado_por`. | — |
| RN-M4-13 PENDIENTE_PAGO → PAGADO o CANCELADO (finales) | CUMPLE | V1: `chk_pedido_estado_reserva` + `trg_pedido_transicion`. | — |
| RN-M4-14 PAGADO solo con pago APROBADO por el total | CUMPLE | V1: `trg_pedido_transicion`. `PagosStore.aplicar` aprueba el pago antes de marcar `PAGADO`. | — |
| RN-M4-15 Monto = total; sin intentos en pedidos cerrados | CUMPLE | V1: `trg_pago_validar`. | — |
| RN-M4-16 Idempotencia y referencia únicas; máximo un APROBADO | CUMPLE | V1: `UNIQUE(clave_idempotencia)`, `uq_pago_referencia_proveedor`, `uq_pago_aprobado_pedido`. | Hay una sola fila `pago` por pedido (`clave='pedido:<id>'`). Los reintentos viven en `intento_order_mercadopago` (V7), no en `pago`. `ConfirmacionPagoService` no se usa e insertaría una segunda fila con la misma `referencia_externa`, lo que violaría el índice. Conviene eliminarlo. |
| RN-M4-17 Webhook verificado, consultado y procesado una vez | CUMPLE | `NotificacionOrderController.recibir` (HMAC con `FirmaWebhookMercadoPago.verificar`, solo encola). `PagosProcesador.procesar` consulta la orden en Mercado Pago. `PagosStore.aplicar` usa `UPDATE pago … WHERE estado='PENDIENTE'` y versiones de notificación. | — |
| RN-M4-18 Mercado Pago fuera de transacciones abiertas | CUMPLE | Mecanismo equivalente: `PagosController.comprar` y `PagosProcesador.procesar` no son `@Transactional`. Las escrituras van en métodos transaccionales cortos de `PagosStore`, con un lease (`tarea_dueno`/`tarea_hasta`) en lugar de `@TransactionalEventListener(AFTER_COMMIT)`. | — |
| RN-M4-19 Vencimiento: cancelar solo tras confirmar con Mercado Pago | PARCIAL | `PagosProcesador.procesar`: si vence, `mercado.cancelar`, luego `consultar` y `aplicar`. `PagosStore.iniciarCreacion` cancela localmente si nunca se creó la orden. El reintento es con backoff en `PagosStore.terminar`. | 1) Un pedido en `CREACION_INCIERTA` vencido lanza `CREACION_INCIERTA_VENCIDA` en cada ciclo y **retiene el stock indefinidamente** sin salida automática ni alerta. 2) Usa un lease por fila en vez de `FOR UPDATE SKIP LOCKED`. Es equivalente, pero distinto de lo documentado. 3) Sin `mercadopago.pagos.enabled` no hay tarea de vencimiento (tampoco hay checkout, así que hoy no hay pedidos huérfanos). |
| RN-M4-20 Stock devuelto una sola vez al cancelar | CUMPLE | V1: `trg_pedido_transicion` (ACTIVA → LIBERADA). `PagosStore.cancelarLocal` solo transiciona desde `PENDIENTE_PAGO`. | Hay un segundo camino de reposición (reembolso con `reponer_stock`) en `PagosStore.aplicar`, protegido por `reembolso_order.stock_repuesto` (V9). |
| RN-M4-21 Aprobado tras cancelar → REEMBOLSO_PENDIENTE y devolución | CUMPLE | `PagosStore.aplicar` (rama `CANCELADO`) → `marcarReembolso` → `PagosProcesador` llama a `mercado.reembolsar`. | — |
| RN-M4-22 Cobrar solo con la cuenta ACTIVA propia | CUMPLE | `CheckoutService.reservar` exige `cuenta_pago_tienda.estado='ACTIVA'`. `CredencialesVendedorService.obtener(tienda)`. `PagosStore.preparar` y `aplicar` comparan `cuenta_receptora` y `orden.vendedor()`. | Hay una lista blanca adicional (`mercadopago.pagos.tiendas`) en `PagosConfiguracion`. |
| RN-M4-23 Carrito y pedido no se reasignan | CUMPLE | V1: `trg_carrito_no_reasignar`, `trg_pedido_no_reasignar`. | — |

---

## 1. Reglas críticas que faltan (seguridad, permisos por tienda, stock)

Ordenadas por impacto:

1. **Edición de tiendas suspendidas o cerradas (permisos por tienda).** `TiendaAuthorizationService.esPropietario` solo compara `id_propietario`. Catálogo (M2), imágenes, inventario (`PUT /inventario/{id}`), pedidos y reembolsos no aplican `TiendaStatePolicy`. Después de `UsuarioService.retirarDueno` (RN-M1-10), el ex-dueño sigue modificando el catálogo y el stock de su tienda `SUSPENDIDA`. Afecta a RN-M1-10, RN-M1-13 y RN-M2-18.
2. **Ajuste de inventario por sobrescritura absoluta (stock).** `InventarioGestionController.actualizar` hace `SET stock_actual = ?` sin bloqueo ni control de versión:
   - Pisa descuentos concurrentes del checkout (lost update).
   - Si hay pedidos `PENDIENTE_PAGO`, al cancelarse el trigger **suma de nuevo** las unidades reservadas sobre un stock que el vendedor ya "corrigió", y el stock queda inflado.
   - Además contradice el criterio 5 de HU-M3-06.

   Recomendación: ajuste por delta (`stock_actual = stock_actual + :delta`) o `SELECT … FOR UPDATE` con versión, y moverlo a M2.
3. **Política de ADMIN_GENERAL inconsistente (permisos).** En M2 e inventario el admin puede escribir en cualquier tienda. En M3, pedidos y pagos no puede. La HU-M3 dice que el admin no adquiere titularidad y RN-M1-13 dice que sí administra. Hay que decidir y unificar en `TiendaAuthorizationService`.
4. **Stock retenido sin salida (stock).** `CREACION_INCIERTA` vencido (RN-M4-19) mantiene la reserva `ACTIVA` para siempre. Hace falta un camino de resolución: consultar la orden por referencia externa o escalar a revisión manual con alerta.
5. **Cierre de cuenta o tienda (RN-M1-18) no existe.** No es un agujero de seguridad hoy porque no se puede cerrar nada, pero cuando se implemente debe verificar pedidos pendientes con bloqueo de fila.
6. **Login con Google (RN-M1-05) no existe.** Solo está la restricción de BD. Riesgo nulo mientras no se agregue, pero no debe implementarse vinculando por coincidencia de email.
7. **Publicación que no se revoca (RN-M2-13).** Desactivar la categoría o la presentación, o suspender la tienda, no despublica el producto.
8. **Menores de seguridad y contrato:**
   - `CrearTiendaRequest.idPropietario` sigue en el contrato (aunque se valida).
   - No se rechazan campos desconocidos en los PATCH (HU-M3-04).
   - `PresentacionImagenController` serializa entidades JPA.
   - `IllegalArgumentException` → 400 para recursos inexistentes (debería ser 404).
   - `ConfirmacionPagoService` es código muerto e inconsistente con `PagosStore`.

**Lo que sí está bien resuelto:**
- CSRF con cookie (`CookieCsrfTokenRepository`), excluido solo en el webhook.
- Sesión JDBC `HttpOnly` y `Secure`, con expiración absoluta y rotación del id de sesión en login.
- `ActiveUserFilter` revalida estado y roles en cada request.
- El id de usuario siempre sale del principal.
- Bloqueo condicional y en orden fijo del inventario en el checkout.
- El webhook nunca es fuente de verdad.
- Vault AES-GCM con AAD y rotación.

---

## 2. Diferencias entre las migraciones V1–V13 y el modelo v1.2

**V1 = modelo v1.2 (30 tablas).** Coincide con las reglas del documento, con estos matices:

| # | Diferencia | Dónde | Impacto |
| --- | --- | --- | --- |
| 1 | Varias restricciones **sin nombre** (el documento dice que "cada restricción tiene nombre"): `UNIQUE(email)`, `UNIQUE(slug)` de tienda, `UNIQUE(id_tienda, slug)` de categoría y colección, `storage_key UNIQUE`, `clave_idempotencia UNIQUE` y varios CHECK. PostgreSQL les pone nombres automáticos (`tienda_slug_key`, etc.). | V1 | El handler depende de nombres generados. CLAUDE.md menciona `uq_tienda_slug`, que no existe. |
| 2 | Comentarios con numeración de v1.1: `uq_sku_tienda -- RN-M2-06` (en v1.2 es M2-07), `uq_presentacion_combinacion -- RN-M2-05` (M2-10), `uq_pv_una_por_variante … (RN-M2-04)` (M2-09), `fk_pedido_impuesto_tasa (RN-M4-06)` (M4-11). | V1 | Documental. Confunde la trazabilidad. |
| 3 | Restricciones redundantes además de las case-insensitive: `uq_variante_tienda_nombre` y `uq_valor_variante`. | V1 | Inocuo. |
| 4 | Trigger adicional `trg_imagen_no_mover`, que no está en el documento. | V1 | Positivo. |
| 5 | **9 tablas nuevas** (total 39): `oauth_mercado_pago_intento` (V2), `cuenta_pago_historial` (V3), `storage_tarea` (V5), `checkout_idempotencia` (V6), `intento_order_mercadopago` (V7), `notificacion_order_mercadopago` (V8), `reembolso_order` (V9), `SPRING_SESSION` y `SPRING_SESSION_ATTRIBUTES` (V13). | V2–V13 | El documento afirma "30 tablas" y "v1.2 no agrega tablas". Hay que actualizarlo. |
| 6 | **Se alteran tablas del modelo** (el documento dice que el trabajo futuro "se agrega sin modificar las tablas actuales"): `plantilla.id_plantilla` pasa a IDENTITY (V4). `pedido.ambiente_pago` (V8/V9). `cuenta_pago_tienda.ambiente_pago`, `renovacion_dueno` y `renovacion_hasta` (V9). `direccion_cliente.departamento`, `provincia`, `distrito` y `place_id` (V12). `pedido.place_id_entrega` (V12). | V4, V8, V9, V12 | Cambio de modelo no reflejado en v1.2. |
| 7 | **Relajación de NOT NULL:** `direccion_cliente.codigo_ubigeo`, `pedido.ubigeo_entrega`, `departamento_entrega`, `provincia_entrega` y `distrito_entrega` pasan a nullable. | V12 | **Contradice RN-M4-01** y debilita el snapshot de RN-M4-08. |
| 8 | Máquina de estados paralela de pago: `intento_order_mercadopago.estado` (`PREPARADO`, `CREACION_INCIERTA`, `CREADA`, `FINALIZADA`, `REEMBOLSO_PENDIENTE`, `REEMBOLSADA`) convive con `pago.estado`. `pago` pasa a tener una fila por pedido, no una por intento. | V7, `PagosStore` | Reinterpreta RN-M4-16. Hay que documentarlo. |
| 9 | Segundo camino de reposición de stock fuera de `trg_pedido_transicion`: reembolso con `reponer_stock`, controlado por `reembolso_order.stock_repuesto`. | V9, `PagosStore.aplicar` | RN-M4-20 dice que la devolución de stock es solo por trigger. |
| 10 | `cuenta_pago_historial.estado_nuevo` solo admite `DESCONECTADA`. | V3 | No registra activaciones ni errores. |
| 11 | `checkout_idempotencia.id_direccion` sin FK (intencional, según el comentario). `renovacion_dueno` y `tarea_dueno` sin FK (son tokens de lease). | V6, V9 | Diseño consciente. |
| 12 | Columnas `ambiente_pago` (`PRUEBA`/`PRODUCCION`) y lista blanca de tiendas para habilitar pagos. | V8/V9 | Concepto inexistente en v1.2. |

---

## 3. Endpoints por módulo

Salvo que se indique otra cosa, todas las rutas requieren sesión autenticada (`anyRequest().authenticated()`). "Propietario" significa `@tiendaAuthorizationService.esPropietario(#idTienda, authentication)`.

### M1 · Identidad, usuarios y tiendas

| Método | Ruta | Autorización | Clase |
| --- | --- | --- | --- |
| GET | `/api/auth/csrf` | pública | `AuthController.csrf` |
| GET | `/api/auth/me` | pública (401 sin sesión) | `AuthController.me` |
| POST | `/api/auth/login` | pública | `AuthController.login` |
| POST | `/api/auth/registro` | pública | `AuthController.registrar` |
| POST | `/logout` | Spring Security (204) | `SecurityConfig` |
| GET | `/api/usuarios/{idUsuario}` | ADMIN o el propio usuario | `UsuarioController.obtener` |
| PUT | `/api/usuarios/{idUsuario}/roles/{rol}` | ADMIN | `UsuarioController.asignar` |
| DELETE | `/api/usuarios/{idUsuario}/roles/DUENO_TIENDA` | ADMIN | `UsuarioController.retirarDueno` |
| POST | `/api/tiendas` | autenticado | `TiendaController.crear` |
| POST | `/api/tiendas/{idTienda}/activacion` | autenticado (propietario con DUENO_TIENDA, validado en el servicio) | `TiendaController.activar` |
| GET | `/api/tiendas/propietario/{idUsuario}` | ADMIN o el propio usuario (en el servicio) | `TiendaController.listarPorPropietario` |
| GET | `/api/admin/usuarios` | ADMIN | `AdministracionController.usuarios` |
| GET | `/api/admin/tiendas` | ADMIN | `AdministracionController.tiendas` |
| GET | `/api/tiendas/{slug}` y `/api/publico/tiendas/{slug}` | pública (vitrina: tienda ACTIVA, productos PUBLICADOS) | `VitrinaController.obtener` |
| GET | `/health` | pública | `HealthController.health` |

### M2 · Catálogo (todas: ADMIN o propietario)

| Recurso | Endpoints | Clase |
| --- | --- | --- |
| Categorías | `POST`, `GET` `/api/tiendas/{idTienda}/categorias`. `GET`, `PUT` `/{idCategoria}`. `PATCH /{idCategoria}/estado` | `CategoriaController` |
| Productos | `POST`, `GET` `/api/tiendas/{idTienda}/productos`. `GET`, `PUT` `/{idProducto}`. `PATCH /{idProducto}/estado?estado=` | `ProductoController` |
| Variantes | `POST`, `GET` `/api/tiendas/{idTienda}/variantes`. `GET`, `PUT` `/{idVariante}` | `VarianteController` |
| Valores | `POST`, `GET` `/api/tiendas/{idTienda}/variantes/{idVariante}/valores`. `GET`, `PUT` `/{idValor}` | `ValorVarianteController` |
| Presentaciones | `POST`, `GET` `/api/tiendas/{idTienda}/productos/{idProducto}/presentaciones`. `GET`, `PUT` `/{idPresentacion}`. `PATCH /{idPresentacion}/estado`. `PUT /{idPresentacion}/valores` | `PresentacionProductoController` |
| Imágenes de producto | `POST` (multipart), `GET` `/api/tiendas/{idTienda}/productos/{idProducto}/imagenes`. `GET`, `PUT`, `DELETE` `/{idImagen}`. `PUT /{idImagen}/archivo` (multipart). `PATCH /{idImagen}/principal` | `ImagenProductoController` |
| Imagen ↔ presentación | `POST`, `DELETE` `/api/tiendas/{idTienda}/presentaciones/{idPresentacion}/imagenes/{idImagen}`. `GET /presentaciones/{idPresentacion}/imagenes`. `GET /imagenes/{idImagen}/presentaciones` | `PresentacionImagenController` |
| Colecciones | `POST`, `GET` `/api/tiendas/{idTienda}/colecciones`. `GET /activas`. `GET`, `PUT` `/{idColeccion}`. `PATCH /{idColeccion}/estado` | `ColeccionController` |
| Colección ↔ producto | `POST`, `DELETE` `/api/tiendas/{idTienda}/colecciones/{idColeccion}/productos/{idProducto}`. `GET /colecciones/{idColeccion}/productos`. `GET /productos/{idProducto}/colecciones` | `ColeccionProductoController` |

No hay endpoints `DELETE` para producto, categoría, variante ni presentación, lo que es coherente con `ON DELETE RESTRICT`.

### M3 · Apariencia, banners, cuenta de pago e inventario

| Método | Ruta | Autorización | Clase |
| --- | --- | --- | --- |
| POST | `/api/plantillas` | ADMIN | `PlantillaController.crearPlantilla` |
| PATCH | `/api/plantillas/{idPlantilla}` | ADMIN | `PlantillaController.actualizarPlantilla` |
| PATCH | `/api/plantillas/{idPlantilla}/estado` | ADMIN | `PlantillaController.actualizarEstadoPlantilla` |
| GET | `/api/plantillas?incluirInactivas=` | ADMIN o DUENO_TIENDA | `PlantillaController.listarPlantillas` |
| GET | `/api/plantillas/{idPlantilla}` | ADMIN o DUENO_TIENDA | `PlantillaController.obtenerPorId` |
| GET, PATCH | `/api/tiendas/{idTienda}/configuracion` | propietario | `ConfiguracionTiendaController` |
| GET, POST (multipart) | `/api/tiendas/{idTienda}/banners` | propietario | `BannerController` |
| PATCH, DELETE | `/api/tiendas/{idTienda}/banners/{idBanner}` | propietario | `BannerController` |
| PUT (multipart) | `/api/tiendas/{idTienda}/banners/{idBanner}/archivo` | propietario | `BannerController.sustituirImagen` |
| PUT | `/api/tiendas/{idTienda}/banners/orden` | propietario | `BannerController.reordenarBanners` |
| GET | `/api/tiendas/{idTienda}/cuenta-pago` | propietario | `CuentaPagoController.consultar` |
| POST | `/api/tiendas/{idTienda}/cuenta-pago/desconexion` | propietario | `CuentaPagoController.desconectar` |
| POST | `/api/tiendas/{idTienda}/cuenta-pago/mercado-pago/oauth/inicios` | propietario. *Requiere `mercadopago.oauth.enabled`* | `OAuthController.iniciar` |
| GET | `/api/integraciones/mercado-pago/oauth/callback` | pública (autoriza el `state`). *Requiere `mercadopago.oauth.enabled`* | `OAuthController.callback` |
| GET | `/api/tiendas/{idTienda}/inventario?texto=&estadoProducto=&estadoPresentacion=&alertaStock=&page=&size=` | propietario | `InventarioController.consultar` |
| PUT | `/api/tiendas/{idTienda}/inventario/{idPresentacion}` | ADMIN o propietario | `InventarioGestionController.actualizar` (ver la sección 1, punto 2) |

### M4 · Compra, pedidos, impuestos y pagos

| Método | Ruta | Autorización | Clase |
| --- | --- | --- | --- |
| GET | `/api/cliente/ubigeo?texto=` | autenticado | `DireccionesController.ubigeo` |
| GET, POST | `/api/cliente/direcciones` | autenticado (propias) | `DireccionesController` |
| PUT, DELETE | `/api/cliente/direcciones/{id}` | autenticado (propias) | `DireccionesController` |
| GET | `/api/compra/tiendas/{slug}/carrito` | autenticado | `CarritoController.obtener` |
| POST, PUT, DELETE | `/api/compra/tiendas/{slug}/carrito/items/{presentacion}` | autenticado | `CarritoController.agregar/guardar/quitar` |
| GET | `/api/compra/tiendas/{slug}/pedidos` | autenticado (propios) | `PedidosController.misPedidos` |
| GET | `/api/compra/tiendas/{slug}/pedidos/{id}` | autenticado (propio) | `PedidosController.miPedido` |
| GET | `/api/tiendas/{idTienda}/pedidos` | propietario | `PedidosController.pedidosTienda` |
| GET | `/api/tiendas/{idTienda}/pedidos/{id}` | propietario | `PedidosController.pedidoTienda` |
| GET | `/api/admin/impuestos` | ADMIN | `ImpuestosController.listar` |
| POST | `/api/admin/impuestos` | ADMIN | `ImpuestosController.crear` |
| GET | `/api/compra/tiendas/{slug}/checkout` | autenticado. *Requiere `mercadopago.pagos.enabled`* | `PagosController.disponibilidad` |
| POST | `/api/compra/tiendas/{slug}/checkout` | autenticado. *Requiere `mercadopago.pagos.enabled`* | `PagosController.comprar` |
| GET | `/api/compra/tiendas/{slug}/pedidos/{pedido}/pago` | autenticado (propio). *Requiere `mercadopago.pagos.enabled`* | `PagosController.consultar` |
| GET | `/api/tiendas/{idTienda}/pedidos/{pedido}/pago` | propietario. *Requiere `mercadopago.pagos.enabled`* | `PagosController.consultarVendedor` |
| POST | `/api/tiendas/{idTienda}/pedidos/{pedido}/reembolsos` | propietario. *Requiere `mercadopago.pagos.enabled`* | `PagosController.reembolsar` |
| POST | `/api/publico/mercadopago/notificaciones` | pública + firma HMAC, sin CSRF. *Requiere `mercadopago.notificaciones.enabled`* | `NotificacionOrderController.recibir` |

**Tareas programadas (sin endpoint):**
- `AlmacenamientoImagenes.procesarPendientes`: limpieza de storage.
- `CredencialesVendedorService.renovarProximos`: renovación OAuth.
- `PagosProcesador.ejecutar`: creación, consulta, vencimiento y reembolso de órdenes.
