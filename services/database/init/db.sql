-- ============================================================
-- PLATAFORMA E-COMMERCE MULTI-TIENDA
-- PostgreSQL - DDL FINAL
-- ============================================================
-- Los UUID principales serán generados por Spring Boot (UUID v7).
-- PostgreSQL únicamente los almacena.
--
-- Soft delete:
--   usuario_plataforma -> estado
--   tienda             -> estado
--   producto           -> visible
--   categoria          -> activo
--   coleccion          -> activo
--
-- IMPORTANTE:
-- fecha_actualizacion tiene DEFAULT para la creación.
-- Las actualizaciones posteriores serán gestionadas por Spring Boot.
-- ============================================================


-- ============================================================
-- 1. PLATFORM_USER
-- ADMIN y VENDEDOR comparten esta tabla.
-- El rol determina sus permisos en Spring Security.
-- ============================================================

CREATE TABLE platform_user (
    user_id UUID PRIMARY KEY,

    first_names VARCHAR(100) NOT NULL,
    last_names VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password TEXT NOT NULL,

    role VARCHAR(20) NOT NULL
        CHECK (role IN ('ADMIN', 'VENDEDOR')),

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
        CHECK (status IN ('ACTIVO', 'INACTIVO', 'SUSPENDIDO')),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Evita correos repetidos aunque cambien mayúsculas/minúsculas.
CREATE UNIQUE INDEX uq_user_email
    ON platform_user (LOWER(email));


-- ============================================================
-- 2. STORE
-- Cada tienda pertenece a un usuario vendedor.
-- ============================================================

CREATE TABLE store (
    store_id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
        CHECK (status IN ('ACTIVA', 'INACTIVA', 'SUSPENDIDA')),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_user
        FOREIGN KEY (user_id)
        REFERENCES platform_user(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_store_slug
        CHECK (
            slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'
        )
);


-- ============================================================
-- 3. TEMPLATE
-- Catálogo interno de diseños.
-- ============================================================

CREATE TABLE template (
    template_id SMALLSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    preview_path TEXT NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE
);


-- ============================================================
-- 4. STORE_CONFIGURATION
-- Relación 1:1 con tienda.
-- ============================================================

CREATE TABLE store_configuration (
    configuration_id UUID PRIMARY KEY,

    store_id UUID NOT NULL UNIQUE,
    template_id SMALLINT NOT NULL,

    logo_path TEXT,

    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),

    facebook_url TEXT,
    instagram_url TEXT,
    whatsapp VARCHAR(30),

    CONSTRAINT fk_store_configuration_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_store_configuration_template
        FOREIGN KEY (template_id)
        REFERENCES template(template_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_store_configuration_primary_color
        CHECK (
            primary_color IS NULL
            OR primary_color ~ '^#[0-9A-Fa-f]{6}$'
        ),

    CONSTRAINT ck_store_configuration_secondary_color
        CHECK (
            secondary_color IS NULL
            OR secondary_color ~ '^#[0-9A-Fa-f]{6}$'
        )
);


-- ============================================================
-- 5. STORE_BANNER
-- Una tienda puede tener varios banners.
-- ============================================================

CREATE TABLE store_banner (
    banner_id BIGSERIAL PRIMARY KEY,

    store_id UUID NOT NULL,

    title VARCHAR(150),
    subtitle VARCHAR(250),

    image_path TEXT NOT NULL,

    button_text VARCHAR(80),
    link TEXT,

    display_order INTEGER NOT NULL DEFAULT 1
        CHECK (display_order > 0),

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_store_banner_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_store_banner_order
        UNIQUE (store_id, display_order)
);


-- ============================================================
-- 6. CUSTOMER
-- Clientes separados por tienda.
-- contrasena NULL = cliente invitado.
-- ============================================================

CREATE TABLE customer (
    customer_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,

    first_names VARCHAR(100) NOT NULL,
    last_names VARCHAR(100) NOT NULL,

    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30),

    password TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    -- Necesario para poder garantizar posteriormente
    -- que cliente y pedido sean de la misma tienda.
    CONSTRAINT uq_customer_store_id
        UNIQUE (customer_id, store_id)
);

-- El correo puede repetirse entre tiendas,
-- pero no dentro de una misma tienda.
CREATE UNIQUE INDEX uq_customer_store_email
    ON customer (store_id, LOWER(email));


-- ============================================================
-- 7. CUSTOMER_ADDRESS
-- ============================================================

CREATE TABLE customer_address (
    address_id BIGSERIAL PRIMARY KEY,

    customer_id UUID NOT NULL,

    address VARCHAR(250) NOT NULL,
    district VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    reference TEXT,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_customer_address_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON DELETE CASCADE
);

-- Un cliente puede tener varias direcciones,
-- pero máximo una marcada como principal.
CREATE UNIQUE INDEX uq_customer_primary_address
    ON customer_address(customer_id)
    WHERE is_primary = TRUE;


-- ============================================================
-- 8. CATEGORY
-- Cada categoría pertenece a una tienda.
-- ============================================================

CREATE TABLE category (
    category_id BIGSERIAL PRIMARY KEY,

    store_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,
    description TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_category_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_category_store_name
        UNIQUE (store_id, name),

    -- Permite la FK compuesta utilizada por producto.
    CONSTRAINT uq_category_store_id
        UNIQUE (category_id, store_id)
);


-- ============================================================
-- 9. PRODUCT
-- Mantiene id_tienda directamente para facilitar aislamiento
-- multi-tenant y consultas por tienda.
-- ============================================================

CREATE TABLE product (
    product_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,
    category_id BIGINT NOT NULL,

    name VARCHAR(150) NOT NULL,
    description TEXT,

    sku VARCHAR(100),

    price NUMERIC(12,2) NOT NULL
        CHECK (price >= 0),

    promotional_price NUMERIC(12,2),

    visible BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    -- FK COMPUESTA.
    -- Impide que un producto de la tienda A
    -- utilice una categoría de la tienda B.
    CONSTRAINT fk_product_category_store
        FOREIGN KEY (category_id, store_id)
        REFERENCES category(category_id, store_id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_product_promotional_price
        CHECK (
            promotional_price IS NULL
            OR (
                promotional_price >= 0
                AND promotional_price < price
            )
        )
);

-- SKU único dentro de cada tienda.
CREATE UNIQUE INDEX uq_product_store_sku
    ON product(store_id, sku)
    WHERE sku IS NOT NULL;


-- ============================================================
-- 10. INVENTORY
-- Relación 1:1 con producto.
-- id_producto funciona como PK y FK.
-- ============================================================

CREATE TABLE inventory (
    product_id UUID PRIMARY KEY,

    current_stock INTEGER NOT NULL DEFAULT 0
        CHECK (current_stock >= 0),

    minimum_stock INTEGER NOT NULL DEFAULT 0
        CHECK (minimum_stock >= 0),

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 11. PRODUCT_IMAGE
-- ============================================================

CREATE TABLE product_image (
    image_id BIGSERIAL PRIMARY KEY,

    product_id UUID NOT NULL,

    image_path TEXT NOT NULL,

    display_order INTEGER NOT NULL DEFAULT 1
        CHECK (display_order > 0),

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_product_image_order
        UNIQUE (product_id, display_order)
);

-- Máximo una imagen principal por producto.
CREATE UNIQUE INDEX uq_product_primary_image
    ON product_image(product_id)
    WHERE is_primary = TRUE;


-- ============================================================
-- 12. COLLECTION
-- Agrupaciones comerciales:
-- Gaming, Ofertas, Destacados, etc.
-- ============================================================

CREATE TABLE collection (
    collection_id BIGSERIAL PRIMARY KEY,

    store_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,
    description TEXT,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_collection_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_collection_store_name
        UNIQUE (store_id, name)
);


-- ============================================================
-- 13. COLLECTION_PRODUCT
-- Tabla puente para relación N:M.
-- ============================================================

CREATE TABLE collection_product (
    collection_id BIGINT NOT NULL,
    product_id UUID NOT NULL,

    CONSTRAINT pk_collection_product
        PRIMARY KEY (collection_id, product_id),

    CONSTRAINT fk_collection_product_collection
        FOREIGN KEY (collection_id)
        REFERENCES collection(collection_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_product_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 14. CART
-- Carrito persistente asociado al cliente.
-- Estados finales acordados:
-- ACTIVO / CONVERTIDO
-- ============================================================

CREATE TABLE cart (
    cart_id UUID PRIMARY KEY,

    customer_id UUID NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
        CHECK (
            status IN ('ACTIVO', 'CONVERTIDO')
        ),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cart_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON DELETE CASCADE
);

-- Un cliente solo puede tener un carrito ACTIVO al mismo tiempo.
CREATE UNIQUE INDEX uq_active_cart_customer
    ON cart(customer_id)
    WHERE status = 'ACTIVO';


-- ============================================================
-- 15. CART_ITEM
-- Productos incluidos dentro del carrito.
-- ============================================================

CREATE TABLE cart_item (
    cart_item_id BIGSERIAL PRIMARY KEY,

    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,

    quantity INTEGER NOT NULL
        CHECK (quantity > 0),

    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id)
        REFERENCES cart(cart_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cart_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE,

    -- El mismo producto ocupa una sola línea del carrito.
    CONSTRAINT uq_cart_item_product
        UNIQUE (cart_id, product_id)
);


-- ============================================================
-- 16. STORE_PAYMENT_ACCOUNT
-- Configuración individual de Mercado Pago por tienda.
-- Relación 1:1.
-- ============================================================

CREATE TABLE store_payment_account (
    payment_account_id UUID PRIMARY KEY,

    store_id UUID NOT NULL UNIQUE,

    provider VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO'
        CHECK (provider = 'MERCADO_PAGO'),

    external_account_id VARCHAR(150),

    encrypted_access_token TEXT,
    encrypted_refresh_token TEXT,

    connection_status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (
            connection_status IN (
                'PENDIENTE',
                'CONECTADA',
                'DESCONECTADA',
                'ERROR'
            )
        ),

    connection_date TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_payment_account_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE CASCADE
);


-- ============================================================
-- 17. ORDER
-- Información histórica y transaccional.
-- No se elimina normalmente: se cambia su estado.
-- ============================================================

CREATE TABLE customer_order (
    order_id UUID PRIMARY KEY,

    store_id UUID NOT NULL,
    customer_id UUID NOT NULL,

    order_number VARCHAR(50) NOT NULL,

    order_status VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (
            order_status IN (
                'PENDIENTE',
                'PAGADO',
                'PROCESANDO',
                'COMPLETADO',
                'CANCELADO'
            )
        ),

    subtotal NUMERIC(12,2) NOT NULL
        CHECK (subtotal >= 0),

    discount NUMERIC(12,2) NOT NULL DEFAULT 0
        CHECK (discount >= 0),

    total NUMERIC(12,2) NOT NULL
        CHECK (total >= 0),

    -- Snapshot de información de entrega.
    recipient_name VARCHAR(150) NOT NULL,
    recipient_phone VARCHAR(30),

    shipping_address VARCHAR(250) NOT NULL,
    shipping_district VARCHAR(100),
    shipping_city VARCHAR(100) NOT NULL,
    shipping_department VARCHAR(100) NOT NULL,

    postal_code VARCHAR(20),
    reference TEXT,

    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    -- FK compuesta adicional:
    -- garantiza que el cliente realmente pertenezca
    -- a la misma tienda del pedido.
    CONSTRAINT fk_order_customer_store
        FOREIGN KEY (customer_id, store_id)
        REFERENCES customer(customer_id, store_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_order_store_number
        UNIQUE (store_id, order_number),

    CONSTRAINT ck_order_discount
        CHECK (discount <= subtotal)
);


-- ============================================================
-- 18. ORDER_ITEM
-- Conserva snapshot histórico del producto.
-- ============================================================

CREATE TABLE order_item (
    order_item_id BIGSERIAL PRIMARY KEY,

    order_id UUID NOT NULL,

    -- Puede quedar NULL si el producto se elimina físicamente.
    product_id UUID,

    -- Snapshot histórico.
    product_name VARCHAR(150) NOT NULL,
    product_sku VARCHAR(100),

    unit_price NUMERIC(12,2) NOT NULL
        CHECK (unit_price >= 0),

    quantity INTEGER NOT NULL
        CHECK (quantity > 0),

    subtotal NUMERIC(12,2) NOT NULL
        CHECK (subtotal >= 0),

    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE SET NULL
);


-- ============================================================
-- 19. PAYMENT
-- Puede haber varios intentos de pago por pedido.
-- ============================================================

CREATE TABLE payment (
    payment_id UUID PRIMARY KEY,

    order_id UUID NOT NULL,

    provider VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO'
        CHECK (provider = 'MERCADO_PAGO'),

    external_payment_id VARCHAR(150),

    amount NUMERIC(12,2) NOT NULL
        CHECK (amount > 0),

    status VARCHAR(30) NOT NULL
        CHECK (
            status IN (
                'PENDIENTE',
                'APROBADO',
                'RECHAZADO',
                'CANCELADO',
                'REEMBOLSADO'
            )
        ),

    payment_method VARCHAR(50),

    payment_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES customer_order(order_id)
        ON DELETE RESTRICT
);

-- El identificador entregado por Mercado Pago no debe repetirse.
CREATE UNIQUE INDEX uq_payment_external_id
    ON payment(external_payment_id)
    WHERE external_payment_id IS NOT NULL;


-- ============================================================
-- ÍNDICES DE RENDIMIENTO
-- ============================================================

-- Tiendas de un vendedor.
CREATE INDEX idx_store_user
    ON store(user_id);

-- Banners de una tienda.
CREATE INDEX idx_store_banner_store
    ON store_banner(store_id);

-- Clientes de una tienda.
CREATE INDEX idx_customer_store
    ON customer(store_id);

-- Direcciones de un cliente.
CREATE INDEX idx_customer_address_customer
    ON customer_address(customer_id);

-- Categorías de una tienda.
CREATE INDEX idx_category_store
    ON category(store_id);

-- Productos de una tienda.
CREATE INDEX idx_product_store
    ON product(store_id);

-- Productos por categoría.
CREATE INDEX idx_product_category
    ON product(category_id);

-- Imágenes de producto.
CREATE INDEX idx_product_image_product
    ON product_image(product_id);

-- Colecciones por tienda.
CREATE INDEX idx_collection_store
    ON collection(store_id);

-- Carritos de cliente.
CREATE INDEX idx_cart_customer
    ON cart(customer_id);

-- Detalles de carrito.
CREATE INDEX idx_cart_item_cart
    ON cart_item(cart_id);

CREATE INDEX idx_cart_item_product
    ON cart_item(product_id);

-- Pedidos por tienda.
CREATE INDEX idx_order_store
    ON customer_order(store_id);

-- Pedidos por cliente.
CREATE INDEX idx_order_customer
    ON customer_order(customer_id);

-- Líneas de un pedido.
CREATE INDEX idx_order_item_order
    ON order_item(order_id);

-- Pagos asociados a un pedido.
CREATE INDEX idx_payment_order
    ON payment(order_id);