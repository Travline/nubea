# Guía técnica de la Base de Datos E-commerce Multi‑Tienda

PostgreSQL + Spring Boot | Modelo DDL final

Documento de contexto para el equipo: explica la función de cada entidad, sus campos, restricciones, índices, relaciones, reglas multi‑tenant y decisiones de integridad.

**Versión basada en el modelo de 19 entidades**

---

# 1. Visión general del modelo

La base de datos representa una plataforma tipo Shopify simplificada. Un usuario vendedor puede administrar una o más tiendas; cada tienda mantiene su propio catálogo, clientes, carritos, pedidos y conexión de pagos. Un usuario con rol ADMIN tiene alcance global desde la capa de autorización de Spring Boot, mientras que un VENDEDOR solo puede operar sobre sus tiendas.

## Entidades del modelo

| Entidad | Responsabilidad |
| --- | --- |
| `platform_user` | Administradores y vendedores de la plataforma. |
| `store` | Tenant o tienda propiedad de un vendedor. |
| `template` | Catálogo de diseños visuales disponibles. |
| `store_configuration` | Configuración visual elegida por cada tienda. |
| `store_banner` | Banners o slides configurables de una tienda. |
| `customer` | Compradores asociados a una tienda. |
| `customer_address` | Direcciones guardadas por un cliente. |
| `category` | Clasificación principal del catálogo por tienda. |
| `product` | Productos publicados por una tienda. |
| `inventory` | Stock actual y mínimo de cada producto. |
| `product_image` | Galería de imágenes de cada producto. |
| `collection` | Agrupaciones comerciales como Ofertas o Destacados. |
| `collection_product` | Tabla puente entre colecciones y productos. |
| `cart` | Carrito persistente del cliente. |
| `cart_item` | Productos y cantidades contenidos en un carrito. |
| `store_payment_account` | Conexión de la tienda con Mercado Pago. |
| `customer_order` | Cabecera transaccional de una compra. |
| `order_item` | Productos comprados y su snapshot histórico. |
| `payment` | Intentos y transacciones de pago de un pedido. |

## Principios de diseño

* **Multi‑tenant:** la tienda (`store`) es el límite lógico principal de datos comerciales.
* **Integridad en PostgreSQL:** PK, FK, UNIQUE y CHECK protegen la base incluso si existe un error en Spring Boot.
* **Soft delete:** `platform_user`, `store`, `category`, `collection` y `product` se desactivan en lugar de borrarse físicamente cuando exista historial.
* **Datos históricos:** `customer_order` y `order_item` conservan snapshots para no alterar ventas antiguas cuando cambian productos o direcciones.
* **UUID v7:** las entidades principales usarán UUID generados por Spring Boot; PostgreSQL solo los almacena en columnas UUID.
* **Índices:** se crean en columnas usadas frecuentemente para filtros, joins o búsquedas por tenant.

---

# 2. Conceptos: constraints, índices y claves

| Elemento | Qué hace | Ejemplo del modelo |
| --- | --- | --- |
| PRIMARY KEY (PK) | Identifica de forma única cada fila. No acepta NULL ni duplicados. | `product_id` en `product`. |
| FOREIGN KEY (FK) | Obliga a que un valor exista previamente en otra tabla. | `customer_id` de `customer_order` debe existir en `customer`. |
| FK compuesta | Valida conjuntamente dos o más columnas. | `(category_id, store_id)` garantiza categoría y producto de la misma tienda. |
| NOT NULL | Impide guardar un campo obligatorio vacío. | `product.name`. |
| UNIQUE | Evita valores o combinaciones repetidas. | `(store_id, name)` en `category`. |
| CHECK | Valida una condición lógica sobre el dato. | `current_stock >= 0`. |
| DEFAULT | Asigna un valor cuando no se envía uno. | `status = 'ACTIVO'`. |
| INDEX | Estructura auxiliar para acelerar consultas. | `product(store_id)`. |
| UNIQUE INDEX | Índice que además impide duplicados; puede ser parcial. | Un solo carrito `ACTIVO` por cliente. |

## Índices: idea práctica

Sin un índice, PostgreSQL puede necesitar revisar muchas filas para encontrar coincidencias. Con un índice B‑tree puede localizar rápidamente el rango de claves buscado. Los índices mejoran las lecturas, pero ocupan espacio y deben actualizarse en INSERT/UPDATE/DELETE; por eso no se crean indiscriminadamente.

* PK y UNIQUE ya generan índices automáticamente.
* Las FK no siempre crean índices automáticamente en PostgreSQL, por lo que conviene indexar aquellas usadas en joins frecuentes.
* Un índice compuesto como `(customer_id, status)` resulta útil si la consulta filtra por ambas columnas.
* No conviene indexar columnas poco selectivas o que casi nunca participan en búsquedas, salvo que exista un caso real.

## Reglas ON DELETE

| Acción | Comportamiento | Uso en este modelo |
| --- | --- | --- |
| CASCADE | Al borrar el padre, borra automáticamente los hijos dependientes. | Banners, imágenes, inventario, detalle de carrito (`cart_item`). |
| RESTRICT | Impide borrar el padre si existen registros relacionados. | Pedidos (`customer_order`), clientes (`customer`) y productos (`product`) con relaciones importantes. |
| SET NULL | Conserva el hijo y elimina únicamente la referencia al padre. | `order_item.product_id` para conservar historial. |

---

# 3. platform_user

**Propósito.** Almacena las cuentas internas de la plataforma. Aquí conviven ADMIN y VENDEDOR. El rol determina el alcance de autorización en Spring Security.

**Contenido esperado.** Una fila por persona que puede acceder al panel administrativo o de vendedor.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `user_id` | UUID | Sí | PK. UUID v7 generado por Spring Boot. |
| `first_names` | VARCHAR(100) | Sí | Nombres del usuario. |
| `last_names` | VARCHAR(100) | Sí | Apellidos del usuario. |
| `email` | VARCHAR(150) | Sí | Identificador de inicio de sesión. |
| `password` | TEXT | Sí | Hash de contraseña; nunca contraseña en texto plano. |
| `role` | VARCHAR(20) | Sí | ADMIN o VENDEDOR. |
| `status` | VARCHAR(20) | Sí | ACTIVO, INACTIVO o SUSPENDIDO. |
| `created_at` | TIMESTAMP | Sí | Fecha/hora de alta. |
| `updated_at` | TIMESTAMP | Sí | Última modificación gestionada por Spring. |

## Constraints y reglas de integridad

* PK sobre `user_id`.
* CHECK de `role`: ADMIN o VENDEDOR.
* CHECK de `status`: ACTIVO, INACTIVO o SUSPENDIDO.
* Índice UNIQUE sobre `LOWER(email)`, para impedir duplicados sin distinguir mayúsculas/minúsculas.

## Índices relevantes

* `uq_user_email` sobre `LOWER(email)`.

## Relaciones

* 1 `platform_user` puede poseer N tiendas (`store`).
* Un ADMIN no necesita una FK especial hacia todas las tiendas: su alcance global se resuelve en autorización.

## Notas de implementación

* Aplicar soft delete cambiando `status`; evitar DELETE físico si el usuario tiene tiendas o historial.
* No usar CascadeType.ALL desde usuario hacia tienda.

---

# 4. store

**Propósito.** Representa cada tienda o tenant. Es el principal límite lógico de aislamiento de datos.

**Contenido esperado.** Una fila por tienda creada por un vendedor.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `store_id` | UUID | Sí | PK. UUID v7 generado en Spring Boot. |
| `user_id` | UUID | Sí | FK al vendedor propietario. |
| `name` | VARCHAR(150) | Sí | Nombre comercial. |
| `slug` | VARCHAR(150) | Sí | Identificador público amigable para URL, por ejemplo `miguel-tech`. |
| `description` | TEXT | No | Descripción general de la tienda. |
| `status` | VARCHAR(20) | Sí | ACTIVA, INACTIVA o SUSPENDIDA. |
| `created_at` | TIMESTAMP | Sí | Fecha/hora de creación. |
| `updated_at` | TIMESTAMP | Sí | Última modificación. |

## Constraints y reglas de integridad

* FK `fk_store_user`: `user_id` -> `platform_user(user_id)` con ON DELETE RESTRICT.
* UNIQUE en `slug`.
* CHECK `ck_store_slug`: regex del slug (`^[a-z0-9]+(-[a-z0-9]+)*$`).
* CHECK de `status`: ACTIVA, INACTIVA o SUSPENDIDA.

## Índices relevantes

* `idx_store_user` sobre `user_id`.

## Relaciones

* N `store` pertenecen a 1 `platform_user`.
* 1 `store` tiene 1 `store_configuration`.
* 1 `store` tiene N `store_banner`, `customer`, `category`, `product`, `collection` y `customer_order`.
* 1 `store` tiene como máximo 1 `store_payment_account`.

## Notas de implementación

* La aplicación debe verificar que `user_id` corresponda a un usuario con rol VENDEDOR.
* Usar `status` para desactivar la tienda en lugar de eliminarla cuando ya exista actividad.

---

# 5. template

**Propósito.** Catálogo de diseños visuales disponibles para las tiendas.

**Contenido esperado.** Filas relativamente estáticas definidas por el equipo: plantilla clásica, moderna, tecnológica, etc.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `template_id` | SMALLSERIAL | Sí | PK numérica; suficiente para un catálogo pequeño. |
| `code` | VARCHAR(50) | Sí | Código interno estable de la plantilla. |
| `name` | VARCHAR(100) | Sí | Nombre mostrado al vendedor. |
| `description` | TEXT | No | Descripción breve del diseño. |
| `preview_path` | TEXT | Sí | Ruta o key de la miniatura/preview. |
| `active` | BOOLEAN | Sí | Permite retirar una plantilla sin borrarla. |

## Constraints y reglas de integridad

* PK en `template_id`.
* UNIQUE en `code`.
* `active` DEFAULT TRUE.

## Índices relevantes

No requiere un índice adicional más allá de los creados automáticamente por su PK/UNIQUE.

## Relaciones

* 1 `template` puede ser seleccionada por N `store_configuration`.

## Notas de implementación

* Preferible desactivar una plantilla antigua antes que borrarla si ya está asociada.

---

# 6. store_configuration

**Propósito.** Guarda la personalización visual básica de una tienda.

**Contenido esperado.** Una sola fila por tienda: plantilla elegida, logo, colores y enlaces sociales.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `configuration_id` | UUID | Sí | PK UUID v7. |
| `store_id` | UUID | Sí | FK UNIQUE: garantiza relación 1:1 con tienda. |
| `template_id` | SMALLINT | Sí | FK a plantilla. |
| `logo_path` | TEXT | No | Ruta/key del logo en almacenamiento externo. |
| `primary_color` | VARCHAR(7) | No | Color HEX #RRGGBB. |
| `secondary_color` | VARCHAR(7) | No | Color HEX #RRGGBB. |
| `facebook_url` | TEXT | No | Enlace a Facebook. |
| `instagram_url` | TEXT | No | Enlace a Instagram. |
| `whatsapp` | VARCHAR(30) | No | Número o identificador de contacto. |

## Constraints y reglas de integridad

* `store_id` UNIQUE para garantizar una sola configuración por tienda.
* FK `fk_store_configuration_store`: `store_id` -> `store(store_id)` con ON DELETE CASCADE.
* FK `fk_store_configuration_template`: `template_id` -> `template(template_id)` con ON DELETE RESTRICT.
* CHECK `ck_store_configuration_primary_color` y `ck_store_configuration_secondary_color` de formato HEX para colores (`^#[0-9A-Fa-f]{6}$`).

## Índices relevantes

* El UNIQUE de `store_id` crea el índice necesario para la búsqueda 1:1.

## Relaciones

* 1 `store` -> 1 `store_configuration`.
* N `store_configuration` -> 1 `template`.

---

# 7. store_banner

**Propósito.** Permite que cada tienda configure banners o slides promocionales.

**Contenido esperado.** Una o varias filas por tienda, ordenadas para su presentación en la página pública.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `banner_id` | BIGSERIAL | Sí | PK. |
| `store_id` | UUID | Sí | FK a tienda. |
| `title` | VARCHAR(150) | No | Texto principal. |
| `subtitle` | VARCHAR(250) | No | Texto secundario. |
| `image_path` | TEXT | Sí | Ruta/key de la imagen. |
| `button_text` | VARCHAR(80) | No | Texto CTA, ej. Ver ofertas. |
| `link` | TEXT | No | Destino del banner. |
| `display_order` | INTEGER | Sí | Posición del banner. |
| `active` | BOOLEAN | Sí | Determina si se muestra. |

## Constraints y reglas de integridad

* FK `fk_store_banner_store`: `store_id` -> `store(store_id)` con ON DELETE CASCADE.
* UNIQUE `uq_store_banner_order`: `(store_id, display_order)`.
* CHECK `display_order > 0`.

## Índices relevantes

* `idx_store_banner_store` sobre `store_id`.

## Relaciones

* N `store_banner` pertenecen a 1 `store`.

---

# 8. customer

**Propósito.** Representa a los compradores dentro de una tienda concreta.

**Contenido esperado.** Una fila por comprador y tienda. Un mismo correo puede existir en tiendas distintas.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `customer_id` | UUID | Sí | PK UUID v7. |
| `store_id` | UUID | Sí | FK al tenant al que pertenece. |
| `first_names` | VARCHAR(100) | Sí | Nombres del comprador. |
| `last_names` | VARCHAR(100) | Sí | Apellidos. |
| `email` | VARCHAR(150) | Sí | Correo del cliente. |
| `phone` | VARCHAR(30) | No | Teléfono de contacto. |
| `password` | TEXT | No | NULL si compró como invitado; hash si tiene cuenta. |
| `created_at` | TIMESTAMP | Sí | Fecha de registro o primera compra. |

## Constraints y reglas de integridad

* FK `fk_customer_store`: `store_id` -> `store(store_id)` con ON DELETE RESTRICT.
* UNIQUE `uq_customer_store_id`: `(customer_id, store_id)` como soporte de la FK compuesta desde `customer_order`.
* UNIQUE INDEX `uq_customer_store_email`: `(store_id, LOWER(email))` para no duplicar un cliente dentro de la misma tienda.

## Índices relevantes

* `idx_customer_store` sobre `store_id`.

## Relaciones

* N `customer` pertenecen a 1 `store`.
* 1 `customer` tiene N `customer_address`.
* 1 `customer` puede tener múltiples carritos históricos (`cart`) y pedidos (`customer_order`).

## Notas de implementación

* El cliente invitado puede convertirse después en registrado asignando un hash a `password`.
* El backend debe filtrar siempre por tienda para evitar cruces entre tenants.

---

# 9. customer_address

**Propósito.** Almacena direcciones reutilizables de un cliente.

**Contenido esperado.** Varias direcciones por cliente; como máximo una marcada como principal.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `address_id` | BIGSERIAL | Sí | PK. |
| `customer_id` | UUID | Sí | FK al cliente. |
| `address` | VARCHAR(250) | Sí | Dirección física. |
| `district` | VARCHAR(100) | No | Distrito. |
| `city` | VARCHAR(100) | Sí | Ciudad. |
| `department` | VARCHAR(100) | Sí | Departamento/región. |
| `postal_code` | VARCHAR(20) | No | Código postal. |
| `reference` | TEXT | No | Indicaciones adicionales. |
| `is_primary` | BOOLEAN | Sí | Marca la dirección predeterminada. |

## Constraints y reglas de integridad

* FK `fk_customer_address_customer`: `customer_id` -> `customer(customer_id)` con ON DELETE CASCADE.
* UNIQUE INDEX parcial `uq_customer_primary_address` para permitir como máximo una dirección principal por cliente (`WHERE is_primary = TRUE`).

## Índices relevantes

* `idx_customer_address_customer` sobre `customer_id`.
* `uq_customer_primary_address` sobre `customer_id WHERE is_primary = TRUE`.

## Relaciones

* N `customer_address` pertenecen a 1 `customer`.

## Notas de implementación

* El pedido no debe depender de esta fila para su historial: copia la dirección utilizada en campos snapshot.

---

# 10. category

**Propósito.** Organiza los productos de una tienda en una clasificación principal.

**Contenido esperado.** Categorías como Laptops, Monitores, Instrumentos o Accesorios.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `category_id` | BIGSERIAL | Sí | PK. |
| `store_id` | UUID | Sí | FK a tienda. |
| `name` | VARCHAR(100) | Sí | Nombre de la categoría. |
| `description` | TEXT | No | Descripción opcional. |
| `active` | BOOLEAN | Sí | Soft delete lógico. |

## Constraints y reglas de integridad

* FK `fk_category_store`: `store_id` -> `store(store_id)` con ON DELETE RESTRICT.
* UNIQUE `uq_category_store_name`: `(store_id, name)`: no repite nombres dentro de la misma tienda.
* UNIQUE `uq_category_store_id`: `(category_id, store_id)`: habilita la FK compuesta con `product`.

## Índices relevantes

* `idx_category_store` sobre `store_id`.

## Relaciones

* N `category` pertenecen a 1 `store`.
* 1 `category` puede tener N `product`.
* La FK compuesta impide que un producto de una tienda use una categoría de otra.

---

# 11. product

**Propósito.** Entidad principal del catálogo. Mantiene `store_id` directamente para aislamiento multi‑tenant y consultas frecuentes.

**Contenido esperado.** Una fila por producto simple publicado o administrado por un vendedor.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `product_id` | UUID | Sí | PK UUID v7. |
| `store_id` | UUID | Sí | FK directa a tienda. |
| `category_id` | BIGINT | Sí | Parte de la FK compuesta con `store_id`. |
| `name` | VARCHAR(150) | Sí | Nombre comercial. |
| `description` | TEXT | No | Descripción extensa. |
| `sku` | VARCHAR(100) | No | Código interno del vendedor. |
| `price` | NUMERIC(12,2) | Sí | Precio regular. |
| `promotional_price` | NUMERIC(12,2) | No | Precio promocional menor al regular. |
| `visible` | BOOLEAN | Sí | Soft delete/publicación: TRUE visible, FALSE oculto. |
| `created_at` | TIMESTAMP | Sí | Alta del producto. |
| `updated_at` | TIMESTAMP | Sí | Última modificación. |

## Constraints y reglas de integridad

* FK `fk_product_store`: `store_id` -> `store(store_id)` con RESTRICT.
* FK compuesta `fk_product_category_store`: `(category_id, store_id)` -> `category(category_id, store_id)` con ON DELETE RESTRICT.
* CHECK `price >= 0`.
* CHECK `ck_product_promotional_price`: `promotional_price IS NULL OR (promotional_price >= 0 AND promotional_price < price)`.
* UNIQUE INDEX parcial `uq_product_store_sku`: `(store_id, sku) WHERE sku IS NOT NULL`.

## Índices relevantes

* `idx_product_store` sobre `store_id`.
* `idx_product_category` sobre `category_id`.
* `uq_product_store_sku` para SKU único por tienda.

## Relaciones

* N `product` pertenecen a 1 `store`.
* N `product` pertenecen a 1 `category`.
* 1 `product` tiene 1 `inventory` y N `product_image`.
* `product` participa en N:M con `collection`.
* `product` aparece en `cart_item` y `order_item`.

## Notas de implementación

* Evitar borrado físico normal: usar `visible = FALSE` si existe historial.
* `store_id` directo mejora filtrado por tenant; la FK compuesta evita inconsistencias con categoría.

---

# 12. inventory

**Propósito.** Separa la gestión de stock de la información comercial del producto.

**Contenido esperado.** Exactamente una fila de inventario por producto.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `product_id` | UUID | Sí | PK y FK simultáneamente. Garantiza relación 1:1. |
| `current_stock` | INTEGER | Sí | Unidades disponibles. |
| `minimum_stock` | INTEGER | Sí | Umbral para alertar stock bajo. |
| `updated_at` | TIMESTAMP | Sí | Último cambio de stock. |

## Constraints y reglas de integridad

* PK en `product_id` impide más de un inventario por producto.
* FK `fk_inventory_product`: `product_id` -> `product(product_id)` con ON DELETE CASCADE.
* CHECK `current_stock >= 0`.
* CHECK `minimum_stock >= 0`.

## Índices relevantes

No requiere un índice adicional más allá de los creados automáticamente por su PK/UNIQUE.

## Relaciones

* 1 `product` -> 1 `inventory`.

## Notas de implementación

* Si en el futuro existe inventario por almacén, el modelo cambiaría a una entidad con PK propia o PK compuesta por producto y almacén.

---

# 13. product_image

**Propósito.** Guarda las imágenes asociadas a un producto sin almacenar archivos binarios dentro de PostgreSQL.

**Contenido esperado.** Varias filas por producto con ruta/key hacia almacenamiento externo.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `image_id` | BIGSERIAL | Sí | PK. |
| `product_id` | UUID | Sí | FK al producto. |
| `image_path` | TEXT | Sí | Ruta/key en Cloudinary/S3/Supabase Storage/etc. |
| `display_order` | INTEGER | Sí | Orden de la galería. |
| `is_primary` | BOOLEAN | Sí | Determina la imagen principal. |

## Constraints y reglas de integridad

* FK `fk_product_image_product`: `product_id` -> `product(product_id)` con ON DELETE CASCADE.
* UNIQUE `uq_product_image_order`: `(product_id, display_order)`.
* CHECK `display_order > 0`.
* UNIQUE INDEX parcial `uq_product_primary_image`: sobre `product_id WHERE is_primary = TRUE`.

## Índices relevantes

* `idx_product_image_product` sobre `product_id`.
* `uq_product_primary_image` WHERE `is_primary = TRUE`.

## Relaciones

* N `product_image` pertenecen a 1 `product`.

---

# 14. collection

**Propósito.** Agrupa productos con una finalidad comercial o de marketing, distinta a la categoría.

**Contenido esperado.** Colecciones como Ofertas, Gaming, Destacados, Nuevos o Regreso a clases.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `collection_id` | BIGSERIAL | Sí | PK. |
| `store_id` | UUID | Sí | FK a tienda. |
| `name` | VARCHAR(100) | Sí | Nombre de la colección. |
| `description` | TEXT | No | Descripción opcional. |
| `active` | BOOLEAN | Sí | Soft delete lógico. |

## Constraints y reglas de integridad

* FK `fk_collection_store`: `store_id` -> `store(store_id)` con ON DELETE RESTRICT.
* UNIQUE `uq_collection_store_name`: `(store_id, name)`.

## Índices relevantes

* `idx_collection_store` sobre `store_id`.

## Relaciones

* N `collection` pertenecen a 1 `store`.
* Colección y producto tienen relación N:M mediante `collection_product`.

## Notas de implementación

* Categoría = clasificación del catálogo. Colección = agrupación comercial; un producto puede estar en varias colecciones.

---

# 15. collection_product

**Propósito.** Tabla puente que materializa la relación muchos-a-muchos entre colecciones y productos.

**Contenido esperado.** Una fila por asociación producto-colección.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `collection_id` | BIGINT | Sí | PK compuesta + FK a `collection`. |
| `product_id` | UUID | Sí | PK compuesta + FK a `product`. |

## Constraints y reglas de integridad

* PRIMARY KEY `pk_collection_product`: `(collection_id, product_id)` impide asociaciones duplicadas.
* FK `fk_collection_product_collection`: `collection_id` -> `collection(collection_id)` con ON DELETE CASCADE.
* FK `fk_collection_product_product`: `product_id` -> `product(product_id)` con ON DELETE CASCADE.

## Índices relevantes

* La PK compuesta ya crea el índice principal sobre la combinación.

## Relaciones

* N productos pueden estar en N colecciones.
* Eliminar una asociación no elimina ni el producto ni la colección.

---

# 16. cart

**Propósito.** Permite persistir el carrito del cliente entre sesiones y convertirlo posteriormente en pedido.

**Contenido esperado.** Un cliente puede tener carritos históricos CONVERTIDOS y como máximo un carrito ACTIVO.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `cart_id` | UUID | Sí | PK UUID v7. |
| `customer_id` | UUID | Sí | FK al cliente. |
| `status` | VARCHAR(20) | Sí | ACTIVO o CONVERTIDO. |
| `created_at` | TIMESTAMP | Sí | Creación del carrito. |
| `updated_at` | TIMESTAMP | Sí | Última modificación general. |
| `last_activity` | TIMESTAMP | Sí | Última interacción; permite detectar inactividad sin estado ABANDONADO. |

## Constraints y reglas de integridad

* FK `fk_cart_customer`: `customer_id` -> `customer(customer_id)` con ON DELETE CASCADE.
* CHECK `status IN ('ACTIVO', 'CONVERTIDO')`.
* UNIQUE INDEX parcial `uq_active_cart_customer`: en `customer_id WHERE status = 'ACTIVO'` para impedir dos carritos activos simultáneamente.

## Índices relevantes

* `idx_cart_customer` sobre `customer_id`.
* `uq_active_cart_customer` WHERE `status = 'ACTIVO'`.

## Relaciones

* N carritos históricos pertenecen a 1 `customer`.
* 1 `cart` contiene N `cart_item`.

## Notas de implementación

* Si el cliente cierra sesión y vuelve después, se busca su carrito ACTIVO.
* La noción de carrito abandonado puede calcularse con `last_activity` sin almacenarla como estado.

---

# 17. cart_item

**Propósito.** Almacena los productos y cantidades agregados al carrito.

**Contenido esperado.** Una fila por producto dentro de cada carrito.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `cart_item_id` | BIGSERIAL | Sí | PK. |
| `cart_id` | UUID | Sí | FK a `cart`. |
| `product_id` | UUID | Sí | FK al producto. |
| `quantity` | INTEGER | Sí | Cantidad seleccionada. |
| `added_at` | TIMESTAMP | Sí | Momento en que se añadió la línea. |

## Constraints y reglas de integridad

* FK `fk_cart_item_cart`: `cart_id` -> `cart(cart_id)` con ON DELETE CASCADE.
* FK `fk_cart_item_product`: `product_id` -> `product(product_id)` con ON DELETE CASCADE.
* CHECK `quantity > 0`.
* UNIQUE `uq_cart_item_product`: `(cart_id, product_id)` para que un producto use una sola línea.

## Índices relevantes

* `idx_cart_item_cart` sobre `cart_id`.
* `idx_cart_item_product` sobre `product_id`.

## Relaciones

* N `cart_item` pertenecen a 1 `cart`.
* Cada `cart_item` referencia 1 `product`.

## Notas de implementación

* Al volver a agregar el mismo producto, se incrementa `quantity` en lugar de crear una segunda fila.

---

# 18. store_payment_account

**Propósito.** Representa la conexión 1:1 de cada tienda con Mercado Pago.

**Contenido esperado.** Una fila por tienda conectada o en proceso de conexión.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `payment_account_id` | UUID | Sí | PK UUID v7. |
| `store_id` | UUID | Sí | FK UNIQUE a `store`. |
| `provider` | VARCHAR(30) | Sí | Actualmente MERCADO_PAGO. |
| `external_account_id` | VARCHAR(150) | No | Identificador externo del proveedor. |
| `encrypted_access_token` | TEXT | No | Token cifrado; nunca exponer en frontend. |
| `encrypted_refresh_token` | TEXT | No | Token de renovación cifrado. |
| `connection_status` | VARCHAR(20) | Sí | PENDIENTE, CONECTADA, DESCONECTADA o ERROR. |
| `connection_date` | TIMESTAMP | No | Fecha de conexión exitosa. |
| `updated_at` | TIMESTAMP | Sí | Última modificación. |

## Constraints y reglas de integridad

* `store_id` UNIQUE asegura 1:1.
* FK `fk_store_payment_account_store`: `store_id` -> `store(store_id)` con ON DELETE CASCADE.
* CHECK `provider = 'MERCADO_PAGO'`.
* CHECK de `connection_status`: PENDIENTE, CONECTADA, DESCONECTADA o ERROR.

## Índices relevantes

* El UNIQUE de `store_id` crea el índice de la relación 1:1.

## Relaciones

* 1 `store` -> 0..1 `store_payment_account`.

## Notas de implementación

* Los tokens deben cifrarse en backend y nunca registrarse en logs.
* La estructura admite que una tienda esté todavía sin conectar.

---

# 19. customer_order

**Propósito.** Cabecera transaccional de una compra. Mantiene datos históricos de entrega y tenant.

**Contenido esperado.** Una fila por compra creada durante checkout.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `order_id` | UUID | Sí | PK UUID v7. |
| `store_id` | UUID | Sí | Tenant directo del pedido. |
| `customer_id` | UUID | Sí | Cliente que realizó la compra. |
| `order_number` | VARCHAR(50) | Sí | Correlativo legible dentro de la tienda. |
| `order_status` | VARCHAR(30) | Sí | PENDIENTE, PAGADO, PROCESANDO, COMPLETADO o CANCELADO. |
| `subtotal` | NUMERIC(12,2) | Sí | Suma antes de descuentos. |
| `discount` | NUMERIC(12,2) | Sí | Descuento aplicado. |
| `total` | NUMERIC(12,2) | Sí | Importe final. |
| `recipient_name` | VARCHAR(150) | Sí | Snapshot del receptor. |
| `recipient_phone` | VARCHAR(30) | No | Teléfono del receptor. |
| `shipping_address` | VARCHAR(250) | Sí | Snapshot de dirección. |
| `shipping_district` | VARCHAR(100) | No | Distrito de entrega. |
| `shipping_city` | VARCHAR(100) | Sí | Ciudad. |
| `shipping_department` | VARCHAR(100) | Sí | Departamento/región. |
| `postal_code` | VARCHAR(20) | No | Código postal. |
| `reference` | TEXT | No | Referencia de entrega. |
| `order_date` | TIMESTAMP | Sí | Fecha/hora de creación. |
| `updated_at` | TIMESTAMP | Sí | Último cambio de estado/datos. |

## Constraints y reglas de integridad

* FK `fk_order_store`: `store_id` -> `store(store_id)` con RESTRICT.
* FK compuesta `fk_order_customer_store`: `(customer_id, store_id)` -> `customer(customer_id, store_id)`, evita cruzar tenants.
* UNIQUE `uq_order_store_number`: `(store_id, order_number)`.
* CHECK de `order_status`: PENDIENTE, PAGADO, PROCESANDO, COMPLETADO o CANCELADO.
* CHECK `subtotal >= 0`, `discount >= 0`, `total >= 0` y `ck_order_discount`: `discount <= subtotal`.

## Índices relevantes

* `idx_order_store` sobre `store_id`.
* `idx_order_customer` sobre `customer_id`.

## Relaciones

* N `customer_order` pertenecen a 1 `store`.
* N `customer_order` pertenecen a 1 `customer`.
* 1 `customer_order` tiene N `order_item` y N `payment`.

## Notas de implementación

* Pedido y pago son históricos: normalmente se cambia su estado y no se eliminan.
* Los datos de dirección se copian para que futuras modificaciones del cliente no alteren una venta pasada.

---

# 20. order_item

**Propósito.** Representa cada línea vendida dentro de un pedido y conserva un snapshot del producto.

**Contenido esperado.** Una fila por producto comprado en un pedido.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `order_item_id` | BIGSERIAL | Sí | PK. |
| `order_id` | UUID | Sí | FK a `customer_order`. |
| `product_id` | UUID | No | FK opcional; puede quedar NULL si el producto se elimina físicamente. |
| `product_name` | VARCHAR(150) | Sí | Snapshot histórico del nombre. |
| `product_sku` | VARCHAR(100) | No | Snapshot histórico del SKU. |
| `unit_price` | NUMERIC(12,2) | Sí | Precio utilizado en la venta. |
| `quantity` | INTEGER | Sí | Unidades compradas. |
| `subtotal` | NUMERIC(12,2) | Sí | Importe de la línea. |

## Constraints y reglas de integridad

* FK `fk_order_item_order`: `order_id` -> `customer_order(order_id)` con ON DELETE CASCADE.
* FK `fk_order_item_product`: `product_id` -> `product(product_id)` con ON DELETE SET NULL.
* CHECK `unit_price >= 0`.
* CHECK `quantity > 0`.
* CHECK `subtotal >= 0`.

## Índices relevantes

* `idx_order_item_order` sobre `order_id`.

## Relaciones

* N `order_item` pertenecen a 1 `customer_order`.
* Cada `order_item` puede mantener referencia al producto actual, pero su información histórica no depende de él.

## Notas de implementación

* La duplicación de nombre/SKU/precio es intencional y se justifica por requerimiento histórico, no por error de normalización.

---

# 21. payment

**Propósito.** Registra intentos y transacciones de pago vinculadas a pedidos.

**Contenido esperado.** Una o varias filas por pedido, ya que puede existir un intento rechazado y luego otro aprobado.

## Campos

| Campo | Tipo sugerido | Obligatorio | Función / contenido esperado |
| --- | --- | --- | --- |
| `payment_id` | UUID | Sí | PK UUID v7. |
| `order_id` | UUID | Sí | FK al pedido. |
| `provider` | VARCHAR(30) | Sí | MERCADO_PAGO. |
| `external_payment_id` | VARCHAR(150) | No | Identificador asignado por Mercado Pago. |
| `amount` | NUMERIC(12,2) | Sí | Importe del intento/transacción. |
| `status` | VARCHAR(30) | Sí | PENDIENTE, APROBADO, RECHAZADO, CANCELADO o REEMBOLSADO. |
| `payment_method` | VARCHAR(50) | No | Método reportado por el proveedor. |
| `payment_date` | TIMESTAMP | No | Fecha de confirmación. |
| `created_at` | TIMESTAMP | Sí | Fecha de creación del intento. |

## Constraints y reglas de integridad

* FK `fk_payment_order`: `order_id` -> `customer_order(order_id)` con ON DELETE RESTRICT.
* CHECK `provider = 'MERCADO_PAGO'`.
* CHECK `amount > 0`.
* CHECK de `status`: PENDIENTE, APROBADO, RECHAZADO, CANCELADO o REEMBOLSADO.
* UNIQUE INDEX parcial `uq_payment_external_id` sobre `external_payment_id` cuando no es NULL.

## Índices relevantes

* `idx_payment_order` sobre `order_id`.
* `uq_payment_external_id` WHERE `external_payment_id IS NOT NULL`.

## Relaciones

* N `payment`/intentos pertenecen a 1 `customer_order`.

## Notas de implementación

* No actualizar el pedido como PAGADO solo por el redirect del frontend; esperar confirmación segura desde backend/webhook.
* No borrar pagos históricos; manejar reembolsos/cancelaciones mediante estado (`status`).

---

# 22. Resumen de índices del modelo

| Índice | Tabla | Columnas | Motivo |
| --- | --- | --- | --- |
| `uq_user_email` | `platform_user` | `LOWER(email)` | Unicidad y login case-insensitive. |
| `idx_store_user` | `store` | `user_id` | Tiendas de un vendedor. |
| `idx_store_banner_store` | `store_banner` | `store_id` | Banners de una tienda. |
| `uq_customer_store_email` | `customer` | `store_id, LOWER(email)` | Cliente único por tienda. |
| `idx_customer_store` | `customer` | `store_id` | Clientes del tenant. |
| `uq_customer_primary_address` | `customer_address` | `customer_id WHERE is_primary` | Una principal por cliente. |
| `idx_customer_address_customer` | `customer_address` | `customer_id` | Direcciones de un cliente. |
| `idx_category_store` | `category` | `store_id` | Categorías del tenant. |
| `idx_product_store` | `product` | `store_id` | Catálogo por tienda. |
| `idx_product_category` | `product` | `category_id` | Productos por categoría. |
| `uq_product_store_sku` | `product` | `store_id, sku` | SKU único dentro de tienda. |
| `idx_product_image_product` | `product_image` | `product_id` | Galería del producto. |
| `uq_product_primary_image` | `product_image` | `product_id WHERE is_primary` | Una imagen principal. |
| `idx_collection_store` | `collection` | `store_id` | Colecciones por tenant. |
| `idx_cart_customer` | `cart` | `customer_id` | Historial de carritos. |
| `uq_active_cart_customer` | `cart` | `customer_id WHERE status='ACTIVO'` | Un solo carrito activo. |
| `idx_cart_item_cart` | `cart_item` | `cart_id` | Líneas del carrito. |
| `idx_cart_item_product` | `cart_item` | `product_id` | Carritos que contienen producto. |
| `idx_order_store` | `customer_order` | `store_id` | Pedidos del tenant. |
| `idx_order_customer` | `customer_order` | `customer_id` | Pedidos del cliente. |
| `idx_order_item_order` | `order_item` | `order_id` | Líneas del pedido. |
| `idx_payment_order` | `payment` | `order_id` | Pagos por pedido. |
| `uq_payment_external_id` | `payment` | `external_payment_id` | Evitar duplicar transacción externa. |

## Qué no necesita índice adicional

Las PRIMARY KEY y restricciones UNIQUE ya generan índices automáticamente. Por ejemplo, no se crea manualmente otro índice sobre `product_id`, `order_id` o `store_id` cuando ya son PK.

---

# 23. Soft delete y protección de información histórica

No todas las entidades deben eliminarse físicamente. Cuando una entidad participa en operaciones comerciales, es preferible desactivarla para conservar trazabilidad.

| Entidad | Mecanismo | Comportamiento recomendado |
| --- | --- | --- |
| `platform_user` | `status` | Cambiar a INACTIVO/SUSPENDIDO. |
| `store` | `status` | Cambiar a INACTIVA/SUSPENDIDA. |
| `product` | `visible` | FALSE para ocultar del catálogo. |
| `category` | `active` | FALSE para dejar de usarla. |
| `collection` | `active` | FALSE para retirarla. |
| `customer_order` | `order_status` | CANCELADO/COMPLETADO; no eliminar. |
| `payment` | `status` | REEMBOLSADO/CANCELADO/etc.; no eliminar. |

## CASCADE recomendado en JPA

El CASCADE de PostgreSQL y el cascade de JPA/Hibernate son mecanismos distintos. En JPA se debe ser conservador y evitar CascadeType.ALL en relaciones de negocio importantes.

* `store` -> `store_configuration`: razonable usar cascade/orphanRemoval.
* `store` -> `store_banner`: razonable.
* `customer` -> `customer_address`: razonable.
* `product` -> `inventory` e `product_image`: razonable.
* `cart` -> `cart_item`: razonable.
* `customer_order` -> `order_item`: razonable si nunca se expone borrado físico normal del pedido.
* Evitar CascadeType.ALL en Usuario -> Tiendas, Tienda -> Productos/Clientes/Pedidos, Cliente -> Pedidos, Pedido -> Pagos.

---

# 24. Flujo funcional de datos

## Flujo del vendedor

* El vendedor inicia sesión como `platform_user` con rol VENDEDOR.
* Spring identifica sus tiendas mediante `store.user_id`.
* El vendedor configura apariencia mediante `store_configuration` y `store_banner`.
* Crea categorías (`category`), productos (`product`), inventario (`inventory`), imágenes (`product_image`) y colecciones (`collection`).
* Conecta Mercado Pago mediante `store_payment_account`.
* Consulta sus pedidos (`customer_order`) y pagos (`payment`) filtrando por `store_id`.

## Flujo del cliente

* El cliente (`customer`) pertenece a una tienda concreta.
* Puede existir como invitado (`password` NULL) o como cuenta registrada.
* Agrega productos a su carrito ACTIVO (`cart`) y `cart_item`.
* Al finalizar compra se crea `customer_order` + `order_item` y el carrito pasa a CONVERTIDO.
* Mercado Pago genera uno o más registros de pago (`payment`) hasta obtener el resultado final.
* El pedido conserva snapshots de entrega y productos para mantener el historial.

## Flujo del administrador

* ADMIN comparte la tabla `platform_user` con VENDEDOR.
* No necesita relación directa con todas las tiendas.
* Spring Security autoriza al ADMIN a listar y gestionar todas las tiendas y vendedores.
* Las restricciones de tenant aplicadas a vendedores no limitan el alcance global del ADMIN.

---

# 25. Correspondencia con la rúbrica

| Entidad solicitada | Entidad del modelo | Estado |
| --- | --- | --- |
| users | `platform_user` + `customer` | Cubierto |
| products | `product` | Cubierto |
| categories | `category` | Cubierto |
| inventory | `inventory` | Cubierto |
| cart | `cart` | Cubierto |
| cart_items | `cart_item` | Cubierto |
| orders | `customer_order` | Cubierto |
| order_items | `order_item` | Cubierto |
| payments | `payment` | Cubierto |
| stores/tenants | `store` | Cubierto - multi-tenant |

## Entidades adicionales que diferencian el proyecto

* `template` y `store_configuration`: personalización visual tipo Shopify.
* `store_banner`: contenido promocional configurable.
* `customer_address`: direcciones reutilizables.
* `product_image`: galería desacoplada.
* `collection` y `collection_product`: agrupaciones comerciales.
* `store_payment_account`: Mercado Pago independiente por tienda.

## Conclusión

El modelo final está pensado para cumplir la rúbrica y, al mismo tiempo, mantener una arquitectura coherente para una plataforma e-commerce multi‑tienda. PostgreSQL asegura integridad estructural y Spring Boot deberá complementarla con autorización, validaciones de negocio, generación UUID v7, cifrado de tokens, actualización de timestamps y control del ciclo de vida de las entidades.