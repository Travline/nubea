# Cierre del backend antes del frontend

> **Fecha:** 2026-11-24 · **Rama:** `feature/miguel-local` · **Tipo:** análisis de solo lectura. No se modificó código ni se consultó la base de Supabase.
>
> **Estado del código analizado**
> - **Trabajo previo A: tiendas suspendidas** (`cac1dbe`). Tiendas suspendidas en solo lectura y `puedeVer`/`puedeEditar`.
> - **Trabajo previo B: slugs, errores por campo y SKU/stock opcionales** (`7bd52d3`). Incluye también el cambio de RN-M4-01 a Google Places.
> - **Bloque 2** (sin commit todavía): política del admin (ve, suspende y reactiva; no edita contenido ni ve pedidos o pagos), inventario seguro (ajustes +/− y fijar por conteo menos apartadas), tienda no disponible en vitrina, carrito y checkout, disponibilidad en la vitrina y RN-M2-13 con stock > 0.
> - **Suite de tests:** 112 tests, 0 fallos, 4 omitidos (almacenamiento S3), ejecutados contra `nubea_test`.
>
> **Actualización tras el Bloque 2:** las filas marcadas con ✅ **Bloque 2** se resolvieron en ese bloque. El resto del informe conserva el análisis original.
>
> **Fuentes**
> - `docs/functional/modules-rules.md` (v1.2) y `docs/functional/uh-m3.md`.
> - `docs/revision/auditoria-backend.md`, `analisis-frontend.md` y `hallazgos-flujo.md`.
> - Código de `backend/src/main` y migraciones V1–V13.

## Resumen

- **Reglas:** tras el Bloque 2 quedan **2 reglas PARCIALES** (RN-M1-05 Google, RN-M4-19 vencimiento) y **1 NO IMPLEMENTADA** (RN-M1-18 cierre). RN-M1-13, RN-M2-13 y RN-M2-18 pasaron a CUMPLE.
- **Problemas críticos de la auditoría:** cerrados el 1 (tiendas suspendidas), el **2 (inventario por sobrescritura)** y el **3 (política del admin)**. Sigue abierto:
  - **el 4, el stock retenido** cuando la creación en Mercado Pago queda incierta (lo aborda el Bloque 3).
- **RN-M2-13 ya exige stock > 0** para publicar (Bloque 2).
- **Endpoints que faltan:** el frontend nuevo necesitaba 14. Ya existen E5 (disponibilidad en la vitrina) y E9 (suspender o reactivar). Para la entrega faltan 4: número corto de pedido, desglose de IGV en el carrito, ambiente de pago en la cuenta y requisitos de publicación (Bloque 3).
- **Riesgos de producción:**
  - **Data API de Supabase:** si expone el esquema `public` sin RLS, se podrían leer tablas como `usuario_identidad` y `SPRING_SESSION`.
  - **Dueños sin rol:** los dueños sin `DUENO_TIENDA` en los datos reales quedarán sin permiso de escritura (403) con el trabajo previo A.
  - **Variables y ejemplos:** faltan 7 variables de pagos en `.env.example` y el `.env.render.example` que cita la guía no existe.

---

## 1. Reglas de negocio que siguen PARCIALES o NO IMPLEMENTADAS

### 1.1 Cambios respecto a la auditoría

| Regla | Auditoría | Ahora | Qué cambió |
| --- | --- | --- | --- |
| RN-M1-10 Retirar DUENO_TIENDA suspende y bloquea | CUMPLE con brecha | **CUMPLE** | Trabajo previo A: `puedeEditar` exige el rol y una tienda editable. Verificado a mano (hallazgos §2) y con `TiendaEdicionIntegracionTest` |
| RN-M1-13 Solo el propietario (o admin) administra | PARCIAL | **CUMPLE** ✅ Bloque 2 | Trabajo previo A: estado y rol. Bloque 2: el admin ve, suspende y reactiva, pero no edita contenido ni ve pedidos o pagos; la regla se actualizó. Queda como menor `CrearTiendaRequest.idPropietario` (§2, 8a) |
| RN-M2-03 Slug único por tienda | CUMPLE con nota | **CUMPLE** | Trabajo previo B: se genera automáticamente, se valida el formato y el error es claro |
| RN-M2-04 Variantes sin repetir | CUMPLE con nota | **CUMPLE** | Trabajo previo B: límite de 80 caracteres alineado con la BD |
| RN-M2-07 SKU único | CUMPLE con nota | **CUMPLE** | Trabajo previo B: la violación de BD se traduce a un mensaje con campo; el SKU puede generarse |
| RN-M2-13 Requisitos para publicar | PARCIAL | **CUMPLE** ✅ Bloque 2 | Exige una versión ACTIVA con `stock_actual > 0`; mensajes por requisito (422 con `campos.estado`). Queda como mejora la despublicación automática (§2, punto 7) |
| RN-M2-18 Solo el propietario modifica el catálogo | PARCIAL | **CUMPLE** ✅ Bloque 2 | Las escrituras de catálogo, inventario e imágenes quedan solo con `puedeEditar` (sin bypass del admin) |
| RN-M4-01 Dirección | CONTRADICE | **CUMPLE** | La regla se actualizó a Google Places, con `codigo_ubigeo` opcional y validado si llega |

### 1.2 Lista de reglas e HU abiertas

| Regla / HU | Estado | Qué falta |
| --- | --- | --- |
| **RN-M1-05** Login con Google | PARCIAL | Solo existe la parte de BD. El cliente OAuth ya está creado en Google Cloud (hallazgos §7). Backlog |
| ~~RN-M1-13~~ Política de administración | ✅ Bloque 2 | Decidido y aplicado. Solo queda quitar `idPropietario` de `CrearTiendaRequest` (menor) |
| **RN-M1-18** Cierre de tienda o cuenta con verificación de pedidos | **NO IMPLEMENTADA** | No hay servicio ni endpoint de cierre |
| ~~RN-M2-13~~ Publicar | ✅ Bloque 2 | (1) y (3) resueltos: stock > 0 y mensajes sin jerga. (2) La publicación no se revoca al desactivar la categoría o la presentación, ni al suspender la tienda; sigue mitigado por los filtros de vitrina, carrito y checkout |
| ~~RN-M2-18~~ Solo el propietario | ✅ Bloque 2 | Resuelto |
| **RN-M4-19** Vencimiento con Mercado Pago | PARCIAL | Un pedido en `CREACION_INCIERTA` vencido retiene la reserva para siempre (ver §2) |
| **HU-M3-03** Consultar plantillas | PARCIAL | `hasAnyRole('ADMIN_GENERAL','DUENO_TIENDA')`: un CLIENTE no puede verlas antes de crear su primera tienda. No es grave si la plantilla se elige después de crear la tienda |
| **HU-M3-04** Configuración | PARCIAL | No se rechazan campos desconocidos en el PATCH (no hay `FAIL_ON_UNKNOWN_PROPERTIES`) |
| ~~HU-M3-06~~ Consultar y gestionar stock | ✅ Bloque 2 | Decisión del equipo: M3 incluye la gestión de stock (ajustes +/− y fijar por conteo, RN-M3-14). La HU se actualizó; ya no sobrescribe sin bloqueo |
| **HU-M3-10** Desconectar Mercado Pago | PARCIAL | No se intenta revocar el token en Mercado Pago después del commit |

**Documentación desactualizada** (no es código, pero confunde):
- ~~`modules-rules.md` RN-M1-13 cita `esPropietario`~~: actualizada en el Bloque 2.
- Sigue diciendo "30 tablas" y "v1.2 no agrega tablas"; hay 39 (auditoría §2.5).
- `render-backend.md` remite a `.env.render.example`, que no existe en el repositorio.

---

## 2. Problemas críticos de la auditoría

| # | Problema | Estado | Detalle |
| --- | --- | --- | --- |
| 1 | Tiendas suspendidas o cerradas editables | ✅ **RESUELTO** (`cac1dbe` + Bloque 2) | La excepción del admin desapareció en el Bloque 2 |
| 2 | Ajuste de inventario por sobrescritura | ✅ **RESUELTO** (Bloque 2) | `AjusteInventarioService`: `FOR UPDATE`, ajustes +/− (`POST …/ajustes`), fijar = unidades en mano − apartadas, `stockEsperado` (409). Probado el caso del stock inflado (ver nota 1) |
| 3 | Política de ADMIN_GENERAL inconsistente | ✅ **RESUELTO** (Bloque 2) | El admin ve catálogo, apariencia, banners, inventario y estado de la cuenta; suspende y reactiva (a BORRADOR); no edita contenido ni ve pedidos o pagos |
| 4 | **Stock retenido sin salida (`CREACION_INCIERTA`)** | ❌ **ABIERTO** (Bloque 3) | `PagosStore.iniciarCreacion` lanza `CREACION_INCIERTA_VENCIDA` en cada ciclo (ver nota 2) |
| 5 | Cierre de cuenta o tienda (RN-M1-18) | ❌ ABIERTO | No hay riesgo hoy porque no se puede cerrar nada; es una funcionalidad pendiente |
| 6 | Login con Google (RN-M1-05) | ❌ ABIERTO | Backlog. No debe vincular cuentas por coincidencia de correo |
| 7 | Publicación que no se revoca (RN-M2-13) | 🟡 Mitigado | La vitrina, el carrito y el checkout filtran. Desde el Bloque 2 ya no se puede publicar con stock 0 |
| 8a | `CrearTiendaRequest.idPropietario` en el contrato | ❌ ABIERTO | Se valida contra el token, pero sobra |
| 8b | No se rechazan campos desconocidos en los PATCH | ❌ ABIERTO | HU-M3-04, criterio 3 |
| 8c | `PresentacionImagenController` serializa entidades JPA | ❌ ABIERTO | `List<ImagenProducto>` y `List<PresentacionProducto>`: exponen `storage_key`, `firma_valores`, etc. |
| 8d | "No existe" responde 400 en lugar de 404 | 🟡 PARCIAL | Desde el trabajo previo B los mensajes están en lenguaje de usuario, pero el código sigue siendo 400 (`IllegalArgumentException`) |
| 8e | `ConfirmacionPagoService` es código muerto | ❌ ABIERTO | Sigue en `compra/`. Si alguien lo usa, insertaría un segundo `pago` con la misma referencia |

**Nota 1 · Ajuste de inventario (resuelto en el Bloque 2).** Antes pisaba los descuentos simultáneos del checkout y, al cancelarse un pedido pendiente, el trigger inflaba el stock. Ahora:
- todas las modificaciones bloquean la fila (`FOR UPDATE`);
- "fijar" guarda `unidades en mano − apartadas`, y las apartadas son solo las de pedidos pendientes de pago (un pedido PAGADO se considera entregado mientras no exista el módulo de envíos, RN-M3-14);
- `stockEsperado` evita pisar cambios simultáneos.

**Nota 2 · Stock retenido sin salida.** Mientras ocurre:
- la reserva queda `ACTIVA` y el stock retenido;
- no hay alerta ni revisión manual.

Solo puede pasar con `MERCADOPAGO_PAGOS_ENABLED=true`. **Si la entrega incluye cobros reales, es imprescindible arreglarlo.**

**Otros puntos menores de la auditoría** (no bloquean):
- `RegistroUsuarioRequest.correoElectronico` admite hasta 100 caracteres y la BD 254.
- El mensaje del `@Pattern` de `paisEmisorDocumento` está mal redactado.
- `nombre_receptor` del pedido copia solo los nombres, sin apellidos.
- `cuenta_pago_historial` solo registra desconexiones.

**Hallazgos del flujo manual con parte de backend:**

| Hallazgo | Estado |
| --- | --- |
| §3 Slugs autogenerados (categoría y colección; "revisar productos") | ✅ Resuelto. `producto` no tiene slug, así que no aplica |
| §3 Error genérico "Los datos no cumplen las reglas…" | ✅ Resuelto para las 27 restricciones de catálogo, tienda, inventario, banners y usuario. Las restricciones sin traducción mantienen el mensaje genérico |
| §4 Cargar el stock al crear la presentación y SKU sugerido | ✅ Resuelto (`stockInicial`, `stockMinimo` y SKU opcional) |
| §4 Presentación única automática para productos sin variantes (RN-M2-11) | 🟡 No hay endpoint, pero **el frontend puede hacerlo** con dos llamadas: `POST /productos` y después `POST /presentaciones` con `{}` y `stockInicial` |
| §2 Vitrina de una tienda suspendida | ✅ Bloque 2: 404 `TIENDA_NO_DISPONIBLE` en vitrina, carrito y checkout, sin datos del catálogo; el comprador sigue viendo sus pedidos y su pago |
| §5 "Últimas N unidades" en la vitrina | ✅ Bloque 2: `disponibilidad` y `unidadesRestantes` (solo con últimas unidades) |
| §5 Plantillas sin diseño | Decisión de producto, no de backend |
| §6 ¿El dueño puede comprar en su propia tienda? | ✅ Decisión del equipo: sí, no se bloquea |
| §7 Verificación de correo, login con Google | Backlog |

---

## 3. Endpoints que necesita el frontend nuevo y no existen

Se basa en los puntos **[requiere backend]** de `analisis-frontend.md` y en los hallazgos del flujo. Los campos nuevos deben ser opcionales, igual que en los trabajos previos A y B.

| # | Necesidad (pantalla) | Propuesta de contrato | Requiere migración | Prioridad |
| --- | --- | --- | --- | --- |
| E1 | **Número corto de pedido** ("Pedido #1042") en las dos apps | Columna `pedido.numero`, consecutivo **por tienda**, asignado en `CheckoutService.reservar` con bloqueo de la tienda (ya existe). Se expone en `Pedido`, `Detalle` y `EstadoPago`. Opcional: `GET …/pedidos?numero=` | **Sí** (V14 + rellenar los pedidos existentes) | Entrega |
| E2 | **Resumen antes de pagar** (subtotal, IGV, total) | Añadir `subtotal`, `impuesto` y `tasa` a la respuesta de `GET /api/compra/tiendas/{slug}/carrito`, con la tasa vigente y el mismo redondeo que `CheckoutService` | No | Entrega |
| E3 | **Ambiente de cobro** en Tienda › Pagos | Añadir `ambientePago` (`PRUEBA`/`PRODUCCION`, ya existe desde V9) a `CuentaPagoResponse` | No | Entrega |
| E4 | **Requisitos para publicar** (lista en la ficha del producto) | `GET /api/tiendas/{id}/productos/{p}/requisitos` → `{tiendaActiva, categoriaActiva, tieneVersionActiva, tieneStock, publicable}`, con la misma consulta que usa `cambiarEstadoProducto`. Así la regla vive en un solo sitio | No | Entrega (junto con la decisión de §1.2 sobre stock > 0) |
| ~~E5~~ | Nivel de stock en la vitrina | ✅ **Bloque 2:** `disponibilidad` (DISPONIBLE, ULTIMAS_UNIDADES, AGOTADO) y `unidadesRestantes` solo en ULTIMAS_UNIDADES; umbral = stock mínimo o 5 | No | Hecho |
| E6 | Lista de pedidos del vendedor con filtros y paginación | `GET /api/tiendas/{id}/pedidos?estado=&desde=&hasta=&numero=&page=&size=`. Hoy devuelve todos los pedidos sin paginar | No | Puede esperar (crece con el volumen) |
| E7 | Lista de productos con miniatura, precio y filtros | `GET /productos?estado=&idCategoria=&texto=&page=` y añadir `imagenPrincipal`, `precioDesde` y `stockTotal` a la respuesta. Hoy el frontend tendría que hacer una consulta de imágenes por cada producto | No | Puede esperar |
| E8 | Admin › Tiendas con dueño legible | Añadir `nombrePropietario` y `correoPropietario` a `GET /api/admin/tiendas` | No | Puede esperar |
| ~~E9~~ | Admin: suspender o reactivar una tienda | ✅ **Bloque 2:** `PATCH /api/admin/tiendas/{id}/estado` (`SUSPENDIDA` / `ACTIVA` → queda en BORRADOR; exige que el dueño tenga `DUENO_TIENDA`) | No | Hecho |
| E10 | Mi perfil (nombres, apellidos, teléfono, documento) y cambio de contraseña | `GET/PATCH /api/auth/me` y `POST /api/auth/me/contrasena`, reutilizando `AuthService.aplicarDocumento` | No | Puede esperar |
| E11 | Configuración de la tienda (nombre; dirección web avanzada) | `PATCH /api/tiendas/{id}` con `puedeEditar`; el slug opcional con validación y unicidad, como en el trabajo previo B | No | Puede esperar |
| E12 | Cerrar tienda o cuenta (RN-M1-17/18) | `POST /api/tiendas/{id}/cierre` y `POST /api/auth/me/cierre`, con bloqueo de fila y sin pedidos `PENDIENTE_PAGO` ni pagos pendientes | No (las columnas ya existen) | Puede esperar |
| E13 | Historial de la cuenta de pago | `GET /cuenta-pago/historial`. V3 solo registra `DESCONECTADA` | Sí, para registrar activaciones | Puede esperar |
| E14 | Estado de envío del pedido | `estado_envio` + `PATCH …/pedidos/{p}/envio` | Sí | Trabajo futuro |

**También pueden esperar:**
- `DELETE` de opción, valor, categoría o producto (hoy solo se desactivan);
- login con Google;
- verificación de correo;
- revocación del token de Mercado Pago (HU-M3-10).

---

## 4. Riesgos antes de pasar a producción

### 4.1 Supabase: exposición y datos existentes

> No se consultó Supabase. Las consultas siguientes son **de solo lectura** y se pueden ejecutar en el SQL Editor del proyecto.

| Riesgo | Por qué importa | Comprobación |
| --- | --- | --- |
| **Data API sobre `public`** | Supabase publica el esquema `public` por PostgREST. Las tablas de Flyway no tienen RLS: con la clave `anon` se podrían leer `usuario_identidad` (hashes de contraseña), `SPRING_SESSION` (sesiones activas), `cuenta_pago_tienda` (credenciales cifradas), etc. `render-backend.md` §2.5 lo menciona como algo a "revisar", pero no lo resuelve | `SELECT relname FROM pg_class WHERE relnamespace='public'::regnamespace AND relkind='r' AND NOT relrowsecurity;` → hay que quitar `public` de *Exposed schemas* en la configuración de la API, o activar RLS sin políticas en todas las tablas |
| **Dueños sin rol `DUENO_TIENDA`** | Con el trabajo previo A reciben 403 al escribir. Pueden existir si sus tiendas se crearon antes de RN-M1-08 o se cargaron a mano | `SELECT t.id_tienda, t.nombre, u.email FROM tienda t JOIN usuario_plataforma u ON u.id_usuario=t.id_propietario WHERE t.estado IN ('ACTIVA','BORRADOR') AND NOT EXISTS (SELECT 1 FROM usuario_rol ur JOIN rol r ON r.id_rol=ur.id_rol WHERE ur.id_usuario=t.id_propietario AND r.nombre='DUENO_TIENDA');` |
| Presentaciones sin inventario | El vendedor las ve como "sin registro" y no puede guardar stock (`PUT` responde 404) | `SELECT p.id_presentacion, p.sku FROM presentacion_producto p LEFT JOIN inventario i USING (id_presentacion) WHERE i.id_presentacion IS NULL;` |
| Productos publicados que ya no cumplen RN-M2-13 | Aparecen publicados en el panel, pero no en la vitrina | `SELECT p.id_producto, p.nombre FROM producto p JOIN tienda t USING (id_tienda) JOIN categoria c ON c.id_categoria=p.id_categoria WHERE p.estado='PUBLICADO' AND (t.estado<>'ACTIVA' OR NOT c.activa OR NOT EXISTS (SELECT 1 FROM presentacion_producto pp WHERE pp.id_producto=p.id_producto AND pp.estado='ACTIVA'));` |
| Intentos de pago inciertos | Stock retenido (§2, punto 4) | `SELECT i.id_pedido, p.fecha_expiracion_reserva FROM intento_order_mercadopago i JOIN pedido p USING (id_pedido) WHERE i.estado='CREACION_INCIERTA' AND p.fecha_expiracion_reserva < now();` |
| Nombres de restricciones | El trabajo previo B traduce los errores por nombre de restricción. Si la base de Supabase no se creó con Flyway (por ejemplo, con SQL manual), los nombres automáticos pueden diferir y el usuario verá el mensaje genérico (no es un fallo grave) | `SELECT conname FROM pg_constraint WHERE connamespace='public'::regnamespace UNION SELECT indexname FROM pg_indexes WHERE schemaname='public';` → comparar con la tabla de `RestriccionesBd` |
| Datos de referencia | Sin la plantilla 1 activa no se pueden crear tiendas; sin una vigencia de IGV el checkout falla | `SELECT activa FROM plantilla WHERE id_plantilla=1;` y `SELECT * FROM configuracion_impuesto WHERE fecha_inicio<=CURRENT_DATE AND (fecha_fin IS NULL OR fecha_fin>CURRENT_DATE);` |

### 4.2 Migraciones

- **Los trabajos previos A y B y el Bloque 2 no añaden migraciones** y no requieren cambios de esquema.
- Hay que confirmar que Supabase tiene V1–V13 aplicadas y sin fallos: `SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;`.
- **Ningún archivo de V1–V13 debe editarse.** Flyway compara el checksum y el arranque fallaría con `validate`. Los cambios nuevos (E1, E13, E14) van en V14 o posteriores.
- **E1 (número de pedido) es la única migración necesaria para la entrega.** Debe rellenar los pedidos existentes por tienda en orden de `fecha_creacion` y luego aplicar `NOT NULL` y `UNIQUE(id_tienda, numero)`.
- **Pooler de Supabase:**
  - El *Session pooler* (5432) es el correcto, como dice la guía.
  - Con el *Transaction pooler* (6543) fallarían los *prepared statements* y el `pg_advisory_xact_lock` de `InicializacionAdministrador`. Hay un test que fija `prepareThreshold=0` por un motivo parecido.

### 4.3 Variables de entorno

**Los trabajos previos A y B y el Bloque 2 no añaden variables nuevas.** Lo pendiente es de antes:

| Problema | Detalle |
| --- | --- |
| **Faltan en `.env.example`** | `MERCADOPAGO_PAGOS_ENABLED`, `MERCADOPAGO_PAGOS_CHECKOUT_ENABLED`, `MERCADOPAGO_PAGOS_STORE_ORIGIN`, `MERCADOPAGO_PAGOS_TIENDAS`, `MERCADOPAGO_PAGOS_DELAY_MS`, `MERCADOPAGO_NOTIFICACIONES_ENABLED` y `MERCADOPAGO_NOTIFICACIONES_SECRET`. Las usa `application.properties` y hoy solo se descubren leyendo el código |
| **`.env.render.example` no existe** | `render-backend.md` §4 remite a él para los formatos completos |
| `MERCADOPAGO_PAGOS_TIENDAS` | Es una lista blanca: si está vacía o no incluye la tienda, el checkout aparece como no disponible aunque la cuenta esté `ACTIVA` |
| `MERCADOPAGO_PAGOS_STORE_ORIGIN` | Por defecto vale `https://nubea.vercel.app`; hay que ajustarlo si cambia el dominio |
| Claves del vault (`MERCADOPAGO_OAUTH_KEYS_*`, `MERCADOPAGO_OAUTH_ACTIVE_KEY`) | Si se pierden o se cambian sin rotación, las credenciales guardadas dejan de poder descifrarse y hay que volver a vincular todas las tiendas. Hay que guardarlas en un gestor de secretos |
| `VITE_MERCADOPAGO_OAUTH_ENABLED` (cms) | Debe coincidir con `MERCADOPAGO_OAUTH_ENABLED` del backend |
| `VITE_GOOGLE_MAPS_API_KEY` (cms y store) | Debe estar restringida por dominio; sin ella el formulario de dirección funciona solo con texto |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | El registro es público y no verifica el correo: quien registre antes ese correo se convierte en administrador. La guía ya recomienda dejarla vacía y asignar el admin por SQL |
| `SESSION_COOKIE_SAME_SITE` / `SESSION_COOKIE_SECURE` / `APP_CORS_ALLOWED_ORIGINS` | Con el dominio único (`dominio-unico.md`) basta `lax`. `none` solo hace falta si el frontend y la API quedan en dominios distintos |

### 4.4 Otros riesgos

- **Almacenamiento sin probar en la suite:** `StorageIntegracionTest` y `StorageReintentoTest` se omiten por defecto (`STORAGE_IT`). La compatibilidad S3 de Supabase Storage "debe comprobarse" (`render-backend.md`). Conviene ejecutarlos una vez contra un bucket de staging.
- **Registro sin verificar el correo:** permite cuentas con correos ajenos. Es aceptable para una demo; no lo es para cobros reales.
- **Cobros reales:** el checkout y el procesador solo existen con `MERCADOPAGO_PAGOS_ENABLED=true`. Activarlos implica que el riesgo §2.4 (stock retenido) puede ocurrir con dinero real; el del inventario (§2.2) quedó resuelto en el Bloque 2.
- **El dueño puede comprar en su propia tienda** (decisión del equipo). Conviene recordarlo al revisar las métricas de ventas.

---

## 5. Lista final priorizada

### Imprescindible para la entrega

| # | Tarea | Tipo | Esfuerzo |
| --- | --- | --- | --- |
| 1 | ~~Ajuste de inventario seguro~~ | ✅ Bloque 2 | — |
| 2 | ~~Política de ADMIN_GENERAL~~ | ✅ Bloque 2 | — |
| 3 | **Revisar la seguridad de Supabase:** quitar `public` de la Data API o activar RLS en todas las tablas | Configuración | Bajo |
| 4 | **Comprobar los datos de Supabase** con las consultas de §4.1, en especial dueños sin rol y presentaciones sin inventario, y corregirlos | SQL | Bajo |
| 5 | ~~RN-M2-13 con stock > 0~~ | ✅ Bloque 2 | — |
| 6 | **E1: número corto de pedido** (V14 + DTOs) | Migración + código | Medio |
| 7 | **E2: subtotal e IGV en el carrito** | Código | Bajo |
| 8 | **E3: `ambientePago` en `CuentaPagoResponse`** | Código | Bajo |
| 9 | **E4: requisitos de publicación** (depende del punto 5) | Código | Bajo |
| 10 | ~~E5: pocas unidades en la vitrina~~ | ✅ Bloque 2 | — |
| 11 | **Documentación de despliegue:** completar `.env.example` con las 7 variables de pagos y crear `.env.render.example` (o corregir la referencia) | Documentación | Bajo |
| 12 | **Solo si la entrega incluye cobros reales:** salida para `CREACION_INCIERTA` vencido. Consultar la orden en Mercado Pago por la clave externa; si no existe, cancelar y liberar el stock; si sigue incierta, marcarla para revisión manual con alerta | Código + test | Medio–alto |

### Puede esperar

| Tarea | Motivo |
| --- | --- |
| E6/E7 filtros y paginación de pedidos y productos | Necesarios cuando crezca el volumen |
| E8 admin con dueño legible | E9 (suspender o reactivar) ya está hecho en el Bloque 2 |
| E10 perfil, E11 editar tienda, E12 cierre (RN-M1-18) | Marcados como deseables en el análisis del frontend |
| E13 historial de cuenta de pago, E14 envíos | Requieren migración; trabajo futuro |
| Login con Google (RN-M1-05) y verificación de correo | Backlog (hallazgos §7) |
| Revocación del token de Mercado Pago (HU-M3-10) | La credencial ya se borra localmente |
| Despublicar automáticamente (RN-M2-13, punto 2) | Mitigado por los filtros de vitrina, carrito y checkout |
| Menores: `idPropietario` en `CrearTiendaRequest`, rechazar campos desconocidos, DTOs en `PresentacionImagenController`, 404 en lugar de 400, borrar `ConfirmacionPagoService`, `DELETE` de catálogo | Limpieza y contrato. Conviene hacer pronto los DTOs de `PresentacionImagenController`, porque exponen campos internos |
| Que el CLIENTE pueda ver las plantillas (HU-M3-03) | La plantilla se elige después de crear la tienda |
| Actualizar `modules-rules.md` (número de tablas) | Documental (RN-M1-13 ya se actualizó en el Bloque 2) |
