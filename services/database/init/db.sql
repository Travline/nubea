-- ============================================================
-- PROYECTO: Plataforma e-commerce tipo Shopify
-- MOTOR: PostgreSQL
-- VERSION: MVP inicial
-- ============================================================


-- ============================================================
-- 0. EXTENSION PARA GENERAR UUID
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;



-- ============================================================
-- 1. USUARIO_PLATAFORMA
-- Administradores y vendedores.
-- ============================================================

CREATE TABLE platform_users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_role
        CHECK (role IN ('ADMIN', 'SELLER')),

    CONSTRAINT chk_user_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);


-- Evita duplicar correos ignorando mayúsculas/minúsculas.
CREATE UNIQUE INDEX ux_user_email
ON platform_user (LOWER(email));



-- ============================================================
-- 2. TIENDA
-- Cada vendedor puede administrar su tienda.
-- ============================================================

CREATE TABLE store (
    store_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_owner
        FOREIGN KEY (owner_id)
        REFERENCES platform_user(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_store_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),

    CONSTRAINT chk_store_slug
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$')
);



-- ============================================================
-- 3. PLANTILLA
-- Diseños disponibles para crear las tiendas.
-- ============================================================

CREATE TABLE template (
    template_id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255),
    preview_path TEXT NOT NULL,
    status BOOLEAN NOT NULL DEFAULT TRUE
);



-- ============================================================
-- 4. CONFIGURACION_TIENDA
-- Configuración visual general.
-- Relación TIENDA 1:1 CONFIGURACION_TIENDA
-- ============================================================

CREATE TABLE store_configuration (
    configuration_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE,
    template_id SMALLINT NOT NULL,
    logo_path TEXT,
    primary_color VARCHAR(7),
    secondary_color VARCHAR(7),
    instagram_url TEXT,
    facebook_url TEXT,
    whatsapp VARCHAR(20),

    CONSTRAINT fk_store_configuration_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_store_configuration_template
        FOREIGN KEY (template_id)
        REFERENCES template(template_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_primary_color
        CHECK (
            primary_color IS NULL
            OR primary_color ~ '^#[0-9A-Fa-f]{6}$'
        ),

    CONSTRAINT chk_secondary_color
        CHECK (
            secondary_color IS NULL
            OR secondary_color ~ '^#[0-9A-Fa-f]{6}$'
        )
);



-- ============================================================
-- 5. BANNER_TIENDA
-- Carrusel / banners de la página principal.
-- ============================================================

CREATE TABLE store_banner (
    banner_id BIGSERIAL PRIMARY KEY,
    store_id UUID NOT NULL,
    title VARCHAR(150),
    subtitle VARCHAR(255),
    image_path TEXT NOT NULL,
    button_text VARCHAR(50),
    link TEXT,
    display_order SMALLINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_store_banner_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_banner_order
        CHECK (display_order >= 1),

    CONSTRAINT uq_banner_order
        UNIQUE (store_id, display_order)
);



-- ============================================================
-- 6. CLIENTE
-- Cada tienda maneja sus propios clientes.
-- La contraseña puede ser NULL si compra como invitado.
-- ============================================================

CREATE TABLE customer (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    password VARCHAR(255),
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT
);


-- El correo es único DENTRO DE UNA TIENDA,
-- pero puede existir nuevamente en otra tienda.
CREATE UNIQUE INDEX ux_customer_store_email
ON customer (store_id, LOWER(email));



-- ============================================================
-- 7. DIRECCION_CLIENTE
-- Un cliente puede almacenar múltiples direcciones.
-- ============================================================

CREATE TABLE customer_address (
    address_id BIGSERIAL PRIMARY KEY,
    customer_id UUID NOT NULL,
    alias VARCHAR(50) NOT NULL,
    address VARCHAR(255) NOT NULL,
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(15),
    reference VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_customer_address_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON DELETE CASCADE
);


-- Un cliente puede tener muchas direcciones,
-- pero solamente UNA principal.
CREATE UNIQUE INDEX ux_primary_customer_address
ON customer_address(customer_id)
WHERE is_primary = TRUE;



-- ============================================================
-- 8. CATEGORIA
-- Categorías independientes para cada tienda.
-- ============================================================

CREATE TABLE category (
    category_id BIGSERIAL PRIMARY KEY,
    store_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_category_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_category_store_name
        UNIQUE (store_id, name)
);



-- ============================================================
-- 9. PRODUCTO
-- Por ahora productos simples, SIN variantes.
-- ============================================================

CREATE TABLE product (
    product_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    sku VARCHAR(100),
    price NUMERIC(12,2) NOT NULL,
    promotional_price NUMERIC(12,2),
    stock INTEGER NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id)
        REFERENCES category(category_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_product_price
        CHECK (price >= 0),

    CONSTRAINT chk_product_promotional_price
        CHECK (
            promotional_price IS NULL
            OR (
                promotional_price >= 0
                AND promotional_price < price
            )
        ),

    CONSTRAINT chk_product_stock
        CHECK (stock >= 0)
);


-- El SKU puede repetirse entre tiendas,
-- pero no dentro de la misma tienda.
CREATE UNIQUE INDEX ux_product_store_sku
ON product(store_id, sku)
WHERE sku IS NOT NULL;



-- ============================================================
-- 10. IMAGEN_PRODUCTO
-- Una imagen principal + galería.
-- ============================================================

CREATE TABLE product_image (
    image_id BIGSERIAL PRIMARY KEY,
    product_id UUID NOT NULL,
    image_path TEXT NOT NULL,
    display_order SMALLINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_product_image_order
        CHECK (display_order >= 1),

    CONSTRAINT uq_product_image_order
        UNIQUE (product_id, display_order)
);


-- Solamente una imagen principal por producto.
CREATE UNIQUE INDEX ux_primary_product_image
ON product_image(product_id)
WHERE is_primary = TRUE;



-- ============================================================
-- 11. COLECCION
-- Ejemplos:
-- Ofertas, Gamer, Novedades, Destacados.
-- ============================================================

CREATE TABLE collection (
    collection_id BIGSERIAL PRIMARY KEY,
    store_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_collection_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_collection_store_name
        UNIQUE (store_id, name)
);



-- ============================================================
-- 12. COLECCION_PRODUCTO
-- Resuelve relación muchos a muchos.
-- ============================================================

CREATE TABLE collection_product (
    collection_id BIGINT NOT NULL,
    product_id UUID NOT NULL,
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
-- 13. CUENTA_PAGO_TIENDA
-- Cada tienda conecta UNA cuenta Mercado Pago.
-- Relación 1:1.
-- ============================================================

CREATE TABLE store_payment_account (
    payment_account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO',
    external_account_id VARCHAR(150),
    encrypted_access_token TEXT,
    encrypted_refresh_token TEXT,
    token_expiration_date TIMESTAMP,
    connection_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    connected_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_store_payment_account_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payment_account_provider
        CHECK (provider = 'MERCADO_PAGO'),

    CONSTRAINT chk_payment_account_status
        CHECK (
            connection_status IN (
                'PENDING',
                'CONNECTED',
                'DISCONNECTED',
                'ERROR'
            )
        )
);



-- ============================================================
-- 14. PEDIDO
-- Guarda también una copia histórica de la dirección.
-- ============================================================

CREATE TABLE order (
    order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    order_number VARCHAR(40) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    discount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total NUMERIC(12,2) NOT NULL,
    order_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    recipient_name VARCHAR(200) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_address VARCHAR(255) NOT NULL,
    shipping_district VARCHAR(100) NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_state VARCHAR(100) NOT NULL,
    shipping_postal_code VARCHAR(15),
    shipping_reference VARCHAR(255),
    ordered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_order_store
        FOREIGN KEY (store_id)
        REFERENCES store(store_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer(customer_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_order_number
        UNIQUE (store_id, order_number),

    CONSTRAINT chk_order_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT chk_order_discount
        CHECK (discount >= 0),

    CONSTRAINT chk_order_discount_subtotal
        CHECK (discount <= subtotal),

    CONSTRAINT chk_order_total
        CHECK (total >= 0),

    CONSTRAINT chk_order_status
        CHECK (
            order_status IN (
                'CREATED',
                'CONFIRMED',
                'PREPARING',
                'COMPLETED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_payment_status
        CHECK (
            payment_status IN (
                'PENDING',
                'PAID',
                'REJECTED',
                'REFUNDED'
            )
        )
);



-- ============================================================
-- 15. DETALLE_PEDIDO
-- Fotografía histórica del producto comprado.
-- ============================================================

CREATE TABLE order_item (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID,
    product_name VARCHAR(200) NOT NULL,
    product_sku VARCHAR(100),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,

    CONSTRAINT fk_order_item_order
        FOREIGN KEY (order_id)
        REFERENCES "order"(order_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_item_product
        FOREIGN KEY (product_id)
        REFERENCES product(product_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_item_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT chk_order_item_subtotal
        CHECK (subtotal >= 0)
);



-- ============================================================
-- 16. PAGO
-- Registro de cada intento/transacción Mercado Pago.
-- ============================================================

CREATE TABLE payment (
    payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'MERCADO_PAGO',
    external_payment_id VARCHAR(150),
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES "order"(order_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_payment_provider
        CHECK (provider = 'MERCADO_PAGO'),

    CONSTRAINT chk_payment_amount
        CHECK (amount > 0),

    CONSTRAINT chk_payment_status
        CHECK (
            status IN (
                'PENDING',
                'APPROVED',
                'REJECTED',
                'CANCELLED',
                'REFUNDED'
            )
        )
);


-- Mercado Pago no debería aparecer dos veces con
-- el mismo identificador de transacción.
CREATE UNIQUE INDEX ux_external_payment
ON payment(external_payment_id)
WHERE external_payment_id IS NOT NULL;



-- ============================================================
-- INDICES ADICIONALES
-- Ayudan en consultas muy utilizadas.
-- ============================================================

CREATE INDEX ix_store_owner
ON store(owner_id);

CREATE INDEX ix_customer_store
ON customer(store_id);

CREATE INDEX ix_category_store
ON category(store_id);

CREATE INDEX ix_product_store
ON product(store_id);

CREATE INDEX ix_product_category
ON product(category_id);

CREATE INDEX ix_order_store
ON "order"(store_id);

CREATE INDEX ix_order_customer
ON "order"(customer_id);

CREATE INDEX ix_order_item_order
ON order_item(order_id);

CREATE INDEX ix_payment_order
ON payment(order_id);
