# Estado final del backend para la entrega

> **Fecha:** 2026-11-24 · **Rama:** `feature/miguel-local` (último commit `ecbc2b6`, Bloque 3) · **Tipo:** análisis de solo lectura; no se modificó código.
>
> **Este es el documento de estado vigente.** Reemplaza las listas de pendientes de `cierre-backend.md` y `auditoria-backend.md`, que quedan como historial.
>
> **Fuentes**
> - Código de `backend/src/main` y migraciones V1–V14.
> - `docs/functional/modules-rules.md` y `uh-m3.md`.
> - `docs/revision/auditoria-backend.md`, `cierre-backend.md`, `analisis-frontend.md` y `hallazgos-flujo.md`.
> - `docs/technical/mercadopago-oauth.md`, `pagos-operacion.md` y `docs/despliegue/render-backend.md`.
>
> **Bloque 4 (cierre):** `.env.example` completo, `.env.render.example`, `pagos-operacion.md` y `modulo-4.md` corregidos, y DTOs en `PresentacionImagenController`.
>
> **Bloque 5 (inicio de sesión con Google):** `POST /api/auth/google` y `POST /api/auth/google/vinculacion`; guía para el frontend en [google-login-frontend.md](google-login-frontend.md).
>
> **Suite de tests:** 139 tests (tras el Bloque 5), 0 fallos, 4 omitidos (almacenamiento S3, que requiere `STORAGE_IT`), ejecutados contra `nubea_test` con `OAUTH_PG_TEST`, `CUENTA_PAGO_PG_TEST` e `INVENTARIO_PG_TEST`.

---

## 1. Resumen

**El backend está listo para la entrega en cuanto a código.**
- Todas las reglas de negocio de M2, M3 y M4 se cumplen. En M1 solo queda fuera el cierre de tienda o cuenta (RN-M1-18, no implementado, previsto como backlog); el login con Google (RN-M1-05) se completó en el Bloque 5.
- Los siete problemas críticos de la auditoría están resueltos o mitigados.
- El frontend nuevo tiene los endpoints que pidió para la entrega.

**Lo que impide darlo por cerrado no es código, sino validación y operación:**
1. El cobro con Mercado Pago **nunca se probó de punta a punta en el entorno desplegado**. Solo se vio una vinculación OAuth que llegó a `ACTIVA`, y todas las pruebas automáticas simulan a Mercado Pago. Hace falta la prueba publicada con credenciales de prueba (§4).
2. **Supabase**:
   - **posible exposición del esquema `public` por la Data API**;
   - datos existentes que pueden chocar con las reglas nuevas (dueños sin rol, presentaciones sin inventario);
   - respaldo antes de aplicar la V14 (§6).
3. **Documentación de despliegue desactualizada:**
   - faltan variables en `.env.example`;
   - no existe el `.env.render.example` que cita la guía;
   - `pagos-operacion.md` y `modulo-4.md` describen comportamientos anteriores a los bloques.

Con esos tres puntos resueltos, se puede pasar a `main` y empezar el frontend.

---

## 2. Estado por módulo

Leyenda: **CUMPLE** · **PARCIAL** · **NO IMPLEMENTADA**. Las notas "(Bn)" indican en qué bloque cambió la regla ("Prev. A/B" = trabajos previos A y B).

### Módulo 1 · Usuarios, roles y tiendas

| Regla | Estado | Dónde se cumple |
| --- | --- | --- |
| RN-M1-01 Correo único sin distinguir mayúsculas | CUMPLE | V1 `UNIQUE(email)` + `chk_usuario_email_formato`; `AuthService` normaliza; `RestriccionesBd` traduce el error |
| RN-M1-02 Varios roles sin repetir | CUMPLE | V1 PK `usuario_rol` |
| RN-M1-03 Documento opcional, conjunto y único | CUMPLE | V1 `chk_documento_conjunto`, `uq_usuario_documento`; `AuthService.aplicarDocumento` |
| RN-M1-04 Formato según tipo de documento | CUMPLE | `AuthService.aplicarDocumento` |
| RN-M1-05 Google solo tras acreditar titularidad | CUMPLE (B5) | V1 `uq_usuario_identidad_oidc`, `uq_usuario_identidad_proveedor`; `GoogleIdTokenVerifier` (RS256 con el JWKS de Google, `iss`, `aud`, `exp`, `iat`, `email_verified`); `AuthService.autenticarConGoogle`/`vincularGoogle`. Sin vinculación por correo: si el correo ya existe → 409 |
| RN-M1-06 LOCAL con contraseña cifrada | CUMPLE | V1 `chk_identidad_modalidad`; Argon2 en `SecurityConfig` |
| RN-M1-07 Rol CLIENTE al registrarse | CUMPLE | `AuthService.registrar` |
| RN-M1-08 Rol DUENO_TIENDA con la primera tienda | CUMPLE | `TiendaService.crearTienda` |
| RN-M1-09 Solo un admin asigna ADMIN_GENERAL | CUMPLE | `UsuarioController` (`hasRole`), `InicializacionAdministrador` |
| RN-M1-10 Retirar DUENO_TIENDA suspende y bloquea | CUMPLE (Prev. A) | `UsuarioService.retirarDueno` + `TiendaAuthorizationService.puedeEditar` |
| RN-M1-11 Un propietario, varias tiendas | CUMPLE | V1 FK `tienda.id_propietario` |
| RN-M1-12 Slug de tienda único y con formato | CUMPLE | V1 `tienda_slug_key`, `chk_tienda_slug`; `SlugUtil` + `TiendaService` |
| RN-M1-13 El propietario administra; el admin ve, suspende y reactiva | CUMPLE (B2) | `puedeVer`/`puedeEditar`; `PATCH /api/admin/tiendas/{id}/estado` → `TiendaService.cambiarEstadoAdministracion`. Menor: `CrearTiendaRequest.idPropietario` sigue en el contrato |
| RN-M1-14 Cliente en varias tiendas sin duplicar | CUMPLE | V1 PK `tienda_cliente` |
| RN-M1-15 `tienda_cliente` al primer "agregar al carrito" | CUMPLE | `CarritoController.guardarCantidad` |
| RN-M1-16 El cliente ve sus pedidos por tienda; id desde la sesión | CUMPLE (B2) | `PedidosController` (con la tienda en cualquier estado, B2) |
| RN-M1-17 Cierre con fecha; el histórico no se borra | CUMPLE | V1 `chk_cierre_usuario`, `chk_tienda_fecha_cierre`, FK `RESTRICT` |
| RN-M1-18 No cerrar con pedidos pendientes | **NO IMPLEMENTADA** | No existe cierre de tienda ni de cuenta (backlog) |

### Módulo 2 · Catálogo

| Regla | Estado | Dónde se cumple |
| --- | --- | --- |
| RN-M2-01 Categoría, variante y colección no cambian de tienda | CUMPLE | V1 `trg_*_no_mover` |
| RN-M2-02 Producto en categoría de su tienda | CUMPLE | V1 `fk_producto_categoria_tienda`, `trg_producto_no_mover` |
| RN-M2-03 Slug de categoría y colección único por tienda | CUMPLE (Prev. B) | V1 `UNIQUE(id_tienda, slug)`; generación y validación en `CategoriaService`/`ColeccionService` |
| RN-M2-04 Variantes y valores sin repetir | CUMPLE (Prev. B) | V1 `uq_variante_nombre_ci`, `uq_valor_variante_ci`; límite de 80 alineado |
| RN-M2-05 Un valor no cambia de variante | CUMPLE | V1 `trg_valor_no_mover` |
| RN-M2-06 Presentación ligada a su producto y tienda | CUMPLE | V1 `fk_presentacion_producto_tienda`, `trg_presentacion_no_mover` |
| RN-M2-07 SKU único por tienda | CUMPLE (Prev. B) | V1 `uq_sku_tienda`; SKU opcional y generado en `PresentacionProductoService` |
| RN-M2-08 Precio efectivo, nunca negativo | CUMPLE | V1 CHECK de precios; `PresentacionProductoService.respuesta` |
| RN-M2-09 Un valor por variante, solo de su tienda | CUMPLE | V1 `uq_pv_una_por_variante`, `trg_presentacion_valor_validar` |
| RN-M2-10 Combinación única por producto | CUMPLE | V1 `uq_presentacion_combinacion` + firma de valores |
| RN-M2-11 Sin opciones = una presentación | CUMPLE | Firma vacía + `uq_presentacion_combinacion` |
| RN-M2-12 Mismo conjunto de variantes (máximo 3) | CUMPLE | `PresentacionProductoService.validarConjuntoVariantes` |
| RN-M2-13 Publicar con una versión ACTIVA con stock > 0 | CUMPLE (B2, B3) | `ProductoService.requisitosPublicacion` / `validarPublicable` (422 con `campos.estado`); `GET …/productos/{p}/requisitos` (B3). Pendiente de mejora: no se despublica sola |
| RN-M2-14 Imágenes fuera de PostgreSQL | CUMPLE | `AlmacenamientoImagenes`, V5 `storage_tarea` |
| RN-M2-15 Una imagen principal | CUMPLE | V1 `uq_imagen_principal_producto` |
| RN-M2-16 Imagen solo de su producto | CUMPLE | V1 `trg_presentacion_imagen_producto` |
| RN-M2-17 Colecciones de la misma tienda | CUMPLE | V1 `trg_coleccion_producto_tienda` |
| RN-M2-18 Solo el propietario modifica el catálogo | CUMPLE (B2) | `puedeEditar` en todas las escrituras; `ImagenProductoService.bloquearProducto` |

### Módulo 3 · Apariencia, banners, cuenta de pago e inventario

| Regla / HU | Estado | Dónde se cumple |
| --- | --- | --- |
| RN-M3-01 Una configuración y una cuenta por tienda | CUMPLE | V1 PK/FK `id_tienda` |
| RN-M3-02 Configuración creada con la tienda | CUMPLE | `TiendaService.crearTienda` |
| RN-M3-03 Solo plantillas activas | CUMPLE | `ConfiguracionTiendaServiceImpl` (`PlantillaInactivaException`) |
| RN-M3-04 Solo el admin gestiona plantillas | CUMPLE | `PlantillaController` |
| RN-M3-05 Orden de banners | CUMPLE | V1 `uq_banner_tienda_orden` (diferible); `BannerServiceImpl.reordenarBanners` |
| RN-M3-06 Banners en S3; enlace HTTPS | CUMPLE | `BannerServiceImpl`, `S3StorageAdapter` |
| RN-M3-07 Solo Mercado Pago; tokens cifrados | CUMPLE | `AesGcmCredentialVault`; `CuentaPagoResponse` sin credenciales |
| RN-M3-08 ACTIVA exige cuenta, credencial y fecha | CUMPLE | V1 `chk_cuenta_pago_activa` |
| RN-M3-09 Solo un OAuth validado habilita cobros | CUMPLE | `OAuthService` + `OAuthTransactionStore` |
| RN-M3-10 Inventario creado con la presentación | CUMPLE | `PresentacionProductoService.crearPresentacion` (con `stockInicial` opcional, Prev. B) |
| RN-M3-11 Stock nunca negativo | CUMPLE | V1 `chk_inventario_*`; `AjusteInventarioService` |
| RN-M3-12 Reserva solo al confirmar el checkout | CUMPLE | `CheckoutService.reservar` |
| RN-M3-13 Última unidad: un solo comprador | CUMPLE | Bloqueo de inventario en orden de id en `CheckoutService` |
| RN-M3-14 Disponibles, apartadas, ajustes y conteo | CUMPLE (B2) | `AjusteInventarioService` (`FOR UPDATE`, `stockEsperado`); `stockReservado` en el GET |
| HU-M3-01 Crear plantilla | CUMPLE | `PlantillaServiceImpl.crearPlantilla` |
| HU-M3-02 Ciclo de vida de plantilla | CUMPLE | `PlantillaServiceImpl` |
| HU-M3-03 Consultar plantillas | **PARCIAL** | Un CLIENTE sin tienda no puede listarlas (`hasAnyRole('ADMIN_GENERAL','DUENO_TIENDA')`). No afecta al flujo: la plantilla se elige después de crear la tienda |
| HU-M3-04 Configurar apariencia | **PARCIAL** | No se rechazan campos desconocidos en el PATCH (criterio 3). El resto se cumple |
| HU-M3-05 Banners | CUMPLE | `BannerServiceImpl` |
| HU-M3-06 Consultar y gestionar stock | CUMPLE (B2) | HU actualizada; `InventarioController`, `InventarioGestionController`, `AjusteInventarioService` |
| HU-M3-07 Estado de la cuenta de pago | CUMPLE (B3) | `CuentaPagoConsultaService` (cuenta enmascarada y `ambientePago`) |
| HU-M3-08 Iniciar OAuth | CUMPLE | `OAuthService.iniciar` |
| HU-M3-09 Callback OAuth | CUMPLE | `OAuthService.callback` |
| HU-M3-10 Desconectar | **PARCIAL** | Desconecta y borra la credencial local; no revoca el token en Mercado Pago (el vendedor puede revocarlo en su cuenta) |

### Módulo 4 · Direcciones, carrito, pedidos, impuestos y pagos

| Regla | Estado | Dónde se cumple |
| --- | --- | --- |
| RN-M4-01 Dirección con Google Places o texto; una principal | CUMPLE (Prev. B) | V1 `uq_direccion_principal_usuario`, V12; `DireccionesController` |
| RN-M4-02 Un carrito ACTIVO por usuario y tienda | CUMPLE | V1 `uq_carrito_activo_usuario_tienda` |
| RN-M4-03 Ítems de su tienda; cantidad > 0 | CUMPLE | V1 `trg_carrito_item_tienda`, CHECK |
| RN-M4-04 Solo versiones activas de productos publicados | CUMPLE | `CarritoController` |
| RN-M4-05 Checkout transaccional y **parcial** | CUMPLE (B3) | `CheckoutService.reservar` (las líneas no disponibles pasan a un carrito nuevo) |
| RN-M4-06 Carrito de origen del mismo contexto | CUMPLE | V1 `fk_pedido_carrito_origen_contexto` |
| RN-M4-07 Importes congelados | CUMPLE | V1 `trg_pedido_transicion` |
| RN-M4-08 Snapshot de dirección y precios | CUMPLE | Columnas snapshot (V1, V12) |
| RN-M4-09 Totales coherentes | CUMPLE (B3) | V1 CHECK; `CalculoImportes` compartido por carrito y checkout |
| RN-M4-10 Precios con IGV; tasa vigente | CUMPLE (B3) | `CalculoImportes.impuestoVigente`; desglose en el carrito |
| RN-M4-11 Tasa del pedido = vigencia | CUMPLE | V1 `fk_pedido_impuesto_tasa` |
| RN-M4-12 Tasa global y vigencias sin solapar | CUMPLE | V1 `excl_impuesto_vigencia`; `ImpuestosController` |
| RN-M4-13 Estados del pedido | CUMPLE | V1 `trg_pedido_transicion` |
| RN-M4-14 PAGADO solo con pago aprobado | CUMPLE | V1 `trg_pedido_transicion`; `PagosStore.aplicar` |
| RN-M4-15 Monto = total | CUMPLE | V1 `trg_pago_validar` |
| RN-M4-16 Idempotencia y un solo aprobado | CUMPLE | V1 índices únicos; V6/V7 (una fila `pago` por pedido; reintentos en `intento_order_mercadopago`) |
| RN-M4-17 Webhook verificado y procesado una vez | CUMPLE | `NotificacionOrderController` (firma), `PagosProcesador`, `PagosStore.aplicar` |
| RN-M4-18 Mercado Pago fuera de transacciones | CUMPLE | `PagosProcesador` + transacciones cortas en `PagosStore` |
| RN-M4-19 Vencimiento siempre, coordinado con Mercado Pago | CUMPLE (B3) | `VencimientoReservasService` (siempre activo; gracia de 40 min para creaciones inciertas; respeta los trabajos del procesador). Regla actualizada |
| RN-M4-20 Stock devuelto una vez; al vencer, los productos vuelven al carrito | CUMPLE (B3) | V1 `trg_pedido_transicion`; `ReservasPedido.cancelar` |
| RN-M4-21 Aprobado tras cancelar → reembolso | CUMPLE | `PagosStore.aplicar` → `marcarReembolso` → `PagosProcesador` |
| RN-M4-22 Cobrar solo con la cuenta propia ACTIVA | CUMPLE | `CheckoutService`, `PagosStore` |
| RN-M4-23 Carrito y pedido no se reasignan | CUMPLE | V1 `trg_carrito_no_reasignar`, `trg_pedido_no_reasignar` |
| RN-M4-24 Número corto de pedido por tienda | CUMPLE (B3) | V14 `trg_pedido_numero`, `uq_pedido_numero_tienda`, `trg_pedido_numero_fijo` |

**Total:** 74 reglas y 10 HU. Las reglas quedan en 73 CUMPLE y 1 NO IMPLEMENTADA (RN-M1-18). De las HU, 3 son PARCIALES por detalles que no afectan al flujo (HU-M3-03, HU-M3-04 y HU-M3-10).

---

## 3. Flujo de compra de punta a punta

| # | Paso | Endpoint(s) | Estado |
| --- | --- | --- | --- |
| 1 | El vendedor se registra y crea su tienda (BORRADOR; recibe DUENO_TIENDA) | `POST /api/auth/registro`, `POST /api/tiendas` | ✅ |
| 2 | Crea una categoría (slug automático) | `POST …/categorias` | ✅ |
| 3 | Crea un producto y su versión con stock inicial (SKU automático) | `POST …/productos`, `POST …/presentaciones` con `stockInicial` | ✅ Para un producto sin opciones, el frontend lo resuelve con dos llamadas |
| 4 | Activa la tienda | `POST /api/tiendas/{id}/activacion` | ✅ |
| 5 | Revisa los requisitos y publica (exige stock > 0) | `GET …/requisitos`, `PATCH …/estado?estado=PUBLICADO` | ✅ |
| 6 | Conecta Mercado Pago | `POST …/oauth/inicios` → callback | ⚙️ Requiere configuración OAuth (§4). Probado una vez en el entorno publicado |
| 7 | El admin habilita los cobros de esa tienda | Variable `MERCADOPAGO_PAGOS_TIENDAS` | ⚙️ **Es configuración, no hay pantalla:** cada tienda nueva se añade a mano a la lista y requiere reiniciar el servicio |
| 8 | El comprador ve la vitrina y la disponibilidad | `GET /api/publico/tiendas/{slug}` | ✅ |
| 9 | Agrega al carrito, crea su dirección y ve el desglose | `…/carrito`, `/api/cliente/direcciones` | ✅ |
| 10 | Checkout (parcial si hace falta) → redirección a Mercado Pago | `POST /api/compra/tiendas/{slug}/checkout` | ⚙️ **Sin `MERCADOPAGO_PAGOS_ENABLED=true` este endpoint no existe** y el carrito muestra "Los pagos aún no están disponibles". Recibe número #1001… |
| 11a | Paga → webhook y procesador → PAGADO | Notificaciones + `PagosProcesador` | ⚠️ Implementado y probado solo con Mercado Pago simulado. **Falta la prueba publicada** |
| 11b | No paga → vence → cancelado, stock liberado y productos de vuelta al carrito | `VencimientoReservasService` | ✅ Funciona con o sin pagos habilitados |
| 12 | El vendedor ve el pedido (#número) y el pago | `GET /api/tiendas/{id}/pedidos…`, `…/pago` | ✅ |
| 13 | Devolución completa, con reposición de stock opcional | `POST …/reembolsos` | ⚠️ Implementado; **falta la prueba publicada** |
| 14 | Entrega o envío | — | ❌ No existe. Mientras no haya envíos, un pedido PAGADO se considera entregado (RN-M3-14) |

**Conclusión:**
- **Ningún paso falla en código.**
- **Requieren configuración:** el cobro (pasos 6, 7 y 10).
- **Requieren validación real:** el pago y la devolución (11a y 13).
- **Falta por diseño:** el envío (14).

**Riesgos operativos del flujo:**
- **Instancia de Render dormida.** Si Render suspende la instancia (plan gratuito), el procesador de pagos y el vencimiento de reservas se detienen hasta que llegue una petición. Las tareas se recuperan al despertar, pero las reservas vencidas retienen stock mientras tanto.
- **Comprador y vendedor iguales.** El dueño puede comprar en su tienda (decisión del equipo), pero Mercado Pago no permite pagarse a uno mismo: en las pruebas hay que usar un comprador distinto del vendedor.

---

## 4. Mercado Pago: qué configurar para probar en el entorno desplegado

> Solo configuración: no hace falta programar nada. Los pasos siguen `docs/technical/pagos-operacion.md`, que debe actualizarse (§5).

### 4.1 En el panel de desarrolladores de Mercado Pago (Perú)

1. **Aplicación de Nubea** (la que ya se usó para OAuth):
   - **PKCE** activado y permisos de **lectura, escritura y acceso sin conexión**;
   - **Redirect URL** exacta: `https://nubea.onrender.com/api/integraciones/mercado-pago/oauth/callback`.
2. **Notificaciones (Webhooks) de Orders:**
   - URL: `https://nubea.onrender.com/api/publico/mercadopago/notificaciones`;
   - evento: **Order** (`type=order`);
   - copiar la **clave secreta de la firma** a `MERCADOPAGO_NOTIFICACIONES_SECRET`.
3. **Cuentas de prueba:**
   - **un vendedor** de prueba, que conecta su cuenta a la tienda de prueba mediante OAuth;
   - **un comprador** de prueba, distinto del vendedor, que paga con las tarjetas de prueba de Perú.

   Las credenciales de la aplicación no sustituyen a estas cuentas. El backend verifica la cuenta con `/users/me` (país MPE y etiqueta de usuario de prueba).
4. **URLs de retorno al comprador:** no se registran en Mercado Pago. El backend las genera en cada orden como `MERCADOPAGO_PAGOS_STORE_ORIGIN` + `/tienda/{slug}/pedidos/{id}`, una ruta que el store ya tiene.

### 4.2 Variables en Render (backend)

| Variable | Valor para la prueba | Propósito |
| --- | --- | --- |
| `MERCADOPAGO_OAUTH_ENABLED` | `true` | Habilita la vinculación OAuth |
| `MERCADOPAGO_OAUTH_CLIENT_ID` | id de la aplicación | Credencial OAuth de Nubea |
| `MERCADOPAGO_OAUTH_CLIENT_SECRET` | secreto de la aplicación | Credencial OAuth de Nubea (solo en Render) |
| `MERCADOPAGO_OAUTH_AUTHORIZATION_URL` | `https://auth.mercadopago.com.pe/authorization` | Pantalla de autorización |
| `MERCADOPAGO_OAUTH_TOKEN_URL` | `https://api.mercadopago.com/oauth/token` | Canje del código y renovación |
| `MERCADOPAGO_OAUTH_CALLBACK_URL` | `https://nubea.onrender.com/api/integraciones/mercado-pago/oauth/callback` | Debe coincidir exactamente con la registrada |
| `MERCADOPAGO_OAUTH_RETORNO_PANEL` | `https://nubea.vercel.app/plataforma/panel/retorno-pago` | Adonde vuelve el vendedor tras autorizar |
| `MERCADOPAGO_OAUTH_EXPIRY_MINUTES` | `10` | Vigencia del intento de vinculación |
| `MERCADOPAGO_OAUTH_ACTIVE_KEY` | `k1` | Identificador de la clave de cifrado activa |
| `MERCADOPAGO_OAUTH_KEYS_K1` | `openssl rand -base64 32` (una sola vez) | Cifra los tokens de los vendedores. **Si se pierde, hay que volver a vincular todas las tiendas.** Guardarla en un gestor de secretos |
| `MERCADOPAGO_PAGOS_ENABLED` | `true` | Crea la API de checkout, el procesador de pagos y la conciliación |
| `MERCADOPAGO_PAGOS_CHECKOUT_ENABLED` | `true` durante la prueba | Permite compras nuevas (apagarlo conserva la conciliación y los reembolsos) |
| `MERCADOPAGO_PAGOS_TIENDAS` | `<UUID_TIENDA_PRUEBA>:PRUEBA` | Lista blanca de tiendas que pueden cobrar y su ambiente (`PRUEBA`/`PRODUCCION`), separadas por comas |
| `MERCADOPAGO_PAGOS_STORE_ORIGIN` | `https://nubea.vercel.app` | Origen HTTPS para las URLs de retorno del comprador |
| `MERCADOPAGO_PAGOS_DELAY_MS` | `5000` (por defecto) | Frecuencia del procesador de pagos |
| `MERCADOPAGO_NOTIFICACIONES_ENABLED` | `true` | Habilita el endpoint de webhooks |
| `MERCADOPAGO_NOTIFICACIONES_SECRET` | clave secreta del webhook | Verificación de `x-signature`. Sin ella el backend no arranca si las notificaciones están habilitadas |
| `MERCADOPAGO_OAUTH_RENEWAL_DELAY_MS` | `60000` (por defecto) | Frecuencia de renovación de tokens |
| `RESERVAS_VENCIMIENTO_DELAY_MS` | `60000` (por defecto) — **nueva, B3** | Frecuencia del vencimiento de reservas |
| `RESERVAS_VENCIMIENTO_GRACIA_MINUTOS` | `40` (por defecto) — **nueva, B3** | Gracia antes de cancelar una creación de orden incierta (debe ser mayor que los 30 min de expiración de la orden) |

**En Vercel (frontends):**
- `VITE_MERCADOPAGO_OAUTH_ENABLED=true` en el cms, para mostrar "Conectar Mercado Pago";
- `VITE_API_URL`;
- `VITE_GOOGLE_MAPS_API_KEY`.

**Nunca** poner secretos en variables `VITE_*`.

### 4.3 Secuencia mínima de prueba

1. Crear una tienda de prueba y conectar el vendedor de prueba.
2. Añadir el UUID de la tienda a `MERCADOPAGO_PAGOS_TIENDAS` como `PRUEBA` y reiniciar el servicio.
3. Publicar un producto con stock.
4. Con el comprador de prueba, comprar y pagar con una tarjeta aprobada. Comprobar `PAGADO` en las dos apps y el número de pedido.
5. Repetir con una tarjeta rechazada y con una compra abandonada: debe vencer y los productos deben volver al carrito.
6. Reembolsar desde el panel y verificar la devolución en Mercado Pago.
7. Comprobar en `notificacion_order_mercadopago` que llegan los webhooks y que la firma es válida.

---

## 5. Pendientes

### Imprescindible antes de pasar a `main`

| # | Tarea | Tipo |
| --- | --- | --- |
| 1 | **Prueba publicada de Mercado Pago con cuentas de prueba** (§4.3): pago aprobado, rechazado, abandono o vencimiento y reembolso | Validación |
| 2 | **Supabase: Data API.** Quitar `public` de los esquemas expuestos o activar RLS en todas las tablas. Hoy, con la clave `anon`, podrían leerse `usuario_identidad` (hashes de contraseña), `SPRING_SESSION`, `cuenta_pago_tienda`, etc. | Configuración |
| 3 | **Revisar los datos de Supabase** con las consultas de §6 y corregir dueños sin rol, presentaciones sin inventario y productos publicados sin stock | SQL |
| 4 | **Respaldo de Supabase** antes de desplegar la V14 (§6) | Operación |
| 5 | ~~`.env.example` y `.env.render.example`~~ ✅ **Bloque 4** (`.env.render.example` ya se versiona: `!.env.render.example` en `.gitignore`) | Documentación |
| 6 | ~~Actualizar la documentación técnica desactualizada~~ ✅ **Bloque 4** (`pagos-operacion.md`, `modulo-4.md`). También corregido "30 tablas" en `modules-rules.md` (39: 30 del modelo + 7 de V2–V9 + 2 de sesión) | Documentación |
|   | `pagos-operacion.md`: dice "Flyway llegue a V11" (ahora V14) y "una creación incierta… no liberar stock" (ahora se libera pasada la gracia, RN-M4-19) | |
|   | `modulo-4.md`: dice que el autocompletado de Places no está habilitado y que el comprador debe confirmar el UBIGEO (contradice RN-M4-01, opción A) | |
|   | `modules-rules.md`: "30 tablas" (ahora 39) | |
| 7 | **Decidir el plan de Render:** una instancia que se duerme detiene el vencimiento de reservas y los pagos | Decisión de operación |

### Backlog (después de la entrega)

| Tarea | Origen |
| --- | --- |
| 🔺 **PRIORIDAD ALTA — Verificación de correo** (código de 6 dígitos al registrarse) | **Riesgo concreto desde el Bloque 5:** el registro con contraseña no verifica el correo, así que alguien puede registrar el correo de otra persona y **bloquearle el alta con Google** (recibe el 409 "Ya tienes una cuenta con este correo…" y no puede entrar con Google ni recuperar esa cuenta). No permite suplantarla, pero sí bloquearla. Además: riesgo con `APP_BOOTSTRAP_ADMIN_EMAIL` (hallazgos §7). Requiere un servicio de envío de correos |
| ~~Login con Google (RN-M1-05)~~ | ✅ Bloque 5. Pendiente en el frontend (rediseño) según `google-login-frontend.md`. Backlog: añadir contraseña a cuentas creadas con Google y desvincular Google |
| "Continuar como…" estilo Shop / reconocimiento entre tiendas | Hallazgos §6–7 |
| Mi perfil (editar datos y cambiar contraseña) | Análisis del frontend, E10 |
| Configuración de la tienda (renombrar, dirección web) | E11 |
| Cerrar tienda o cuenta (RN-M1-18) | E12 |
| Envíos y estado de entrega (con UBIGEO obligatorio) | E14, trabajo futuro de `modules-rules.md` |
| Pantalla de admin para habilitar cobros por tienda (hoy se hace con una variable de entorno) | Flujo §3, paso 7 |
| Filtros y paginación de pedidos y productos; admin con dueño legible | E6–E8 |
| Historial de la cuenta de pago; revocar el token en Mercado Pago al desconectar (HU-M3-10) | E13 |
| Despublicar automáticamente al perder requisitos (RN-M2-13) | Mitigado por los filtros de vitrina, carrito y checkout |
| `DELETE` de catálogo (hoy solo se desactiva) | Análisis del frontend |
| **Limpieza de contrato:** ~~DTOs en `PresentacionImagenController`~~ (✅ Bloque 4); quitar `CrearTiendaRequest.idPropietario`; rechazar campos desconocidos (HU-M3-04); 404 en lugar de 400 para "no existe"; borrar `ConfirmacionPagoService` (código muerto); que el CLIENTE pueda ver plantillas (HU-M3-03) | Auditoría §8 |
| Mensaje con jerga en el carrito: "La presentación no está disponible en esta tienda" (`CarritoController`) | Lenguaje de usuario |

---

## 6. Checklist de paso a producción

> Las consultas son de solo lectura y se pueden ejecutar en el SQL Editor de Supabase. No se consultó Supabase para este informe.

**Antes del despliegue**
- [ ] **Respaldo:** `pg_dump` completo (esquema y datos) con el usuario de migración, guardado fuera del repositorio. Comprobar además qué respaldos automáticos o PITR incluye el plan de Supabase.
- [ ] **Migraciones aplicadas:** `SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;` → deben ser V1–V13 con éxito y sin V14. **No editar ningún archivo V1–V13** (el checksum haría fallar el arranque).
- [ ] **Data API de Supabase:** `SELECT relname FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND NOT relrowsecurity;` → quitar `public` de *Exposed schemas* o activar RLS.
- [ ] **Dueños sin rol** (desde el trabajo previo A reciben 403 al escribir): `SELECT t.id_tienda, t.nombre, u.email FROM tienda t JOIN usuario_plataforma u ON u.id_usuario=t.id_propietario WHERE t.estado IN ('ACTIVA','BORRADOR') AND NOT EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id_rol=ur.id_rol WHERE ur.id_usuario=t.id_propietario AND r.nombre='DUENO_TIENDA');`
- [ ] **Presentaciones sin inventario:** `SELECT p.id_presentacion, p.sku FROM presentacion_producto p LEFT JOIN inventario i USING (id_presentacion) WHERE i.id_presentacion IS NULL;`
- [ ] **Productos publicados sin una versión activa con stock** (siguen visibles como "Agotado"; despublicarlos si hace falta): `SELECT p.id_producto, p.nombre FROM producto p WHERE p.estado='PUBLICADO' AND NOT EXISTS (SELECT 1 FROM presentacion_producto pp JOIN inventario i USING (id_presentacion) WHERE pp.id_producto=p.id_producto AND pp.estado='ACTIVA' AND i.stock_actual>0);`
- [ ] **Pedidos pendientes vencidos** (el vencimiento nuevo los cancelará al arrancar y devolverá los productos al carrito): `SELECT id_pedido, fecha_expiracion_reserva FROM pedido WHERE estado='PENDIENTE_PAGO' AND fecha_expiracion_reserva < now();`
- [ ] **Datos de referencia:** plantilla 1 activa (`SELECT activa FROM plantilla WHERE id_plantilla=1;`) y una vigencia de IGV (`SELECT * FROM configuracion_impuesto WHERE fecha_inicio<=CURRENT_DATE AND (fecha_fin IS NULL OR fecha_fin>CURRENT_DATE);`).
- [ ] **Nombres de restricciones** (para los mensajes por campo): comparar `SELECT conname FROM pg_constraint WHERE connamespace='public'::regnamespace UNION SELECT indexname FROM pg_indexes WHERE schemaname='public';` con `RestriccionesBd`.

**V14 (número de pedido)**
- [ ] **Una sola transacción:** se aplica entera o no se aplica.
- [ ] **Bloqueo breve:** `ADD COLUMN … DEFAULT` no reescribe la tabla. El relleno y el índice único bloquean `pedido` y `tienda` unos instantes, así que conviene desplegar con poco tráfico.
- [ ] **Compatible con un despliegue gradual:** el número lo asigna un trigger, así que una instancia antigua puede seguir creando pedidos mientras arranca la nueva.
- [ ] **Después de desplegar:**
  - `SELECT max(version) FROM flyway_schema_history;` → 14;
  - `SELECT id_tienda, count(*), min(numero), max(numero) FROM pedido GROUP BY id_tienda;` → sin huecos, empezando en 1001;
  - `SELECT id_tienda, ultimo_numero_pedido FROM tienda;` → igual al máximo de cada tienda.
- [ ] Ya se probó en `nubea_test` con pedidos de varias tiendas y estados (numeración por fecha, contador, pedido nuevo y bloqueo de cambios).

**Variables nuevas o pendientes**
- [ ] `RESERVAS_VENCIMIENTO_DELAY_MS` y `RESERVAS_VENCIMIENTO_GRACIA_MINUTOS` (opcionales, con valor por defecto).
- [ ] `GOOGLE_CLIENT_ID` en Render y `VITE_GOOGLE_CLIENT_ID` en Vercel (Bloque 5; el mismo valor, no es secreto). Configurar en Google Cloud los orígenes JavaScript autorizados y, en modo prueba, los usuarios de prueba. Vacío = Google deshabilitado (503).
- [ ] Las de Mercado Pago de §4.2, si se activa el cobro.
- [ ] Revisar `APP_BOOTSTRAP_ADMIN_EMAIL` (dejarla vacía hasta tener verificación de correo), `SESSION_COOKIE_*` y `APP_CORS_ALLOWED_ORIGINS`.

**Después del despliegue**
- [ ] Iniciar sesión desde Vercel (sesión y CSRF).
- [ ] Crear una tienda y un producto de prueba.
- [ ] Comprobar en los logs que no hay errores del vencimiento ni del procesador.
- [ ] Ejecutar la prueba de §4.3 si se activa el cobro.
- [ ] Ejecutar una vez `StorageIntegracionTest` (`STORAGE_IT=true`) contra un bucket de staging de Supabase Storage. Hasta ahora siempre se ha omitido.
